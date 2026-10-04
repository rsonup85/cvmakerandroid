package com.example.editor.player

import android.graphics.Bitmap
import android.util.LruCache
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe, bounded-memory LRU cache for video preview frames during active timeline scrubbing.
 * Uses normalized time buckets to eliminate seek storms and deliver instantaneous visual feedback.
 */
class TimelineFrameCache(
    maxSizeBytes: Long = 32L * 1024L * 1024L // 32MB max heap allocation
) {
    private val bucketSizeMs: Long = 100L // 10fps scrubbing resolution bucket

    private val lruCache = object : LruCache<String, Bitmap>((maxSizeBytes / 1024L).toInt()) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return (bitmap.byteCount / 1024).coerceAtLeast(1)
        }
    }

    private val timestampsByAsset = ConcurrentHashMap<String, MutableSet<Long>>()

    private fun buildKey(assetId: String, bucketTimeMs: Long): String {
        return "${assetId}_bucket_${bucketTimeMs}"
    }

    fun getFrame(assetId: String, timelineTimeMs: Long): Bitmap? {
        val bucket = (timelineTimeMs / bucketSizeMs) * bucketSizeMs
        val exactKey = buildKey(assetId, bucket)

        synchronized(lruCache) {
            val exactMatch = lruCache.get(exactKey)
            if (exactMatch != null && !exactMatch.isRecycled) {
                return exactMatch
            }

            // Fallback: Nearest frame lookup within +/- 300ms
            val knownBuckets = timestampsByAsset[assetId]
            if (!knownBuckets.isNullOrEmpty()) {
                val nearest = knownBuckets.minByOrNull { kotlin.math.abs(it - bucket) }
                if (nearest != null && kotlin.math.abs(nearest - bucket) <= 300L) {
                    val candidate = lruCache.get(buildKey(assetId, nearest))
                    if (candidate != null && !candidate.isRecycled) {
                        return candidate
                    }
                }
            }
        }
        return null
    }

    fun putFrame(assetId: String, timelineTimeMs: Long, bitmap: Bitmap) {
        if (bitmap.isRecycled) return
        val bucket = (timelineTimeMs / bucketSizeMs) * bucketSizeMs
        val key = buildKey(assetId, bucket)

        synchronized(lruCache) {
            lruCache.put(key, bitmap)
            timestampsByAsset.computeIfAbsent(assetId) { ConcurrentHashMap.newKeySet() }.add(bucket)
        }
    }

    fun clear() {
        synchronized(lruCache) {
            lruCache.evictAll()
            timestampsByAsset.clear()
        }
    }

    fun invalidateAsset(assetId: String) {
        synchronized(lruCache) {
            val buckets = timestampsByAsset.remove(assetId) ?: return
            for (bucket in buckets) {
                lruCache.remove(buildKey(assetId, bucket))
            }
        }
    }
}
