package com.example.ui

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.JarvisApplication
import com.example.data.model.JarvisLog
import com.example.data.model.LogType
import com.example.data.model.MemoryCategory
import com.example.data.model.PendingQuestion
import com.example.data.model.UserMemory
import com.example.data.preferences.JarvisPreferences
import com.example.data.repository.ChatPersona
import com.example.data.repository.GroundingCitation
import com.example.data.repository.OutgoingMessageAction
import com.example.receiver.CallReceiver
import com.example.service.JarvisForegroundService
import com.example.service.JarvisNotificationService
import com.example.speech.JarvisSpeechManager
import com.example.util.DeviceActionExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionStatus(
    val hasAudio: Boolean = false,
    val hasPhoneState: Boolean = false,
    val hasCallLog: Boolean = false,
    val hasSms: Boolean = false,
    val hasContacts: Boolean = false,
    val hasNotifications: Boolean = false,
    val hasNotificationListener: Boolean = false
) {
    val isAllEssentialGranted: Boolean
        get() = hasAudio && hasPhoneState && hasCallLog && hasSms && hasNotificationListener
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as JarvisApplication
    private val repository = app.repository
    private val geminiRepository = app.geminiRepository
    private val preferences = app.preferences
    private val speechManager = JarvisSpeechManager.getInstance(application)

    // Service & status states
    val isServiceRunning: StateFlow<Boolean> = JarvisForegroundService.isRunning
    val isNotificationServiceConnected: StateFlow<Boolean> = JarvisNotificationService.isConnected
    val isWakeWordListening: StateFlow<Boolean> = speechManager.isListening
    val micRmsLevel: StateFlow<Float> = speechManager.rmsLevel
    val lastDetectedPhrase: StateFlow<String?> = speechManager.lastDetectedPhrase

    // Preferences
    val isServiceEnabled = preferences.isServiceEnabled
    val isVoiceWakeEnabled = preferences.isVoiceWakeEnabled
    val isMissedCallTtsEnabled = preferences.isMissedCallTtsEnabled
    val isMissedCallSmsEnabled = preferences.isMissedCallSmsEnabled
    val isNotificationReplyEnabled = preferences.isNotificationReplyEnabled
    val isAnnounceMessagesEnabled = preferences.isAnnounceMessagesEnabled
    val learnedToneSamples = preferences.learnedToneSamples
    val smsTemplate = preferences.smsTemplate
    val notificationTemplate = preferences.notificationTemplate
    val wakeWord = preferences.wakeWord
    val monitoredPackages = preferences.monitoredPackages
    val ttsPitch = preferences.ttsPitch
    val ttsSpeed = preferences.ttsSpeed
    val voiceStyle = preferences.voiceStyle

    // Permissions State
    private val _permissions = MutableStateFlow(checkPermissions())
    val permissions: StateFlow<PermissionStatus> = _permissions.asStateFlow()

    // Logs & Filtering
    private val _selectedLogFilter = MutableStateFlow<LogType?>(null)
    val selectedLogFilter: StateFlow<LogType?> = _selectedLogFilter.asStateFlow()

    val logs: StateFlow<List<JarvisLog>> = combine(
        repository.getAllLogs(),
        _selectedLogFilter
    ) { allLogs, filter ->
        if (filter == null) allLogs else allLogs.filter { it.type == filter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalLogsCount = repository.getTotalLogCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val missedCallsCount = repository.getLogCountByType(LogType.CALL_MISSED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val autoRepliesCount = repository.getLogCountByType(LogType.AUTO_REPLY_SENT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val wakeWordCount = repository.getLogCountByType(LogType.WAKE_WORD_DETECTED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- User Memories & Personality Vault ---
    val userMemories: StateFlow<List<UserMemory>> = repository.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Pending Questions (Questions needing user guidance) ---
    val activePendingQuestions: StateFlow<List<PendingQuestion>> = repository.getActivePendingQuestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastLearnedMemoryNotice = MutableStateFlow<String?>(null)
    val lastLearnedMemoryNotice: StateFlow<String?> = _lastLearnedMemoryNotice.asStateFlow()

    // --- Gemini Live Voice State ---
    private val _isLiveVoiceActive = MutableStateFlow(false)
    val isLiveVoiceActive: StateFlow<Boolean> = _isLiveVoiceActive.asStateFlow()

    private val _liveVoiceTranscript = MutableStateFlow("")
    val liveVoiceTranscript: StateFlow<String> = _liveVoiceTranscript.asStateFlow()

    private val _liveVoiceStatus = MutableStateFlow("Tap to start Gemini Live voice")
    val liveVoiceStatus: StateFlow<String> = _liveVoiceStatus.asStateFlow()

    // --- Dedicated Gemini Multi-turn Chatbot State & Grounding ---
    data class ChatbotUiMessage(
        val id: String = java.util.UUID.randomUUID().toString(),
        val sender: String, // "user" or "gemini"
        val message: String,
        val modelUsed: String? = null,
        val persona: ChatPersona? = null,
        val citations: List<GroundingCitation> = emptyList(),
        val searchQueries: List<String> = emptyList(),
        val learnedFact: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _selectedPersona = MutableStateFlow(ChatPersona.JARVIS_CORE)
    val selectedPersona: StateFlow<ChatPersona> = _selectedPersona.asStateFlow()

    private val _selectedModel = MutableStateFlow("gemini-3.5-flash")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _isSearchGroundingEnabled = MutableStateFlow(true)
    val isSearchGroundingEnabled: StateFlow<Boolean> = _isSearchGroundingEnabled.asStateFlow()

    private val _isMapsGroundingEnabled = MutableStateFlow(false)
    val isMapsGroundingEnabled: StateFlow<Boolean> = _isMapsGroundingEnabled.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatbotUiMessage>>(
        listOf(
            ChatbotUiMessage(
                sender = "gemini",
                message = "Hello! I am your Jarvis Gemini AI chatbot. I support multi-turn conversations, live Google Search data, Google Maps grounding, and intelligent model routing across Gemini 3.5 Flash, 3.1 Pro, and 3.1 Flash-Lite. How can I help you today?",
                modelUsed = "gemini-3.5-flash",
                persona = ChatPersona.JARVIS_CORE
            )
        )
    )
    val chatMessages: StateFlow<List<ChatbotUiMessage>> = _chatMessages.asStateFlow()

    private val _isChatbotGenerating = MutableStateFlow(false)
    val isChatbotGenerating: StateFlow<Boolean> = _isChatbotGenerating.asStateFlow()

    // --- Live Companion AI Dialogue State ---
    data class CompanionChatMessage(
        val id: String = java.util.UUID.randomUUID().toString(),
        val sender: String, // "user" or "companion"
        val message: String,
        val outgoingAction: OutgoingMessageAction? = null,
        val learnedInsight: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _companionChat = MutableStateFlow<List<CompanionChatMessage>>(
        listOf(
            CompanionChatMessage(
                sender = "companion",
                message = "Hello! I am Jarvis, your Gemini AI assistant. I can converse in real-time, perform background searches, answer your questions, and learn your communication style. What would you like to explore?"
            )
        )
    )
    val companionChat: StateFlow<List<CompanionChatMessage>> = _companionChat.asStateFlow()

    private val _isGeneratingCompanion = MutableStateFlow(false)
    val isGeneratingCompanion: StateFlow<Boolean> = _isGeneratingCompanion.asStateFlow()

    private val _companionMood = MutableStateFlow("Online & Ready")
    val companionMood: StateFlow<String> = _companionMood.asStateFlow()

    init {
        refreshPermissions()
        if (preferences.isServiceEnabledSync()) {
            JarvisForegroundService.startService(app)
        }

        // Connect continuous live speech listener
        speechManager.setOnLiveVoiceUtteranceListener { spokenText ->
            if (_isLiveVoiceActive.value && spokenText.isNotBlank()) {
                handleLiveVoiceInput(spokenText)
            }
        }
    }

    /**
     * Send message in dedicated Gemini Multi-turn Chat with model selection, persona roles, and grounding
     */
    fun sendChatMessage(userText: String, speak: Boolean = false) {
        if (userText.isBlank()) return
        val userMsg = ChatbotUiMessage(
            sender = "user",
            message = userText.trim(),
            persona = _selectedPersona.value,
            modelUsed = _selectedModel.value
        )
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isChatbotGenerating.value = true

            val history = _chatMessages.value.map { it.sender to it.message }
            val tone = preferences.getLearnedToneSamplesSync()
            val memories = repository.getRecentMemories(20).map { it.factOrRule }

            val reply = geminiRepository.chatWithGemini(
                message = userText.trim(),
                chatHistory = history,
                persona = _selectedPersona.value,
                selectedModel = _selectedModel.value,
                enableSearchGrounding = _isSearchGroundingEnabled.value,
                enableMapsGrounding = _isMapsGroundingEnabled.value,
                learnedTone = tone,
                userMemories = memories
            )

            // Save learned fact if discovered
            if (!reply.learnedFact.isNullOrBlank()) {
                repository.insertMemory(
                    category = MemoryCategory.PERSONAL_FACT,
                    factOrRule = reply.learnedFact,
                    sourceContext = "Gemini Chatbot"
                )
                _lastLearnedMemoryNotice.value = "💡 Learned: ${reply.learnedFact}"
            }

            val botMsg = ChatbotUiMessage(
                sender = "gemini",
                message = reply.text,
                modelUsed = reply.modelUsed,
                persona = _selectedPersona.value,
                citations = reply.citations,
                searchQueries = reply.searchQueries,
                learnedFact = reply.learnedFact
            )
            _chatMessages.value = _chatMessages.value + botMsg
            _isChatbotGenerating.value = false

            // Audibly speak reply if requested
            if (speak && reply.text.isNotBlank()) {
                speechManager.speak(reply.text)
            }

            // Log interaction
            repository.insertLog(
                type = LogType.WAKE_WORD_DETECTED,
                title = "Gemini Chat (${reply.modelUsed})",
                description = "User: \"$userText\"\nBot: \"${reply.text.take(80)}...\""
            )
        }
    }

    fun setChatPersona(persona: ChatPersona) {
        _selectedPersona.value = persona
        _selectedModel.value = persona.defaultModel
    }

    fun setSelectedModel(model: String) {
        _selectedModel.value = model
    }

    fun toggleSearchGrounding(enabled: Boolean) {
        _isSearchGroundingEnabled.value = enabled
    }

    fun toggleMapsGrounding(enabled: Boolean) {
        _isMapsGroundingEnabled.value = enabled
    }

    fun clearChatHistory() {
        _chatMessages.value = listOf(
            ChatbotUiMessage(
                sender = "gemini",
                message = "Conversation cleared. Ready for a new topic!",
                modelUsed = _selectedModel.value,
                persona = _selectedPersona.value
            )
        )
    }

    /**
     * Start Gemini Live Voice Mode
     */
    fun startLiveVoiceMode() {
        _isLiveVoiceActive.value = true
        _liveVoiceStatus.value = "Listening live... Speak anything!"
        speechManager.setLiveVoiceSessionActive(true)
        speechManager.playWakeConfirmationTone()
    }

    /**
     * Stop Gemini Live Voice Mode
     */
    fun stopLiveVoiceMode() {
        _isLiveVoiceActive.value = false
        _liveVoiceStatus.value = "Gemini Live ended."
        speechManager.setLiveVoiceSessionActive(false)
    }

    /**
     * Handle speech recognized during Gemini Live mode
     */
    private fun handleLiveVoiceInput(spokenText: String) {
        _liveVoiceTranscript.value = spokenText
        _liveVoiceStatus.value = "Thinking..."
        sendCompanionMessage(spokenText, speakOutput = true, fromLiveVoice = true)
    }

    /**
     * Talk with the companion AI (via text or voice input)
     */
    fun sendCompanionMessage(userText: String, speakOutput: Boolean = true, fromLiveVoice: Boolean = false) {
        if (userText.isBlank()) return
        val userMsg = CompanionChatMessage(sender = "user", message = userText)
        _companionChat.value = _companionChat.value + userMsg

        viewModelScope.launch {
            _isGeneratingCompanion.value = true
            _companionMood.value = "Listening & Processing..."

            val lower = userText.lowercase()

            // 1. Direct "Who messaged me?" query
            if (lower.contains("who messaged") || lower.contains("who texted") || lower.contains("koi message") || lower.contains("kasle message") || lower.contains("any message")) {
                val notifLogs = repository.getRecentLogsByType(LogType.NOTIFICATION_RECEIVED, limit = 5)
                val parsedMessages = notifLogs.map { log ->
                    val app = if (log.title.startsWith("[")) log.title.substringAfter("[").substringBefore("]") else "App"
                    val sender = if (log.title.contains("] ")) log.title.substringAfter("] ") else log.title
                    Triple(app, sender, log.description)
                }

                val summary = geminiRepository.summarizeIncomingMessages(
                    messages = parsedMessages,
                    learnedTone = preferences.getLearnedToneSamplesSync()
                )

                val companionMsg = CompanionChatMessage(
                    sender = "companion",
                    message = summary
                )
                _companionChat.value = _companionChat.value + companionMsg
                _isGeneratingCompanion.value = false
                _companionMood.value = "Online & Active"
                if (fromLiveVoice) _liveVoiceStatus.value = "Listening..."

                if (speakOutput) {
                    speechManager.speak(summary)
                }
                return@launch
            }

            // 2. Fetch active memory facts to feed into context
            val currentMemories = repository.getRecentMemories(20).map { it.factOrRule }
            val history = _companionChat.value.map { it.sender to it.message }
            val learnedTone = preferences.getLearnedToneSamplesSync()

            val response = geminiRepository.converseWithCompanion(
                userUtterance = userText,
                chatHistory = history,
                learnedTone = learnedTone,
                userMemories = currentMemories
            )

            // 3. Process Device Actions (Explicit app opening ONLY - never launch browser for background searches)
            if (!response.appToOpen.isNullOrBlank()) {
                val opened = DeviceActionExecutor.openApp(getApplication(), response.appToOpen)
                if (opened) {
                    repository.insertLog(
                        type = LogType.SERVICE_EVENT,
                        title = "App Opened",
                        description = "Opened ${response.appToOpen} upon voice command"
                    )
                }
            }

            // 4. Process Learned Memory Fact
            var learnedNotice: String? = null
            if (!response.learnedMemoryFact.isNullOrBlank()) {
                val cat = when (response.learnedMemoryCategory?.uppercase()) {
                    "STYLE_SLANG" -> MemoryCategory.STYLE_SLANG
                    "RELATIONSHIP" -> MemoryCategory.RELATIONSHIP
                    "DAILY_ROUTINE" -> MemoryCategory.DAILY_ROUTINE
                    "SEARCH_QUERY" -> MemoryCategory.SEARCH_QUERY
                    else -> MemoryCategory.PERSONAL_FACT
                }
                repository.insertMemory(cat, response.learnedMemoryFact, "Dialogue & Voice Sandbox")
                learnedNotice = "💡 Learned: ${response.learnedMemoryFact}"
                _lastLearnedMemoryNotice.value = learnedNotice
            }

            // 5. Append message to chat state
            val companionMsg = CompanionChatMessage(
                sender = "companion",
                message = response.dialogueResponse,
                outgoingAction = response.outgoingMessage,
                learnedInsight = learnedNotice
            )
            _companionChat.value = _companionChat.value + companionMsg
            _isGeneratingCompanion.value = false

            if (fromLiveVoice) {
                _liveVoiceStatus.value = "Listening..."
            }

            // 6. Update companion mood based on state
            if (response.appStateCommand == "SLEEP") {
                _companionMood.value = "Resting / Sleep Standby 🌙"
                speechManager.stopWakeWordListening()
            } else {
                _companionMood.value = "Online & Active"
            }

            // 7. Audible speech output
            if (speakOutput && response.dialogueResponse.isNotBlank()) {
                speechManager.speak(response.dialogueResponse)
            }

            // 8. Log outgoing message action if present
            if (response.outgoingMessage != null) {
                val action = response.outgoingMessage
                repository.insertLog(
                    type = LogType.AUTO_REPLY_SENT,
                    title = "Companion Tool Action: ${action.platform ?: "SMS"}",
                    description = "To: ${action.recipient ?: "Contact"}\nText: \"${action.messageText}\""
                )
            }
        }
    }

    /**
     * Respond to a Pending Question flagged for user guidance
     */
    fun respondToPendingQuestion(question: PendingQuestion, rawAnswer: String) {
        viewModelScope.launch {
            val learnedTone = preferences.getLearnedToneSamplesSync()
            val memories = repository.getRecentMemories(20).map { it.factOrRule }

            val transformedReply = geminiRepository.transformUserAnswerIntoPersonalStyle(
                userRawAnswer = rawAnswer,
                senderName = question.senderName,
                originalQuestion = question.extractedQuestion,
                learnedTone = learnedTone,
                userMemories = memories
            )

            val updated = question.copy(
                status = "ANSWERED",
                userAnswerRaw = rawAnswer,
                finalDispatchedReply = transformedReply
            )
            repository.updateQuestion(updated)

            // Log auto reply dispatch
            repository.insertLog(
                type = LogType.AUTO_REPLY_SENT,
                title = "AI Replied with User Answer on ${question.platform}",
                description = "To: ${question.senderName}\nRaw Input: \"$rawAnswer\"\nSent: \"$transformedReply\""
            )

            // Learn this interaction for future reference
            repository.insertMemory(
                category = MemoryCategory.PERSONAL_FACT,
                factOrRule = "When asked '${question.extractedQuestion}', user responded: '$rawAnswer'",
                sourceContext = "User Answer Escalation"
            )

            speechManager.speak("Sent to ${question.senderName}: \"$transformedReply\"")
        }
    }

    fun dismissPendingQuestion(id: Long) {
        viewModelScope.launch {
            repository.deleteQuestion(id)
        }
    }

    fun addManualMemory(category: MemoryCategory, fact: String) {
        if (fact.isBlank()) return
        viewModelScope.launch {
            repository.insertMemory(category, fact, "Manual Input")
            _lastLearnedMemoryNotice.value = "💡 Added to memory: $fact"
        }
    }

    fun deleteUserMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearAllMemories()
        }
    }

    fun clearMemoryNotice() {
        _lastLearnedMemoryNotice.value = null
    }

    /**
     * Trigger a spontaneous, proactive caring check-in
     */
    fun triggerProactiveCheckIn() {
        viewModelScope.launch {
            _isGeneratingCompanion.value = true
            _companionMood.value = "Checking in on you 💕"

            val timeOfDay = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
                in 5..11 -> "morning"
                in 12..17 -> "afternoon"
                in 18..22 -> "evening"
                else -> "late night"
            }

            val learnedTone = preferences.getLearnedToneSamplesSync()
            val checkInText = geminiRepository.generateProactiveCheckIn(timeOfDay, "working or relaxing", learnedTone)
            val checkInMsg = CompanionChatMessage(
                sender = "companion",
                message = checkInText
            )
            _companionChat.value = _companionChat.value + checkInMsg
            _isGeneratingCompanion.value = false
            _companionMood.value = "Devoted & Caring 💕"

            speechManager.playWakeConfirmationTone()
            speechManager.speak(checkInText)

            repository.insertLog(
                type = LogType.SERVICE_EVENT,
                title = "Proactive Check-In",
                description = checkInText
            )
        }
    }

    fun checkWhoMessagedMe() {
        sendCompanionMessage("Who messaged me recently?", speakOutput = true)
    }

    fun refreshPermissions() {
        _permissions.value = checkPermissions()
    }

    private fun checkPermissions(): PermissionStatus {
        val ctx = getApplication<Application>()
        val hasAudio = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasPhone = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val hasCallLog = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        val hasSms = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val hasContacts = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        val hasNotifListener = JarvisNotificationService.isNotificationAccessGranted(ctx)

        return PermissionStatus(
            hasAudio = hasAudio,
            hasPhoneState = hasPhone,
            hasCallLog = hasCallLog,
            hasSms = hasSms,
            hasContacts = hasContacts,
            hasNotifications = hasNotif,
            hasNotificationListener = hasNotifListener
        )
    }

    fun toggleMasterService(enabled: Boolean) {
        preferences.setServiceEnabled(enabled)
        if (enabled) {
            JarvisForegroundService.startService(getApplication())
        } else {
            JarvisForegroundService.stopService(getApplication())
        }
    }

    fun toggleVoiceWake(enabled: Boolean) {
        preferences.setVoiceWakeEnabled(enabled)
        if (enabled && isServiceRunning.value) {
            speechManager.startWakeWordListening()
        } else {
            speechManager.stopWakeWordListening()
        }
    }

    fun toggleMissedCallTts(enabled: Boolean) = preferences.setMissedCallTtsEnabled(enabled)
    fun toggleMissedCallSms(enabled: Boolean) = preferences.setMissedCallSmsEnabled(enabled)
    fun toggleNotificationReply(enabled: Boolean) = preferences.setNotificationReplyEnabled(enabled)
    fun toggleAnnounceMessages(enabled: Boolean) = preferences.setAnnounceMessagesEnabled(enabled)

    fun updateSmsTemplate(template: String) = preferences.setSmsTemplate(template)
    fun updateNotificationTemplate(template: String) = preferences.setNotificationTemplate(template)
    fun updateWakeWord(word: String) = preferences.setWakeWord(word)
    fun updateLearnedToneSamples(samples: String) = preferences.setLearnedToneSamples(samples)
    fun addLearnedToneSample(sample: String) = preferences.addLearnedToneSample(sample)

    fun toggleMonitoredPackage(pkg: String, enabled: Boolean) =
        preferences.togglePackageMonitoring(pkg, enabled)

    fun updateTtsSettings(pitch: Float, speed: Float) {
        preferences.setTtsPitch(pitch)
        preferences.setTtsSpeed(speed)
        speechManager.applyTtsPreferences()
    }

    fun updateVoiceStyle(style: String) {
        preferences.setVoiceStyle(style)
        speechManager.applyTtsPreferences()
    }

    fun testTts(text: String = "Hello! I am Jarvis, your Gemini personal assistant.") {
        speechManager.speak(text)
    }

    fun filterLogs(type: LogType?) {
        _selectedLogFilter.value = type
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            repository.deleteLog(id)
        }
    }

    /**
     * Interactive Simulation Methods to thoroughly test all features with Gemini AI
     */
    fun simulateMissedCall(callerName: String, callerNumber: String) {
        viewModelScope.launch {
            val receiver = CallReceiver()
            receiver.handleMissedCall(getApplication(), callerNumber)
        }
    }

    fun simulateNotificationAutoReply(packageName: String, senderName: String, incomingMessage: String) {
        viewModelScope.launch {
            val appName = when {
                packageName.contains("instagram") -> "Instagram"
                packageName.contains("whatsapp") -> "WhatsApp"
                packageName.contains("orca") -> "Messenger"
                packageName.contains("telegram") -> "Telegram"
                else -> packageName.substringAfterLast(".")
            }

            // 1. Log incoming notification
            repository.insertLog(
                type = LogType.NOTIFICATION_RECEIVED,
                title = "[$appName] $senderName",
                description = incomingMessage,
                sourcePackage = packageName
            )

            // 2. Audible Who Messaged Me announcement
            if (preferences.isAnnounceMessagesEnabledSync()) {
                val cleanContent = if (incomingMessage.length > 80) incomingMessage.take(77) + "..." else incomingMessage
                speechManager.speak("$senderName messaged you on $appName: $cleanContent")
            }

            // 3. Background Tone Training: Learn Roman Nepali and writing patterns
            preferences.addLearnedToneSample(incomingMessage)

            // 4. Generate Smart AI Reply using Gemini API with full conversation context and analysis
            val fallbackTemplate = preferences.getNotificationTemplateSync()
            val learnedTone = preferences.getLearnedToneSamplesSync()
            val priorLogs = repository.getRecentSenderLogs(senderName, limit = 5)
            val conversationHistory = priorLogs.map { log ->
                if (log.type == LogType.NOTIFICATION_RECEIVED) "$senderName: ${log.description}"
                else "Me: ${log.description}"
            }.reversed()

            val memories = repository.getRecentMemories(20).map { it.factOrRule }

            val result = geminiRepository.generateNotificationReplyWithAnalysis(
                appName = appName,
                senderName = senderName,
                incomingMessage = incomingMessage,
                conversationHistory = conversationHistory,
                defaultFallback = fallbackTemplate,
                learnedTone = learnedTone,
                userMemories = memories
            )

            // Check if this needs user clarification
            if (result.needsUserClarification && result.extractedQuestion != null) {
                repository.insertPendingQuestion(
                    senderName = senderName,
                    platform = appName,
                    incomingMessage = incomingMessage,
                    extractedQuestion = result.extractedQuestion
                )
                speechManager.speak("Babe, $senderName asked: \"${result.extractedQuestion}\". What should I tell them?")
            } else {
                // 5. Log simulated auto-reply dispatch
                repository.insertLog(
                    type = LogType.AUTO_REPLY_SENT,
                    title = "AI Auto-Replied on $appName",
                    description = "To: $senderName\nGenerated: \"${result.replyText}\"",
                    sourcePackage = packageName
                )
            }

            // Learn any new fact found in chat
            if (!result.learnedMemoryFact.isNullOrBlank()) {
                repository.insertMemory(
                    category = MemoryCategory.PERSONAL_FACT,
                    factOrRule = result.learnedMemoryFact,
                    sourceContext = "Auto-Reply from $appName"
                )
            }

            // 6. Audio feedback
            speechManager.playWakeConfirmationTone()
        }
    }

    fun simulateWakeWordTrigger() {
        speechManager.playWakeConfirmationTone()
        speechManager.speak("Yes, boss. Jarvis is online.")
        viewModelScope.launch {
            repository.insertLog(
                type = LogType.WAKE_WORD_DETECTED,
                title = "Wake Word Simulated",
                description = "Triggered test wake-word activation sequence.",
                extraData = "jarvis"
            )
        }
    }
}
