package com.example.editor.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.FileUtils
import com.example.data.local.AppDatabase
import com.example.data.repository.ProjectRepository
import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.ClipTransform
import com.example.domain.model.ColorFilter
import com.example.domain.model.ExportSettings
import com.example.domain.model.ImageLayerProperties
import com.example.domain.model.ItemType
import com.example.domain.model.Keyframe
import com.example.domain.model.KeyframeInterpolation
import com.example.domain.model.KeyframeProperty
import com.example.domain.model.MediaAsset
import com.example.domain.model.MediaType
import com.example.domain.model.Project
import com.example.domain.model.SpeedCurve
import com.example.domain.model.TextLayerProperties
import com.example.domain.model.TimelineItem
import com.example.domain.model.Track
import com.example.domain.model.TrackType
import com.example.domain.model.TransitionConfig
import com.example.domain.model.TransitionType
import com.example.editor.export.ExportState
import com.example.editor.export.VideoExporter
import com.example.editor.font.FontManager
import com.example.editor.player.TimelinePlayer
import com.example.editor.speed.TimeWarpMapper
import com.example.editor.undo.UndoRedoManager
import com.example.media.MediaMetadataReader
import com.example.media.ThumbnailLoader
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

enum class EditorSheet {
    NONE,
    SPEED,
    VOLUME,
    TRANSFORM,
    CROP,
    KEYFRAMES,
    CANVAS,
    TEXT_EDITOR,
    TRANSITION,
    ADD_MEDIA_CHOICE,
    TRACK_MANAGER,
    EXPORT_CONFIG,
    COLOR_GRADING
}

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(AppDatabase.getInstance(application), application)
    val timelinePlayer = TimelinePlayer(application, viewModelScope)
    val videoExporter = VideoExporter(application)
    private val undoRedoManager = UndoRedoManager()

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    private val _selectedItemId = MutableStateFlow<String?>(null)
    val selectedItemId: StateFlow<String?> = _selectedItemId.asStateFlow()

    private val _activeSheet = MutableStateFlow(EditorSheet.NONE)
    val activeSheet: StateFlow<EditorSheet> = _activeSheet.asStateFlow()

    private val _autosaveStatus = MutableStateFlow("Saved")
    val autosaveStatus: StateFlow<String> = _autosaveStatus.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    val playheadMs = timelinePlayer.playheadMs
    val isPlaying = timelinePlayer.isPlaying
    val isInGap = timelinePlayer.isInGap
    val exportState = videoExporter.exportState

    // Thread-safe debounced autosave mutex
    private val autosaveMutex = Mutex()
    private var autosaveJob: Job? = null
    private var pendingAutosaveProject: Project? = null

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            val loaded = repository.getProject(projectId)
            if (loaded != null) {
                _project.value = loaded
                timelinePlayer.setProject(loaded)
                _selectedItemId.value = loaded.videoClips.firstOrNull()?.id
                undoRedoManager.clear()
                updateUndoRedoStates()
                repository.saveDraftRecovery(loaded)
            }
        }
    }

    fun selectItem(itemId: String?) {
        _selectedItemId.value = itemId
    }

    fun openSheet(sheet: EditorSheet) {
        _activeSheet.value = sheet
    }

    fun closeSheet() {
        _activeSheet.value = EditorSheet.NONE
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun seekTo(timeMs: Long) {
        timelinePlayer.seekTo(timeMs)
    }

    fun startScrubbing() {
        timelinePlayer.startScrubbing()
    }

    fun stopScrubbing() {
        timelinePlayer.stopScrubbing()
    }

    fun jumpToStart() {
        timelinePlayer.jumpToStart()
    }

    fun jumpToEnd() {
        timelinePlayer.jumpToEnd()
    }

    fun togglePlayPause() {
        timelinePlayer.togglePlayPause()
    }

    fun toggleSnap() {
        mutateProject(recordUndo = false) { proj ->
            proj.copy(isSnapEnabled = !proj.isSnapEnabled)
        }
    }

    // --- Track Lock Validation ---
    private fun isItemTrackLocked(itemId: String?): Boolean {
        if (itemId == null) return false
        val current = _project.value ?: return false
        val item = current.items.find { it.id == itemId } ?: return false
        val track = current.tracks.find { it.id == item.trackId }
        val locked = track?.isLocked == true
        if (locked) {
            _userMessage.value = "Track '${track?.name ?: "Track"}' is locked. Unlock to edit."
        }
        return locked
    }

    private fun isTrackIdLocked(trackId: String): Boolean {
        val current = _project.value ?: return false
        val track = current.tracks.find { it.id == trackId }
        val locked = track?.isLocked == true
        if (locked) {
            _userMessage.value = "Track '${track?.name ?: "Track"}' is locked. Unlock to edit."
        }
        return locked
    }

    private fun mutateProject(recordUndo: Boolean = true, mutator: (Project) -> Project) {
        val current = _project.value ?: return
        if (recordUndo) {
            undoRedoManager.pushState(current)
            updateUndoRedoStates()
        }
        val mutated = mutator(current).copy(updatedAt = System.currentTimeMillis())
        _project.value = mutated
        timelinePlayer.setProject(mutated)
        triggerAutosave(mutated)
    }

    private fun updateUndoRedoStates() {
        _canUndo.value = undoRedoManager.canUndo()
        _canRedo.value = undoRedoManager.canRedo()
    }

    fun undo() {
        val current = _project.value ?: return
        val previous = undoRedoManager.undo(current) ?: return
        _project.value = previous
        timelinePlayer.setProject(previous)
        updateUndoRedoStates()
        triggerAutosave(previous)
    }

    fun redo() {
        val current = _project.value ?: return
        val next = undoRedoManager.redo(current) ?: return
        _project.value = next
        timelinePlayer.setProject(next)
        updateUndoRedoStates()
        triggerAutosave(next)
    }

    // --- Thread-Safe Latest-State Autosave ---
    private fun triggerAutosave(projectToSave: Project) {
        pendingAutosaveProject = projectToSave
        _autosaveStatus.value = "Saving…"
        repository.saveDraftRecovery(projectToSave)

        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(400)
            autosaveMutex.withLock {
                val latest = pendingAutosaveProject
                if (latest != null) {
                    repository.saveProject(latest)
                    _autosaveStatus.value = "Saved"
                }
            }
        }
    }

    // --- Multi-Track Controls ---
    fun toggleTrackVisibility(trackId: String) {
        mutateProject { proj ->
            val updatedTracks = proj.tracks.map { trk ->
                if (trk.id == trackId) trk.copy(isVisible = !trk.isVisible) else trk
            }
            proj.copy(tracks = updatedTracks)
        }
    }

    fun toggleTrackLock(trackId: String) {
        mutateProject { proj ->
            val updatedTracks = proj.tracks.map { trk ->
                if (trk.id == trackId) trk.copy(isLocked = !trk.isLocked) else trk
            }
            proj.copy(tracks = updatedTracks)
        }
    }

    fun toggleTrackMute(trackId: String) {
        mutateProject { proj ->
            val updatedTracks = proj.tracks.map { trk ->
                if (trk.id == trackId) trk.copy(isMuted = !trk.isMuted) else trk
            }
            proj.copy(tracks = updatedTracks)
        }
    }

    // --- Timeline Operations with Track Lock & Keyframe Remapping ---
    fun splitSelectedClip() {
        val current = _project.value ?: return
        val selectedId = _selectedItemId.value ?: return
        if (isItemTrackLocked(selectedId)) return

        val currentPlayhead = playheadMs.value
        val clip = current.items.find { it.id == selectedId } ?: return
        val clipStart = clip.timelineStartMs
        val clipEnd = clipStart + clip.durationMs

        if (currentPlayhead <= clipStart + 50 || currentPlayhead >= clipEnd - 50) return

        mutateProject { proj ->
            val splitTimelineOffset = currentPlayhead - clipStart
            val splitSourceOffset = (splitTimelineOffset * clip.speed).toLong()

            val clip1Keyframes = clip.keyframes.filter { it.timeMs <= splitTimelineOffset }
            val clip2Keyframes = clip.keyframes.filter { it.timeMs > splitTimelineOffset }
                .map { it.copy(timeMs = (it.timeMs - splitTimelineOffset).coerceAtLeast(0L)) }

            val clip1 = clip.copy(
                durationMs = splitTimelineOffset,
                sourceDurationMs = splitSourceOffset,
                keyframes = clip1Keyframes
            )
            // FIXED (BUG E): Invalidate speedCurve on split
            val clip2 = clip.copy(
                id = UUID.randomUUID().toString(),
                timelineStartMs = currentPlayhead,
                durationMs = clip.durationMs - splitTimelineOffset,
                sourceStartMs = clip.sourceStartMs + splitSourceOffset,
                sourceDurationMs = clip.sourceDurationMs - splitSourceOffset,
                transition = TransitionConfig(),
                keyframes = clip2Keyframes,
                speedCurve = null
            )

            val updatedItems = proj.items.toMutableList()
            val clipIndex = updatedItems.indexOfFirst { it.id == clip.id }
            if (clipIndex != -1) {
                updatedItems[clipIndex] = clip1
                updatedItems.add(clipIndex + 1, clip2)
            }
            _selectedItemId.value = clip2.id
            proj.copy(items = updatedItems)
        }
    }

    /**
     * Non-destructive Normal Trim: updates clip window in place WITHOUT shifting subsequent clips
     */
    fun trimSelectedClipNormal(newSourceStartMs: Long, newSourceDurationMs: Long) {
        val selectedId = _selectedItemId.value ?: return
        if (isItemTrackLocked(selectedId)) return

        mutateProject(recordUndo = true) { proj ->
            val updated = proj.items.map { item ->
                if (item.id == selectedId) {
                    val safeSourceStart = newSourceStartMs.coerceAtLeast(0L)
                    val safeSourceDuration = newSourceDurationMs.coerceAtLeast(300L)
                    val newTimelineDuration = (safeSourceDuration / item.speed).toLong()

                    val deltaSourceStart = safeSourceStart - item.sourceStartMs
                    val deltaTimeline = (deltaSourceStart / item.speed).toLong()

                    val adjustedKeyframes = if (deltaTimeline > 0) {
                        item.keyframes.mapNotNull {
                            if (it.timeMs >= deltaTimeline) it.copy(timeMs = it.timeMs - deltaTimeline) else null
                        }
                    } else item.keyframes

                    item.copy(
                        sourceStartMs = safeSourceStart,
                        sourceDurationMs = safeSourceDuration,
                        durationMs = newTimelineDuration,
                        keyframes = adjustedKeyframes
                    )
                } else item
            }
            proj.copy(items = updated)
        }
    }

    /**
     * Ripple Trim: trims clip and shifts subsequent clips on that track
     */
    fun trimSelectedClipRipple(newSourceStartMs: Long, newSourceDurationMs: Long) {
        val selectedId = _selectedItemId.value ?: return
        if (isItemTrackLocked(selectedId)) return

        mutateProject(recordUndo = true) { proj ->
            val targetItem = proj.items.find { it.id == selectedId } ?: return@mutateProject proj
            val safeSourceStart = newSourceStartMs.coerceAtLeast(0L)
            val safeSourceDuration = newSourceDurationMs.coerceAtLeast(300L)
            val newTimelineDuration = (safeSourceDuration / targetItem.speed).toLong()
            val oldDuration = targetItem.durationMs
            val durationDiff = newTimelineDuration - oldDuration

            val deltaSourceStart = safeSourceStart - targetItem.sourceStartMs
            val deltaTimeline = (deltaSourceStart / targetItem.speed).toLong()

            val adjustedKeyframes = if (deltaTimeline > 0) {
                targetItem.keyframes.mapNotNull {
                    if (it.timeMs >= deltaTimeline) it.copy(timeMs = it.timeMs - deltaTimeline) else null
                }
            } else targetItem.keyframes

            val updatedItems = proj.items.map { item ->
                if (item.id == selectedId) {
                    item.copy(
                        sourceStartMs = safeSourceStart,
                        sourceDurationMs = safeSourceDuration,
                        durationMs = newTimelineDuration,
                        keyframes = adjustedKeyframes
                    )
                } else if (item.trackId == targetItem.trackId && item.timelineStartMs > targetItem.timelineStartMs) {
                    item.copy(timelineStartMs = (item.timelineStartMs + durationDiff).coerceAtLeast(0L))
                } else item
            }
            proj.copy(items = updatedItems)
        }
    }

    // Default trim called from timeline drag handles
    fun trimSelectedClip(newSourceStartMs: Long, newSourceDurationMs: Long) {
        trimSelectedClipRipple(newSourceStartMs, newSourceDurationMs)
    }

    fun moveLayer(itemId: String, newTimelineStartMs: Long) {
        if (isItemTrackLocked(itemId)) return
        mutateProject(recordUndo = true) { proj ->
            val updated = proj.items.map { item ->
                if (item.id == itemId) {
                    item.copy(timelineStartMs = newTimelineStartMs.coerceAtLeast(0L))
                } else item
            }
            proj.copy(items = updated)
        }
    }

    fun trimLayer(itemId: String, newDurationMs: Long) {
        if (isItemTrackLocked(itemId)) return
        mutateProject(recordUndo = true) { proj ->
            val updated = proj.items.map { item ->
                if (item.id == itemId) {
                    val safeDuration = newDurationMs.coerceAtLeast(400L)
                    val safeSourceDuration = (safeDuration * item.speed).toLong()
                    item.copy(
                        durationMs = safeDuration,
                        sourceDurationMs = safeSourceDuration
                    )
                } else item
            }
            proj.copy(items = updated)
        }
    }

    fun deleteSelectedItem(ripple: Boolean = true) {
        val selectedId = _selectedItemId.value ?: return
        if (isItemTrackLocked(selectedId)) return

        mutateProject(recordUndo = true) { proj ->
            val itemToDelete = proj.items.find { it.id == selectedId } ?: return@mutateProject proj
            val remainingItems = proj.items.filterNot { it.id == selectedId }

            val updatedItems = if (ripple && itemToDelete.trackId == "track_video_1") {
                val videoClips = remainingItems.filter { it.trackId == "track_video_1" }
                    .sortedBy { it.timelineStartMs }
                var currentTimeline = 0L
                val resequenced = videoClips.map { clip ->
                    val shifted = clip.copy(timelineStartMs = currentTimeline)
                    currentTimeline += shifted.durationMs
                    shifted
                }
                resequenced + remainingItems.filterNot { it.trackId == "track_video_1" }
            } else {
                remainingItems
            }

            _selectedItemId.value = updatedItems.firstOrNull()?.id
            proj.copy(items = updatedItems)
        }
    }

    fun duplicateSelectedItem() {
        val selectedId = _selectedItemId.value ?: return
        if (isItemTrackLocked(selectedId)) return

        mutateProject(recordUndo = true) { proj ->
            val item = proj.items.find { it.id == selectedId } ?: return@mutateProject proj
            val newItem = item.copy(
                id = UUID.randomUUID().toString(),
                keyframes = item.keyframes.map { it.copy(id = UUID.randomUUID().toString()) }
            )

            val updated = proj.items.toMutableList()
            val targetStart = item.timelineStartMs + item.durationMs
            updated.add(newItem.copy(timelineStartMs = targetStart))
            _selectedItemId.value = newItem.id
            proj.copy(items = updated)
        }
    }

    /**
     * Speed Change: Normal vs Ripple
     */
    fun updateClipSpeed(speed: Float, isRipple: Boolean = false) {
        val selectedId = _selectedItemId.value ?: return
        if (isItemTrackLocked(selectedId)) return

        mutateProject(recordUndo = true) { proj ->
            val targetItem = proj.items.find { it.id == selectedId } ?: return@mutateProject proj
            val safeSpeed = speed.coerceIn(0.1f, 10.0f)
            val newDuration = (targetItem.sourceDurationMs / safeSpeed).toLong()
            val durationDiff = newDuration - targetItem.durationMs

            val updatedItems = proj.items.map { item ->
                if (item.id == selectedId) {
                    item.copy(speed = safeSpeed, durationMs = newDuration)
                } else if (isRipple && item.trackId == targetItem.trackId && item.timelineStartMs > targetItem.timelineStartMs) {
                    item.copy(timelineStartMs = (item.timelineStartMs + durationDiff).coerceAtLeast(0L))
                } else item
            }
            proj.copy(items = updatedItems)
        }
    }

    fun toggleReverseClip() {
        val selectedId = _selectedItemId.value ?: return
        if (isItemTrackLocked(selectedId)) return

        mutateProject(recordUndo = true) { proj ->
            val updated = proj.items.map { item ->
                if (item.id == selectedId) {
                    item.copy(isReversed = !item.isReversed)
                } else item
            }
            proj.copy(items = updated)
        }
    }

    fun updateClipColorFilter(filter: ColorFilter) {
        val selectedId = _selectedItemId.value ?: return
        if (isItemTrackLocked(selectedId)) return

        mutateProject(recordUndo = true) { proj ->
            val updated = proj.items.map { item ->
                if (item.id == selectedId) item.copy(colorFilter = filter) else item
            }
            proj.copy(items = updated)
        }
    }

    fun updateClipSpeedCurve(curve: SpeedCurve, isRipple: Boolean = false) {
        val selectedId = _selectedItemId.value ?: return
        if (isItemTrackLocked(selectedId)) return

        mutateProject(recordUndo = true) { proj ->
            val target = proj.items.find { it.id == selectedId } ?: return@mutateProject proj
            val newDuration = TimeWarpMapper.calculateTimelineDurationMs(target.sourceDurationMs, curve, target.speed)
            val durationDiff = newDuration - target.durationMs

            val updated = proj.items.map { item ->
                if (item.id == selectedId) {
                    item.copy(speedCurve = curve, durationMs = newDuration)
                } else if (isRipple && item.trackId == target.trackId && item.timelineStartMs > target.timelineStartMs) {
                    item.copy(timelineStartMs = (item.timelineStartMs + durationDiff).coerceAtLeast(0L))
                } else item
            }
            proj.copy(items = updated)
        }
    }

    fun importCustomFont(uri: Uri) {
        viewModelScope.launch {
            FontManager.importFontFromUri(getApplication(), uri)
        }
    }

    /**
     * Real Freeze Frame: extracts the exact video frame at playhead
     */
    fun freezeFrameAtPlayhead() {
        val current = _project.value ?: return
        val selectedId = _selectedItemId.value ?: return
        if (isItemTrackLocked(selectedId)) return

        val currentPlayhead = playheadMs.value
        val clip = current.items.find { it.id == selectedId && it.type == ItemType.VIDEO } ?: return
        val asset = current.assets.find { it.id == clip.assetId } ?: return

        viewModelScope.launch {
            val context = getApplication<Application>()
            val relativeOffsetMs = (currentPlayhead - clip.timelineStartMs).coerceAtLeast(0L)
            val frameSourceMs = clip.sourceStartMs + (relativeOffsetMs * clip.speed).toLong()

            val frameBitmap = ThumbnailLoader.getFrameThumbnail(context, asset.uriString, frameSourceMs, 1920, 1080)
            if (frameBitmap != null) {
                val savedImagePath = ThumbnailLoader.saveFreezeFrame(context, frameBitmap)
                val freezeAsset = MediaAsset(
                    id = UUID.randomUUID().toString(),
                    uriString = Uri.fromFile(File(savedImagePath)).toString(),
                    fileName = "${asset.fileName}_freeze",
                    mediaType = MediaType.IMAGE,
                    durationMs = 3000L,
                    width = frameBitmap.width,
                    height = frameBitmap.height
                )

                mutateProject(recordUndo = true) { proj ->
                    val freezeItem = TimelineItem(
                        id = UUID.randomUUID().toString(),
                        trackId = clip.trackId,
                        assetId = freezeAsset.id,
                        name = "${clip.name} (Freeze)",
                        type = ItemType.IMAGE,
                        timelineStartMs = currentPlayhead,
                        durationMs = 3000L,
                        sourceStartMs = 0L,
                        sourceDurationMs = 3000L,
                        isFrozen = true,
                        imageProperties = ImageLayerProperties()
                    )
                    proj.copy(
                        assets = proj.assets + freezeAsset,
                        items = proj.items + freezeItem
                    )
                }
            }
        }
    }

    fun updateClipVolume(volume: Float, isMuted: Boolean) {
        val selectedId = _selectedItemId.value ?: return
        if (isItemTrackLocked(selectedId)) return

        mutateProject(recordUndo = true) { proj ->
            val updatedItems = proj.items.map { item ->
                if (item.id == selectedId) {
                    item.copy(volume = volume.coerceIn(0f, 2f), isMuted = isMuted)
                } else item
            }
            proj.copy(items = updatedItems)
        }
    }

    fun updateClipTransform(transform: ClipTransform) {
        val selectedId = _selectedItemId.value ?: return
        if (isItemTrackLocked(selectedId)) return

        mutateProject(recordUndo = false) { proj ->
            val updatedItems = proj.items.map { item ->
                if (item.id == selectedId) {
                    item.copy(transform = transform)
                } else item
            }
            proj.copy(items = updatedItems)
        }
    }

    fun commitClipTransform(transform: ClipTransform) {
        val selectedId = _selectedItemId.value ?: return
        if (isItemTrackLocked(selectedId)) return

        mutateProject(recordUndo = true) { proj ->
            val updatedItems = proj.items.map { item ->
                if (item.id == selectedId) {
                    item.copy(transform = transform)
                } else item
            }
            proj.copy(items = updatedItems)
        }
    }

    fun setCanvasAspectRatio(ratio: CanvasAspectRatio) {
        mutateProject(recordUndo = true) { proj ->
            proj.copy(canvasRatio = ratio)
        }
    }

    fun setCanvasBackgroundColor(hex: String) {
        mutateProject(recordUndo = true) { proj ->
            proj.copy(canvasBackgroundColorHex = hex)
        }
    }

    // --- Keyframe Operations ---
    fun addKeyframe(property: KeyframeProperty, value: Float, interpolation: KeyframeInterpolation = KeyframeInterpolation.LINEAR) {
        val selectedId = _selectedItemId.value ?: return
        if (isItemTrackLocked(selectedId)) return

        val currentPlayhead = playheadMs.value

        mutateProject(recordUndo = true) { proj ->
            val updated = proj.items.map { item ->
                if (item.id == selectedId) {
                    val relativeTimeMs = (currentPlayhead - item.timelineStartMs).coerceAtLeast(0L)
                    val filtered = item.keyframes.filterNot { it.property == property && it.timeMs == relativeTimeMs }
                    val newKf = Keyframe(
                        property = property,
                        timeMs = relativeTimeMs,
                        value = value,
                        interpolation = interpolation
                    )
                    item.copy(keyframes = (filtered + newKf).sortedBy { it.timeMs })
                } else item
            }
            proj.copy(items = updated)
        }
    }

    fun deleteKeyframe(keyframeId: String) {
        val selectedId = _selectedItemId.value ?: return
        if (isItemTrackLocked(selectedId)) return

        mutateProject(recordUndo = true) { proj ->
            val updated = proj.items.map { item ->
                if (item.id == selectedId) {
                    item.copy(keyframes = item.keyframes.filterNot { it.id == keyframeId })
                } else item
            }
            proj.copy(items = updated)
        }
    }

    fun jumpToNextKeyframe() {
        val selectedId = _selectedItemId.value ?: return
        val current = _project.value ?: return
        val item = current.items.find { it.id == selectedId } ?: return
        val currentOffset = (playheadMs.value - item.timelineStartMs).coerceAtLeast(0L)

        val nextKf = item.keyframes.filter { it.timeMs > currentOffset }.minByOrNull { it.timeMs }
        if (nextKf != null) {
            seekTo(item.timelineStartMs + nextKf.timeMs)
        }
    }

    fun jumpToPreviousKeyframe() {
        val selectedId = _selectedItemId.value ?: return
        val current = _project.value ?: return
        val item = current.items.find { it.id == selectedId } ?: return
        val currentOffset = (playheadMs.value - item.timelineStartMs).coerceAtLeast(0L)

        val prevKf = item.keyframes.filter { it.timeMs < currentOffset }.maxByOrNull { it.timeMs }
        if (prevKf != null) {
            seekTo(item.timelineStartMs + prevKf.timeMs)
        }
    }

    fun moveKeyframe(keyframeId: String, newRelativeTimeMs: Long) {
        val selectedId = _selectedItemId.value ?: return
        if (isItemTrackLocked(selectedId)) return

        mutateProject(recordUndo = true) { proj ->
            val updated = proj.items.map { item ->
                if (item.id == selectedId) {
                    val updatedKfs = item.keyframes.map {
                        if (it.id == keyframeId) it.copy(timeMs = newRelativeTimeMs.coerceAtLeast(0L)) else it
                    }.sortedBy { it.timeMs }
                    item.copy(keyframes = updatedKfs)
                } else item
            }
            proj.copy(items = updated)
        }
    }

    // --- Layers & Overlays ---
    fun addTextLayer(text: String, colorHex: String, bgHex: String?, fontSize: Float) {
        if (isTrackIdLocked("track_text")) return
        mutateProject(recordUndo = true) { proj ->
            val currentPlayhead = playheadMs.value
            val newItem = TimelineItem(
                id = UUID.randomUUID().toString(),
                trackId = "track_text",
                name = text,
                type = ItemType.TEXT,
                timelineStartMs = currentPlayhead,
                durationMs = 3000L,
                textProperties = TextLayerProperties(
                    text = text,
                    fontSizeSp = fontSize,
                    colorHex = colorHex,
                    backgroundColorHex = bgHex
                )
            )
            _selectedItemId.value = newItem.id
            proj.copy(items = proj.items + newItem)
        }
    }

    fun updateTextProperties(itemId: String, properties: TextLayerProperties) {
        if (isItemTrackLocked(itemId)) return
        mutateProject(recordUndo = false) { proj ->
            val updatedItems = proj.items.map { item ->
                if (item.id == itemId && item.type == ItemType.TEXT) {
                    item.copy(textProperties = properties, name = properties.text)
                } else item
            }
            proj.copy(items = updatedItems)
        }
    }

    fun addImageOverlay(rawUri: Uri) {
        if (isTrackIdLocked("track_image")) return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val localUri = FileUtils.cacheMediaLocally(context, rawUri, prefix = "overlay")
            val asset = MediaMetadataReader.analyzeMedia(context, localUri)
            mutateProject(recordUndo = true) { proj ->
                val currentPlayhead = playheadMs.value
                val newItem = TimelineItem(
                    id = UUID.randomUUID().toString(),
                    trackId = "track_image",
                    assetId = asset.id,
                    name = asset.fileName,
                    type = ItemType.IMAGE,
                    timelineStartMs = currentPlayhead,
                    durationMs = 3000L,
                    imageProperties = ImageLayerProperties()
                )
                _selectedItemId.value = newItem.id
                proj.copy(
                    assets = proj.assets + asset,
                    items = proj.items + newItem
                )
            }
        }
    }

    fun updateImageProperties(itemId: String, properties: ImageLayerProperties) {
        if (isItemTrackLocked(itemId)) return
        mutateProject(recordUndo = false) { proj ->
            val updatedItems = proj.items.map { item ->
                if (item.id == itemId && item.type == ItemType.IMAGE) {
                    item.copy(imageProperties = properties)
                } else item
            }
            proj.copy(items = updatedItems)
        }
    }

    fun setClipTransition(transition: TransitionConfig) {
        val selectedId = _selectedItemId.value ?: return
        if (isItemTrackLocked(selectedId)) return

        mutateProject(recordUndo = true) { proj ->
            val updatedItems = proj.items.map { item ->
                if (item.id == selectedId && item.type == ItemType.VIDEO) {
                    item.copy(transition = transition)
                } else item
            }
            proj.copy(items = updatedItems)
        }
    }

    fun addMediaClips(uris: List<Uri>, targetTrack: String = "track_video_1") {
        if (uris.isEmpty()) return
        if (isTrackIdLocked(targetTrack)) return

        viewModelScope.launch {
            val context = getApplication<Application>()
            val newAssets = mutableListOf<MediaAsset>()

            for ((index, rawUri) in uris.withIndex()) {
                val localUri = FileUtils.cacheMediaLocally(context, rawUri, prefix = "add_${index}")
                val asset = MediaMetadataReader.analyzeMedia(context, localUri)
                newAssets.add(asset)
            }

            mutateProject(recordUndo = true) { proj ->
                val newItems = mutableListOf<TimelineItem>()
                val currentPlayhead = playheadMs.value
                var currentTimeline = if (targetTrack == "track_video_1") proj.totalDurationMs else currentPlayhead

                for (asset in newAssets) {
                    val actualTrack = when {
                        asset.mediaType == MediaType.AUDIO -> "track_audio_1"
                        targetTrack == "track_image" -> "track_image"
                        targetTrack == "track_video_2" -> "track_video_2"
                        else -> "track_video_1"
                    }

                    val itemType = when (asset.mediaType) {
                        MediaType.AUDIO -> ItemType.AUDIO
                        MediaType.IMAGE -> ItemType.IMAGE
                        MediaType.VIDEO -> ItemType.VIDEO
                    }

                    val item = TimelineItem(
                        id = UUID.randomUUID().toString(),
                        trackId = actualTrack,
                        assetId = asset.id,
                        name = asset.fileName,
                        type = itemType,
                        timelineStartMs = currentTimeline,
                        durationMs = asset.durationMs,
                        sourceStartMs = 0L,
                        sourceDurationMs = asset.durationMs,
                        imageProperties = if (itemType == ItemType.IMAGE) ImageLayerProperties() else null
                    )
                    newItems.add(item)
                    if (actualTrack == "track_video_1") {
                        currentTimeline += asset.durationMs
                    }
                }

                proj.copy(
                    assets = proj.assets + newAssets,
                    items = proj.items + newItems
                )
            }
        }
    }

    // --- Export Pipeline ---
    fun startExport(settings: ExportSettings) {
        val current = _project.value ?: return
        _activeSheet.value = EditorSheet.NONE
        viewModelScope.launch {
            val updated = current.copy(exportSettings = settings)
            _project.value = updated
            videoExporter.exportProject(updated)
        }
    }

    fun cancelExport() {
        videoExporter.cancelExport()
    }

    fun resetExportState() {
        videoExporter.resetState()
    }

    override fun onCleared() {
        super.onCleared()
        timelinePlayer.release()
    }
}