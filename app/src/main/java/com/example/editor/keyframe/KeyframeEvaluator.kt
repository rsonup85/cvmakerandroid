// FIXED: Removed unused applyInterpolation() function
package com.example.editor.keyframe

import com.example.domain.model.Keyframe
import com.example.domain.model.KeyframeProperty

object KeyframeEvaluator {

    fun evaluate(
        keyframes: List<Keyframe>,
        property: KeyframeProperty,
        timeMs: Long,
        defaultValue: Float
    ): Float {
        val propertyKeyframes = keyframes.filter { it.property == property }.sortedBy { it.timeMs }
        if (propertyKeyframes.isEmpty()) return defaultValue

        if (timeMs <= propertyKeyframes.first().timeMs) {
            return propertyKeyframes.first().value
        }

        if (timeMs >= propertyKeyframes.last().timeMs) {
            return propertyKeyframes.last().value
        }

        for (i in 0 until propertyKeyframes.size - 1) {
            val kf0 = propertyKeyframes[i]
            val kf1 = propertyKeyframes[i + 1]

            if (timeMs in kf0.timeMs..kf1.timeMs) {
                val totalSpan = (kf1.timeMs - kf0.timeMs).toFloat().coerceAtLeast(1f)
                val rawProgress = ((timeMs - kf0.timeMs).toFloat() / totalSpan).coerceIn(0f, 1f)
                val easedProgress = if (kf0.bezierCurve != null) {
                    KeyframeCurveInterpolator.evaluateBezier(kf0.bezierCurve, rawProgress)
                } else {
                    KeyframeCurveInterpolator.interpolate(kf0.interpolation, null, rawProgress)
                }
                return kf0.value + (kf1.value - kf0.value) * easedProgress
            }
        }

        return defaultValue
    }
}