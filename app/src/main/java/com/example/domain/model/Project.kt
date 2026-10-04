package com.example.domain.model

data class Project(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val canvasRatio: CanvasAspectRatio = CanvasAspectRatio.RATIO_9_16,
    val canvasBackgroundColorHex: String = "#000000",
    val isSnapEnabled: Boolean = true,
    val exportSettings: ExportSettings = ExportSettings(),
    val tracks: List<Track> = emptyList(),
    val items: List<TimelineItem> = emptyList(),
    val assets: List<MediaAsset> = emptyList()
) {
    val totalDurationMs: Long
        get() {
            if (items.isEmpty()) return 0L
            return items.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L
        }

    val mainVideoClips: List<TimelineItem>
        get() = items.filter { it.type == ItemType.VIDEO && it.trackId == "track_video_1" }
            .ifEmpty { items.filter { it.type == ItemType.VIDEO } }
            .sortedBy { it.timelineStartMs }

    val videoClips: List<TimelineItem>
        get() = mainVideoClips

    val overlayVideoClips: List<TimelineItem>
        get() = items.filter { it.type == ItemType.VIDEO && it.trackId != "track_video_1" }
            .sortedBy { it.timelineStartMs }

    val audioClips: List<TimelineItem>
        get() = items.filter { it.type == ItemType.AUDIO }.sortedBy { it.timelineStartMs }

    val textLayers: List<TimelineItem>
        get() = items.filter { it.type == ItemType.TEXT }.sortedBy { it.timelineStartMs }

    val imageLayers: List<TimelineItem>
        get() = items.filter { it.type == ItemType.IMAGE }.sortedBy { it.timelineStartMs }
}
