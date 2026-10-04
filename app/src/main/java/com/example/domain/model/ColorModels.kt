package com.example.domain.model

data class ColorFilter(
    val brightness: Float = 0f,   // -1.0f to +1.0f (0 = neutral)
    val contrast: Float = 0f,     // -1.0f to +1.0f (0 = neutral)
    val saturation: Float = 0f,   // -1.0f to +1.0f (0 = neutral)
    val temperature: Float = 0f,  // -1.0f to +1.0f (warm / cool)
    val tint: Float = 0f,         // -1.0f to +1.0f (green / magenta)
    val highlights: Float = 0f,   // -1.0f to +1.0f
    val shadows: Float = 0f,      // -1.0f to +1.0f
    val lutId: String? = null,
    val lutIntensity: Float = 1.0f
) {
    val isDefault: Boolean
        get() = brightness == 0f && contrast == 0f && saturation == 0f &&
                temperature == 0f && tint == 0f && highlights == 0f && shadows == 0f &&
                lutId == null
}

interface ColorLut {
    val id: String
    val name: String
    val intensity: Float
}

data class CubeLut(
    override val id: String,
    override val name: String,
    val size: Int,
    val tableData: FloatArray,
    override val intensity: Float = 1.0f
) : ColorLut {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CubeLut
        return id == other.id && size == other.size && intensity == other.intensity
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
