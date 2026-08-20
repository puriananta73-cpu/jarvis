package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MemoryCategory {
    STYLE_SLANG,
    PERSONAL_FACT,
    RELATIONSHIP,
    DAILY_ROUTINE,
    SEARCH_QUERY
}

@Entity(tableName = "user_memories")
data class UserMemory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: MemoryCategory,
    val factOrRule: String,
    val sourceContext: String = "Conversation",
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "pending_questions")
data class PendingQuestion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderName: String,
    val platform: String,
    val incomingMessage: String,
    val extractedQuestion: String,
    val status: String = "PENDING", // PENDING, ANSWERED, DISMISSED
    val userAnswerRaw: String? = null,
    val finalDispatchedReply: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
