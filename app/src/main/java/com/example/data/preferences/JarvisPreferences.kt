package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JarvisPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_SERVICE_ENABLED = "key_service_enabled"
        const val KEY_VOICE_WAKE_ENABLED = "key_voice_wake_enabled"
        const val KEY_MISSED_CALL_TTS_ENABLED = "key_missed_call_tts_enabled"
        const val KEY_MISSED_CALL_SMS_ENABLED = "key_missed_call_sms_enabled"
        const val KEY_NOTIFICATION_REPLY_ENABLED = "key_notification_reply_enabled"
        const val KEY_SMS_TEMPLATE = "key_sms_template"
        const val KEY_NOTIFICATION_TEMPLATE = "key_notification_template"
        const val KEY_MONITORED_PACKAGES = "key_monitored_packages"
        const val KEY_TTS_PITCH = "key_tts_pitch"
        const val KEY_TTS_SPEED = "key_tts_speed"
        const val KEY_TTS_VOICE_STYLE = "key_tts_voice_style"
        const val KEY_WAKE_WORD = "key_wake_word"
        const val KEY_ANNOUNCE_MESSAGES = "key_announce_incoming_messages"
        const val KEY_LEARNED_TONE_SAMPLES = "key_learned_tone_samples"

        const val DEFAULT_SMS_TEMPLATE =
            "hey, missed your call! tied up rn, will call you in a bit."
        const val DEFAULT_NOTIF_TEMPLATE =
            "thik xa hai, timi sunau!"
        const val DEFAULT_WAKE_WORD = "jarvis"
        const val DEFAULT_VOICE_STYLE = "GEMINI_NATURAL_FEMALE"
        const val DEFAULT_PACKAGES = "com.instagram.android,com.whatsapp,com.facebook.orca,org.telegram.messenger"
        const val DEFAULT_TONE_SAMPLES = "k gardai xau? | aaja vetne ho? | thik xa hai | paxi kura garumla | ma aaudai xu | khana khayeu?"
    }

    private val _isAnnounceMessagesEnabled = MutableStateFlow(prefs.getBoolean(KEY_ANNOUNCE_MESSAGES, true))
    val isAnnounceMessagesEnabled: StateFlow<Boolean> = _isAnnounceMessagesEnabled.asStateFlow()

    private val _learnedToneSamples = MutableStateFlow(
        prefs.getString(KEY_LEARNED_TONE_SAMPLES, DEFAULT_TONE_SAMPLES) ?: DEFAULT_TONE_SAMPLES
    )
    val learnedToneSamples: StateFlow<String> = _learnedToneSamples.asStateFlow()

    private val _isServiceEnabled = MutableStateFlow(prefs.getBoolean(KEY_SERVICE_ENABLED, true))
    val isServiceEnabled: StateFlow<Boolean> = _isServiceEnabled.asStateFlow()

    private val _isVoiceWakeEnabled = MutableStateFlow(prefs.getBoolean(KEY_VOICE_WAKE_ENABLED, true))
    val isVoiceWakeEnabled: StateFlow<Boolean> = _isVoiceWakeEnabled.asStateFlow()

    private val _isMissedCallTtsEnabled = MutableStateFlow(prefs.getBoolean(KEY_MISSED_CALL_TTS_ENABLED, true))
    val isMissedCallTtsEnabled: StateFlow<Boolean> = _isMissedCallTtsEnabled.asStateFlow()

    private val _isMissedCallSmsEnabled = MutableStateFlow(prefs.getBoolean(KEY_MISSED_CALL_SMS_ENABLED, true))
    val isMissedCallSmsEnabled: StateFlow<Boolean> = _isMissedCallSmsEnabled.asStateFlow()

    private val _isNotificationReplyEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATION_REPLY_ENABLED, true))
    val isNotificationReplyEnabled: StateFlow<Boolean> = _isNotificationReplyEnabled.asStateFlow()

    private val _smsTemplate = MutableStateFlow(prefs.getString(KEY_SMS_TEMPLATE, DEFAULT_SMS_TEMPLATE) ?: DEFAULT_SMS_TEMPLATE)
    val smsTemplate: StateFlow<String> = _smsTemplate.asStateFlow()

    private val _notificationTemplate = MutableStateFlow(prefs.getString(KEY_NOTIFICATION_TEMPLATE, DEFAULT_NOTIF_TEMPLATE) ?: DEFAULT_NOTIF_TEMPLATE)
    val notificationTemplate: StateFlow<String> = _notificationTemplate.asStateFlow()

    private val _wakeWord = MutableStateFlow(prefs.getString(KEY_WAKE_WORD, DEFAULT_WAKE_WORD) ?: DEFAULT_WAKE_WORD)
    val wakeWord: StateFlow<String> = _wakeWord.asStateFlow()

    private val _voiceStyle = MutableStateFlow(prefs.getString(KEY_TTS_VOICE_STYLE, DEFAULT_VOICE_STYLE) ?: DEFAULT_VOICE_STYLE)
    val voiceStyle: StateFlow<String> = _voiceStyle.asStateFlow()

    private val _monitoredPackages = MutableStateFlow(
        prefs.getString(KEY_MONITORED_PACKAGES, DEFAULT_PACKAGES)?.split(",")?.toSet() ?: setOf("com.instagram.android")
    )
    val monitoredPackages: StateFlow<Set<String>> = _monitoredPackages.asStateFlow()

    private val _ttsPitch = MutableStateFlow(prefs.getFloat(KEY_TTS_PITCH, 1.0f))
    val ttsPitch: StateFlow<Float> = _ttsPitch.asStateFlow()

    private val _ttsSpeed = MutableStateFlow(prefs.getFloat(KEY_TTS_SPEED, 1.05f))
    val ttsSpeed: StateFlow<Float> = _ttsSpeed.asStateFlow()

    fun setServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
        _isServiceEnabled.value = enabled
    }

    fun setVoiceWakeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_WAKE_ENABLED, enabled).apply()
        _isVoiceWakeEnabled.value = enabled
    }

    fun setMissedCallTtsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MISSED_CALL_TTS_ENABLED, enabled).apply()
        _isMissedCallTtsEnabled.value = enabled
    }

    fun setMissedCallSmsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MISSED_CALL_SMS_ENABLED, enabled).apply()
        _isMissedCallSmsEnabled.value = enabled
    }

    fun setNotificationReplyEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_REPLY_ENABLED, enabled).apply()
        _isNotificationReplyEnabled.value = enabled
    }

    fun setSmsTemplate(template: String) {
        prefs.edit().putString(KEY_SMS_TEMPLATE, template).apply()
        _smsTemplate.value = template
    }

    fun setNotificationTemplate(template: String) {
        prefs.edit().putString(KEY_NOTIFICATION_TEMPLATE, template).apply()
        _notificationTemplate.value = template
    }

    fun setWakeWord(word: String) {
        val clean = word.trim().lowercase()
        prefs.edit().putString(KEY_WAKE_WORD, clean).apply()
        _wakeWord.value = clean
    }

    fun setVoiceStyle(style: String) {
        prefs.edit().putString(KEY_TTS_VOICE_STYLE, style).apply()
        _voiceStyle.value = style
    }

    fun setMonitoredPackages(packages: Set<String>) {
        prefs.edit().putString(KEY_MONITORED_PACKAGES, packages.joinToString(",")).apply()
        _monitoredPackages.value = packages
    }

    fun togglePackageMonitoring(pkg: String, enabled: Boolean) {
        val current = _monitoredPackages.value.toMutableSet()
        if (enabled) current.add(pkg) else current.remove(pkg)
        setMonitoredPackages(current)
    }

    fun setTtsPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_TTS_PITCH, pitch).apply()
        _ttsPitch.value = pitch
    }

    fun setTtsSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_TTS_SPEED, speed).apply()
        _ttsSpeed.value = speed
    }

    fun setAnnounceMessagesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANNOUNCE_MESSAGES, enabled).apply()
        _isAnnounceMessagesEnabled.value = enabled
    }

    fun setLearnedToneSamples(samples: String) {
        prefs.edit().putString(KEY_LEARNED_TONE_SAMPLES, samples).apply()
        _learnedToneSamples.value = samples
    }

    fun addLearnedToneSample(newSample: String) {
        val clean = newSample.trim()
        if (clean.length < 3 || clean.length > 200) return
        val current = _learnedToneSamples.value.split(" | ").toMutableList()
        if (!current.contains(clean)) {
            current.add(0, clean)
            val trimmed = current.take(25).joinToString(" | ")
            setLearnedToneSamples(trimmed)
        }
    }

    // Direct synchronous getters for background services and broadcast receivers
    fun isServiceEnabledSync(): Boolean = prefs.getBoolean(KEY_SERVICE_ENABLED, true)
    fun isVoiceWakeEnabledSync(): Boolean = prefs.getBoolean(KEY_VOICE_WAKE_ENABLED, true)
    fun isMissedCallTtsEnabledSync(): Boolean = prefs.getBoolean(KEY_MISSED_CALL_TTS_ENABLED, true)
    fun isMissedCallSmsEnabledSync(): Boolean = prefs.getBoolean(KEY_MISSED_CALL_SMS_ENABLED, true)
    fun isNotificationReplyEnabledSync(): Boolean = prefs.getBoolean(KEY_NOTIFICATION_REPLY_ENABLED, true)
    fun isAnnounceMessagesEnabledSync(): Boolean = prefs.getBoolean(KEY_ANNOUNCE_MESSAGES, true)
    fun getLearnedToneSamplesSync(): String = prefs.getString(KEY_LEARNED_TONE_SAMPLES, DEFAULT_TONE_SAMPLES) ?: DEFAULT_TONE_SAMPLES
    fun getSmsTemplateSync(): String = prefs.getString(KEY_SMS_TEMPLATE, DEFAULT_SMS_TEMPLATE) ?: DEFAULT_SMS_TEMPLATE
    fun getNotificationTemplateSync(): String = prefs.getString(KEY_NOTIFICATION_TEMPLATE, DEFAULT_NOTIF_TEMPLATE) ?: DEFAULT_NOTIF_TEMPLATE
    fun getWakeWordSync(): String = prefs.getString(KEY_WAKE_WORD, DEFAULT_WAKE_WORD) ?: DEFAULT_WAKE_WORD
    fun getVoiceStyleSync(): String = prefs.getString(KEY_TTS_VOICE_STYLE, DEFAULT_VOICE_STYLE) ?: DEFAULT_VOICE_STYLE
    fun getMonitoredPackagesSync(): Set<String> =
        prefs.getString(KEY_MONITORED_PACKAGES, DEFAULT_PACKAGES)?.split(",")?.toSet() ?: setOf("com.instagram.android")
    fun getTtsPitchSync(): Float = prefs.getFloat(KEY_TTS_PITCH, 1.0f)
    fun getTtsSpeedSync(): Float = prefs.getFloat(KEY_TTS_SPEED, 1.05f)
}
