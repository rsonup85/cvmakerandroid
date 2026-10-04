package com.example.editor.audio

data class WaveformData(
    val assetId: String,
    val peaks: FloatArray, // Normalized 0.0f to 1.0f
    val durationMs: Long,
    val sampleIntervalMs: Long = 50L // 20 peaks per second of audio
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as WaveformData
        return assetId == other.assetId && durationMs == other.durationMs && peaks.contentEquals(other.peaks)
    }

    override fun hashCode(): Int {
        var result = assetId.hashCode()
        result = 31 * result + peaks.contentHashCode()
        result = 31 * result + durationMs.hashCode()
        return result
    }
}
