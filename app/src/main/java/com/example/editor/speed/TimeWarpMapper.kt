package com.example.editor.speed

import com.example.domain.model.SpeedCurve

/**
 * Deterministic mapping between non-linear Timeline playback time and Source Media time.
 * Uses piecewise numerical integration so variable speed curves and ramps are identical
 * across Preview and Media3 Transformer export.
 */
object TimeWarpMapper {

    private const val INTEGRATION_STEPS = 100

    /**
     * Calculates the effective timeline duration for a given source duration, base speed, and optional speed curve.
     */
    fun calculateTimelineDurationMs(
        sourceDurationMs: Long,
        curve: SpeedCurve?,
        baseSpeed: Float = 1.0f
    ): Long {
        val safeBaseSpeed = baseSpeed.coerceIn(0.05f, 15.0f)
        if (curve == null || !curve.isEnabled || curve.points.isEmpty()) {
            return (sourceDurationMs / safeBaseSpeed).toLong().coerceAtLeast(100L)
        }

        // Integrate average speed over [0, 1]
        var totalSpeedArea = 0f
        val dt = 1f / INTEGRATION_STEPS
        for (i in 0 until INTEGRATION_STEPS) {
            val tMid = (i + 0.5f) * dt
            totalSpeedArea += SpeedCurveEvaluator.speedAt(curve, tMid) * dt
        }

        val effectiveSpeed = (totalSpeedArea * safeBaseSpeed).coerceIn(0.05f, 15.0f)
        return (sourceDurationMs / effectiveSpeed).toLong().coerceAtLeast(100L)
    }

    /**
     * Maps timeline offset (milliseconds from clip start) to source media time (milliseconds from sourceStartMs).
     */
    fun timelineToSourceMs(
        timelineOffsetMs: Long,
        timelineDurationMs: Long,
        sourceStartMs: Long,
        sourceDurationMs: Long,
        curve: SpeedCurve?,
        baseSpeed: Float = 1.0f
    ): Long {
        val safeTimelineDur = timelineDurationMs.coerceAtLeast(1L)
        val clampedOffset = timelineOffsetMs.coerceIn(0L, safeTimelineDur)

        if (curve == null || !curve.isEnabled || curve.points.isEmpty()) {
            val linearSourceOffset = (clampedOffset * baseSpeed).toLong()
            return (sourceStartMs + linearSourceOffset).coerceIn(sourceStartMs, sourceStartMs + sourceDurationMs)
        }

        val fraction = (clampedOffset.toFloat() / safeTimelineDur.toFloat()).coerceIn(0f, 1f)
        if (fraction <= 0f) return sourceStartMs
        if (fraction >= 1f) return sourceStartMs + sourceDurationMs

        // Cumulative integration up to fraction
        var cumulativeArea = 0f
        var totalArea = 0f
        val dt = 1f / INTEGRATION_STEPS

        for (i in 0 until INTEGRATION_STEPS) {
            val tMid = (i + 0.5f) * dt
            val s = SpeedCurveEvaluator.speedAt(curve, tMid)
            if (tMid <= fraction) {
                cumulativeArea += s * dt
            }
            totalArea += s * dt
        }

        val progress = if (totalArea > 0f) (cumulativeArea / totalArea).coerceIn(0f, 1f) else fraction
        val mappedOffset = (progress * sourceDurationMs).toLong()
        return (sourceStartMs + mappedOffset).coerceIn(sourceStartMs, sourceStartMs + sourceDurationMs)
    }
}
