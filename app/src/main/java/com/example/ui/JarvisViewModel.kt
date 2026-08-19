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
import com.example.data.preferences.JarvisPreferences
import com.example.receiver.CallReceiver
import com.example.service.JarvisForegroundService
import com.example.service.JarvisNotificationService
import com.example.speech.JarvisSpeechManager
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

    // --- Live Companion AI Dialogue State ---
    data class CompanionChatMessage(
        val id: String = java.util.UUID.randomUUID().toString(),
        val sender: String, // "user" or "companion"
        val message: String,
        val outgoingAction: com.example.data.repository.OutgoingMessageAction? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _companionChat = MutableStateFlow<List<CompanionChatMessage>>(
        listOf(
            CompanionChatMessage(
                sender = "companion",
                message = "Hey you 💕 I'm right here with you. How are you feeling today? Did you eat yet?"
            )
        )
    )
    val companionChat: StateFlow<List<CompanionChatMessage>> = _companionChat.asStateFlow()

    private val _isGeneratingCompanion = MutableStateFlow(false)
    val isGeneratingCompanion: StateFlow<Boolean> = _isGeneratingCompanion.asStateFlow()

    private val _companionMood = MutableStateFlow("Devoted & Caring 💕")
    val companionMood: StateFlow<String> = _companionMood.asStateFlow()

    init {
        refreshPermissions()
        // If enabled, ensure service is running
        if (preferences.isServiceEnabledSync()) {
            JarvisForegroundService.startService(app)
        }
    }

    /**
     * Talk with the companion AI (via text or voice transcribed input)
     */
    fun sendCompanionMessage(userText: String, speakOutput: Boolean = true) {
        if (userText.isBlank()) return
        val userMsg = CompanionChatMessage(sender = "user", message = userText)
        _companionChat.value = _companionChat.value + userMsg

        viewModelScope.launch {
            _isGeneratingCompanion.value = true
            _companionMood.value = "Listening & Thinking..."

            val lower = userText.lowercase()
            // Check if user specifically asked who messaged them
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
                _companionMood.value = "Devoted & Caring 💕"

                if (speakOutput) {
                    speechManager.speak(summary)
                }
                return@launch
            }

            val history = _companionChat.value.map { it.sender to it.message }
            val learnedTone = preferences.getLearnedToneSamplesSync()
            val response = geminiRepository.converseWithCompanion(userText, history, learnedTone)

            val companionMsg = CompanionChatMessage(
                sender = "companion",
                message = response.dialogueResponse,
                outgoingAction = response.outgoingMessage
            )
            _companionChat.value = _companionChat.value + companionMsg
            _isGeneratingCompanion.value = false

            // Update companion mood based on state
            if (response.appStateCommand == "SLEEP") {
                _companionMood.value = "Resting / Sleep Standby 🌙"
                speechManager.stopWakeWordListening()
            } else {
                _companionMood.value = "Devoted & Caring 💕"
            }

            // Speak response via TTS if enabled
            if (speakOutput && response.dialogueResponse.isNotBlank()) {
                speechManager.speak(response.dialogueResponse)
            }

            // Log action if outgoing message was created
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

    fun testTts(text: String = "Hello! I am Jarvis, your hands-free background assistant.") {
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
                speechManager.speak("Babe, $senderName messaged you on $appName: $cleanContent")
            }

            // 3. Background Tone Training: Learn Roman Nepali and writing patterns
            preferences.addLearnedToneSample(incomingMessage)

            // 4. Generate Smart AI Reply using Gemini API
            val fallbackTemplate = preferences.getNotificationTemplateSync()
            val learnedTone = preferences.getLearnedToneSamplesSync()
            val aiReply = geminiRepository.generateNotificationReply(
                appName = appName,
                senderName = senderName,
                incomingMessage = incomingMessage,
                defaultFallback = fallbackTemplate,
                learnedTone = learnedTone
            )

            // 5. Log simulated auto-reply dispatch
            repository.insertLog(
                type = LogType.AUTO_REPLY_SENT,
                title = "AI Auto-Replied on $appName",
                description = "To: $senderName\nGenerated: \"$aiReply\"",
                sourcePackage = packageName
            )

            // 6. Audio & TTS feedback
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
