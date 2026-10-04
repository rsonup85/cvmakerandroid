package com.example.editor.composition

import android.graphics.Color
import com.example.domain.model.ClipTransform
import com.example.domain.model.ItemType
import com.example.domain.model.KeyframeProperty
import com.example.domain.model.Project
import com.example.domain.model.TimelineItem
import com.example.domain.model.TransitionType
import com.example.editor.keyframe.KeyframeEvaluator

data class TransitionRenderState(
    val type: TransitionType,
    val progress: Float, // 0f (start of transition) to 1f (end of transition)
    val outgoingAlpha: Float,
    val incomingAlpha: Float,
    val outgoingOffsetX: Float,
    val incomingOffsetX: Float,
    val outgoingOffsetY: Float,
    val incomingOffsetY: Float,
    val outgoingScale: Float,
    val incomingScale: Float,
    val wipeProgress: Float = 0f,
    val overlayColorArgb: Int? = null
)

data class EvaluatedTransform(
    val positionX: Float, // Normalized 0..1
    val positionY: Float, // Normalized 0..1
    val scaleX: Float,
    val scaleY: Float,
    val rotationDegrees: Float,
    val opacity: Float,
    val cropLeft: Float,
    val cropTop: Float,
    val cropRight: Float,
    val cropBottom: Float
) {
    val isCropped: Boolean
        get() = cropLeft > 0f || cropTop > 0f || cropRight > 0f || cropBottom > 0f
}

data class EvaluatedCompositionState(
    val timelineMs: Long,
    val activeMainVideo: TimelineItem?,
    val incomingMainVideo: TimelineItem?,
    val activeOverlayVideos: List<TimelineItem>,
    val activeImages: List<TimelineItem>,
    val activeTexts: List<TimelineItem>,
    val activeAudios: List<TimelineItem>,
    val transition: TransitionRenderState?,
    val mainVideoTransform: EvaluatedTransform,
    val incomingVideoTransform: EvaluatedTransform?,
    val overlayTransforms: Map<String, EvaluatedTransform>,
    val imageTransforms: Map<String, EvaluatedTransform>,
    val textTransforms: Map<String, EvaluatedTransform>,
    val isTimelineGap: Boolean
)

object CompositionEngine {

    /**
     * Evaluates the complete composition state at the given timeline millisecond.
     * Guaranteed to produce identical evaluation logic for both Compose UI Preview and Export Renderer.
     */
    fun evaluateAt(project: Project, timelineMs: Long): EvaluatedCompositionState {
        val tracksById = project.tracks.associateBy { it.id }

        // Filter items whose tracks are visible
        val visibleItems = project.items.filter { item ->
            val track = tracksById[item.trackId]
            track == null || track.isVisible
        }

        // Active Main Video Track Clips
        val mainVideoClips = visibleItems.filter { it.type == ItemType.VIDEO && it.trackId == "track_video_1" }
            .ifEmpty { visibleItems.filter { it.type == ItemType.VIDEO } }
            .sortedBy { it.timelineStartMs }

        // Authoritative active clip check (DO NOT fall back to last clip outside its timeline window)
        val activeClipIndex = mainVideoClips.indexOfFirst { clip ->
            timelineMs >= clip.timelineStartMs && timelineMs < (clip.timelineStartMs + clip.durationMs)
        }

        val activeMainVideo = if (activeClipIndex != -1) mainVideoClips[activeClipIndex] else null
        val isGap = activeMainVideo == null

        // Overlays
        val activeOverlayVideos = visibleItems.filter { item ->
            item.type == ItemType.VIDEO && item.trackId != "track_video_1" &&
                    timelineMs >= item.timelineStartMs && timelineMs < (item.timelineStartMs + item.durationMs)
        }

        val activeImages = visibleItems.filter { item ->
            item.type == ItemType.IMAGE &&
                    timelineMs >= item.timelineStartMs && timelineMs < (item.timelineStartMs + item.durationMs)
        }

        val activeTexts = visibleItems.filter { item ->
            item.type == ItemType.TEXT &&
                    timelineMs >= item.timelineStartMs && timelineMs < (item.timelineStartMs + item.durationMs)
        }

        val activeAudios = visibleItems.filter { item ->
            item.type == ItemType.AUDIO &&
                    timelineMs >= item.timelineStartMs && timelineMs < (item.timelineStartMs + item.durationMs)
        }

        // Real Dual-Clip Transition State Calculation
        var transitionState: TransitionRenderState? = null
        var incomingClip: TimelineItem? = null
        var incomingTransform: EvaluatedTransform? = null

        // FIXED: Guard against negative remaining (transition already finished)
        if (activeMainVideo != null && activeMainVideo.transition.type != TransitionType.NONE && activeClipIndex != -1) {
            val clipEnd = activeMainVideo.timelineStartMs + activeMainVideo.durationMs
            val remaining = clipEnd - timelineMs
            val transDuration = activeMainVideo.transition.durationMs.coerceAtLeast(100L)

            // FIXED: 1..transDuration instead of 0..transDuration
            if (remaining in 1..transDuration) {
                // Progress: 0.0 at transition start, 1.0 at transition end
                val progress = 1f - (remaining.toFloat() / transDuration.toFloat()).coerceIn(0f, 1f)
                transitionState = calculateDualClipTransition(activeMainVideo.transition.type, progress)

                // Locate the incoming clip B on the main track
                if (activeClipIndex < mainVideoClips.size - 1) {
                    val next = mainVideoClips[activeClipIndex + 1]
                    incomingClip = next
                    incomingTransform = evaluateClipTransform(next, timelineMs)
                }
            }
        }

        // Evaluated Transform for Main Video (including keyframes if present)
        val mainVideoTransform = if (activeMainVideo != null) {
            evaluateClipTransform(activeMainVideo, timelineMs)
        } else {
            EvaluatedTransform(0.5f, 0.5f, 1f, 1f, 0f, 1f, 0f, 0f, 0f, 0f)
        }

        val overlayTransforms = activeOverlayVideos.associate { it.id to evaluateClipTransform(it, timelineMs) }
        val imageTransforms = activeImages.associate { it.id to evaluateImageTransform(it, timelineMs) }
        val textTransforms = activeTexts.associate { it.id to evaluateTextTransform(it, timelineMs) }

        return EvaluatedCompositionState(
            timelineMs = timelineMs,
            activeMainVideo = activeMainVideo,
            incomingMainVideo = incomingClip,
            activeOverlayVideos = activeOverlayVideos,
            activeImages = activeImages,
            activeTexts = activeTexts,
            activeAudios = activeAudios,
            transition = transitionState,
            mainVideoTransform = mainVideoTransform,
            incomingVideoTransform = incomingTransform,
            overlayTransforms = overlayTransforms,
            imageTransforms = imageTransforms,
            textTransforms = textTransforms,
            isTimelineGap = isGap
        )
    }

    /**
     * Real Dual-Clip Transition calculation for Clip A -> Clip B overlap
     */
    private fun calculateDualClipTransition(type: TransitionType, progress: Float): TransitionRenderState {
        val p = progress.coerceIn(0f, 1f)
        return when (type) {
            TransitionType.NONE -> TransitionRenderState(
                type = type,
                progress = p,
                outgoingAlpha = 1f,
                incomingAlpha = 0f,
                outgoingOffsetX = 0f,
                incomingOffsetX = 0f,
                outgoingOffsetY = 0f,
                incomingOffsetY = 0f,
                outgoingScale = 1f,
                incomingScale = 1f
            )
            TransitionType.CROSS_DISSOLVE, TransitionType.FADE -> TransitionRenderState(
                type = type,
                progress = p,
                outgoingAlpha = (1f - p).coerceIn(0f, 1f),
                incomingAlpha = p.coerceIn(0f, 1f),
                outgoingOffsetX = 0f,
                incomingOffsetX = 0f,
                outgoingOffsetY = 0f,
                incomingOffsetY = 0f,
                outgoingScale = 1f,
                incomingScale = 1f
            )
            TransitionType.DIP_TO_BLACK -> {
                val outA = (1f - 2f * p).coerceIn(0f, 1f)
                val inA = (2f * (p - 0.5f)).coerceIn(0f, 1f)
                val blackAlpha = if (p < 0.5f) p * 2f else (1f - p) * 2f
                val color = Color.argb((blackAlpha * 255).toInt().coerceIn(0, 255), 0, 0, 0)
                TransitionRenderState(
                    type = type,
                    progress = p,
                    outgoingAlpha = outA,
                    incomingAlpha = inA,
                    outgoingOffsetX = 0f,
                    incomingOffsetX = 0f,
                    outgoingOffsetY = 0f,
                    incomingOffsetY = 0f,
                    outgoingScale = 1f,
                    incomingScale = 1f,
                    overlayColorArgb = color
                )
            }
            TransitionType.DIP_TO_WHITE -> {
                val outA = (1f - 2f * p).coerceIn(0f, 1f)
                val inA = (2f * (p - 0.5f)).coerceIn(0f, 1f)
                val whiteAlpha = if (p < 0.5f) p * 2f else (1f - p) * 2f
                val color = Color.argb((whiteAlpha * 255).toInt().coerceIn(0, 255), 255, 255, 255)
                TransitionRenderState(
                    type = type,
                    progress = p,
                    outgoingAlpha = outA,
                    incomingAlpha = inA,
                    outgoingOffsetX = 0f,
                    incomingOffsetX = 0f,
                    outgoingOffsetY = 0f,
                    incomingOffsetY = 0f,
                    outgoingScale = 1f,
                    incomingScale = 1f,
                    overlayColorArgb = color
                )
            }
            TransitionType.SLIDE_LEFT, TransitionType.PUSH_LEFT -> TransitionRenderState(
                type = type,
                progress = p,
                outgoingAlpha = 1f,
                incomingAlpha = 1f,
                outgoingOffsetX = -p,
                incomingOffsetX = 1f - p,
                outgoingOffsetY = 0f,
                incomingOffsetY = 0f,
                outgoingScale = 1f,
                incomingScale = 1f
            )
            TransitionType.SLIDE_RIGHT -> TransitionRenderState(
                type = type,
                progress = p,
                outgoingAlpha = 1f,
                incomingAlpha = 1f,
                outgoingOffsetX = p,
                incomingOffsetX = -(1f - p),
                outgoingOffsetY = 0f,
                incomingOffsetY = 0f,
                outgoingScale = 1f,
                incomingScale = 1f
            )
            TransitionType.SLIDE_UP -> TransitionRenderState(
                type = type,
                progress = p,
                outgoingAlpha = 1f,
                incomingAlpha = 1f,
                outgoingOffsetX = 0f,
                incomingOffsetX = 0f,
                outgoingOffsetY = -p,
                incomingOffsetY = 1f - p,
                outgoingScale = 1f,
                incomingScale = 1f
            )
            TransitionType.SLIDE_DOWN -> TransitionRenderState(
                type = type,
                progress = p,
                outgoingAlpha = 1f,
                incomingAlpha = 1f,
                outgoingOffsetX = 0f,
                incomingOffsetX = 0f,
                outgoingOffsetY = p,
                incomingOffsetY = -(1f - p),
                outgoingScale = 1f,
                incomingScale = 1f
            )
            TransitionType.ZOOM_IN -> TransitionRenderState(
                type = type,
                progress = p,
                outgoingAlpha = (1f - p).coerceIn(0f, 1f),
                incomingAlpha = p.coerceIn(0f, 1f),
                outgoingOffsetX = 0f,
                incomingOffsetX = 0f,
                outgoingOffsetY = 0f,
                incomingOffsetY = 0f,
                outgoingScale = 1f + p * 0.5f,
                incomingScale = 0.7f + p * 0.3f
            )
            TransitionType.ZOOM_OUT -> TransitionRenderState(
                type = type,
                progress = p,
                outgoingAlpha = (1f - p).coerceIn(0f, 1f),
                incomingAlpha = p.coerceIn(0f, 1f),
                outgoingOffsetX = 0f,
                incomingOffsetX = 0f,
                outgoingOffsetY = 0f,
                incomingOffsetY = 0f,
                outgoingScale = (1f - p * 0.3f).coerceAtLeast(0.1f),
                incomingScale = 1.3f - p * 0.3f
            )
            TransitionType.WIPE_LEFT, TransitionType.WIPE_RIGHT -> TransitionRenderState(
                type = type,
                progress = p,
                outgoingAlpha = 1f,
                incomingAlpha = 1f,
                outgoingOffsetX = 0f,
                incomingOffsetX = 0f,
                outgoingOffsetY = 0f,
                incomingOffsetY = 0f,
                outgoingScale = 1f,
                incomingScale = 1f,
                wipeProgress = p
            )
            TransitionType.BLUR -> TransitionRenderState(
                type = type,
                progress = p,
                outgoingAlpha = (1f - p).coerceIn(0f, 1f),
                incomingAlpha = p.coerceIn(0f, 1f),
                outgoingOffsetX = 0f,
                incomingOffsetX = 0f,
                outgoingOffsetY = 0f,
                incomingOffsetY = 0f,
                outgoingScale = 1f + p * 0.1f,
                incomingScale = 1f
            )
        }
    }

    private fun evaluateClipTransform(item: TimelineItem, timelineMs: Long): EvaluatedTransform {
        val t = item.transform
        val kfs = item.keyframes

        val relativeTimeMs = (timelineMs - item.timelineStartMs).coerceAtLeast(0L)

        val posX = KeyframeEvaluator.evaluate(kfs, KeyframeProperty.POSITION_X, relativeTimeMs, 0.5f + t.offsetX)
        val posY = KeyframeEvaluator.evaluate(kfs, KeyframeProperty.POSITION_Y, relativeTimeMs, 0.5f + t.offsetY)
        val scale = KeyframeEvaluator.evaluate(kfs, KeyframeProperty.SCALE, relativeTimeMs, t.scale)
        val rotation = KeyframeEvaluator.evaluate(kfs, KeyframeProperty.ROTATION, relativeTimeMs, t.rotationDegrees.toFloat())
        val opacity = KeyframeEvaluator.evaluate(kfs, KeyframeProperty.OPACITY, relativeTimeMs, t.opacity)

        val scaleX = scale * (if (t.flipHorizontal) -1f else 1f)
        val scaleY = scale * (if (t.flipVertical) -1f else 1f)

        return EvaluatedTransform(
            positionX = posX,
            positionY = posY,
            scaleX = scaleX,
            scaleY = scaleY,
            rotationDegrees = rotation,
            opacity = opacity.coerceIn(0f, 1f),
            cropLeft = t.cropLeft,
            cropTop = t.cropTop,
            cropRight = t.cropRight,
            cropBottom = t.cropBottom
        )
    }

    private fun evaluateImageTransform(item: TimelineItem, timelineMs: Long): EvaluatedTransform {
        val props = item.imageProperties
        val kfs = item.keyframes
        val relativeTimeMs = (timelineMs - item.timelineStartMs).coerceAtLeast(0L)

        val basePosX = props?.positionX ?: 0.5f
        val basePosY = props?.positionY ?: 0.5f
        val baseScale = props?.scale ?: 1.0f
        val baseRot = props?.rotationDegrees ?: 0.0f
        val baseOpacity = props?.opacity ?: 1.0f

        val posX = KeyframeEvaluator.evaluate(kfs, KeyframeProperty.POSITION_X, relativeTimeMs, basePosX)
        val posY = KeyframeEvaluator.evaluate(kfs, KeyframeProperty.POSITION_Y, relativeTimeMs, basePosY)
        val scale = KeyframeEvaluator.evaluate(kfs, KeyframeProperty.SCALE, relativeTimeMs, baseScale)
        val rotation = KeyframeEvaluator.evaluate(kfs, KeyframeProperty.ROTATION, relativeTimeMs, baseRot)
        val opacity = KeyframeEvaluator.evaluate(kfs, KeyframeProperty.OPACITY, relativeTimeMs, baseOpacity)

        return EvaluatedTransform(
            positionX = posX,
            positionY = posY,
            scaleX = scale,
            scaleY = scale,
            rotationDegrees = rotation,
            opacity = opacity.coerceIn(0f, 1f),
            cropLeft = item.transform.cropLeft,
            cropTop = item.transform.cropTop,
            cropRight = item.transform.cropRight,
            cropBottom = item.transform.cropBottom
        )
    }

    private fun evaluateTextTransform(item: TimelineItem, timelineMs: Long): EvaluatedTransform {
        val props = item.textProperties
        val kfs = item.keyframes
        val relativeTimeMs = (timelineMs - item.timelineStartMs).coerceAtLeast(0L)

        val basePosX = props?.positionX ?: 0.5f
        val basePosY = props?.positionY ?: 0.5f
        val baseScale = props?.scale ?: 1.0f
        val baseRot = props?.rotationDegrees ?: 0.0f
        val baseOpacity = props?.opacity ?: 1.0f

        val posX = KeyframeEvaluator.evaluate(kfs, KeyframeProperty.POSITION_X, relativeTimeMs, basePosX)
        val posY = KeyframeEvaluator.evaluate(kfs, KeyframeProperty.POSITION_Y, relativeTimeMs, basePosY)
        val scale = KeyframeEvaluator.evaluate(kfs, KeyframeProperty.SCALE, relativeTimeMs, baseScale)
        val rotation = KeyframeEvaluator.evaluate(kfs, KeyframeProperty.ROTATION, relativeTimeMs, baseRot)
        val opacity = KeyframeEvaluator.evaluate(kfs, KeyframeProperty.OPACITY, relativeTimeMs, baseOpacity)

        return EvaluatedTransform(
            positionX = posX,
            positionY = posY,
            scaleX = scale,
            scaleY = scale,
            rotationDegrees = rotation,
            opacity = opacity.coerceIn(0f, 1f),
            cropLeft = 0f, cropTop = 0f, cropRight = 0f, cropBottom = 0f
        )
    }

    fun getEffectiveVolume(item: TimelineItem, project: Project): Float {
        val track = project.tracks.find { it.id == item.trackId }
        if (track?.isMuted == true || item.isMuted) return 0f
        return item.volume.coerceIn(0f, 2f)
    }
}
