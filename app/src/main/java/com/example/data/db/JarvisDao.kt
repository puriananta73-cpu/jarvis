package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.JarvisLog
import com.example.data.model.LogType
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
}
