// FIXED: Ticker restart after clip end + proper release + listener cleanup
package com.example.editor.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.domain.model.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimelinePlayer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
        setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playheadMs = MutableStateFlow(0L)
    val playheadMs: StateFlow<Long> = _playheadMs.asStateFlow()

    private val _isScrubbing = MutableStateFlow(false)
    val isScrubbing: StateFlow<Boolean> = _isScrubbing.asStateFlow()

    private val _isInGap = MutableStateFlow(false)
    val isInGap: StateFlow<Boolean> = _isInGap.asStateFlow()

    private var currentProject: Project? = null
    private var currentLoadedAssetId: String? = null
    private var tickerJob: Job? = null

    val frameCache = TimelineFrameCache()

    val scrubController = ScrubController(scope, throttleIntervalMs = 35L) { targetMs, isExact ->
        hardwareSeekTo(targetMs, isExact)
    }
    val scrubState: StateFlow<ScrubState> get() = scrubController.scrubState

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    startPlaybackTicker()
                } else {
                    stopPlaybackTicker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    handleClipEnded()
                }
            }
        })
    }

    fun setProject(project: Project) {
        this.currentProject = project
        val maxDuration = project.totalDurationMs
        if (_playheadMs.value > maxDuration) {
            seekTo(maxDuration)
        } else {
            syncPlayerToCurrentPlayhead(forceReload = false)
        }
    }

    fun play() {
        val project = currentProject ?: return
        if (project.items.isEmpty()) return
        if (_playheadMs.value >= project.totalDurationMs && project.totalDurationMs > 0) {
            seekTo(0L)
        }
        _isPlaying.value = true
        syncPlayerToCurrentPlayhead(forceReload = false)
        if (!_isInGap.value) {
            exoPlayer.play()
        }
        startPlaybackTicker()
    }

    fun pause() {
        _isPlaying.value = false
        exoPlayer.pause()
        stopPlaybackTicker()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun startScrubbing(initialMs: Long? = null) {
        _isScrubbing.value = true
        if (_isPlaying.value) {
            pause()
        }
        val pos = initialMs ?: _playheadMs.value
        scrubController.onScrubStart(pos)
    }

    fun scrubTo(timelineMs: Long, velocity: Float = 0f) {
        val project = currentProject ?: return
        val clampedTime = timelineMs.coerceIn(0L, project.totalDurationMs.coerceAtLeast(0L))
        _playheadMs.value = clampedTime
        scrubController.onScrubMove(clampedTime, velocity)
    }

    fun stopScrubbing(finalMs: Long? = null) {
        _isScrubbing.value = false
        val finalPos = finalMs ?: _playheadMs.value
        scrubController.onScrubEnd(finalPos)
    }

    fun seekTo(timelineMs: Long) {
        val project = currentProject ?: return
        val clampedTime = timelineMs.coerceIn(0L, project.totalDurationMs.coerceAtLeast(0L))
        _playheadMs.value = clampedTime
        if (_isScrubbing.value) {
            scrubController.onScrubMove(clampedTime)
        } else {
            hardwareSeekTo(clampedTime, isExact = true)
        }
    }

    fun jumpToStart() {
        seekTo(0L)
    }

    fun jumpToEnd() {
        val project = currentProject ?: return
        seekTo(project.totalDurationMs)
    }

    private fun hardwareSeekTo(timelineMs: Long, isExact: Boolean) {
        syncPlayerToCurrentPlayhead(forceReload = false, isExact = isExact)
    }

    private fun syncPlayerToCurrentPlayhead(forceReload: Boolean, isExact: Boolean = false) {
        val project = currentProject ?: return
        val currentPlayhead = _playheadMs.value

        val activeClip = project.videoClips.find { clip ->
            currentPlayhead >= clip.timelineStartMs &&
            currentPlayhead < (clip.timelineStartMs + clip.durationMs)
        }

        if (activeClip == null) {
            _isInGap.value = true
            exoPlayer.volume = 0f
            exoPlayer.pause()
            return
        }

        _isInGap.value = false

        val asset = project.assets.find { it.id == activeClip.assetId } ?: return
        val relativeOffsetMs = (currentPlayhead - activeClip.timelineStartMs).coerceAtLeast(0L)

        val sourceMediaSeekMs = if (activeClip.isReversed) {
            (activeClip.sourceStartMs + activeClip.sourceDurationMs - (relativeOffsetMs * activeClip.speed).toLong())
                .coerceIn(activeClip.sourceStartMs, activeClip.sourceStartMs + activeClip.sourceDurationMs)
        } else {
            (activeClip.sourceStartMs + (relativeOffsetMs * activeClip.speed).toLong()).coerceAtLeast(0L)
        }

        val needsNewMedia = currentLoadedAssetId != asset.id || forceReload
        if (needsNewMedia) {
            currentLoadedAssetId = asset.id
            val mediaItem = MediaItem.fromUri(Uri.parse(asset.uriString))
            exoPlayer.setMediaItem(mediaItem, sourceMediaSeekMs)
            exoPlayer.prepare()
        } else {
            exoPlayer.seekTo(sourceMediaSeekMs)
        }

        exoPlayer.playbackParameters = PlaybackParameters(activeClip.speed)

        val track = project.tracks.find { it.id == activeClip.trackId }
        val isMuted = activeClip.isMuted || (track?.isMuted == true)
        exoPlayer.volume = if (isMuted) 0f else activeClip.volume.coerceIn(0f, 2f)
    }

    private fun startPlaybackTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch(Dispatchers.Main) {
            while (isActive && _isPlaying.value) {
                val project = currentProject
                if (project != null && project.items.isNotEmpty()) {
                    val currentPlayhead = _playheadMs.value
                    val activeClip = project.videoClips.find { clip ->
                        currentPlayhead >= clip.timelineStartMs &&
                        currentPlayhead < (clip.timelineStartMs + clip.durationMs)
                    }

                    if (activeClip != null) {
                        _isInGap.value = false
                        val playerPos = exoPlayer.currentPosition
                        val relativeSourceMs = (playerPos - activeClip.sourceStartMs).coerceAtLeast(0L)
                        val timelineDerivedMs = activeClip.timelineStartMs +
                            (relativeSourceMs / activeClip.speed).toLong()

                        if (timelineDerivedMs >= (activeClip.timelineStartMs + activeClip.durationMs)) {
                            val nextClip = project.videoClips.find {
                                it.timelineStartMs >= (activeClip.timelineStartMs + activeClip.durationMs)
                            }
                            if (nextClip != null) {
                                _playheadMs.value = nextClip.timelineStartMs
                                syncPlayerToCurrentPlayhead(forceReload = true)
                                if (_isPlaying.value) exoPlayer.play()
                            } else {
                                val nextAnyItem = project.items.find {
                                    it.timelineStartMs > (activeClip.timelineStartMs + activeClip.durationMs)
                                }
                                if (nextAnyItem != null) {
                                    _playheadMs.value = nextAnyItem.timelineStartMs
                                    syncPlayerToCurrentPlayhead(forceReload = true)
                                } else {
                                    _playheadMs.value = project.totalDurationMs
                                    pause()
                                    seekTo(0L)
                                    break
                                }
                            }
                        } else {
                            _playheadMs.value = timelineDerivedMs.coerceIn(0L, project.totalDurationMs)
                        }
                    } else {
                        _isInGap.value = true
                        val newPlayhead = currentPlayhead + 25L
                        if (newPlayhead >= project.totalDurationMs) {
                            _playheadMs.value = project.totalDurationMs
                            pause()
                            seekTo(0L)
                            break
                        } else {
                            _playheadMs.value = newPlayhead
                            val nextClip = project.videoClips.find {
                                newPlayhead >= it.timelineStartMs &&
                                newPlayhead < (it.timelineStartMs + it.durationMs)
                            }
                            if (nextClip != null) {
                                syncPlayerToCurrentPlayhead(forceReload = true)
                                if (_isPlaying.value) exoPlayer.play()
                            }
                        }
                    }
                }
                delay(20)
            }
        }
    }

    private fun stopPlaybackTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    // FIXED: Restart ticker after handling clip end
    private fun handleClipEnded() {
        val project = currentProject ?: return
        val nextClip = project.videoClips.find { it.timelineStartMs > _playheadMs.value }
        if (nextClip != null) {
            seekTo(nextClip.timelineStartMs)
            exoPlayer.play()
            if (_isPlaying.value) {
                startPlaybackTicker()
            }
        } else {
            pause()
            seekTo(0L)
        }
    }

    // FIXED: Proper cleanup before release
    fun release() {
        stopPlaybackTicker()
        scrubController.reset()
        frameCache.clear()
        try {
            exoPlayer.clearMediaItems()
        } catch (_: Exception) {}
        exoPlayer.release()
    }
}