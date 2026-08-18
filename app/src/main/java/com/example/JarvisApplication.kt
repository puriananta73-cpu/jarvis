package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.db.JarvisDatabase
import com.example.data.preferences.JarvisPreferences
import com.example.data.repository.GeminiRepository
import com.example.data.repository.JarvisRepository

class JarvisApplication : Application() {

    companion object {
        const val FOREGROUND_CHANNEL_ID = "jarvis_foreground_service_channel"
        const val ALERTS_CHANNEL_ID = "jarvis_assistant_alerts_channel"

        lateinit var instance: JarvisApplication
            private set
    }

    val database: JarvisDatabase by lazy { JarvisDatabase.getInstance(this) }
    val preferences: JarvisPreferences by lazy { JarvisPreferences(this) }
    val repository: JarvisRepository by lazy { JarvisRepository(database.jarvisDao(), preferences) }
    val geminiRepository: GeminiRepository by lazy { GeminiRepository() }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Foreground Service Persistent Channel
            val foregroundChannel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "Jarvis Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent background status for Jarvis hands-free assistant"
                setShowBadge(false)
            }

            // High Priority Alert Channel
            val alertChannel = NotificationChannel(
                ALERTS_CHANNEL_ID,
                "Jarvis Assistant Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for missed calls, auto-replies, and assistant voice activations"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(foregroundChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }
}
