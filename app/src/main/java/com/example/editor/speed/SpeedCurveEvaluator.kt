package com.example.editor.speed

import com.example.domain.model.SpeedCurve
import com.example.domain.model.SpeedPoint

object SpeedCurveEvaluator {

    /**
     * Evaluates speed multiplier at any normalized timestamp (0.0 to 1.0) using smooth Hermite interpolation.
     */
    fun speedAt(curve: SpeedCurve?, normalizedTime: Float): Float {
        if (curve == null || !curve.isEnabled || curve.points.isEmpty()) {
            return 1.0f
        }
        return speedAt(curve.points, normalizedTime)
    }

    fun speedAt(points: List<SpeedPoint>, normalizedTime: Float): Float {
        if (points.isEmpty()) return 1.0f
        if (points.size == 1) return points[0].speed.coerceIn(0.05f, 10.0f)

        val clampedT = normalizedTime.coerceIn(0f, 1f)
        val sorted = points.sortedBy { it.timeNormalized }

        if (clampedT <= sorted.first().timeNormalized) {
            return sorted.first().speed.coerceIn(0.05f, 10.0f)
        }
        if (clampedT >= sorted.last().timeNormalized) {
            return sorted.last().speed.coerceIn(0.05f, 10.0f)
        }

        for (i in 0 until sorted.size - 1) {
            val p0 = sorted[i]
            val p1 = sorted[i + 1]

            if (clampedT in p0.timeNormalized..p1.timeNormalized) {
                val span = (p1.timeNormalized - p0.timeNormalized).coerceAtLeast(0.0001f)
                val localT = (clampedT - p0.timeNormalized) / span
                // Smooth Hermite S-curve interpolation
                val smoothT = localT * localT * (3f - 2f * localT)
                val interpolated = p0.speed + (p1.speed - p0.speed) * smoothT
                return interpolated.coerceIn(0.05f, 10.0f)
            }
        }

        return sorted.last().speed.coerceIn(0.05f, 10.0f)
    }
}
