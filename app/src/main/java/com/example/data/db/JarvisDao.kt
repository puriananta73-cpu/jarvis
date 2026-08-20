package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.JarvisLog
import com.example.data.model.LogType
import com.example.data.model.MemoryCategory
import com.example.data.model.PendingQuestion
import com.example.data.model.UserMemory
import kotlinx.coroutines.flow.Flow

@Dao
interface JarvisDao {
    @Query("SELECT * FROM jarvis_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<JarvisLog>>

    @Query("SELECT * FROM jarvis_logs WHERE type = :type ORDER BY timestamp DESC")
    fun getLogsByType(type: LogType): Flow<List<JarvisLog>>

    @Query("SELECT * FROM jarvis_logs WHERE type = :type ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogsByTypeSync(type: LogType, limit: Int = 5): List<JarvisLog>

    @Query("SELECT * FROM jarvis_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogsSync(limit: Int = 10): List<JarvisLog>

    @Query("SELECT * FROM jarvis_logs WHERE title LIKE '%' || :senderTitle || '%' OR description LIKE '%' || :senderTitle || '%' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSenderLogsSync(senderTitle: String, limit: Int = 6): List<JarvisLog>

    @Query("SELECT COUNT(*) FROM jarvis_logs WHERE type = :type")
    fun getLogCountByType(type: LogType): Flow<Int>

    @Query("SELECT COUNT(*) FROM jarvis_logs")
    fun getTotalLogCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: JarvisLog): Long

    @Query("DELETE FROM jarvis_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM jarvis_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    // --- User Memory & Learning Vault ---
    @Query("SELECT * FROM user_memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<UserMemory>>

    @Query("SELECT * FROM user_memories ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMemoriesSync(limit: Int = 20): List<UserMemory>

    @Query("SELECT * FROM user_memories WHERE category = :category ORDER BY timestamp DESC")
    fun getMemoriesByCategory(category: MemoryCategory): Flow<List<UserMemory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: UserMemory): Long

    @Query("DELETE FROM user_memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM user_memories")
    suspend fun clearAllMemories()

    // --- Pending Questions & Escalations ---
    @Query("SELECT * FROM pending_questions WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getActivePendingQuestions(): Flow<List<PendingQuestion>>

    @Query("SELECT * FROM pending_questions ORDER BY timestamp DESC")
    fun getAllQuestions(): Flow<List<PendingQuestion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: PendingQuestion): Long

    @Update
    suspend fun updateQuestion(question: PendingQuestion)

    @Query("DELETE FROM pending_questions WHERE id = :id")
    suspend fun deleteQuestionById(id: Long)
}
