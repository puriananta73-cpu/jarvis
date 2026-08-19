package com.example.service

import android.app.Notification
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.JarvisApplication
import com.example.data.model.LogType
import com.example.speech.JarvisSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground notification listener that intercepts incoming messaging & social alerts
 * (Instagram, WhatsApp, Messenger, SMS), queries the Gemini API for a natural contextual reply,
 * and delivers the response directly via Notification Action RemoteInput.
 */
class JarvisNotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "JarvisNotifService"

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        // Cache recent replies to avoid duplicate notification loops
        private val recentReplies = mutableMapOf<String, Long>()
        private const val REPLY_COOLDOWN_MS = 15000L // 15 seconds cooldown per sender/channel

        fun isNotificationAccessGranted(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            val myComponent = ComponentName(context, JarvisNotificationService::class.java).flattenToString()
            return flat?.contains(myComponent) == true
        }

        fun openNotificationAccessSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isConnected.value = true
        Log.d(TAG, "Jarvis Notification Listener Service Connected!")

        serviceScope.launch {
            JarvisApplication.instance.repository.insertLog(
                type = LogType.SERVICE_EVENT,
                title = "AI Notification Interceptor Connected",
                description = "Jarvis & Gemini are actively monitoring messages from target apps (Instagram, WhatsApp, etc.)."
            )
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isConnected.value = false
        Log.w(TAG, "Jarvis Notification Listener Service Disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val prefs = JarvisApplication.instance.preferences

        if (!prefs.isServiceEnabledSync() || !prefs.isNotificationReplyEnabledSync()) {
            return
        }

        // Check if package is in monitored packages list (Instagram, WhatsApp, Messenger, etc.)
        val isMonitored = packageName.contains("instagram", ignoreCase = true) ||
                packageName.contains("whatsapp", ignoreCase = true) ||
                packageName.contains("orca", ignoreCase = true) || // Facebook Messenger
                packageName.contains("telegram", ignoreCase = true) ||
                packageName.contains("messaging", ignoreCase = true)

        if (!isMonitored) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        // Ignore summary/empty/system group alerts
        if (title.isBlank() && text.isBlank()) return
        if (text.contains("typing...", ignoreCase = true) || text.contains("new messages", ignoreCase = true)) return

        val senderKey = "$packageName:$title"
        val now = System.currentTimeMillis()
        val lastReplyTime = recentReplies[senderKey] ?: 0L

        if (now - lastReplyTime < REPLY_COOLDOWN_MS) {
            Log.d(TAG, "Skipping notification from $senderKey due to cooldown")
            return
        }

        Log.d(TAG, "Intercepted message from [$packageName]: Sender='$title', Text='$text'")

        serviceScope.launch {
            val repository = JarvisApplication.instance.repository
            val geminiRepository = JarvisApplication.instance.geminiRepository
            val appLabel = getAppLabel(packageName)
            val incomingContent = text.ifEmpty { subText }

            // 1. Log incoming message
            repository.insertLog(
                type = LogType.NOTIFICATION_RECEIVED,
                title = "[$appLabel] $title",
                description = incomingContent,
                sourcePackage = packageName,
                extraData = sbn.key
            )

            // 2. Announce Who Messaged Me audibly if enabled
            if (prefs.isAnnounceMessagesEnabledSync()) {
                val speechManager = JarvisSpeechManager.getInstance(applicationContext)
                val cleanContent = if (incomingContent.length > 80) incomingContent.take(77) + "..." else incomingContent
                speechManager.speak("Babe, $title messaged you on $appLabel: $cleanContent")
            }

            // 3. Background Tone Training: Learn Roman Nepali and texting patterns
            prefs.addLearnedToneSample(incomingContent)

            // 4. Generate Smart AI Reply with Gemini API using learned tone profile
            val fallbackTemplate = prefs.getNotificationTemplateSync()
            val learnedTone = prefs.getLearnedToneSamplesSync()
            val aiReply = geminiRepository.generateNotificationReply(
                appName = appLabel,
                senderName = title,
                incomingMessage = incomingContent,
                defaultFallback = fallbackTemplate,
                learnedTone = learnedTone
            )

            // 5. Extract RemoteInput and dispatch auto-reply
            val replySuccess = executeAutoReply(notification, aiReply)

            if (replySuccess) {
                recentReplies[senderKey] = now
                repository.insertLog(
                    type = LogType.AUTO_REPLY_SENT,
                    title = "AI Auto-Replied on $appLabel",
                    description = "To: $title\nGenerated: \"$aiReply\"",
                    sourcePackage = packageName
                )

                // Optional speech announcement
                JarvisSpeechManager.getInstance(applicationContext).playWakeConfirmationTone()
            }
        }
    }

    /**
     * Finds RemoteInput from Notification Actions and dispatches auto-reply intent
     */
    private fun executeAutoReply(notification: Notification, replyMessage: String): Boolean {
        val actions = notification.actions ?: return false

        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            for (remoteInput in remoteInputs) {
                if (remoteInput.resultKey != null) {
                    try {
                        val replyIntent = Intent()
                        val bundle = Bundle().apply {
                            putCharSequence(remoteInput.resultKey, replyMessage)
                        }
                        RemoteInput.addResultsToIntent(arrayOf(remoteInput), replyIntent, bundle)

                        action.actionIntent.send(applicationContext, 0, replyIntent)
                        Log.d(TAG, "Successfully executed RemoteInput auto-reply action with text: $replyMessage")
                        return true
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send auto-reply via RemoteInput: ${e.message}", e)
                    }
                }
            }
        }
        return false
    }

    private fun getAppLabel(packageName: String): String {
        return when {
            packageName.contains("instagram") -> "Instagram"
            packageName.contains("whatsapp") -> "WhatsApp"
            packageName.contains("orca") -> "Messenger"
            packageName.contains("telegram") -> "Telegram"
            packageName.contains("messaging") -> "SMS"
            else -> packageName.substringAfterLast(".")
        }
    }
}
