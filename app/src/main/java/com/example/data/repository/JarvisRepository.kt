package com.example.data.repository

import com.example.data.db.JarvisDao
import com.example.data.model.JarvisLog
import com.example.data.model.LogType
import com.example.data.model.MemoryCategory
import com.example.data.model.PendingQuestion
import com.example.data.model.UserMemory
import com.example.data.preferences.JarvisPreferences
import kotlinx.coroutines.flow.Flow

class JarvisRepository(
    private val dao: JarvisDao,
    val preferences: JarvisPreferences
) {
    fun getAllLogs(): Flow<List<JarvisLog>> = dao.getAllLogs()

    fun getLogsByType(type: LogType): Flow<List<JarvisLog>> = dao.getLogsByType(type)

    suspend fun getRecentLogsByType(type: LogType, limit: Int = 5): List<JarvisLog> =
        dao.getRecentLogsByTypeSync(type, limit)

    suspend fun getRecentLogs(limit: Int = 10): List<JarvisLog> =
        dao.getRecentLogsSync(limit)

    suspend fun getRecentSenderLogs(senderTitle: String, limit: Int = 6): List<JarvisLog> =
        dao.getRecentSenderLogsSync(senderTitle, limit)

    fun getLogCountByType(type: LogType): Flow<Int> = dao.getLogCountByType(type)

    fun getTotalLogCount(): Flow<Int> = dao.getTotalLogCount()

    suspend fun insertLog(
        type: LogType,
        title: String,
        description: String,
        sourcePackage: String = "",
        extraData: String = ""
    ): Long {
        val log = JarvisLog(
            type = type,
            title = title,
            description = description,
            sourcePackage = sourcePackage,
            extraData = extraData,
            timestamp = System.currentTimeMillis()
        )
        return dao.insertLog(log)
    }

    suspend fun clearLogs() = dao.clearAllLogs()

    suspend fun deleteLog(id: Long) = dao.deleteLogById(id)

    // --- User Memory & Learning Vault ---
    fun getAllMemories(): Flow<List<UserMemory>> = dao.getAllMemories()

    suspend fun getRecentMemories(limit: Int = 20): List<UserMemory> = dao.getRecentMemoriesSync(limit)

    fun getMemoriesByCategory(category: MemoryCategory): Flow<List<UserMemory>> = dao.getMemoriesByCategory(category)

    suspend fun insertMemory(
        category: MemoryCategory,
        factOrRule: String,
        sourceContext: String = "Conversation",
        confidence: Float = 1.0f
    ): Long {
        val memory = UserMemory(
            category = category,
            factOrRule = factOrRule,
            sourceContext = sourceContext,
            confidence = confidence,
            timestamp = System.currentTimeMillis()
        )
        return dao.insertMemory(memory)
    }

    suspend fun deleteMemory(id: Long) = dao.deleteMemoryById(id)

    suspend fun clearAllMemories() = dao.clearAllMemories()

    // --- Pending Questions ---
    fun getActivePendingQuestions(): Flow<List<PendingQuestion>> = dao.getActivePendingQuestions()

    fun getAllQuestions(): Flow<List<PendingQuestion>> = dao.getAllQuestions()

    suspend fun insertPendingQuestion(
        senderName: String,
        platform: String,
        incomingMessage: String,
        extractedQuestion: String
    ): Long {
        val question = PendingQuestion(
            senderName = senderName,
            platform = platform,
            incomingMessage = incomingMessage,
            extractedQuestion = extractedQuestion,
            status = "PENDING"
        )
        return dao.insertQuestion(question)
    }

    suspend fun updateQuestion(question: PendingQuestion) = dao.updateQuestion(question)

    suspend fun deleteQuestion(id: Long) = dao.deleteQuestionById(id)
}
