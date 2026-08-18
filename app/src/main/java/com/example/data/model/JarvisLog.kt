package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LogType {
    CALL_MISSED,
    CALL_INCOMING,
    SMS_AUTO_SENT,
    NOTIFICATION_RECEIVED,
    AUTO_REPLY_SENT,
    WAKE_WORD_DETECTED,
    SERVICE_EVENT
}

@Entity(tableName = "jarvis_logs")
data class JarvisLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: LogType,
    val title: String,
    val description: String,
    val sourcePackage: String = "",
    val extraData: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
