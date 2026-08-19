package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.JarvisApplication
import com.example.MainActivity
import com.example.R
import com.example.data.model.LogType
import com.example.speech.JarvisSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JarvisForegroundService : Service() {

    companion object {
        private const val TAG = "JarvisForegroundService"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_JARVIS_SERVICE"
        const val ACTION_STOP = "ACTION_STOP_JARVIS_SERVICE"
        const val ACTION_TOGGLE_MIC = "ACTION_TOGGLE_MIC"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun startService(context: Context) {
            try {
                val intent = Intent(context, JarvisForegroundService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting JarvisForegroundService: ${e.message}", e)
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, JarvisForegroundService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping JarvisForegroundService: ${e.message}", e)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var speechManager: JarvisSpeechManager

    override fun onCreate() {
        super.onCreate()
        speechManager = JarvisSpeechManager.getInstance(this)
        setupWakeWordListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_MIC -> {
                val prefs = JarvisApplication.instance.preferences
                val newState = !prefs.isVoiceWakeEnabledSync()
                prefs.setVoiceWakeEnabled(newState)
                val hasRecordAudio = ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (newState && hasRecordAudio) {
                    speechManager.startWakeWordListening()
                } else {
                    speechManager.stopWakeWordListening()
                }
                updateNotification()
            }
            ACTION_START, null -> {
                startForegroundService()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = buildForegroundNotification()

        val hasRecordAudio = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasRecordAudio) {
                    serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
                startForeground(NOTIFICATION_ID, notification, serviceType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting FGS with microphone type: ${e.message}. Retrying with SPECIAL_USE only.")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback startForeground failed: ${e2.message}")
                try {
                    startForeground(NOTIFICATION_ID, notification)
                } catch (e3: Exception) {
                    Log.e(TAG, "Default startForeground failed: ${e3.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in startForeground: ${e.message}")
        }

        _isRunning.value = true

        val prefs = JarvisApplication.instance.preferences
        if (prefs.isVoiceWakeEnabledSync() && hasRecordAudio) {
            speechManager.startWakeWordListening()
        }

        serviceScope.launch {
            JarvisApplication.instance.repository.insertLog(
                type = LogType.SERVICE_EVENT,
                title = "Jarvis Online",
                description = "Foreground background service active. Call monitor, SMS trigger, and wake-word ready."
            )
        }
    }

    private fun stopForegroundService() {
        _isRunning.value = false
        speechManager.stopWakeWordListening()
        serviceScope.launch {
            JarvisApplication.instance.repository.insertLog(
                type = LogType.SERVICE_EVENT,
                title = "Jarvis Offline",
                description = "Jarvis foreground background service stopped."
            )
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun setupWakeWordListener() {
        speechManager.setOnWakeWordDetectedListener { phrase ->
            Log.d(TAG, "Wake word triggered: $phrase")
            onWakeWordDetected(phrase)
        }
        speechManager.setOnSleepWordDetectedListener { phrase ->
            Log.d(TAG, "Sleep word triggered: $phrase")
            onSleepWordDetected(phrase)
        }
        speechManager.setOnQueryMessagesListener { phrase ->
            Log.d(TAG, "Query messages triggered: $phrase")
            onQueryMessagesDetected(phrase)
        }
    }

    private fun onQueryMessagesDetected(phrase: String) {
        serviceScope.launch {
            val repository = JarvisApplication.instance.repository
            val geminiRepo = JarvisApplication.instance.geminiRepository
            val prefs = JarvisApplication.instance.preferences
            val notifLogs = repository.getRecentLogsByType(LogType.NOTIFICATION_RECEIVED, limit = 5)

            val parsedMessages = notifLogs.map { log ->
                val app = if (log.title.startsWith("[")) log.title.substringAfter("[").substringBefore("]") else "App"
                val sender = if (log.title.contains("] ")) log.title.substringAfter("] ") else log.title
                Triple(app, sender, log.description)
            }

            val summary = geminiRepo.summarizeIncomingMessages(
                messages = parsedMessages,
                learnedTone = prefs.getLearnedToneSamplesSync()
            )

            speechManager.speak(summary)

            repository.insertLog(
                type = LogType.SERVICE_EVENT,
                title = "Who Messaged Me Announced",
                description = "Answered inquiry \"$phrase\": $summary"
            )
        }
    }

    private fun onWakeWordDetected(phrase: String) {
        serviceScope.launch {
            JarvisApplication.instance.repository.insertLog(
                type = LogType.WAKE_WORD_DETECTED,
                title = "Companion Activated",
                description = "Heard: \"$phrase\". Activated companion HUD.",
                extraData = phrase
            )
        }

        // Announce warm companion greeting via TTS
        speechManager.speak("Hey! I'm right here with you 💕")

        // Bring MainActivity to the front
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_TRIGGER_REASON", "wake_word")
            putExtra("EXTRA_DETECTED_PHRASE", phrase)
        }
        startActivity(openIntent)
    }

    private fun onSleepWordDetected(phrase: String) {
        serviceScope.launch {
            JarvisApplication.instance.repository.insertLog(
                type = LogType.SERVICE_EVENT,
                title = "Sleep Mode",
                description = "Heard sleep command: \"$phrase\". Entering standby sleep mode.",
                extraData = phrase
            )
        }

        // Announce warm affectionate goodnight
        speechManager.speak("Going to sleep now. Call me anytime you need me 💕")
        speechManager.stopWakeWordListening()
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, JarvisForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleMicIntent = Intent(this, JarvisForegroundService::class.java).apply {
            action = ACTION_TOGGLE_MIC
        }
        val toggleMicPendingIntent = PendingIntent.getService(
            this, 2, toggleMicIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isMicActive = JarvisApplication.instance.preferences.isVoiceWakeEnabledSync()
        val micStatus = if (isMicActive) "Wake-Word Active (\"Jarvis\")" else "Wake-Word Paused"

        return NotificationCompat.Builder(this, JarvisApplication.FOREGROUND_CHANNEL_ID)
            .setContentTitle("Jarvis Assistant Active")
            .setContentText("Monitoring calls, notification auto-replies & $micStatus")
            .setSmallIcon(R.drawable.ic_jarvis_logo)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_menu_camera, if (isMicActive) "Mute Mic" else "Enable Mic", toggleMicPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Jarvis", stopPendingIntent)
            .build()
    }

    private fun updateNotification() {
        val notification = buildForegroundNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        speechManager.stopWakeWordListening()
        serviceScope.cancel()
    }
}
