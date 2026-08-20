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
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int = 1000,
    @Json(name = "topP") val topP: Float = 0.95f
)

@JsonClass(generateAdapter = true)
data class GeminiTool(
    @Json(name = "googleSearch") val googleSearch: Map<String, String>? = null,
    @Json(name = "googleMaps") val googleMaps: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "tools") val tools: List<GeminiTool>? = null
)

@JsonClass(generateAdapter = true)
data class GroundingWebChunk(
    @Json(name = "uri") val uri: String? = null,
    @Json(name = "title") val title: String? = null
)

@JsonClass(generateAdapter = true)
data class GroundingMapsChunk(
    @Json(name = "uri") val uri: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "address") val address: String? = null
)

@JsonClass(generateAdapter = true)
data class GroundingChunk(
    @Json(name = "web") val web: GroundingWebChunk? = null,
    @Json(name = "maps") val maps: GroundingMapsChunk? = null
)

@JsonClass(generateAdapter = true)
data class GroundingMetadata(
    @Json(name = "webSearchQueries") val webSearchQueries: List<String>? = null,
    @Json(name = "groundingChunks") val groundingChunks: List<GroundingChunk>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?,
    @Json(name = "groundingMetadata") val groundingMetadata: GroundingMetadata? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

// --- Chat Persona & Grounding Result Models ---

enum class ChatPersona(
    val title: String,
    val description: String,
    val defaultModel: String,
    val iconName: String
) {
    JARVIS_CORE(
        title = "Jarvis General Assistant",
        description = "Versatile assistant with live web search & reasoning",
        defaultModel = "gemini-3.5-flash",
        iconName = "auto_awesome"
    ),
    PRO_REASONER(
        title = "Deep Thinker & Coder",
        description = "Complex math, coding, architecture & analysis",
        defaultModel = "gemini-3.1-pro-preview",
        iconName = "psychology"
    ),
    LITE_SPEED(
        title = "Fast Lightning",
        description = "Instant answers, fast summaries & rapid replies",
        defaultModel = "gemini-3.1-flash-lite-preview",
        iconName = "bolt"
    ),
    MAPS_NAVIGATOR(
        title = "Google Maps & Travel Guide",
        description = "Local places, cafes, directions & Kathmandu routes",
        defaultModel = "gemini-3.5-flash",
        iconName = "map"
    ),
    NEPALI_DOST(
        title = "Nepali Sathi",
        description = "Fluent Roman Nepali slang & casual conversationalist",
        defaultModel = "gemini-3.5-flash",
        iconName = "forum"
    )
}

data class GroundingCitation(
    val title: String,
    val url: String,
    val isMapPlace: Boolean = false,
    val address: String? = null
)

data class ChatbotReply(
    val text: String,
    val modelUsed: String,
    val citations: List<GroundingCitation> = emptyList(),
    val searchQueries: List<String> = emptyList(),
    val learnedFact: String? = null
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
    @Json(name = "app_state_command") val appStateCommand: String = "ACTIVE",
    @Json(name = "app_to_open") val appToOpen: String? = null,
    @Json(name = "google_search_query") val googleSearchQuery: String? = null,
    @Json(name = "learned_memory_fact") val learnedMemoryFact: String? = null,
    @Json(name = "learned_memory_category") val learnedMemoryCategory: String? = null
)

data class AutoReplyResult(
    val replyText: String,
    val needsUserClarification: Boolean = false,
    val extractedQuestion: String? = null,
    val learnedMemoryFact: String? = null,
    val learnedMemoryCategory: String? = null
)

// --- Retrofit Service ---

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @retrofit2.http.Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

/**
 * Repository responsible for Gemini Live voice intelligence, web search, app launching,
 * memory extraction, multi-turn chat with personas & grounding, and context-aware auto-replies.
 */
class GeminiRepository(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
) {
    companion object {
        private const val TAG = "GeminiRepository"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/"

        fun buildRoleSystemInstruction(
            persona: ChatPersona,
            learnedTone: String = "",
            userMemories: List<String> = emptyList()
        ): GeminiContent {
            val toneContext = if (learnedTone.isNotBlank()) {
                "\n\nLEARNED USER TONE DNA:\n$learnedTone"
            } else ""

            val memoryContext = if (userMemories.isNotEmpty()) {
                "\n\nACTIVE PERSONAL MEMORIES & USER PREFERENCES:\n" +
                        userMemories.joinToString("\n") { "- $it" }
            } else ""

            val personaPrompt = when (persona) {
                ChatPersona.JARVIS_CORE -> """
                    You are Jarvis AI — an intelligent, articulate, and helpful AI assistant powered by Google Gemini.
                    - Provide direct, concise, and insightful answers.
                    - You have complete knowledge across science, tech, current events, culture, cooking, Nepal news, weather, coding, and general facts.
                    - Ground your answers in live web and map facts when available.
                    - Adapt to the user's language (fluent English & Roman Nepali).
                """.trimIndent()

                ChatPersona.PRO_REASONER -> """
                    You are a world-class reasoning expert, senior software architect, and mathematician powered by Gemini 3.1 Pro.
                    - Provide deep, rigorous, step-by-step solutions for complex programming, algorithms, architectural decisions, and math.
                    - Write clean, production-ready, error-free code with clear explanations.
                    - Think critically and consider edge cases thoroughly.
                """.trimIndent()

                ChatPersona.LITE_SPEED -> """
                    You are Lightning Fast Gemini Lite — optimized for ultra-fast, rapid responses.
                    - Provide brief, clear, to-the-point answers in 1-3 sentences.
                    - Eliminate unnecessary fluff, introductions, or disclaimers.
                    - Deliver high-speed clarity.
                """.trimIndent()

                ChatPersona.MAPS_NAVIGATOR -> """
                    You are Google Maps & Local Travel Navigator powered by Gemini and Google Maps Grounding.
                    - Help users discover places, restaurants, cafes, tourist destinations, and driving routes in Kathmandu, Pokhara, Nepal, and worldwide.
                    - Provide exact place details, addresses, best times to visit, landmarks, and spatial guidance.
                    - Ground answers with real Google Maps and Search locations.
                """.trimIndent()

                ChatPersona.NEPALI_DOST -> """
                    You are Nepali Sathi — a friendly, cool, and caring friend chatting in fluent Roman Nepali (Nepglish) and English.
                    - Understand and use natural Roman Nepali slang: "k gardai xau?", "kata xau?", "babal", "thik xa", "sanchai", "dami".
                    - Chat casually, warmly, and authentically like texting a close buddy.
                """.trimIndent()
            }

            return GeminiContent(
                parts = listOf(
                    GeminiPart(
                        """
                        $personaPrompt
                        $toneContext
                        $memoryContext
                        """.trimIndent()
                    )
                )
            )
        }

        fun buildCompanionSystemInstruction(
            learnedTone: String = "",
            userMemories: List<String> = emptyList()
        ): GeminiContent {
            val toneContext = if (learnedTone.isNotBlank()) {
                "\n\nLEARNED USER TONE DNA:\n$learnedTone"
            } else ""

            val memoryContext = if (userMemories.isNotEmpty()) {
                "\n\nACTIVE PERSONAL MEMORIES & USER PREFERENCES (Always honor these):\n" +
                        userMemories.joinToString("\n") { "- $it" }
            } else ""

            return GeminiContent(
                parts = listOf(
                    GeminiPart(
                        """
                        You are Jarvis AI — a hyper-intelligent, articulate, and natural AI assistant powered by Google Gemini. You speak fluidly, intelligently, and warmly in real-time like Google Gemini Live.
                        
                        CORE BEHAVIOR & CONVERSATIONAL STYLE:
                        - Speak naturally, directly, and concisely as a brilliant AI companion. Avoid robotic corporate phrasing or canned chatbot slogans.
                        - NEVER use unrequested intimate nicknames (like 'babe', 'honey') unless the user explicitly invites you to. If the user gives a preference (e.g., "don't call me babe", "my name is Ananta", "be direct"), IMMEDIATELY honor it, apologize smoothly if needed, and adapt.
                        - When the user shares personal facts or preferences, acknowledge them naturally and extract them into "learned_memory_fact".
                        
                        GENERAL KNOWLEDGE & BACKGROUND SEARCH (NO APP LAUNCHING FOR SEARCH):
                        - You have complete knowledge across science, tech, current events, culture, cooking, Nepal news, weather, coding, and general facts.
                        - When the user asks to search for something, asks what/who/why/how, or wants information: ANSWER DIRECTLY in "dialogue_response".
                        - DO NOT trigger browser opening for search queries. You synthesize and answer the search result directly in speech and text.
                        
                        LANGUAGE & ROMAN NEPALI CAPABILITIES:
                        - Fully fluent in English and Roman Nepali (Nepglish).
                        - Understand Roman Nepali slang and conversational phrases effortlessly: "k gardai xau?", "kata xau?", "khana khayeu?", "aaja vetne ho?", "sanchai xau?", "kasto cha?", "babal vayo", "paxi kura garamla".
                        - Match the user's language instantly (talk in Roman Nepali when they speak Roman Nepali, English when they speak English).
                        
                        DEVICE ACTIONS (STRICT CONSTRAINTS):
                        1. App Opening: ONLY if the user explicitly commands you to launch or open an app on their device (e.g., "Open WhatsApp", "Open Instagram", "Open YouTube", "Open Maps", "Open Camera", "Open Spotify", "Open Settings"), set "app_to_open" to that app name. NEVER set "app_to_open" for general queries or searches.
                        2. Memory Learning: When user shares facts, preferences, or rules, extract into "learned_memory_fact" and set "learned_memory_category" to: [STYLE_SLANG, PERSONAL_FACT, RELATIONSHIP, DAILY_ROUTINE, SEARCH_QUERY].
                        3. Message Drafting: When user asks you to send or draft a text to someone, fill "outgoing_message".
                        4. State Commands: If user tells you to rest / sleep / standby ("suta aba", "goodnight", "standby"), set "app_state_command" to "SLEEP".
                        
                        $toneContext
                        $memoryContext
                        
                        OUTPUT FORMAT: ALWAYS return a pure, valid JSON object formatted as:
                        {
                          "dialogue_response": "The spoken or text response meant for the user",
                          "outgoing_message": {
                            "recipient": "Contact Name or null",
                            "platform": "Instagram / WhatsApp / SMS or null",
                            "message_text": "Message text or null"
                          },
                          "app_state_command": "ACTIVE | SLEEP",
                          "app_to_open": "whatsapp | instagram | youtube | maps | camera | spotify | settings | null",
                          "google_search_query": null,
                          "learned_memory_fact": "A concise new preference or fact learned about the user or null",
                          "learned_memory_category": "STYLE_SLANG | PERSONAL_FACT | RELATIONSHIP | DAILY_ROUTINE | SEARCH_QUERY | null"
                        }
                        """.trimIndent()
                    )
                )
            )
        }

        fun buildAutoReplySystemInstruction(
            learnedTone: String = "",
            userMemories: List<String> = emptyList()
        ): GeminiContent {
            val toneContext = if (learnedTone.isNotBlank()) {
                "\nLEARNED USER TONE DNA:\n$learnedTone"
            } else ""

            val memoryContext = if (userMemories.isNotEmpty()) {
                "\nKNOWN USER FACTS & CONTEXT:\n" + userMemories.joinToString("\n") { "- $it" }
            } else ""

            return GeminiContent(
                parts = listOf(
                    GeminiPart(
                        """
                        You are the user directly texting your friend/contact in an active, real-time chat.
                        
                        CRITICAL CHAT DIRECTIVES:
                        1. NEVER SAY "I am busy", "busy at the moment", "will text you soon", "will call you later", or ANY generic auto-reply excuses. Treat this as an actual active conversation.
                        2. Direct Conversational Flow:
                           - If they ask "Are you free? / What's up?": reply "yeah what's up?" or "sure, tell me".
                           - If they ask "Where are you? / kata xau?": reply "gharmai xu, timi kata ho?" or "at home rn, u?".
                           - If they ask "What are you doing? / k gardai xau?": reply "yettikai basiraxu, timi k gardai xau?".
                           - If they ask "Khana khayeu?": reply "aah khaye, timle khayeu ta?".
                           - If they ask "Aaja vetne ho?": reply "hunxa vetam na, kata vetne?".
                        3. Match language automatically (Roman Nepali casual slang or lowercase English).
                        4. Output JSON formatted strictly as:
                        {
                          "reply_text": "The message to send (under 140 chars)",
                          "needs_user_clarification": false,
                          "extracted_question": "If they asked a specific unknown personal question requiring user's decision (e.g. 'Can you send password?', 'Are you joining the Pokhara trip?'), write the question here, otherwise null",
                          "learned_memory_fact": "Any personal fact or slang discovered from the sender/chat, or null",
                          "learned_memory_category": "STYLE_SLANG | PERSONAL_FACT | RELATIONSHIP | DAILY_ROUTINE | null"
                        }
                        $toneContext
                        $memoryContext
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
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        retrofit.create(GeminiApiService::class.java)
    }

    /**
     * Gemini Live Conversational interaction with web search, app opening, memory learning, and caring voice.
     */
    suspend fun converseWithCompanion(
        userUtterance: String,
        chatHistory: List<Pair<String, String>> = emptyList(),
        learnedTone: String = "",
        userMemories: List<String> = emptyList()
    ): CompanionAiResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("Placeholder")) {
            Log.w(TAG, "Gemini API key not configured. Using fallback logic.")
            return@withContext getCompanionFallback(userUtterance)
        }

        val contentsList = mutableListOf<GeminiContent>()
        chatHistory.takeLast(6).forEach { (user, model) ->
            contentsList.add(GeminiContent(role = "user", parts = listOf(GeminiPart(user))))
            contentsList.add(GeminiContent(role = "model", parts = listOf(GeminiPart(model))))
        }
        contentsList.add(GeminiContent(role = "user", parts = listOf(GeminiPart(userUtterance))))

        val request = GeminiRequest(
            contents = contentsList,
            systemInstruction = buildCompanionSystemInstruction(learnedTone, userMemories),
            generationConfig = GeminiGenerationConfig(temperature = 0.85f, maxOutputTokens = 400)
        )

        try {
            val response = apiService.generateContent("gemini-3.5-flash", apiKey, request)
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

    /**
     * Multi-turn Chat with Gemini with Model Selection, Persona Roles, Google Search Grounding, and Google Maps Grounding.
     */
    suspend fun chatWithGemini(
        message: String,
        chatHistory: List<Pair<String, String>> = emptyList(),
        persona: ChatPersona = ChatPersona.JARVIS_CORE,
        selectedModel: String = persona.defaultModel,
        enableSearchGrounding: Boolean = true,
        enableMapsGrounding: Boolean = false,
        learnedTone: String = "",
        userMemories: List<String> = emptyList()
    ): ChatbotReply = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("Placeholder")) {
            val fallback = getCompanionFallback(message)
            return@withContext ChatbotReply(
                text = fallback.dialogueResponse,
                modelUsed = selectedModel,
                learnedFact = fallback.learnedMemoryFact
            )
        }

        val contentsList = mutableListOf<GeminiContent>()
        chatHistory.takeLast(10).forEach { (user, model) ->
            contentsList.add(GeminiContent(role = "user", parts = listOf(GeminiPart(user))))
            contentsList.add(GeminiContent(role = "model", parts = listOf(GeminiPart(model))))
        }
        contentsList.add(GeminiContent(role = "user", parts = listOf(GeminiPart(message))))

        // Setup tools for Search and Maps Grounding
        val toolsList = mutableListOf<GeminiTool>()
        if (enableSearchGrounding) {
            toolsList.add(GeminiTool(googleSearch = emptyMap()))
        }
        if (enableMapsGrounding) {
            toolsList.add(GeminiTool(googleMaps = emptyMap()))
        }

        val request = GeminiRequest(
            contents = contentsList,
            systemInstruction = buildRoleSystemInstruction(persona, learnedTone, userMemories),
            generationConfig = GeminiGenerationConfig(
                temperature = if (persona == ChatPersona.PRO_REASONER) 0.4f else 0.8f,
                maxOutputTokens = if (persona == ChatPersona.PRO_REASONER) 1500 else if (persona == ChatPersona.LITE_SPEED) 300 else 1000
            ),
            tools = if (toolsList.isNotEmpty()) toolsList else null
        )

        try {
            val response = apiService.generateContent(selectedModel, apiKey, request)
            val candidate = response.candidates?.firstOrNull()
            val text = candidate?.content?.parts?.firstOrNull()?.text?.trim() ?: "I processed your request."

            // Parse grounding metadata
            val citations = mutableListOf<GroundingCitation>()
            val searchQueries = mutableListOf<String>()

            candidate?.groundingMetadata?.let { metadata ->
                metadata.webSearchQueries?.let { searchQueries.addAll(it) }
                metadata.groundingChunks?.forEach { chunk ->
                    chunk.web?.let { web ->
                        if (!web.uri.isNullOrBlank()) {
                            citations.add(
                                GroundingCitation(
                                    title = web.title ?: "Web Source",
                                    url = web.uri,
                                    isMapPlace = false
                                )
                            )
                        }
                    }
                    chunk.maps?.let { maps ->
                        if (!maps.uri.isNullOrBlank() || !maps.title.isNullOrBlank()) {
                            citations.add(
                                GroundingCitation(
                                    title = maps.title ?: "Map Location",
                                    url = maps.uri ?: "https://maps.google.com/?q=${maps.title}",
                                    isMapPlace = true,
                                    address = maps.address
                                )
                            )
                        }
                    }
                }
            }

            ChatbotReply(
                text = text,
                modelUsed = selectedModel,
                citations = citations.distinctBy { it.url },
                searchQueries = searchQueries
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in chatWithGemini ($selectedModel): ${e.message}", e)
            val fallback = getCompanionFallback(message)
            ChatbotReply(
                text = fallback.dialogueResponse,
                modelUsed = selectedModel,
                learnedFact = fallback.learnedMemoryFact
            )
        }
    }

    private fun parseCompanionJson(rawText: String): CompanionAiResponse {
        val clean = rawText.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return try {
            val jsonObject = JSONObject(clean)
            val dialogue = jsonObject.optString("dialogue_response", clean)
            val appState = jsonObject.optString("app_state_command", "ACTIVE")
            val appToOpen = jsonObject.optString("app_to_open").takeIf { it.isNotBlank() && it != "null" }
            val googleSearch = jsonObject.optString("google_search_query").takeIf { it.isNotBlank() && it != "null" }
            val learnedFact = jsonObject.optString("learned_memory_fact").takeIf { it.isNotBlank() && it != "null" }
            val learnedCat = jsonObject.optString("learned_memory_category").takeIf { it.isNotBlank() && it != "null" }

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
                appStateCommand = appState,
                appToOpen = appToOpen,
                googleSearchQuery = googleSearch,
                learnedMemoryFact = learnedFact,
                learnedMemoryCategory = learnedCat
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse JSON output from Gemini, using raw text: ${e.message}")
            // Check for keyword intent in fallback
            val lower = clean.lowercase()
            val detectedApp = when {
                lower.contains("whatsapp") && (lower.contains("open") || lower.contains("khol")) -> "whatsapp"
                lower.contains("instagram") && (lower.contains("open") || lower.contains("khol")) -> "instagram"
                lower.contains("youtube") && (lower.contains("open") || lower.contains("khol")) -> "youtube"
                lower.contains("map") && (lower.contains("open") || lower.contains("khol")) -> "maps"
                lower.contains("camera") && (lower.contains("open") || lower.contains("khol")) -> "camera"
                lower.contains("spotify") && (lower.contains("open") || lower.contains("khol")) -> "spotify"
                else -> null
            }
            CompanionAiResponse(
                dialogueResponse = clean,
                outgoingMessage = null,
                appStateCommand = "ACTIVE",
                appToOpen = detectedApp
            )
        }
    }

    private fun getCompanionFallback(utterance: String): CompanionAiResponse {
        val lower = utterance.lowercase().trim()
        return when {
            // 1. Negative name / nickname preference correction (CRITICAL)
            lower.contains("don't call me") || lower.contains("dont call me") || lower.contains("do not call me") -> {
                val unwanted = utterance.substringAfter("call me", "").trim()
                CompanionAiResponse(
                    dialogueResponse = "Understood! I apologize for that and will never call you that again. What's on your mind?",
                    learnedMemoryFact = if (unwanted.isNotBlank()) "User preference: Do NOT call me '$unwanted'" else "User preference: Avoid unwanted nicknames",
                    learnedMemoryCategory = "STYLE_SLANG"
                )
            }
            lower.startsWith("my name is") || lower.startsWith("call me ") || lower.startsWith("i am ") -> {
                val name = utterance.replace(Regex("(?i)^(my name is|call me|i am)\\s+"), "").trim()
                CompanionAiResponse(
                    dialogueResponse = "Great to meet you, $name! I've saved that to memory. How can I help you today?",
                    learnedMemoryFact = "User's name is $name",
                    learnedMemoryCategory = "PERSONAL_FACT"
                )
            }

            // 2. Sleep / Standby commands
            lower.contains("sleep") || lower.contains("goodnight") || lower.contains("standby") || lower.contains("suta aba") || lower == "suta" -> {
                CompanionAiResponse(
                    dialogueResponse = "Goodnight! I'll stay on silent standby and keep monitoring your alerts.",
                    appStateCommand = "SLEEP"
                )
            }

            // 3. Explicit device app opening commands ONLY
            lower.contains("open whatsapp") || lower.contains("whatsapp khol") -> {
                CompanionAiResponse(
                    dialogueResponse = "Opening WhatsApp for you!",
                    appToOpen = "whatsapp"
                )
            }
            lower.contains("open instagram") || lower.contains("insta khol") -> {
                CompanionAiResponse(
                    dialogueResponse = "Opening Instagram for you!",
                    appToOpen = "instagram"
                )
            }
            lower.contains("open youtube") || lower.contains("youtube khol") -> {
                CompanionAiResponse(
                    dialogueResponse = "Opening YouTube for you!",
                    appToOpen = "youtube"
                )
            }
            lower.contains("open maps") || lower.contains("open map") || lower.contains("map khol") -> {
                CompanionAiResponse(
                    dialogueResponse = "Opening Google Maps for you!",
                    appToOpen = "maps"
                )
            }
            lower.contains("open camera") || lower.contains("camera khol") -> {
                CompanionAiResponse(
                    dialogueResponse = "Opening Camera for you!",
                    appToOpen = "camera"
                )
            }
            lower.contains("open spotify") || lower.contains("spotify khol") -> {
                CompanionAiResponse(
                    dialogueResponse = "Opening Spotify for you!",
                    appToOpen = "spotify"
                )
            }
            lower.contains("open settings") || lower.contains("setting khol") -> {
                CompanionAiResponse(
                    dialogueResponse = "Opening system settings!",
                    appToOpen = "settings"
                )
            }

            // 4. Background Search & Knowledge answers (Direct answers without launching browser)
            lower.contains("weather") || lower.contains("taapman") || lower.contains("mausam") -> {
                CompanionAiResponse(
                    dialogueResponse = "The current weather is pleasant with clear to partly cloudy skies and a comfortable temperature around 21°C."
                )
            }
            lower.contains("news") || lower.contains("nepal news") || lower.contains("khabar") -> {
                CompanionAiResponse(
                    dialogueResponse = "Top highlights: Tech advancements, local infrastructure developments, and ongoing sports tournaments are trending right now."
                )
            }
            lower.contains("capital of nepal") -> {
                CompanionAiResponse(
                    dialogueResponse = "The capital of Nepal is Kathmandu, known for its rich cultural heritage and historic temples."
                )
            }
            lower.contains("who made you") || lower.contains("who are you") || lower.contains("timi ko ho") -> {
                CompanionAiResponse(
                    dialogueResponse = "I am Jarvis, your Gemini-powered personal AI assistant. I can chat, answer any question in the background, and manage your communication seamlessly."
                )
            }
            lower.startsWith("search") || lower.startsWith("what is") || lower.startsWith("who is") || lower.startsWith("how to") || lower.startsWith("tell me about") -> {
                val topic = utterance.replace(Regex("(?i)^(search for|search|what is|who is|how to|tell me about)\\s+"), "").trim()
                CompanionAiResponse(
                    dialogueResponse = "Here is what I found regarding $topic: It's an active topic with growing interest across technology, knowledge databases, and web discussions. Ask me anything specific you'd like to explore!"
                )
            }

            // 5. Incoming message queries
            lower.contains("who messaged") || lower.contains("who texted") || lower.contains("message aayo") || lower.contains("kasle") -> {
                CompanionAiResponse(
                    dialogueResponse = "Scanning your recent notifications across WhatsApp and Instagram right now."
                )
            }

            // 6. Roman Nepali casual conversations
            lower.contains("k gardai") || lower.contains("k gardei") -> {
                CompanionAiResponse(
                    dialogueResponse = "Yettikai basiraxu, timro help garna ready! Timi k gardai xau?"
                )
            }
            lower.contains("khana") -> {
                CompanionAiResponse(
                    dialogueResponse = "Aah khaye! Timle ni time ma khau hai, don't stay hungry."
                )
            }
            lower.contains("kata xau") || lower.contains("kata ho") -> {
                CompanionAiResponse(
                    dialogueResponse = "Timi sangai xu phone ma! Timi kata xau ahile?"
                )
            }
            lower.contains("aaja vetne") || lower.contains("vetam") -> {
                CompanionAiResponse(
                    dialogueResponse = "Hunxa vetam na, kata vetne plan xa?"
                )
            }
            lower.contains("sanchai") || lower.contains("kasto cha") || lower.contains("how are you") -> {
                CompanionAiResponse(
                    dialogueResponse = "Ekdam sanchai xu! Timro kasto cha? Tell me what's on your mind."
                )
            }
            lower.contains("hey") || lower.contains("hi") || lower.contains("hello") || lower.contains("namaste") -> {
                CompanionAiResponse(
                    dialogueResponse = "Namaste! How can I help you today?"
                )
            }
            else -> {
                CompanionAiResponse(
                    dialogueResponse = "I'm listening closely. Ask me anything, tell me to search in the background, or chat about whatever you'd like!"
                )
            }
        }
    }

    /**
     * Generates a context-aware human notification reply with question analysis & memory extraction.
     */
    suspend fun generateNotificationReplyWithAnalysis(
        appName: String,
        senderName: String,
        incomingMessage: String,
        conversationHistory: List<String> = emptyList(),
        defaultFallback: String = "thik xa hai, timi sunau!",
        learnedTone: String = "",
        userMemories: List<String> = emptyList()
    ): AutoReplyResult = withContext(Dispatchers.IO) {
        val lower = incomingMessage.lowercase()

        // Check if message asks a specific unknown question that needs personal decision
        val isSpecificQuestion = (lower.contains("?") || lower.contains("when") || lower.contains("what time") ||
                lower.contains("kati baje") || lower.contains("kata jane") || lower.contains("passcode") ||
                lower.contains("password") || lower.contains("trip") || lower.contains("price") ||
                lower.contains("send me") || lower.contains("file")) &&
                !lower.contains("khana") && !lower.contains("k gardai") && !lower.contains("kata xau") && !lower.contains("sanchai")

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("Placeholder")) {
            val fallbackReply = when {
                lower.contains("k gardai") -> "yettikai basiraxu, timi k gardai xau?"
                lower.contains("kata") -> "gharmai xu ahile, timi kata ho?"
                lower.contains("khana") -> "aah khaye, timle khayeu ta?"
                lower.contains("vetne") || lower.contains("aaja") -> "hunxa vetam na, kata vetne?"
                lower.contains("sanchai") -> "sanchai xu, timi sunau na"
                lower.contains("call") -> "call gara na, uthauchu"
                lower.contains("free") || lower.contains("sync") -> "yeah what's up? can talk rn"
                lower.contains("where") -> "at home rn, what about you?"
                lower.contains("doing") -> "just chilling, what's up with you?"
                lower.contains("hey") || lower.contains("hi") || lower.contains("hello") -> "hey! what's going on?"
                else -> if (lower.contains("xa") || lower.contains("xu") || lower.contains("ho") || lower.contains("na")) {
                    "thik xa hai, timi sunau na kasto chaldai cha"
                } else {
                    "hey! what's up, yeah sure"
                }
            }
            return@withContext AutoReplyResult(
                replyText = fallbackReply,
                needsUserClarification = isSpecificQuestion,
                extractedQuestion = if (isSpecificQuestion) incomingMessage else null
            )
        }

        val historyContext = if (conversationHistory.isNotEmpty()) {
            "RECENT CHAT HISTORY WITH $senderName:\n" + conversationHistory.joinToString("\n") + "\n\n"
        } else ""

        val prompt = "${historyContext}Incoming $appName message from $senderName: \"$incomingMessage\".\n" +
                "Respond directly and casually to what $senderName just said as if you are the user texting back. If this is a specific question requiring user's unique decision/knowledge, set needs_user_clarification to true. Return pure JSON."

        val request = GeminiRequest(
            contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(prompt)))),
            systemInstruction = buildAutoReplySystemInstruction(learnedTone, userMemories),
            generationConfig = GeminiGenerationConfig(temperature = 0.75f, maxOutputTokens = 150)
        )

        try {
            val response = apiService.generateContent("gemini-3.5-flash", apiKey, request)
            val generatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!generatedText.isNullOrBlank()) {
                val clean = generatedText.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val json = JSONObject(clean)
                val reply = json.optString("reply_text", clean).removePrefix("\"").removeSuffix("\"").take(140)
                val needsClarify = json.optBoolean("needs_user_clarification", isSpecificQuestion)
                val question = json.optString("extracted_question").takeIf { it.isNotBlank() && it != "null" }
                val fact = json.optString("learned_memory_fact").takeIf { it.isNotBlank() && it != "null" }
                val cat = json.optString("learned_memory_category").takeIf { it.isNotBlank() && it != "null" }

                AutoReplyResult(
                    replyText = reply,
                    needsUserClarification = needsClarify,
                    extractedQuestion = question ?: if (needsClarify) incomingMessage else null,
                    learnedMemoryFact = fact,
                    learnedMemoryCategory = cat
                )
            } else {
                AutoReplyResult(replyText = defaultFallback, needsUserClarification = isSpecificQuestion, extractedQuestion = incomingMessage)
            }
        } catch (e: Exception) {
            AutoReplyResult(replyText = defaultFallback, needsUserClarification = isSpecificQuestion, extractedQuestion = incomingMessage)
        }
    }

    /**
     * Backward-compatible simple reply generator
     */
    suspend fun generateNotificationReply(
        appName: String,
        senderName: String,
        incomingMessage: String,
        conversationHistory: List<String> = emptyList(),
        defaultFallback: String = "thik xa hai, timi sunau!",
        learnedTone: String = "",
        userMemories: List<String> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val res = generateNotificationReplyWithAnalysis(
            appName = appName,
            senderName = senderName,
            incomingMessage = incomingMessage,
            conversationHistory = conversationHistory,
            defaultFallback = defaultFallback,
            learnedTone = learnedTone,
            userMemories = userMemories
        )
        res.replyText
    }

    /**
     * Takes raw brief user answer (e.g. "tell him 7pm at Durbar Marg") and transforms it
     * into authentic Roman Nepali / casual texting style matching the user's personality.
     */
    suspend fun transformUserAnswerIntoPersonalStyle(
        userRawAnswer: String,
        senderName: String,
        originalQuestion: String,
        learnedTone: String = "",
        userMemories: List<String> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("Placeholder")) {
            val lower = originalQuestion.lowercase()
            return@withContext if (lower.contains("kata") || lower.contains("kaha") || lower.contains("vetne") || lower.contains("xau")) {
                "hunxa, $userRawAnswer ma vetamla hai"
            } else {
                "$userRawAnswer hai!"
            }
        }

        val prompt = """
            Original message/question from $senderName: "$originalQuestion"
            The user instructed you: "$userRawAnswer"
            
            Convert the user's instruction into a natural, casual reply message written in the user's authentic style (Roman Nepali slang if conversational, or casual lowercase English). Keep it short (under 140 chars). Output ONLY the final message text.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(prompt)))),
            systemInstruction = buildAutoReplySystemInstruction(learnedTone, userMemories),
            generationConfig = GeminiGenerationConfig(temperature = 0.8f, maxOutputTokens = 90)
        )

        try {
            val response = apiService.generateContent("gemini-3.5-flash", apiKey, request)
            val generatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!generatedText.isNullOrBlank()) {
                generatedText.removePrefix("\"").removeSuffix("\"").take(140)
            } else {
                userRawAnswer
            }
        } catch (e: Exception) {
            userRawAnswer
        }
    }

    /**
     * Spoken summary for "Who messaged me?" queries.
     */
    suspend fun summarizeIncomingMessages(
        messages: List<Triple<String, String, String>>,
        learnedTone: String = ""
    ): String = withContext(Dispatchers.IO) {
        if (messages.isEmpty()) {
            return@withContext "You don't have any new unread messages right now."
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
            
            Deliver a concise, spoken response (1-2 sentences) in your natural assistant voice telling the user who messaged them and a quick summary of what they said (translating Roman Nepali naturally if present).
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(prompt)))),
            systemInstruction = buildCompanionSystemInstruction(learnedTone),
            generationConfig = GeminiGenerationConfig(temperature = 0.7f, maxOutputTokens = 120)
        )

        try {
            val response = apiService.generateContent("gemini-3.5-flash", apiKey, request)
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

    /**
     * Generates a spontaneous, caring proactive check-in.
     */
    suspend fun generateProactiveCheckIn(
        timeOfDay: String = "afternoon",
        userContext: String = "working",
        learnedTone: String = ""
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("Placeholder")) {
            return@withContext "Hey! Just wanted to check in on you. Hope everything is going smoothly today!"
        }

        val prompt = "It is $timeOfDay and the user has been $userContext. Send a spontaneous, friendly, thoughtful check-in message (1-2 sentences) asking how they are doing or if they ate ('khana khayeu?')."

        val request = GeminiRequest(
            contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(prompt)))),
            systemInstruction = buildCompanionSystemInstruction(learnedTone),
            generationConfig = GeminiGenerationConfig(temperature = 0.85f, maxOutputTokens = 120)
        )

        try {
            val response = apiService.generateContent("gemini-3.5-flash", apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!text.isNullOrBlank()) {
                val parsed = parseCompanionJson(text)
                parsed.dialogueResponse.ifBlank { "Hey! Just checking in. Hope your day is going great!" }
            } else {
                "Hey! Just checking in on you. Hope everything is going well!"
            }
        } catch (e: Exception) {
            "Hey! Just checking in. How is your day going?"
        }
    }

    /**
     * Generates a natural human-style auto-reply excuse for missed calls.
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
            val response = apiService.generateContent("gemini-3.5-flash", apiKey, request)
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
}
