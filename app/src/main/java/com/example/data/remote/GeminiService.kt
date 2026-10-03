package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    data class GeminiReq(
        val contents: List<GeminiContent>
    )

    data class GeminiContent(
        val parts: List<GeminiPart>
    )

    data class GeminiPart(
        val text: String
    )

    data class GeminiResp(
        val candidates: List<GeminiCandidate>?
    )

    data class GeminiCandidate(
        val content: GeminiContent?
    )

    private val reqAdapter = moshi.adapter(GeminiReq::class.java)
    private val respAdapter = moshi.adapter(GeminiResp::class.java)

    suspend fun improveCareerObjective(original: String, targetRole: String): String = withContext(Dispatchers.IO) {
        val prompt = """
            You are a professional resume coach. Please enhance the following Career Objective for a resume targeting the role of "$targetRole".
            Make it concise (2 to 3 sentences maximum), impactful, confident, and ATS-friendly with strong action words.
            Do not include explanations or markdown headers. Return ONLY the polished objective text.
            
            Original Text:
            $original
        """.trimIndent()
        callGemini(prompt, fallback = generateLocalObjectiveImprovement(original, targetRole))
    }

    suspend fun improveWorkExperience(original: String, jobTitle: String): String = withContext(Dispatchers.IO) {
        val prompt = """
            You are an executive resume writer. Enhance the following work responsibilities for a "$jobTitle".
            Use strong action verbs (e.g., spearheaded, engineered, orchestrated), bullet points, and quantify results where possible.
            Keep it professional, grammatically flawless, and concise. Return ONLY the improved bullet points or paragraph.
            
            Original Text:
            $original
        """.trimIndent()
        callGemini(prompt, fallback = generateLocalExperienceImprovement(original, jobTitle))
    }

    suspend fun improveAboutMe(original: String, values: String): String = withContext(Dispatchers.IO) {
        val prompt = """
            You are a sensitive, cultured matrimonial profile advisor. Rewrite the following 'About Me / Expectations' for an Indian matrimonial biodata.
            Tone: Polite, humble, family-oriented yet progressive with $values values.
            Ensure excellent English grammar and warm, graceful phrasing.
            Do not include any headers or meta commentary. Return ONLY the revised profile text.
            
            Original Text:
            $original
        """.trimIndent()
        callGemini(prompt, fallback = generateLocalAboutMeImprovement(original, values))
    }

    private fun callGemini(prompt: String, fallback: String): String {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d(TAG, "GEMINI_API_KEY is unset or placeholder. Using intelligent local enhancement.")
            return fallback
        }

        return try {
            val reqPayload = GeminiReq(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt))
                    )
                )
            )
            val jsonBody = reqAdapter.toJson(reqPayload)
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Gemini API error code: ${response.code}. Using fallback.")
                    return fallback
                }
                val bodyStr = response.body?.string() ?: return fallback
                val parsed = respAdapter.fromJson(bodyStr)
                val text = parsed?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    text.trim()
                } else {
                    fallback
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to Gemini API", e)
            fallback
        }
    }

    // Local high-quality enhancement fallbacks if offline or key is unconfigured
    private fun generateLocalObjectiveImprovement(original: String, targetRole: String): String {
        val role = if (targetRole.isNotBlank()) targetRole else "Professional"
        if (original.isBlank()) {
            return "Dynamic and results-driven $role with a proven track record of excellence, seeking to leverage technical competence and strategic problem-solving skills to drive growth in a forward-thinking organization."
        }
        val cleaned = original.trim().removeSuffix(".")
        return "Results-oriented $role with a dedicated background in delivering high-impact solutions. $cleaned, with a strong focus on driving continuous improvement, collaborative leadership, and sustainable organizational growth."
    }

    private fun generateLocalExperienceImprovement(original: String, jobTitle: String): String {
        if (original.isBlank()) {
            return "• Led core deliverables and cross-functional project execution for the team.\n• Streamlined operational processes, improving overall delivery efficiency.\n• Collaborated closely with stakeholders to ensure strict quality standards."
        }
        val lines = original.split("\n").filter { it.isNotBlank() }
        return lines.joinToString("\n") { line ->
            val trimmed = line.trim().removePrefix("•").removePrefix("-").trim()
            "• Spearheaded key initiatives: $trimmed with a consistent emphasis on quality and timely milestones."
        }
    }

    private fun generateLocalAboutMeImprovement(original: String, values: String): String {
        if (original.isBlank()) {
            return "I am a simple, down-to-earth person who believes in strong family values, mutual respect, and continuous self-growth. Looking for an understanding partner who shares an appreciation for traditional roots while embracing modern aspirations."
        }
        val cleaned = original.trim().removeSuffix(".")
        return "Raised with deep-rooted $values values, I consider myself thoughtful, grounded, and ambitious. $cleaned. I believe that marriage is a harmonious journey built upon trust, open communication, and shared laughter."
    }
}
