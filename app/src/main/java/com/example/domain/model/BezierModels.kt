package com.example.domain.model

data class BezierCurve(
    val p1x: Float = 0.42f,
    val p1y: Float = 0.0f,
    val p2x: Float = 0.58f,
    val p2y: Float = 1.0f
) {
    companion object {
        val LINEAR = BezierCurve(0f, 0f, 1f, 1f)
        val EASE_IN = BezierCurve(0.42f, 0.0f, 1.0f, 1.0f)
        val EASE_OUT = BezierCurve(0.0f, 0.0f, 0.58f, 1.0f)
        val EASE_IN_OUT = BezierCurve(0.42f, 0.0f, 0.58f, 1.0f)
        val DRAMATIC = BezierCurve(0.2f, 0.0f, 0.2f, 1.0f)
    }
}
