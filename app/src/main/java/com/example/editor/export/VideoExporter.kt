package com.example.editor.export

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Crop
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.common.FileUtils
import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.ExportResolution
import com.example.domain.model.Project
import com.example.domain.model.TransitionType
import com.example.editor.composition.CompositionEngine
import com.example.media.ThumbnailLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

sealed class ExportState {
    object Idle : ExportState()
    data class Exporting(
        val progress: Float,
        val currentClipIndex: Int,
        val totalClips: Int,
        val statusMessage: String = "Processing video…"
    ) : ExportState()
    data class Success(
        val mediaStoreUri: Uri,
        val localFile: File,
        val durationMs: Long
    ) : ExportState()
    data class Error(val message: String) : ExportState()
}

class VideoExporter(private val context: Context) {
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val isCancelled = AtomicBoolean(false)
    private var activeTransformer: Transformer? = null

    fun cancelExport() {
        isCancelled.set(true)
        activeTransformer?.cancel()
    }

    suspend fun exportProject(project: Project): Uri? = withContext(Dispatchers.IO) {
        val tracksById = project.tracks.associateBy { it.id }

        val videoClips = project.videoClips.filter { clip ->
            val track = tracksById[clip.trackId]
            track == null || track.isVisible
        }

        if (videoClips.isEmpty()) {
            _exportState.value = ExportState.Error("Project contains no visible video clips to export.")
            return@withContext null
        }

        isCancelled.set(false)
        _exportState.value = ExportState.Exporting(0.02f, 0, videoClips.size, "Configuring rendering engine…")

        val tempOutputFile = FileUtils.createTempExportFile(context, project.name)

        try {
            val (targetWidth, targetHeight) = calculateOutputDimensions(project)

            val loadedBitmaps = mutableMapOf<String, Bitmap>()
            for (overlay in project.imageLayers) {
                val asset = project.assets.find { it.id == overlay.assetId }
                if (asset != null && !loadedBitmaps.containsKey(asset.id)) {
                    runCatching {
                        context.contentResolver.openInputStream(Uri.parse(asset.uriString))?.use { input ->
                            BitmapFactory.decodeStream(input)
                        }
                    }.getOrNull()?.let { bmp ->
                        loadedBitmaps[asset.id] = bmp
                    }
                }
            }

            for (clip in videoClips) {
                val asset = project.assets.find { it.id == clip.assetId }
                if (asset != null && !loadedBitmaps.containsKey("head_${clip.id}")) {
                    val frame = ThumbnailLoader.getFrameThumbnail(
                        context,
                        asset.uriString,
                        clip.sourceStartMs + 100L,
                        targetWidth = targetWidth / 2,
                        targetHeight = targetHeight / 2
                    )
                    if (frame != null) {
                        loadedBitmaps["head_${clip.id}"] = frame
                    }
                }
            }

            val editedMediaItems = mutableListOf<EditedMediaItem>()
            for (clip in videoClips) {
                val asset = project.assets.find { it.id == clip.assetId }
                    ?: throw IllegalStateException("Missing asset for clip ${clip.id}")

                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(asset.uriString))
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(clip.sourceStartMs.coerceAtLeast(0L))
                            .setEndPositionMs((clip.sourceStartMs + clip.sourceDurationMs).coerceAtLeast(100L))
                            .build()
                    )
                    .build()

                val clipVideoEffects = mutableListOf<Effect>()

                if (clip.transform.isCropped) {
                    val leftNorm = -1f + 2f * clip.transform.cropLeft.coerceIn(0f, 0.49f)
                    val rightNorm = 1f - 2f * clip.transform.cropRight.coerceIn(0f, 0.49f)
                    val bottomNorm = -1f + 2f * clip.transform.cropBottom.coerceIn(0f, 0.49f)
                    val topNorm = 1f - 2f * clip.transform.cropTop.coerceIn(0f, 0.49f)
                    clipVideoEffects.add(Crop(leftNorm, rightNorm, bottomNorm, topNorm))
                }

                val scaleX = clip.transform.scale * (if (clip.transform.flipHorizontal) -1f else 1f)
                val scaleY = clip.transform.scale * (if (clip.transform.flipVertical) -1f else 1f)
                val rotationDeg = clip.transform.rotationDegrees.toFloat()

                if (scaleX != 1f || scaleY != 1f || rotationDeg != 0f) {
                    val transformEffect = ScaleAndRotateTransformation.Builder()
                        .setRotationDegrees(rotationDeg)
                        .setScale(scaleX, scaleY)
                        .build()
                    clipVideoEffects.add(transformEffect)
                }

                val colorEffects = com.example.editor.color.ColorProcessor.createMedia3Effects(clip.colorFilter)
                clipVideoEffects.addAll(colorEffects)

                val track = tracksById[clip.trackId]
                val isAudioMuted = clip.isMuted || (track?.isMuted == true) || clip.volume == 0f

                val audioProcessors = mutableListOf<androidx.media3.common.audio.AudioProcessor>()
                if (clip.speedCurve != null && clip.speedCurve.isEnabled) {
                    val effectiveSpeed = (clip.sourceDurationMs.toFloat() / clip.durationMs.toFloat()).coerceIn(0.1f, 10.0f)
                    val sonic = SonicAudioProcessor().apply {
                        setSpeed(effectiveSpeed)
                        setPitch(1.0f)
                    }
                    audioProcessors.add(sonic)
                } else if (clip.speed != 1.0f) {
                    val sonic = SonicAudioProcessor().apply {
                        setSpeed(clip.speed.coerceIn(0.1f, 10.0f))
                        setPitch(1.0f)
                    }
                    audioProcessors.add(sonic)
                }

                val editedItemBuilder = EditedMediaItem.Builder(mediaItem)
                    .setRemoveAudio(isAudioMuted)
                    .setEffects(Effects(audioProcessors, clipVideoEffects))

                editedMediaItems.add(editedItemBuilder.build())
            }

            val sequence = EditedMediaItemSequence(editedMediaItems)

            val presentation = Presentation.createForWidthAndHeight(
                targetWidth,
                targetHeight,
                Presentation.LAYOUT_SCALE_TO_FIT
            )

            // FIXED (BUG A): Ping-pong bitmap buffers for async render safety
            // Media3 Transformer may still be reading the previous frame's bitmap when
            // the next getBitmap() call arrives. Alternate between 2 buffers to avoid
            // frame tearing / glitches.
            val overlayBitmapA = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val overlayBitmapB = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val overlayCanvasA = Canvas(overlayBitmapA)
            val overlayCanvasB = Canvas(overlayBitmapB)
            val useBufferA = AtomicBoolean(true)

            val compositorOverlay: TextureOverlay = object : BitmapOverlay() {
                @OptIn(UnstableApi::class)
                override fun getBitmap(presentationTimeUs: Long): Bitmap {
                    // FIXED (BUG A): Atomic toggle to pick which buffer to draw on
                    val useA = useBufferA.getAndSet(!useBufferA.get())
                    val targetBitmap = if (useA) overlayBitmapA else overlayBitmapB
                    val overlayCanvas = if (useA) overlayCanvasA else overlayCanvasB

                    // Clear with PorterDuff.CLEAR for reliable transparency
                    overlayCanvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

                    val timelineMs = presentationTimeUs / 1000L
                    val compState = CompositionEngine.evaluateAt(project, timelineMs)

                    // A. Dual-Clip Transition Effects
                    val transition = compState.transition
                    if (transition != null && transition.type != TransitionType.NONE) {
                        val trans = transition

                        if (trans.overlayColorArgb != null) {
                            val dipPaint = Paint().apply { color = trans.overlayColorArgb }
                            overlayCanvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), dipPaint)
                        }

                        val incomingClip = compState.incomingMainVideo
                        val incomingHeadBmp = if (incomingClip != null) loadedBitmaps["head_${incomingClip.id}"] else null

                        if (incomingHeadBmp != null) {
                            val inAlpha = (trans.incomingAlpha * 255).toInt().coerceIn(0, 255)
                            if (inAlpha > 0) {
                                val inMatrix = Matrix().apply {
                                    val scaleX = targetWidth.toFloat() / incomingHeadBmp.width.toFloat() * trans.incomingScale
                                    val scaleY = targetHeight.toFloat() / incomingHeadBmp.height.toFloat() * trans.incomingScale
                                    postScale(scaleX, scaleY)
                                    postTranslate(trans.incomingOffsetX * targetWidth, trans.incomingOffsetY * targetHeight)
                                }
                                val inPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = inAlpha }

                                if (trans.wipeProgress > 0f) {
                                    overlayCanvas.save()
                                    overlayCanvas.clipRect(0f, 0f, targetWidth * trans.wipeProgress, targetHeight.toFloat())
                                    overlayCanvas.drawBitmap(incomingHeadBmp, inMatrix, inPaint)
                                    overlayCanvas.restore()
                                } else {
                                    overlayCanvas.drawBitmap(incomingHeadBmp, inMatrix, inPaint)
                                }
                            }
                        }
                    }

                    // B. Image Overlays
                    for (overlayItem in compState.activeImages) {
                        val asset = project.assets.find { it.id == overlayItem.assetId }
                        val bmp = loadedBitmaps[asset?.id] ?: continue
                        val evalTransform = compState.imageTransforms[overlayItem.id] ?: continue

                        val centerX = evalTransform.positionX * targetWidth
                        val centerY = evalTransform.positionY * targetHeight
                        val baseW = targetWidth * 0.45f * evalTransform.scaleX
                        val baseH = baseW * (bmp.height.toFloat() / bmp.width.toFloat())

                        val matrix = Matrix().apply {
                            postScale(baseW / bmp.width, baseH / bmp.height)
                            postRotate(evalTransform.rotationDegrees, baseW / 2f, baseH / 2f)
                            postTranslate(centerX - baseW / 2f, centerY - baseH / 2f)
                        }
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            alpha = (evalTransform.opacity * 255).toInt().coerceIn(0, 255)
                        }
                        overlayCanvas.drawBitmap(bmp, matrix, paint)
                    }

                    // C. Text Layers
                    for (textItem in compState.activeTexts) {
                        val props = textItem.textProperties ?: continue
                        val evalTransform = compState.textTransforms[textItem.id] ?: continue

                        val centerX = evalTransform.positionX * targetWidth
                        val centerY = evalTransform.positionY * targetHeight
                        val scaleFactor = (targetWidth / 360f).coerceAtLeast(1.8f)
                        val textSizePx = props.fontSizeSp * scaleFactor

                        val typeface = com.example.editor.font.FontManager.getTypeface(context, props.fontFamily)
                        val align = when (props.alignment) {
                            "LEFT" -> Paint.Align.LEFT
                            "RIGHT" -> Paint.Align.RIGHT
                            else -> Paint.Align.CENTER
                        }

                        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = runCatching { android.graphics.Color.parseColor(props.colorHex) }
                                .getOrDefault(android.graphics.Color.WHITE)
                            textSize = textSizePx
                            this.typeface = typeface
                            textAlign = align
                            alpha = (evalTransform.opacity * 255).toInt().coerceIn(0, 255)
                            if (props.hasShadow) {
                                setShadowLayer(10f, 4f, 4f, android.graphics.Color.BLACK)
                            }
                        }

                        overlayCanvas.save()
                        overlayCanvas.translate(centerX, centerY)
                        overlayCanvas.rotate(evalTransform.rotationDegrees)
                        overlayCanvas.scale(evalTransform.scaleX, evalTransform.scaleY)

                        val bounds = Rect()
                        textPaint.getTextBounds(props.text, 0, props.text.length, bounds)

                        if (props.backgroundColorHex != null) {
                            val bgPaint = Paint().apply {
                                color = runCatching { android.graphics.Color.parseColor(props.backgroundColorHex) }
                                    .getOrDefault(android.graphics.Color.argb(160, 0, 0, 0))
                            }
                            val padH = 20f * (targetWidth / 720f)
                            val padV = 10f * (targetHeight / 1280f)
                            val rectF = RectF(
                                -bounds.width() / 2f - padH,
                                -bounds.height() / 2f - padV,
                                bounds.width() / 2f + padH,
                                bounds.height() / 2f + padV
                            )
                            overlayCanvas.drawRoundRect(rectF, 12f, 12f, bgPaint)
                        }

                        val yOffset = bounds.height() / 2f - bounds.bottom

                        if (props.strokeWidth > 0f && !props.strokeColorHex.isNullOrBlank()) {
                            val strokePaint = Paint(textPaint).apply {
                                style = Paint.Style.STROKE
                                strokeWidth = props.strokeWidth * (targetWidth / 360f).coerceAtLeast(1.5f)
                                color = runCatching { android.graphics.Color.parseColor(props.strokeColorHex) }
                                    .getOrDefault(android.graphics.Color.BLACK)
                                alpha = (evalTransform.opacity * 255).toInt().coerceIn(0, 255)
                            }
                            overlayCanvas.drawText(props.text, 0f, yOffset, strokePaint)
                        }

                        overlayCanvas.drawText(props.text, 0f, yOffset, textPaint)
                        overlayCanvas.restore()
                    }

                    return targetBitmap
                }
            }

            val overlayEffect = OverlayEffect(listOf(compositorOverlay))
            val composition = Composition.Builder(listOf(sequence))
                .setEffects(Effects(emptyList(), listOf(presentation, overlayEffect)))
                .build()

            // FIXED (BUG B): Thread-safe atomic flags for export completion
            val exportCompleted = AtomicBoolean(false)
            val exportException = AtomicReference<ExportException?>(null)

            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        exportCompleted.set(true)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exception: ExportException
                    ) {
                        exportException.set(exception)
                    }
                })
                .build()

            activeTransformer = transformer

            withContext(Dispatchers.Main) {
                transformer.start(composition, tempOutputFile.absolutePath)
            }

            val progressHolder = ProgressHolder()
            // FIXED (BUG B): Use atomic getters in loop condition
            while (!exportCompleted.get() && exportException.get() == null && !isCancelled.get()) {
                withContext(Dispatchers.Main) {
                    val progressState = transformer.getProgress(progressHolder)
                    if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) {
                        val progressVal = (progressHolder.progress / 100f).coerceIn(0.05f, 0.95f)
                        _exportState.value = ExportState.Exporting(
                            progress = progressVal,
                            currentClipIndex = (progressVal * videoClips.size).toInt() + 1,
                            totalClips = videoClips.size,
                            statusMessage = "Compositing video (${(progressVal * 100).toInt()}%)…"
                        )
                    }
                }
                delay(120)
            }

            if (isCancelled.get()) {
                withContext(Dispatchers.Main) { transformer.cancel() }
                tempOutputFile.delete()
                _exportState.value = ExportState.Idle
                return@withContext null
            }

            // FIXED (BUG B): Use atomic getter for exception check
            val finalException = exportException.get()
            if (finalException != null) {
                finalException.printStackTrace()
                tempOutputFile.delete()
                _exportState.value = ExportState.Error("Export failed: ${finalException.message ?: "Encoder error"}")
                return@withContext null
            }

            _exportState.value = ExportState.Exporting(
                progress = 0.98f,
                currentClipIndex = videoClips.size,
                totalClips = videoClips.size,
                statusMessage = "Saving video to Gallery…"
            )

            val mediaStoreUri = FileUtils.saveVideoToGallery(context, tempOutputFile, project.name)
            if (mediaStoreUri == null) {
                tempOutputFile.delete()
                _exportState.value = ExportState.Error("Render completed but failed to register file in Android Gallery.")
                return@withContext null
            }

            _exportState.value = ExportState.Success(
                mediaStoreUri = mediaStoreUri,
                localFile = tempOutputFile,
                durationMs = project.totalDurationMs
            )
            mediaStoreUri
        } catch (e: CancellationException) {
            tempOutputFile.delete()
            _exportState.value = ExportState.Idle
            null
        } catch (e: Exception) {
            e.printStackTrace()
            tempOutputFile.delete()
            val userMsg = when {
                e.message?.contains("ENOSPC", true) == true -> "Device storage is full."
                else -> "Export encountered an error: ${e.localizedMessage ?: "Unknown media processing error"}"
            }
            _exportState.value = ExportState.Error(userMsg)
            null
        } finally {
            try {
                activeTransformer?.release()
            } catch (_: Exception) {}
            activeTransformer = null
        }
    }

    private fun calculateOutputDimensions(project: Project): Pair<Int, Int> {
        val (baseLongSide, baseShortSide) = when (project.exportSettings.resolution) {
            ExportResolution.RES_480P -> Pair(854, 480)
            ExportResolution.RES_720P -> Pair(1280, 720)
            ExportResolution.RES_1080P -> Pair(1920, 1080)
            ExportResolution.RES_1440P -> Pair(2560, 1440)
            ExportResolution.RES_4K -> Pair(3840, 2160)
        }

        return when (project.canvasRatio) {
            CanvasAspectRatio.RATIO_9_16 -> Pair(baseShortSide, baseLongSide)
            CanvasAspectRatio.RATIO_16_9 -> Pair(baseLongSide, baseShortSide)
            CanvasAspectRatio.RATIO_1_1 -> Pair(baseShortSide, baseShortSide)
            CanvasAspectRatio.RATIO_4_5 -> {
                val h = (baseShortSide * 5f / 4f).toInt()
                Pair(baseShortSide, h)
            }
            CanvasAspectRatio.RATIO_3_4 -> {
                val h = (baseShortSide * 4f / 3f).toInt()
                Pair(baseShortSide, h)
            }
            CanvasAspectRatio.RATIO_21_9 -> {
                val w = (baseShortSide * 21f / 9f).toInt()
                Pair(w, baseShortSide)
            }
            CanvasAspectRatio.ORIGINAL -> {
                val firstAsset = project.videoClips.firstOrNull()?.let { clip ->
                    project.assets.find { it.id == clip.assetId }
                }
                if (firstAsset != null && firstAsset.width > 0 && firstAsset.height > 0) {
                    val ratio = firstAsset.width.toFloat() / firstAsset.height.toFloat()
                    if (ratio >= 1f) {
                        Pair(baseLongSide, (baseLongSide / ratio).toInt().coerceAtLeast(360))
                    } else {
                        Pair((baseLongSide * ratio).toInt().coerceAtLeast(360), baseLongSide)
                    }
                } else {
                    Pair(baseShortSide, baseLongSide)
                }
            }
        }
    }

    fun resetState() {
        _exportState.value = ExportState.Idle
    }
}