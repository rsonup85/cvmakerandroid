package com.example.domain.model

import java.util.UUID

enum class ItemType {
    VIDEO,
    AUDIO,
    IMAGE,
    TEXT
}

enum class TransitionType(val label: String) {
    NONE("None"),
    FADE("Fade to Black"),
    CROSS_DISSOLVE("Cross Dissolve"),
    DIP_TO_BLACK("Dip to Black"),
    DIP_TO_WHITE("Dip to White"),
    SLIDE_LEFT("Slide Left"),
    SLIDE_RIGHT("Slide Right"),
    SLIDE_UP("Slide Up"),
    SLIDE_DOWN("Slide Down"),
    ZOOM_IN("Zoom In"),
    ZOOM_OUT("Zoom Out"),
    WIPE_LEFT("Wipe Left"),
    WIPE_RIGHT("Wipe Right"),
    PUSH_LEFT("Push Left"),
    BLUR("Blur Transition")
}

data class TransitionConfig(
    val id: String = UUID.randomUUID().toString(),
    val type: TransitionType = TransitionType.NONE,
    val durationMs: Long = 500L,
    val fromClipId: String = "",
    val toClipId: String = "",
    val easing: String = "LINEAR",
    val startPositionMs: Long = 0L
)

enum class KeyframeProperty {
    POSITION_X,
    POSITION_Y,
    SCALE,
    ROTATION,
    OPACITY,
    VOLUME
}

enum class KeyframeInterpolation {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT
}

data class Keyframe(
    val id: String = UUID.randomUUID().toString(),
    val property: KeyframeProperty,
    val timeMs: Long,
    val value: Float,
    val interpolation: KeyframeInterpolation = KeyframeInterpolation.LINEAR,
    val bezierCurve: BezierCurve? = null
)

data class ClipTransform(
    val rotationDegrees: Int = 0, // 0, 90, 180, 270
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val scale: Float = 1.0f,
    val offsetX: Float = 0.0f,
    val offsetY: Float = 0.0f,
    val cropLeft: Float = 0.0f,
    val cropTop: Float = 0.0f,
    val cropRight: Float = 0.0f,
    val cropBottom: Float = 0.0f,
    val opacity: Float = 1.0f,
    val anchorX: Float = 0.5f,
    val anchorY: Float = 0.5f
) {
    val isCropped: Boolean
        get() = cropLeft > 0.001f || cropTop > 0.001f || cropRight > 0.001f || cropBottom > 0.001f
}

enum class EditorFont(val displayName: String, val fontId: String) {
    SANS("Sans", "sans"),
    SERIF("Serif", "serif"),
    MONO("Mono", "mono"),
    CURSIVE("Handwritten", "cursive"),
    BOLD("Heavy", "bold"),
    LIGHT("Clean Light", "light");

    companion object {
        fun fromId(id: String?): EditorFont {
            return entries.firstOrNull {
                it.fontId.equals(id, ignoreCase = true) || it.displayName.equals(id, ignoreCase = true)
            } ?: SANS
        }
    }
}

data class TextLayerProperties(
    val text: String = "Title",
    val fontFamily: String = "sans",
    val fontSizeSp: Float = 24f,
    val colorHex: String = "#FFFFFF",
    val backgroundColorHex: String? = null,
    val strokeColorHex: String? = null,
    val strokeWidth: Float = 0f,
    val hasShadow: Boolean = false,
    val alignment: String = "CENTER", // "LEFT", "CENTER", "RIGHT"
    val positionX: Float = 0.5f, // Normalized 0..1
    val positionY: Float = 0.5f, // Normalized 0..1
    val scale: Float = 1.0f,
    val rotationDegrees: Float = 0.0f,
    val opacity: Float = 1.0f
)

data class ImageLayerProperties(
    val positionX: Float = 0.5f,
    val positionY: Float = 0.5f,
    val scale: Float = 1.0f,
    val rotationDegrees: Float = 0.0f,
    val opacity: Float = 1.0f
)

data class TimelineItem(
    val id: String,
    val trackId: String,
    val assetId: String = "",
    val name: String = "",
    val type: ItemType = ItemType.VIDEO,
    val timelineStartMs: Long = 0L,
    val durationMs: Long = 3000L,
    val sourceStartMs: Long = 0L,
    val sourceDurationMs: Long = 3000L,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val isFrozen: Boolean = false,
    val isReversed: Boolean = false,
    val transform: ClipTransform = ClipTransform(),
    val transition: TransitionConfig = TransitionConfig(),
    val textProperties: TextLayerProperties? = null,
    val imageProperties: ImageLayerProperties? = null,
    val keyframes: List<Keyframe> = emptyList(),
    val colorFilter: ColorFilter = ColorFilter(),
    val speedCurve: SpeedCurve? = null
)
