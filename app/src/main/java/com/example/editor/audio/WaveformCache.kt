package com.example.editor.audio

import android.util.LruCache
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe memory cache for extracted audio waveforms with bounded memory.
 */
object WaveformCache {
    private val lruCache = object : LruCache<String, WaveformData>(50) {
        override fun sizeOf(key: String, value: WaveformData): Int {
            return 1
        }
    }

    private val inFlightExtractions = ConcurrentHashMap<String, Boolean>()

    fun get(assetId: String, durationMs: Long): WaveformData? {
        val key = "${assetId}_$durationMs"
        synchronized(lruCache) {
            return lruCache.get(key)
        }
    }

    fun put(assetId: String, durationMs: Long, data: WaveformData) {
        val key = "${assetId}_$durationMs"
        synchronized(lruCache) {
            lruCache.put(key, data)
        }
    }

    fun isExtracting(assetId: String): Boolean {
        return inFlightExtractions[assetId] == true
    }

    fun markExtracting(assetId: String) {
        inFlightExtractions[assetId] = true
    }

    fun markFinished(assetId: String) {
        inFlightExtractions.remove(assetId)
    }

    fun clear() {
        synchronized(lruCache) {
            lruCache.evictAll()
            inFlightExtractions.clear()
        }
    }
}
