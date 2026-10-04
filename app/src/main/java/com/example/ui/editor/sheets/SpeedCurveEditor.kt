package com.example.ui.editor.sheets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.SpeedCurve
import com.example.domain.model.SpeedCurvePreset
import com.example.domain.model.SpeedPoint
import com.example.editor.speed.SpeedCurveEvaluator
import com.example.ui.theme.VistaraDarkSurfaceBorder
import com.example.ui.theme.VistaraDarkSurfaceHighlight
import com.example.ui.theme.VistaraPrimary
import com.example.ui.theme.VistaraSecondary
import com.example.ui.theme.VistaraTextMuted
import com.example.ui.theme.VistaraTextPrimary
import com.example.ui.theme.VistaraTextSecondary

@Composable
fun SpeedCurveEditor(
    initialCurve: SpeedCurve?,
    onApplyCurve: (SpeedCurve) -> Unit,
    modifier: Modifier = Modifier
) {
    var curve by remember {
        mutableStateOf(initialCurve ?: SpeedCurve.createPreset(SpeedCurvePreset.CONSTANT))
    }
    var selectedPointIndex by remember { mutableIntStateOf(0) }

    fun updatePoints(newPoints: List<SpeedPoint>, preset: SpeedCurvePreset = SpeedCurvePreset.CUSTOM) {
        val sorted = newPoints.sortedBy { it.timeNormalized }
        val updated = curve.copy(
            points = sorted,
            preset = preset,
            isEnabled = true
        )
        curve = updated
        onApplyCurve(updated)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Preset Chips
        Text(text = "Speed Ramping Presets", color = VistaraTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(SpeedCurvePreset.entries) { p ->
                val isSelected = curve.preset == p
                Surface(
                    onClick = {
                        val newPresetCurve = SpeedCurve.createPreset(p)
                        curve = newPresetCurve
                        selectedPointIndex = 0
                        onApplyCurve(newPresetCurve)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) VistaraSecondary else VistaraDarkSurfaceHighlight,
                    modifier = Modifier.height(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                        Text(
                            text = p.displayName,
                            color = if (isSelected) Color.Black else VistaraTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Graph Canvas
        val maxSpeedDisplay = 4.0f
        val minSpeedDisplay = 0.2f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFF13131A), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(curve.points) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val w = size.width
                                val h = size.height
                                val normX = (offset.x / w).coerceIn(0f, 1f)

                                // Select closest point
                                val closestIdx = curve.points.indices.minByOrNull { idx ->
                                    val ptX = curve.points[idx].timeNormalized * w
                                    kotlin.math.abs(ptX - offset.x)
                                } ?: 0
                                selectedPointIndex = closestIdx
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val w = size.width
                                val h = size.height
                                val normX = (change.position.x / w).coerceIn(0f, 1f)
                                val normY = 1f - (change.position.y / h).coerceIn(0f, 1f)
                                val newSpeed = (minSpeedDisplay + normY * (maxSpeedDisplay - minSpeedDisplay)).coerceIn(0.1f, 10f)

                                val updatedList = curve.points.toMutableList()
                                if (selectedPointIndex in updatedList.indices) {
                                    val canMoveX = selectedPointIndex > 0 && selectedPointIndex < updatedList.size - 1
                                    val finalX = if (canMoveX) normX else updatedList[selectedPointIndex].timeNormalized
                                    updatedList[selectedPointIndex] = SpeedPoint(finalX, newSpeed)
                                    updatePoints(updatedList)
                                }
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height

                // Draw Horizontal Guideline for 1x Normal Speed
                val y1x = h * (1f - (1.0f - minSpeedDisplay) / (maxSpeedDisplay - minSpeedDisplay))
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(0f, y1x),
                    end = Offset(w, y1x),
                    strokeWidth = 1.5f
                )

                // Draw Smooth Evaluated Curve
                val curvePath = Path()
                val steps = 80
                for (s in 0..steps) {
                    val t = s.toFloat() / steps.toFloat()
                    val speed = SpeedCurveEvaluator.speedAt(curve.points, t)
                    val x = t * w
                    val y = h * (1f - (speed - minSpeedDisplay) / (maxSpeedDisplay - minSpeedDisplay)).coerceIn(0f, 1f)

                    if (s == 0) curvePath.moveTo(x, y) else curvePath.lineTo(x, y)
                }

                drawPath(
                    path = curvePath,
                    color = VistaraSecondary,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Draw Control Points
                for ((idx, pt) in curve.points.withIndex()) {
                    val ptX = pt.timeNormalized * w
                    val ptY = h * (1f - (pt.speed - minSpeedDisplay) / (maxSpeedDisplay - minSpeedDisplay)).coerceIn(0f, 1f)
                    val isSelected = idx == selectedPointIndex

                    drawCircle(
                        color = if (isSelected) Color.White else VistaraPrimary,
                        radius = if (isSelected) 8.dp.toPx() else 6.dp.toPx(),
                        center = Offset(ptX, ptY)
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = 2.dp.toPx(),
                        center = Offset(ptX, ptY)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Curve Toolbar: Add Point, Delete Point, Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Add Point
                Button(
                    onClick = {
                        val currentPoints = curve.points.sortedBy { it.timeNormalized }.toMutableList()
                        if (currentPoints.size < 8) {
                            currentPoints.add(SpeedPoint(0.5f, 1.0f))
                            updatePoints(currentPoints)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VistaraDarkSurfaceHighlight),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Point", tint = VistaraSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Point", color = VistaraTextPrimary, fontSize = 12.sp)
                }

                // Delete Point
                if (curve.points.size > 2 && selectedPointIndex in 1 until curve.points.size - 1) {
                    Button(
                        onClick = {
                            val currentPoints = curve.points.toMutableList()
                            currentPoints.removeAt(selectedPointIndex)
                            selectedPointIndex = (selectedPointIndex - 1).coerceAtLeast(0)
                            updatePoints(currentPoints)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Point", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            }

            // Reset Curve
            IconButton(
                onClick = {
                    val resetCurve = SpeedCurve.createPreset(SpeedCurvePreset.CONSTANT)
                    curve = resetCurve
                    onApplyCurve(resetCurve)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Curve", tint = VistaraTextMuted)
            }
        }
    }
}
