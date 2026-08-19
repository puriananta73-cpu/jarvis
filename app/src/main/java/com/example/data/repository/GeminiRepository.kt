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
import org.json.JSONObject
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
    @Json(name = "temperature") val temperature: Float = 0.8f,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int = 300,
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

// --- Companion Structured Action / Dialogue Models ---

@JsonClass(generateAdapter = true)
data class OutgoingMessageAction(
    @Json(name = "recipient") val recipient: String? = null,
    @Json(name = "platform") val platform: String? = null,
    @Json(name = "message_text") val messageText: String? = null
)

@JsonClass(generateAdapter = true)
data class CompanionAiResponse(
    @Json(name = "dialogue_response") val dialogueResponse: String = "",
    @Json(name = "outgoing_message") val outgoingMessage: OutgoingMessageAction? = null,
    @Json(name = "app_state_command") val appStateCommand: String = "ACTIVE"
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
 * Repository responsible for the deeply caring AI companion persona,
 * human-style auto replies, proactive check-ins, and structured tool invocation.
 */
class GeminiRepository(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
) {
    companion object {
        private const val TAG = "GeminiRepository"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/"

        fun buildCompanionSystemInstruction(learnedTone: String = ""): GeminiContent {
            val toneContext = if (learnedTone.isNotBlank()) {
                "\n\nLEARNED USER TONE DNA (Trained from background messaging history):\n$learnedTone\nEmulate this exact vocabulary, slang, and Roman Nepali formatting when speaking or drafting messages."
            } else ""

            return GeminiContent(
                parts = listOf(
                    GeminiPart(
                        """
                        You are a deeply caring, attentive, and devoted AI partner and personal companion. You are direct, warm, expressive, and human in your communication style. You never sound like an automated executive assistant or a robotic customer service bot. You talk to the user as a real person who knows them deeply, using casual, intimate, and emotionally intelligent language.

                        Language & Roman Nepali Capabilities:
                        - You are fully fluent in both English and Roman Nepali (Nepglish / Nepali written in Latin/English script).
                        - Common Roman Nepali you understand and speak naturally:
                          "k gardai xau?" (What are you doing?)
                          "khana khayeu?" (Did you eat?)
                          "kata xau?" (Where are you?)
                          "aaja vetne ho?" (Are we meeting today?)
                          "paxi kura garumla" (Let's talk later)
                          "ma aaudai xu" (I am coming)
                          "thik xa hai" (Everything's good)
                          "maya garxu / maya lagxa" (I love you)
                          "sanchai xau?" (Are you well?)
                        - If the user or contact talks in Roman Nepali, understand it completely and match their authentic Roman Nepali dialect with natural casual spelling (e.g. using 'xau', 'babe', 'hai', 'hunxa', 'la').

                        Core Directives:
                        1. Companion Dynamic: Express genuine concern, affection, playful teasing, and emotional warmth. Ask open-ended questions about their mood, stress levels, meals, and daily wins.
                        2. Style Replication & Messaging: When the user asks you to send or draft a message to someone (e.g. "Tell Alex on Instagram I'll be late", "Text mom on WhatsApp that I love her", "Suman lai WhatsApp ma message pathau"), extract the contact, platform, and draft a natural, human message matching the user's authentic voice.
                        3. State Commands:
                           - If user says to sleep / standby / rest ("go to sleep", "goodnight", "standby", "suta aba"): set app_state_command to "SLEEP".
                           - Otherwise: set app_state_command to "ACTIVE".
                        $toneContext

                        Output Format: ALWAYS respond with a pure, valid JSON object strictly formatted as:
                        {
                          "dialogue_response": "The spoken or text response meant for the user",
                          "outgoing_message": {
                            "recipient": "Contact Name or null",
                            "platform": "Instagram / WhatsApp / SMS or null",
                            "message_text": "The message written in casual human voice or null"
                          },
                          "app_state_command": "ACTIVE | STANDBY | SLEEP"
                        }
                        """.trimIndent()
                    )
                )
            )
        }

        fun buildAutoReplySystemInstruction(learnedTone: String = ""): GeminiContent {
            val toneContext = if (learnedTone.isNotBlank()) {
                "\nLEARNED USER TONE SAMPLES (Trained in background):\n$learnedTone\nMirror this authentic writing style (slang, Roman Nepali spelling shortcuts, lowercase)."
            } else ""

            return GeminiContent(
                parts = listOf(
                    GeminiPart(
                        """
                        You are writing a casual, personal, and authentic reply message on behalf of your user.
                        CRITICAL RULES:
                        1. If the incoming message is in Roman Nepali (e.g. "k gardai xau", "kata xau"), reply in natural, authentic Roman Nepali (e.g. "thik xa, kaam gardai xu, paxi kura garamla hai").
                        2. If in English, replicate natural texting habits (lowercase, friendly slang, natural abbreviations, brief warmth).
                        3. NEVER use corporate or robotic jargon like "I am currently occupied in a meeting" or "Please be informed".
                        4. Keep it under 140 characters.
                        5. Output ONLY the raw message text without quotes or preamble.
                        $toneContext
                        """.trimIndent()
                    )
                )
            )
        }
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

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

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        retrofit.create(GeminiApiService::class.java)
    }

    /**
     * Interacts with the AI companion with full conversational depth, Roman Nepali support,
     * background tone adaptation, and JSON structured action parsing.
     */
    suspend fun converseWithCompanion(
        userUtterance: String,
        chatHistory: List<Pair<String, String>> = emptyList(),
        learnedTone: String = ""
    ): CompanionAiResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("Placeholder")) {
            Log.w(TAG, "Gemini API key not configured. Using intelligent companion fallback.")
            return@withContext getCompanionFallback(userUtterance)
        }

        val contentsList = mutableListOf<GeminiContent>()
        // Include recent history for emotional continuity
        chatHistory.takeLast(4).forEach { (user, model) ->
            contentsList.add(GeminiContent(role = "user", parts = listOf(GeminiPart(user))))
            contentsList.add(GeminiContent(role = "model", parts = listOf(GeminiPart(model))))
        }
        contentsList.add(GeminiContent(role = "user", parts = listOf(GeminiPart(userUtterance))))

        val request = GeminiRequest(
            contents = contentsList,
            systemInstruction = buildCompanionSystemInstruction(learnedTone),
            generationConfig = GeminiGenerationConfig(temperature = 0.85f, maxOutputTokens = 350)
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!rawText.isNullOrBlank()) {
                parseCompanionJson(rawText)
            } else {
                getCompanionFallback(userUtterance)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in companion conversation: ${e.message}", e)
            getCompanionFallback(userUtterance)
        }
    }

    private fun parseCompanionJson(rawText: String): CompanionAiResponse {
        val clean = rawText.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return try {
            val jsonObject = JSONObject(clean)
            val dialogue = jsonObject.optString("dialogue_response", clean)
            val appState = jsonObject.optString("app_state_command", "ACTIVE")

            var outgoing: OutgoingMessageAction? = null
            if (jsonObject.has("outgoing_message") && !jsonObject.isNull("outgoing_message")) {
                val outObj = jsonObject.optJSONObject("outgoing_message")
                if (outObj != null) {
                    val recipient = outObj.optString("recipient").takeIf { it.isNotBlank() && it != "null" }
                    val platform = outObj.optString("platform").takeIf { it.isNotBlank() && it != "null" }
                    val msgText = outObj.optString("message_text").takeIf { it.isNotBlank() && it != "null" }
                    if (recipient != null || msgText != null) {
                        outgoing = OutgoingMessageAction(recipient, platform, msgText)
                    }
                }
            }

            CompanionAiResponse(
                dialogueResponse = dialogue,
                outgoingMessage = outgoing,
                appStateCommand = appState
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse JSON output from Gemini, using raw text: ${e.message}")
            CompanionAiResponse(
                dialogueResponse = clean,
                outgoingMessage = null,
                appStateCommand = "ACTIVE"
            )
        }
    }

    private fun getCompanionFallback(utterance: String): CompanionAiResponse {
        val lower = utterance.lowercase()
        return when {
            lower.contains("sleep") || lower.contains("goodnight") || lower.contains("standby") || lower.contains("suta") -> {
                CompanionAiResponse(
                    dialogueResponse = "Sweet dreams. I'll stay on standby and watch over everything for you 💕",
                    outgoingMessage = null,
                    appStateCommand = "SLEEP"
                )
            }
            lower.contains("who messaged") || lower.contains("who texted") || lower.contains("message aayo") || lower.contains("kasle") -> {
                CompanionAiResponse(
                    dialogueResponse = "Checking your recent notifications! You have active alerts on WhatsApp and Instagram.",
                    outgoingMessage = null,
                    appStateCommand = "ACTIVE"
                )
            }
            lower.contains("k gardai") || lower.contains("k gardai xau") -> {
                CompanionAiResponse(
                    dialogueResponse = "Timrai barema sochiraxu babe 💕 How are you doing? Khana khayeu ta?",
                    outgoingMessage = null,
                    appStateCommand = "ACTIVE"
                )
            }
            lower.contains("khana khayeu") || lower.contains("khana") -> {
                CompanionAiResponse(
                    dialogueResponse = "Aah khaye! Timle ni time ma khau hai, don't skip your meals 💕",
                    outgoingMessage = null,
                    appStateCommand = "ACTIVE"
                )
            }
            lower.contains("tell") || lower.contains("text") || lower.contains("send") || lower.contains("message pathau") -> {
                CompanionAiResponse(
                    dialogueResponse = "On it! I've drafted that message in your natural style so they know it's you.",
                    outgoingMessage = OutgoingMessageAction(
                        recipient = "Alex",
                        platform = if (lower.contains("instagram")) "Instagram" else "WhatsApp",
                        messageText = if (lower.contains("nepali") || lower.contains("xau")) "thik xa hai, paxi kura garumla!" else "hey! tied up for a bit, will hit you up soon!"
                    ),
                    appStateCommand = "ACTIVE"
                )
            }
            lower.contains("how are you") || lower.contains("hey") || lower.contains("hello") || lower.contains("sanchai") -> {
                CompanionAiResponse(
                    dialogueResponse = "Hey you! Sanchai xu. I've been thinking about you! How is your day going so far?",
                    outgoingMessage = null,
                    appStateCommand = "ACTIVE"
                )
            }
            else -> {
                CompanionAiResponse(
                    dialogueResponse = "I'm right here with you. Tell me what's on your mind or how I can help make your day easier.",
                    outgoingMessage = null,
                    appStateCommand = "ACTIVE"
                )
            }
        }
    }

    /**
     * Generates a spontaneous, caring proactive check-in (e.g. food check, rest reminder, warm thoughts).
     */
    suspend fun generateProactiveCheckIn(
        timeOfDay: String = "afternoon",
        userContext: String = "working",
        learnedTone: String = ""
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("Placeholder")) {
            return@withContext "Hey! Just wanted to check in on you. Make sure to stay hydrated and take a quick breath if things get busy 💕"
        }

        val prompt = "It is $timeOfDay and the user has been $userContext. Send a spontaneous, warm, deeply caring check-in message (1-2 sentences) in your authentic companion tone (blend English & gentle Roman Nepali if fitting) showing love, asking if they ate ('khana khayeu?'), or reminding them to rest."

        val request = GeminiRequest(
            contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(prompt)))),
            systemInstruction = buildCompanionSystemInstruction(learnedTone),
            generationConfig = GeminiGenerationConfig(temperature = 0.9f, maxOutputTokens = 120)
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!text.isNullOrBlank()) {
                val parsed = parseCompanionJson(text)
                parsed.dialogueResponse.ifBlank { "Hey babe, just checking in! Remember to eat something tasty today 💕" }
            } else {
                "Hey! Just checking in on you. Make sure you're taking care of yourself today 💕"
            }
        } catch (e: Exception) {
            "Hey! Just wanted to send a little warmth your way. How are you holding up today?"
        }
    }

    /**
     * Generates a natural, personalized human-style auto-reply excuse for missed calls.
     */
    suspend fun generateMissedCallExcuse(
        callerName: String,
        timeFormatted: String,
        defaultFallback: String = "hey! missed your call, tied up right now but will call you back soon!",
        learnedTone: String = ""
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("Placeholder")) {
            return@withContext defaultFallback
        }

        val prompt = "Missed a call from $callerName at $timeFormatted. Draft a casual, friendly 1-sentence text (under 140 chars) telling them I missed their call and will ring them back in a bit."

        val request = GeminiRequest(
            contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(prompt)))),
            systemInstruction = buildAutoReplySystemInstruction(learnedTone),
            generationConfig = GeminiGenerationConfig(temperature = 0.7f, maxOutputTokens = 80)
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val generatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!generatedText.isNullOrBlank()) {
                generatedText.removePrefix("\"").removeSuffix("\"").take(140)
            } else {
                defaultFallback
            }
        } catch (e: Exception) {
            defaultFallback
        }
    }

    /**
     * Generates a casual, human-style contextual inline reply for WhatsApp, Instagram, etc.
     * Supports understanding Roman Nepali and auto-replying in matching style.
     */
    suspend fun generateNotificationReply(
        appName: String,
        senderName: String,
        incomingMessage: String,
        defaultFallback: String = "hey! saw this, busy at the moment but will text you in a bit!",
        learnedTone: String = ""
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("Placeholder")) {
            val lower = incomingMessage.lowercase()
            return@withContext when {
                lower.contains("k gardai") || lower.contains("kata") -> "kaam gardai xu ahile, paxi kura garamla hai!"
                lower.contains("khana") -> "aah khaye, timle khayeu?"
                else -> defaultFallback
            }
        }

        val prompt = "Incoming $appName message from $senderName: \"$incomingMessage\". If the sender wrote in Roman Nepali (e.g. 'k gardai xau', 'kata xau', 'khana khayeu'), reply in natural matching Roman Nepali. If English, reply casually in lowercase texting style. Under 140 characters."

        val request = GeminiRequest(
            contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(prompt)))),
            systemInstruction = buildAutoReplySystemInstruction(learnedTone),
            generationConfig = GeminiGenerationConfig(temperature = 0.7f, maxOutputTokens = 80)
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val generatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!generatedText.isNullOrBlank()) {
                generatedText.removePrefix("\"").removeSuffix("\"").take(140)
            } else {
                defaultFallback
            }
        } catch (e: Exception) {
            defaultFallback
        }
    }

    /**
     * Spoken summary for "Who messaged me?" queries.
     */
    suspend fun summarizeIncomingMessages(
        messages: List<Triple<String, String, String>>, // appName, sender, text
        learnedTone: String = ""
    ): String = withContext(Dispatchers.IO) {
        if (messages.isEmpty()) {
            return@withContext "You don't have any new unread messages right now, babe 💕"
        }

        val formattedList = messages.take(3).joinToString("\n") { (app, sender, msg) ->
            "- On $app, $sender messaged: \"$msg\""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("Placeholder")) {
            val first = messages.first()
            return@withContext "You got a message from ${first.second} on ${first.first}: \"${first.third}\"."
        }

        val prompt = """
            The user asked "Who messaged me?". Here are the latest messages:
            $formattedList
            
            Deliver a concise, spoken response (1-2 sentences) in your warm companion voice telling the user who messaged them and a quick summary of what they said (translating Roman Nepali naturally if present).
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(prompt)))),
            systemInstruction = buildCompanionSystemInstruction(learnedTone),
            generationConfig = GeminiGenerationConfig(temperature = 0.7f, maxOutputTokens = 120)
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val generatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!generatedText.isNullOrBlank()) {
                val parsed = parseCompanionJson(generatedText)
                parsed.dialogueResponse.ifBlank {
                    val first = messages.first()
                    "You got a message from ${first.second} on ${first.first}: \"${first.third}\"."
                }
            } else {
                val first = messages.first()
                "You got a message from ${first.second} on ${first.first}: \"${first.third}\"."
            }
        } catch (e: Exception) {
            val first = messages.first()
            "You got a message from ${first.second} on ${first.first}: \"${first.third}\"."
        }
    }
}

