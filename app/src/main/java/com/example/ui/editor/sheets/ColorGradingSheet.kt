package com.example.ui.editor.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ColorFilter
import com.example.ui.theme.VistaraDarkSurface
import com.example.ui.theme.VistaraDarkSurfaceHighlight
import com.example.ui.theme.VistaraPrimary
import com.example.ui.theme.VistaraSecondary
import com.example.ui.theme.VistaraTextMuted
import com.example.ui.theme.VistaraTextPrimary
import com.example.ui.theme.VistaraTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorGradingSheet(
    colorFilter: ColorFilter,
    onApplyColorFilter: (ColorFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var currentFilter by remember { mutableStateOf(colorFilter) }

    fun update(newFilter: ColorFilter) {
        currentFilter = newFilter
        onApplyColorFilter(newFilter)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VistaraDarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .testTag("color_grading_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Color Grading",
                        tint = VistaraSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = "GPU Color Grading",
                        color = VistaraTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Reset All Button
                Surface(
                    onClick = { update(ColorFilter()) },
                    shape = RoundedCornerShape(8.dp),
                    color = VistaraDarkSurfaceHighlight
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset All",
                            tint = VistaraTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Reset All", color = VistaraTextMuted, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Brightness
            ColorSliderRow(
                label = "Brightness",
                value = currentFilter.brightness,
                valueRange = -1.0f..1.0f,
                onValueChange = { update(currentFilter.copy(brightness = it)) },
                onReset = { update(currentFilter.copy(brightness = 0f)) }
            )

            // 2. Contrast
            ColorSliderRow(
                label = "Contrast",
                value = currentFilter.contrast,
                valueRange = -1.0f..1.0f,
                onValueChange = { update(currentFilter.copy(contrast = it)) },
                onReset = { update(currentFilter.copy(contrast = 0f)) }
            )

            // 3. Saturation
            ColorSliderRow(
                label = "Saturation",
                value = currentFilter.saturation,
                valueRange = -1.0f..1.0f,
                onValueChange = { update(currentFilter.copy(saturation = it)) },
                onReset = { update(currentFilter.copy(saturation = 0f)) }
            )

            // 4. Temperature (Warm / Cool)
            ColorSliderRow(
                label = "Temperature (Warm / Cool)",
                value = currentFilter.temperature,
                valueRange = -1.0f..1.0f,
                onValueChange = { update(currentFilter.copy(temperature = it)) },
                onReset = { update(currentFilter.copy(temperature = 0f)) }
            )

            // 5. Tint (Green / Magenta)
            ColorSliderRow(
                label = "Tint (Green / Magenta)",
                value = currentFilter.tint,
                valueRange = -1.0f..1.0f,
                onValueChange = { update(currentFilter.copy(tint = it)) },
                onReset = { update(currentFilter.copy(tint = 0f)) }
            )

            // 6. Highlights
            ColorSliderRow(
                label = "Highlights",
                value = currentFilter.highlights,
                valueRange = -1.0f..1.0f,
                onValueChange = { update(currentFilter.copy(highlights = it)) },
                onReset = { update(currentFilter.copy(highlights = 0f)) }
            )

            // 7. Shadows
            ColorSliderRow(
                label = "Shadows",
                value = currentFilter.shadows,
                valueRange = -1.0f..1.0f,
                onValueChange = { update(currentFilter.copy(shadows = it)) },
                onReset = { update(currentFilter.copy(shadows = 0f)) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VistaraPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_color_grading_button")
            ) {
                Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ColorSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    val displayPercent = (value * 100).toInt()

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = VistaraTextSecondary, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (displayPercent > 0) "+$displayPercent" else "$displayPercent",
                    color = if (displayPercent != 0) VistaraSecondary else VistaraTextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (value != 0f) {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.size(24.dp).padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset $label",
                            tint = VistaraTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = VistaraSecondary,
                activeTrackColor = VistaraSecondary,
                inactiveTrackColor = VistaraDarkSurfaceHighlight
            )
        )
    }
}
