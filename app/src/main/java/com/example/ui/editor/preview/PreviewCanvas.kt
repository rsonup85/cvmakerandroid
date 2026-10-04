package com.example.ui.editor.preview

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.common.TimeUtils
import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.ClipTransform
import com.example.domain.model.ImageLayerProperties
import com.example.domain.model.Project
import com.example.domain.model.TextLayerProperties
import com.example.domain.model.TransitionType
import com.example.editor.color.ColorProcessor
import com.example.editor.composition.CompositionEngine
import com.example.editor.font.FontManager
import com.example.media.ThumbnailLoader
import com.example.ui.theme.VistaraDarkBackground
import com.example.ui.theme.VistaraDarkSurface
import com.example.ui.theme.VistaraSecondary
import com.example.ui.theme.VistaraTextPrimary
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun PreviewCanvas(
    project: Project,
    player: ExoPlayer,
    playheadMs: Long,
    isPlaying: Boolean,
    isFullscreen: Boolean,
    selectedItemId: String?,
    onTogglePlayPause: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onSeek: (Long) -> Unit,
    onJumpToStart: () -> Unit,
    onJumpToEnd: () -> Unit,
    onUpdateTextProperties: (String, TextLayerProperties) -> Unit,
    onUpdateImageProperties: (String, ImageLayerProperties) -> Unit,
    onUpdateClipTransform: (ClipTransform) -> Unit,
    onSelectItem: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val compState = CompositionEngine.evaluateAt(project, playheadMs)
    val activeClip = compState.activeMainVideo
    val incomingClip = compState.incomingMainVideo

    // Determine target canvas aspect ratio
    val targetRatio = when (val ratioEnum = project.canvasRatio) {
        CanvasAspectRatio.ORIGINAL -> {
            val asset = project.assets.find { it.id == activeClip?.assetId }
            if (asset != null && asset.width > 0 && asset.height > 0) {
                asset.width.toFloat() / asset.height.toFloat()
            } else 9f / 16f
        }
        else -> ratioEnum.ratio ?: (9f / 16f)
    }

    val canvasBg = runCatching { Color(android.graphics.Color.parseColor(project.canvasBackgroundColorHex)) }
        .getOrDefault(Color.Black)

    // Cached frames for incoming transitions & overlay video clips
    val frameThumbnails = remember { mutableStateMapOf<String, android.graphics.Bitmap?>() }

    LaunchedEffect(incomingClip?.id) {
        if (incomingClip != null && !frameThumbnails.containsKey(incomingClip.id)) {
            val asset = project.assets.find { it.id == incomingClip.assetId }
            if (asset != null) {
                val frame = ThumbnailLoader.getFrameThumbnail(context, asset.uriString, incomingClip.sourceStartMs + 100L, 640, 360)
                frameThumbnails[incomingClip.id] = frame
            }
        }
    }

    Column(
        modifier = modifier
            .background(VistaraDarkBackground)
            .testTag("preview_canvas_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Video Preview Viewport Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(if (isFullscreen) 0.dp else 8.dp),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .aspectRatio(targetRatio)
                    .clip(RoundedCornerShape(if (isFullscreen) 0.dp else 12.dp))
                    .background(canvasBg),
                contentAlignment = Alignment.Center
            ) {
                val density = LocalDensity.current
                val boxWidth = maxWidth
                val boxHeight = maxHeight
                val boxWidthPx = with(density) { boxWidth.toPx() }
                val boxHeightPx = with(density) { boxHeight.toPx() }

                val mainTransform = compState.mainVideoTransform
                val transition = compState.transition

                // 1. Render Video Surface or Black Gap Surface (Bug 15)
                if (compState.isTimelineGap || activeClip == null) {
                    // Blank black composition for timeline gaps
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                } else {
                    // Physical Non-Destructive Crop Shape (Bug 3)
                    val cropShape = GenericShape { size, _ ->
                        val left = size.width * mainTransform.cropLeft.coerceIn(0f, 0.49f)
                        val top = size.height * mainTransform.cropTop.coerceIn(0f, 0.49f)
                        val right = size.width * (1f - mainTransform.cropRight.coerceIn(0f, 0.49f))
                        val bottom = size.height * (1f - mainTransform.cropBottom.coerceIn(0f, 0.49f))
                        addRect(androidx.compose.ui.geometry.Rect(left, top, right, bottom))
                    }

                    var videoGestureBase by remember(activeClip.id) { mutableStateOf<ClipTransform?>(null) }
                    var videoAccumZoom by remember(activeClip.id) { mutableFloatStateOf(1f) }
                    var videoAccumRot by remember(activeClip.id) { mutableFloatStateOf(0f) }
                    var videoAccumPanX by remember(activeClip.id) { mutableFloatStateOf(0f) }
                    var videoAccumPanY by remember(activeClip.id) { mutableFloatStateOf(0f) }

                    // Main Video Surface
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                this.player = player
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { view ->
                            if (view.player != player) {
                                view.player = player
                            }
                        },
                        // FIXED: Prevent ExoPlayer view leak on recomposition
                        onRelease = { view ->
                            view.player = null
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(cropShape)
                            .graphicsLayer {
                                rotationZ = mainTransform.rotationDegrees
                                scaleX = mainTransform.scaleX * (transition?.outgoingScale ?: 1f)
                                scaleY = mainTransform.scaleY * (transition?.outgoingScale ?: 1f)
                                translationX = (mainTransform.positionX - 0.5f) * boxWidthPx + ((transition?.outgoingOffsetX ?: 0f) * boxWidthPx)
                                translationY = (mainTransform.positionY - 0.5f) * boxHeightPx + ((transition?.outgoingOffsetY ?: 0f) * boxHeightPx)
                                alpha = (mainTransform.opacity * (transition?.outgoingAlpha ?: 1f)).coerceIn(0f, 1f)
                            }
                            .drawWithContent {
                                val matrix = ColorProcessor.createComposeColorMatrix(activeClip.colorFilter)
                                if (!activeClip.colorFilter.isDefault) {
                                    val paint = androidx.compose.ui.graphics.Paint().apply {
                                        colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(matrix)
                                    }
                                    drawContext.canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), paint)
                                    drawContent()
                                    drawContext.canvas.restore()
                                } else {
                                    drawContent()
                                }
                            }
                            .pointerInput(activeClip.id) {
                                detectTransformGestures { _, pan, zoom, rotationChange ->
                                    onSelectItem(activeClip.id)
                                    if (videoGestureBase == null) {
                                        videoGestureBase = activeClip.transform
                                        videoAccumZoom = 1f
                                        videoAccumRot = 0f
                                        videoAccumPanX = 0f
                                        videoAccumPanY = 0f
                                    }
                                    videoAccumZoom *= zoom
                                    videoAccumRot += rotationChange
                                    videoAccumPanX += pan.x
                                    videoAccumPanY += pan.y

                                    val base = videoGestureBase ?: activeClip.transform
                                    val newScale = (base.scale * videoAccumZoom).coerceIn(0.2f, 4f)
                                    val rawRot = base.rotationDegrees + videoAccumRot
                                    val newRot = (((rawRot % 360f) + 360f) % 360f).roundToInt()
                                    val newOffsetX = base.offsetX + videoAccumPanX / boxWidthPx
                                    val newOffsetY = base.offsetY + videoAccumPanY / boxHeightPx
                                    onUpdateClipTransform(
                                        base.copy(
                                            scale = newScale,
                                            rotationDegrees = newRot,
                                            offsetX = newOffsetX,
                                            offsetY = newOffsetY
                                        )
                                    )
                                }
                            }
                    )
                }

                // 2. Render Incoming Clip during Real Dual-Clip Transition (Bug 1)
                if (transition != null && incomingClip != null && compState.incomingVideoTransform != null) {
                    val inTrans = compState.incomingVideoTransform
                    val inBitmap = frameThumbnails[incomingClip.id]

                    if (inBitmap != null && transition.incomingAlpha > 0f) {
                        val wipeClipShape = if (transition.wipeProgress > 0f) {
                            GenericShape { size, _ ->
                                addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width * transition.wipeProgress, size.height))
                            }
                        } else null

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (wipeClipShape != null) Modifier.clip(wipeClipShape) else Modifier)
                                .graphicsLayer {
                                    alpha = transition.incomingAlpha.coerceIn(0f, 1f)
                                    translationX = transition.incomingOffsetX * boxWidthPx
                                    translationY = transition.incomingOffsetY * boxHeightPx
                                    scaleX = transition.incomingScale
                                    scaleY = transition.incomingScale
                                }
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = inBitmap.asImageBitmap(),
                                contentDescription = "Incoming Clip",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Optional Transition Color Overlay (Dip to Black / Dip to White)
                    if (transition.overlayColorArgb != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(transition.overlayColorArgb))
                        )
                    }
                }

                // 3. Render Active Image Overlays (Stickers / Graphics) with Keyframes & Transforms
                for (overlay in compState.activeImages) {
                    val asset = project.assets.find { it.id == overlay.assetId }
                    val evalTransform = compState.imageTransforms[overlay.id]
                    val props = overlay.imageProperties
                    val isSelected = selectedItemId == overlay.id

                    if (asset != null && evalTransform != null && props != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset {
                                    IntOffset(
                                        x = ((evalTransform.positionX - 0.5f) * boxWidthPx).roundToInt(),
                                        y = ((evalTransform.positionY - 0.5f) * boxHeightPx).roundToInt()
                                    )
                                }
                                .graphicsLayer {
                                    rotationZ = evalTransform.rotationDegrees
                                    alpha = evalTransform.opacity
                                    scaleX = evalTransform.scaleX
                                    scaleY = evalTransform.scaleY
                                }
                                .pointerInput(overlay.id) {
                                    detectTransformGestures { _, pan, zoom, rotationChange ->
                                        onSelectItem(overlay.id)
                                        val newX = (props.positionX + pan.x / boxWidthPx).coerceIn(0f, 1f)
                                        val newY = (props.positionY + pan.y / boxHeightPx).coerceIn(0f, 1f)
                                        val newScale = (props.scale * zoom).coerceIn(0.2f, 4f)
                                        val newRot = (props.rotationDegrees + rotationChange) % 360f
                                        onUpdateImageProperties(
                                            overlay.id,
                                            props.copy(
                                                positionX = newX,
                                                positionY = newY,
                                                scale = newScale,
                                                rotationDegrees = (newRot + 360f) % 360f
                                            )
                                        )
                                    }
                                }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, VistaraSecondary, RoundedCornerShape(8.dp))
                                    } else Modifier
                                )
                                .padding(if (isSelected) 4.dp else 0.dp)
                        ) {
                            AsyncImage(
                                model = asset.uriString,
                                contentDescription = "Overlay",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size((boxWidth * 0.45f) * props.scale)
                            )
                        }
                    }
                }

                // 4. Render Active Text Layers with Full Typography, Real Stroke & Shadow (Bug 18)
                for (textItem in compState.activeTexts) {
                    val props = textItem.textProperties ?: continue
                    val evalTransform = compState.textTransforms[textItem.id] ?: continue
                    val isSelected = selectedItemId == textItem.id

                    val scaleFactor = (boxWidthPx / 360f).coerceAtLeast(1.5f)
                    val fontSizePx = props.fontSizeSp * scaleFactor
                    val typeface = FontManager.getTypeface(context, props.fontFamily)

                    var textGestureBase by remember(textItem.id) { mutableStateOf<TextLayerProperties?>(null) }
                    var textAccumZoom by remember(textItem.id) { mutableFloatStateOf(1f) }
                    var textAccumRot by remember(textItem.id) { mutableFloatStateOf(0f) }
                    var textAccumPanX by remember(textItem.id) { mutableFloatStateOf(0f) }
                    var textAccumPanY by remember(textItem.id) { mutableFloatStateOf(0f) }

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset {
                                IntOffset(
                                    x = ((evalTransform.positionX - 0.5f) * boxWidthPx).roundToInt(),
                                    y = ((evalTransform.positionY - 0.5f) * boxHeightPx).roundToInt()
                                )
                            }
                            .graphicsLayer {
                                rotationZ = evalTransform.rotationDegrees
                                scaleX = evalTransform.scaleX
                                scaleY = evalTransform.scaleY
                                alpha = evalTransform.opacity
                            }
                            .pointerInput(textItem.id) {
                                detectTransformGestures { _, pan, zoom, rotationChange ->
                                    onSelectItem(textItem.id)
                                    if (textGestureBase == null) {
                                        textGestureBase = props
                                        textAccumZoom = 1f
                                        textAccumRot = 0f
                                        textAccumPanX = 0f
                                        textAccumPanY = 0f
                                    }
                                    textAccumZoom *= zoom
                                    textAccumRot += rotationChange
                                    textAccumPanX += pan.x
                                    textAccumPanY += pan.y

                                    val base = textGestureBase ?: props
                                    val newScale = (base.scale * textAccumZoom).coerceIn(0.2f, 4.0f)
                                    val rawRot = base.rotationDegrees + textAccumRot
                                    val newRot = (((rawRot % 360f) + 360f) % 360f)
                                    val newX = (base.positionX + textAccumPanX / boxWidthPx).coerceIn(0f, 1f)
                                    val newY = (base.positionY + textAccumPanY / boxHeightPx).coerceIn(0f, 1f)

                                    onUpdateTextProperties(
                                        textItem.id,
                                        base.copy(
                                            positionX = newX,
                                            positionY = newY,
                                            scale = newScale,
                                            rotationDegrees = newRot
                                        )
                                    )
                                }
                            }
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, VistaraSecondary, RoundedCornerShape(8.dp))
                                } else Modifier
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        // Precise native Canvas rendering for Text with Outline Stroke & Shadow (Bug 18)
                        Canvas(modifier = Modifier.size(width = (boxWidth * 0.85f), height = 80.dp)) {
                            drawContext.canvas.nativeCanvas.apply {
                                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = runCatching { android.graphics.Color.parseColor(props.colorHex) }
                                        .getOrDefault(android.graphics.Color.WHITE)
                                    textSize = fontSizePx
                                    this.typeface = typeface
                                    textAlign = when (props.alignment) {
                                        "LEFT" -> Paint.Align.LEFT
                                        "RIGHT" -> Paint.Align.RIGHT
                                        else -> Paint.Align.CENTER
                                    }
                                    if (props.hasShadow) {
                                        setShadowLayer(8f, 3f, 3f, android.graphics.Color.BLACK)
                                    }
                                }

                                val bounds = Rect()
                                textPaint.getTextBounds(props.text, 0, props.text.length, bounds)
                                val cx = size.width / 2f
                                val cy = size.height / 2f + bounds.height() / 2f - bounds.bottom

                                // Background box if enabled
                                if (props.backgroundColorHex != null) {
                                    val bgPaint = Paint().apply {
                                        color = runCatching { android.graphics.Color.parseColor(props.backgroundColorHex) }
                                            .getOrDefault(android.graphics.Color.argb(160, 0, 0, 0))
                                    }
                                    val padH = 16f
                                    val padV = 8f
                                    val rectF = RectF(
                                        cx - bounds.width() / 2f - padH,
                                        cy - bounds.height() - padV,
                                        cx + bounds.width() / 2f + padH,
                                        cy + padV
                                    )
                                    drawRoundRect(rectF, 12f, 12f, bgPaint)
                                }

                                // Text Stroke Rendering
                                if (props.strokeWidth > 0f && !props.strokeColorHex.isNullOrBlank()) {
                                    val strokePaint = Paint(textPaint).apply {
                                        style = Paint.Style.STROKE
                                        strokeWidth = props.strokeWidth * scaleFactor
                                        color = runCatching { android.graphics.Color.parseColor(props.strokeColorHex) }
                                            .getOrDefault(android.graphics.Color.BLACK)
                                    }
                                    drawText(props.text, cx, cy, strokePaint)
                                }

                                // Text Fill
                                drawText(props.text, cx, cy, textPaint)
                            }
                        }
                    }
                }
            }
        }

        // Live Scrub Bar & Transport Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            val maxMs = project.totalDurationMs.coerceAtLeast(1000L).toFloat()
            Slider(
                value = playheadMs.coerceIn(0L, project.totalDurationMs).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..maxMs,
                colors = SliderDefaults.colors(
                    thumbColor = VistaraSecondary,
                    activeTrackColor = VistaraSecondary,
                    inactiveTrackColor = Color.DarkGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .testTag("preview_scrubber_slider")
            )

            // Transport Control Buttons Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Jump to Start (0:00)
                IconButton(
                    onClick = onJumpToStart,
                    modifier = Modifier.size(36.dp).testTag("jump_start_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Jump to Start",
                        tint = VistaraTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Play / Pause Circle
                Surface(
                    onClick = onTogglePlayPause,
                    shape = CircleShape,
                    color = VistaraDarkSurface,
                    modifier = Modifier.size(42.dp).testTag("play_pause_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = VistaraSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Jump to End
                IconButton(
                    onClick = onJumpToEnd,
                    modifier = Modifier.size(36.dp).testTag("jump_end_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Jump to End",
                        tint = VistaraTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Time Indicator (00:01.24 / 00:15.00)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VistaraDarkSurface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = TimeUtils.formatTimeDetailed(playheadMs),
                            color = VistaraSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " / ${TimeUtils.formatTimeDetailed(project.totalDurationMs)}",
                            color = VistaraTextPrimary.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }

                // Fullscreen Toggle
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.size(36.dp).testTag("fullscreen_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = VistaraTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
