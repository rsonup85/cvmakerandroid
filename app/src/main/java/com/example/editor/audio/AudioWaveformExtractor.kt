// FIXED: Replaced invalid isActive with currentCoroutineContext().isActive
package com.example.editor.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

object AudioWaveformExtractor {
    private const val TAG = "WaveformExtractor"
    private const val SAMPLE_INTERVAL_MS = 50L

    suspend fun extractWaveform(
        context: Context,
        assetId: String,
        uriString: String,
        durationMs: Long
    ): WaveformData = withContext(Dispatchers.IO) {
        WaveformCache.get(assetId, durationMs)?.let { return@withContext it }

        WaveformCache.markExtracting(assetId)
        try {
            val totalBuckets = (durationMs / SAMPLE_INTERVAL_MS).toInt().coerceAtLeast(1)
            val bucketPeaks = FloatArray(totalBuckets)

            val uri = Uri.parse(uriString)
            val extractor = MediaExtractor()

            try {
                extractor.setDataSource(context, uri, null)
            } catch (e: Exception) {
                WaveformCache.markFinished(assetId)
                val fallback = WaveformData(assetId, FloatArray(totalBuckets) { 0.15f }, durationMs)
                WaveformCache.put(assetId, durationMs, fallback)
                return@withContext fallback
            }

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                extractor.release()
                WaveformCache.markFinished(assetId)
                val empty = WaveformData(assetId, FloatArray(totalBuckets) { 0.1f }, durationMs)
                WaveformCache.put(assetId, durationMs, empty)
                return@withContext empty
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: ""
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(audioFormat, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var isEOS = false

            // FIXED: proper coroutine cancellation check
            while (currentCoroutineContext().isActive && !isEOS) {
                val inIndex = codec.dequeueInputBuffer(5000L)
                if (inIndex >= 0) {
                    val inBuffer = codec.getInputBuffer(inIndex)
                    if (inBuffer != null) {
                        val sampleSize = extractor.readSampleData(inBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
                            val sampleTimeUs = extractor.sampleTime
                            codec.queueInputBuffer(inIndex, 0, sampleSize, sampleTimeUs, 0)
                            extractor.advance()
                        }
                    }
                }

                var outIndex = codec.dequeueOutputBuffer(bufferInfo, 5000L)
                while (outIndex >= 0) {
                    val outBuffer = codec.getOutputBuffer(outIndex)
                    if (outBuffer != null && bufferInfo.size > 0) {
                        val presentationTimeMs = bufferInfo.presentationTimeUs / 1000L
                        val bucketIdx = (presentationTimeMs / SAMPLE_INTERVAL_MS).toInt()
                            .coerceIn(0, totalBuckets - 1)

                        outBuffer.order(ByteOrder.LITTLE_ENDIAN)
                        val shortBuffer = outBuffer.asShortBuffer()
                        var maxAmp = 0
                        val step = (shortBuffer.remaining() / 16).coerceAtLeast(1)

                        var pos = 0
                        while (pos < shortBuffer.remaining()) {
                            val amp = abs(shortBuffer.get(pos).toInt())
                            if (amp > maxAmp) maxAmp = amp
                            pos += step
                        }

                        val normalized = (maxAmp.toFloat() / 32768f).coerceIn(0f, 1f)
                        bucketPeaks[bucketIdx] = max(bucketPeaks[bucketIdx], normalized)
                    }

                    codec.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEOS = true
                        break
                    }
                    outIndex = codec.dequeueOutputBuffer(bufferInfo, 0L)
                }
            }

            try {
                codec.stop()
                codec.release()
                extractor.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing audio codec", e)
            }

            val maxGlobal = bucketPeaks.maxOrNull()?.coerceAtLeast(0.01f) ?: 1f
            for (i in bucketPeaks.indices) {
                bucketPeaks[i] = (bucketPeaks[i] / maxGlobal).coerceIn(0.05f, 1.0f)
            }

            val result = WaveformData(assetId, bucketPeaks, durationMs, SAMPLE_INTERVAL_MS)
            WaveformCache.put(assetId, durationMs, result)
            WaveformCache.markFinished(assetId)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Waveform extraction failed for asset $assetId", e)
            WaveformCache.markFinished(assetId)
            val fallbackBuckets = (durationMs / SAMPLE_INTERVAL_MS).toInt().coerceAtLeast(1)
            val fallback = WaveformData(assetId, FloatArray(fallbackBuckets) { 0.15f }, durationMs)
            WaveformCache.put(assetId, durationMs, fallback)
            fallback
        }
    }
}