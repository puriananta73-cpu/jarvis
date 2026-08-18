package com.example.data.repository

import com.example.data.db.JarvisDao
import com.example.data.model.JarvisLog
import com.example.data.model.LogType
import com.example.data.preferences.JarvisPreferences
import kotlinx.coroutines.flow.Flow

class JarvisRepository(
    private val dao: JarvisDao,
    val preferences: JarvisPreferences
) {
    fun getAllLogs(): Flow<List<JarvisLog>> = dao.getAllLogs()

    fun getLogsByType(type: LogType): Flow<List<JarvisLog>> = dao.getLogsByType(type)

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
}
