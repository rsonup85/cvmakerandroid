package com.example.domain.model

enum class ExportResolution(val label: String, val width: Int, val height: Int) {
    RES_480P("480p SD", 854, 480),
    RES_720P("720p HD", 1280, 720),
    RES_1080P("1080p Full HD", 1920, 1080),
    RES_1440P("1440p 2K", 2560, 1440),
    RES_4K("4K Ultra HD", 3840, 2160)
}

enum class ExportQuality(val label: String, val bitrateMultiplier: Float) {
    LOW("Low", 0.6f),
    MEDIUM("Standard", 1.0f),
    HIGH("High Quality", 1.5f)
}

data class ExportSettings(
    val resolution: ExportResolution = ExportResolution.RES_1080P,
    val fps: Int = 30,
    val quality: ExportQuality = ExportQuality.MEDIUM,
    val format: String = "mp4"
)
