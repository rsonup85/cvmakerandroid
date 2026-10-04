// FIXED: Correct matrix order — scale → contrast → offset
package com.example.editor.color

import androidx.compose.ui.graphics.ColorMatrix
import androidx.media3.common.Effect
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.RgbAdjustment
import com.example.domain.model.ColorFilter

object ColorProcessor {

    fun createComposeColorMatrix(filter: ColorFilter): ColorMatrix {
        val matrix = ColorMatrix()
        if (filter.isDefault) return matrix

        val sat = (1f + filter.saturation).coerceIn(0f, 3f)
        matrix.setToSaturation(sat)

        val contrastFactor = (1f + filter.contrast).coerceIn(0.1f, 3f)
        val brightOffset = (filter.brightness * 128f) +
                           (filter.highlights * 64f) -
                           (filter.shadows * 32f)
        // FIXED: Proper contrast offset formula
        val contrastOffset = 128f * (1f - contrastFactor)

        val rScale = (1f + filter.temperature * 0.4f - filter.tint * 0.2f).coerceIn(0.2f, 2.5f)
        val gScale = (1f + filter.tint * 0.4f).coerceIn(0.2f, 2.5f)
        val bScale = (1f - filter.temperature * 0.4f - filter.tint * 0.2f).coerceIn(0.2f, 2.5f)

        val values = matrix.values
        // FIXED: Apply scale * contrast first, then add offsets
        values[0] = values[0] * contrastFactor * rScale
        values[4] += contrastOffset + brightOffset

        values[6] = values[6] * contrastFactor * gScale
        values[9] += contrastOffset + brightOffset

        values[12] = values[12] * contrastFactor * bScale
        values[14] += contrastOffset + brightOffset

        return matrix
    }

    fun createMedia3Effects(filter: ColorFilter): List<Effect> {
        if (filter.isDefault) return emptyList()
        val effects = mutableListOf<Effect>()

        if (filter.contrast != 0f) {
            effects.add(Contrast(filter.contrast.coerceIn(-1f, 1f)))
        }

        if (filter.saturation != 0f || filter.brightness != 0f ||
            filter.highlights != 0f || filter.shadows != 0f) {
            val satDegrees = (filter.saturation * 100f).coerceIn(-100f, 100f)
            val lightDegrees = ((filter.brightness + filter.highlights * 0.5f - filter.shadows * 0.25f) * 100f)
                .coerceIn(-100f, 100f)
            val hsl = HslAdjustment.Builder()
                .adjustSaturation(satDegrees)
                .adjustLightness(lightDegrees)
                .build()
            effects.add(hsl)
        }

        if (filter.temperature != 0f || filter.tint != 0f) {
            val rScale = (1f + filter.temperature * 0.4f - filter.tint * 0.2f).coerceIn(0.1f, 2.0f)
            val gScale = (1f + filter.tint * 0.4f).coerceIn(0.1f, 2.0f)
            val bScale = (1f - filter.temperature * 0.4f - filter.tint * 0.2f).coerceIn(0.1f, 2.0f)

            val rgb = RgbAdjustment.Builder()
                .setRedScale(rScale)
                .setGreenScale(gScale)
                .setBlueScale(bScale)
                .build()
            effects.add(rgb)
        }

        return effects
    }
}