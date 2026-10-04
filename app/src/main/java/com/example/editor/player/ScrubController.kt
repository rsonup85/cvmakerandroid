package com.example.editor.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScrubState {
    data object Idle : ScrubState
    data class Scrubbing(val positionMs: Long, val velocity: Float = 0f) : ScrubState
    data class Settling(val positionMs: Long) : ScrubState
}

/**
 * Coalesces timeline scrub pointer events to prevent ExoPlayer seek storms.
 * Ensures the playhead tracks the user's finger smoothly while throttling expensive
 * hardware decodes to ~25-30fps during drag, and issuing an exact seek upon release.
 */
class ScrubController(
    private val scope: CoroutineScope,
    private val throttleIntervalMs: Long = 35L,
    private val onPerformHardwareSeek: (positionMs: Long, isExact: Boolean) -> Unit
) {
    private val _scrubState = MutableStateFlow<ScrubState>(ScrubState.Idle)
    val scrubState: StateFlow<ScrubState> = _scrubState.asStateFlow()

    private var pendingSeekTarget: Long? = null
    private var throttleJob: Job? = null
    private var lastHardwareSeekTime: Long = 0L

    fun onScrubStart(initialPositionMs: Long) {
        throttleJob?.cancel()
        pendingSeekTarget = initialPositionMs
        _scrubState.value = ScrubState.Scrubbing(initialPositionMs)
        lastHardwareSeekTime = System.currentTimeMillis()
        onPerformHardwareSeek(initialPositionMs, false)
    }

    fun onScrubMove(targetPositionMs: Long, velocity: Float = 0f) {
        pendingSeekTarget = targetPositionMs
        _scrubState.value = ScrubState.Scrubbing(targetPositionMs, velocity)

        val now = System.currentTimeMillis()
        if (now - lastHardwareSeekTime >= throttleIntervalMs) {
            lastHardwareSeekTime = now
            onPerformHardwareSeek(targetPositionMs, false)
        } else if (throttleJob == null || !throttleJob!!.isActive) {
            throttleJob = scope.launch(Dispatchers.Main) {
                delay(throttleIntervalMs)
                pendingSeekTarget?.let { target ->
                    lastHardwareSeekTime = System.currentTimeMillis()
                    onPerformHardwareSeek(target, false)
                }
            }
        }
    }

    fun onScrubEnd(finalPositionMs: Long) {
        throttleJob?.cancel()
        pendingSeekTarget = null
        _scrubState.value = ScrubState.Settling(finalPositionMs)

        // Perform final exact seek
        onPerformHardwareSeek(finalPositionMs, true)

        scope.launch(Dispatchers.Main) {
            delay(50)
            _scrubState.value = ScrubState.Idle
        }
    }

    fun reset() {
        throttleJob?.cancel()
        pendingSeekTarget = null
        _scrubState.value = ScrubState.Idle
    }
}
