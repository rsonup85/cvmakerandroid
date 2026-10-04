package com.example.editor.ai

import com.example.BuildConfig
import com.example.domain.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Clean architectural extension points for future AI-assisted editing modules (Phase 6):
 * - AI Auto Cut
 * - AI Scene & Highlight Detection
 * - AI Smart Captions & Subtitles
 * - AI Silence Removal
 * - AI Voice Enhancement & Beat Sync
 *
 * Security Note:
 * Any direct client-side Gemini calls use the key injected securely via BuildConfig from the AI Studio Secrets panel.
 * For production deployment, requests should route through a secure server-side proxy backend.
 */
interface VideoAiService {
    suspend fun analyzeVideoScenes(project: Project): Result<List<String>>
    suspend fun generateSmartCaptions(project: Project): Result<List<String>>
    fun isAiConfigured(): Boolean
}

class GeminiVideoAiServiceImpl : VideoAiService {

    private fun getApiKey(): String {
        return try {
            // Injected via Secrets Gradle Plugin from AI Studio secrets
            BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    override fun isAiConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() && !key.contains("MY_GEMINI_API_KEY")
    }

    override suspend fun analyzeVideoScenes(project: Project): Result<List<String>> = withContext(Dispatchers.IO) {
        if (!isAiConfigured()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured in Secrets panel."))
        }
        // Extension point ready for model call without exposing fake demo results
        Result.success(emptyList())
    }

    override suspend fun generateSmartCaptions(project: Project): Result<List<String>> = withContext(Dispatchers.IO) {
        if (!isAiConfigured()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured in Secrets panel."))
        }
        // Extension point ready for audio transcript / caption generation
        Result.success(emptyList())
    }
}
