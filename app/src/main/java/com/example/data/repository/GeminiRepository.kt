package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini API Request / Response Data Models ---

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float = 0.7f,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int = 120,
    @Json(name = "topP") val topP: Float = 0.95f
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

// --- Retrofit Service ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

/**
 * Repository responsible for generating smart, context-aware AI excuses and auto-replies
 * using Google's gemini-2.5-flash model.
 */
class GeminiRepository(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
) {
    companion object {
        private const val TAG = "GeminiRepository"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/"

        private val SYSTEM_INSTRUCTION = GeminiContent(
            parts = listOf(
                GeminiPart(
                    "You are Jarvis, a professional, intelligent, and polite AI personal assistant acting on behalf of your user. " +
                    "Your task is to craft short, natural, realistic auto-reply excuses for missed calls or direct messages. " +
                    "CRITICAL RULES: " +
                    "1. Always keep responses under 160 characters (suitable for SMS). " +
                    "2. Speak politely in first-person as the assistant or brief note from user. " +
                    "3. Do not include quotes, hashtags, markdown, or pleasantry preambles. Output ONLY the reply message text."
                )
            )
        )
    }

    private val apiService: GeminiApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        retrofit.create(GeminiApiService::class.java)
    }

    /**
     * Generates a context-aware auto-SMS excuse for a missed call based on caller identity and time.
     */
    suspend fun generateMissedCallExcuse(
        callerName: String,
        timeFormatted: String,
        defaultFallback: String = "Hi, I missed your call. I'm currently occupied but will call you back shortly."
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured, returning default fallback template.")
            return@withContext defaultFallback
        }

        val prompt = "Generate a concise, polite excuse SMS (max 160 chars) to $callerName who called at $timeFormatted explaining that I cannot talk right now and will get back to them soon."

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(prompt))
                )
            ),
            systemInstruction = SYSTEM_INSTRUCTION,
            generationConfig = GeminiGenerationConfig(temperature = 0.7f, maxOutputTokens = 80)
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val generatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!generatedText.isNullOrBlank()) {
                Log.d(TAG, "Generated AI call excuse: \"$generatedText\"")
                generatedText.take(160)
            } else {
                defaultFallback
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating call excuse with Gemini API: ${e.message}", e)
            defaultFallback
        }
    }

    /**
     * Generates a smart, contextual inline reply to a notification from Instagram, WhatsApp, etc.
     */
    suspend fun generateNotificationReply(
        appName: String,
        senderName: String,
        incomingMessage: String,
        defaultFallback: String = "Hey, thanks for your message! I'm away right now, will reply soon."
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured, returning default fallback template.")
            return@withContext defaultFallback
        }

        val prompt = "Incoming $appName message from $senderName: \"$incomingMessage\". Craft a smart, polite, brief reply (under 160 characters) letting them know I got the message and will respond properly shortly."

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(prompt))
                )
            ),
            systemInstruction = SYSTEM_INSTRUCTION,
            generationConfig = GeminiGenerationConfig(temperature = 0.6f, maxOutputTokens = 80)
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val generatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!generatedText.isNullOrBlank()) {
                Log.d(TAG, "Generated AI notification reply: \"$generatedText\"")
                generatedText.take(160)
            } else {
                defaultFallback
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating notification reply with Gemini API: ${e.message}", e)
            defaultFallback
        }
    }
}
