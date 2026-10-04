package com.example.domain.model

import java.util.UUID

data class SpeedPoint(
    val timeNormalized: Float, // 0.0f to 1.0f
    val speed: Float           // 0.1f to 10.0f
) {
    init {
        require(timeNormalized in 0f..1f) { "timeNormalized must be in 0f..1f, was $timeNormalized" }
        require(speed in 0.05f..15.0f) { "speed must be in 0.05f..15.0f, was $speed" }
    }
}

enum class SpeedCurvePreset(val displayName: String) {
    CONSTANT("Constant"),
    HERO("Hero (Dramatic Slow)"),
    BULLET("Bullet Time"),
    MONTAGE("Montage Ramp"),
    CUSTOM("Custom Curve")
}

data class SpeedCurve(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Normal",
    val preset: SpeedCurvePreset = SpeedCurvePreset.CONSTANT,
    val points: List<SpeedPoint> = listOf(
        SpeedPoint(0.0f, 1.0f),
        SpeedPoint(1.0f, 1.0f)
    ),
    val isEnabled: Boolean = false
) {
    companion object {
        fun createPreset(preset: SpeedCurvePreset): SpeedCurve {
            return when (preset) {
                SpeedCurvePreset.CONSTANT -> SpeedCurve(
                    name = "Constant",
                    preset = SpeedCurvePreset.CONSTANT,
                    points = listOf(SpeedPoint(0f, 1f), SpeedPoint(1f, 1f)),
                    isEnabled = false
                )
                SpeedCurvePreset.HERO -> SpeedCurve(
                    name = "Hero",
                    preset = SpeedCurvePreset.HERO,
                    points = listOf(
                        SpeedPoint(0.0f, 1.0f),
                        SpeedPoint(0.3f, 1.0f),
                        SpeedPoint(0.5f, 0.35f),
                        SpeedPoint(0.7f, 1.0f),
                        SpeedPoint(1.0f, 2.0f)
                    ),
                    isEnabled = true
                )
                SpeedCurvePreset.BULLET -> SpeedCurve(
                    name = "Bullet Time",
                    preset = SpeedCurvePreset.BULLET,
                    points = listOf(
                        SpeedPoint(0.0f, 1.0f),
                        SpeedPoint(0.35f, 0.2f),
                        SpeedPoint(0.65f, 0.2f),
                        SpeedPoint(1.0f, 1.0f)
                    ),
                    isEnabled = true
                )
                SpeedCurvePreset.MONTAGE -> SpeedCurve(
                    name = "Montage",
                    preset = SpeedCurvePreset.MONTAGE,
                    points = listOf(
                        SpeedPoint(0.0f, 1.0f),
                        SpeedPoint(0.3f, 2.2f),
                        SpeedPoint(0.6f, 0.6f),
                        SpeedPoint(1.0f, 2.0f)
                    ),
                    isEnabled = true
                )
                SpeedCurvePreset.CUSTOM -> SpeedCurve(
                    name = "Custom",
                    preset = SpeedCurvePreset.CUSTOM,
                    points = listOf(
                        SpeedPoint(0.0f, 1.0f),
                        SpeedPoint(0.5f, 1.5f),
                        SpeedPoint(1.0f, 1.0f)
                    ),
                    isEnabled = true
                )
            }
        }
    }
}
