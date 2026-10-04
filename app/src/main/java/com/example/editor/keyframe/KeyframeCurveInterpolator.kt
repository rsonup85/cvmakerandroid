package com.example.editor.keyframe

import com.example.domain.model.BezierCurve
import com.example.domain.model.KeyframeInterpolation

object KeyframeCurveInterpolator {

    /**
     * Solves cubic bezier y for a given progress x (0.0 to 1.0) using Newton-Raphson approximation.
     */
    fun evaluateBezier(curve: BezierCurve, x: Float): Float {
        if (x <= 0f) return 0f
        if (x >= 1f) return 1f

        val p1x = curve.p1x.coerceIn(0f, 1f)
        val p1y = curve.p1y
        val p2x = curve.p2x.coerceIn(0f, 1f)
        val p2y = curve.p2y

        // Newton-Raphson to solve for t given x
        var t = x
        for (i in 0 until 8) {
            val currentX = sampleCurveX(p1x, p2x, t) - x
            if (kotlin.math.abs(currentX) < 1e-5f) break
            val derivativeX = sampleDerivativeX(p1x, p2x, t)
            if (kotlin.math.abs(derivativeX) < 1e-5f) break
            t -= currentX / derivativeX
            t = t.coerceIn(0f, 1f)
        }

        return sampleCurveY(p1y, p2y, t).coerceIn(0f, 1f)
    }

    private fun sampleCurveX(p1x: Float, p2x: Float, t: Float): Float {
        // (1-t)^3 * 0 + 3(1-t)^2 * t * p1x + 3(1-t) * t^2 * p2x + t^3 * 1
        val oneMinusT = 1f - t
        return 3f * oneMinusT * oneMinusT * t * p1x + 3f * oneMinusT * t * t * p2x + t * t * t
    }

    private fun sampleCurveY(p1y: Float, p2y: Float, t: Float): Float {
        val oneMinusT = 1f - t
        return 3f * oneMinusT * oneMinusT * t * p1y + 3f * oneMinusT * t * t * p2y + t * t * t
    }

    private fun sampleDerivativeX(p1x: Float, p2x: Float, t: Float): Float {
        val oneMinusT = 1f - t
        return 3f * oneMinusT * oneMinusT * p1x + 6f * oneMinusT * t * (p2x - p1x) + 3f * t * t * (1f - p2x)
    }

    fun interpolate(
        interpolation: KeyframeInterpolation,
        customCurve: BezierCurve?,
        progress: Float
    ): Float {
        val p = progress.coerceIn(0f, 1f)
        return when (interpolation) {
            KeyframeInterpolation.LINEAR -> p
            KeyframeInterpolation.EASE_IN -> evaluateBezier(BezierCurve.EASE_IN, p)
            KeyframeInterpolation.EASE_OUT -> evaluateBezier(BezierCurve.EASE_OUT, p)
            KeyframeInterpolation.EASE_IN_OUT -> evaluateBezier(BezierCurve.EASE_IN_OUT, p)
        }
    }
}
