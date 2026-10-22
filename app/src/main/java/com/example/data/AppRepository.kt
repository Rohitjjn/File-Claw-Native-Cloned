package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppRepository(
    private val recentFileDao: RecentFileDao,
    private val settingDao: SettingDao
) {
    val allRecentFiles: Flow<List<RecentFileEntity>> = recentFileDao.getAllRecentFiles()

    val settings: Flow<SettingEntity> = settingDao.getSettingsFlow().map { it ?: SettingEntity() }

    suspend fun getSettingsDirect(): SettingEntity {
        return settingDao.getSettingsDirect() ?: SettingEntity()
    }

    suspend fun insertRecentFile(file: RecentFileEntity): Long {
        // Enforce existing file matching to prevent duplicates and preserve stable IDs
        val existing = recentFileDao.getRecentFileByPath(file.path)
        val entityToInsert = if (existing != null) {
            file.copy(id = existing.id)
        } else {
            if (file.id > 0) file else file.copy(id = 0)
        }
        val id = recentFileDao.insertRecentFile(entityToInsert)
        
        // Trim history based on settings limit
        val currentLimit = getSettingsDirect().historyLimit
        val count = recentFileDao.getRecentFileCount()
        if (count > currentLimit) {
            val toDelete = count - currentLimit
            val oldest = recentFileDao.getOldestFiles(toDelete)
            oldest.forEach { 
                recentFileDao.deleteRecentFileById(it.id)
            }
        }
        return id
    }

    suspend fun getRecentFileByPath(path: String): RecentFileEntity? {
        return recentFileDao.getRecentFileByPath(path)
    }

    suspend fun removeRecentFileById(id: Int) {
        recentFileDao.deleteRecentFileById(id)
    }

    suspend fun removeRecentFileByPath(path: String) {
        recentFileDao.deleteRecentFileByPath(path)
    }

    suspend fun clearHistory() {
        recentFileDao.clearAllRecentFiles()
    }

    suspend fun deleteSampleFiles() {
        recentFileDao.deleteSampleFiles()
    }

    suspend fun updateSettings(settings: SettingEntity) {
        settingDao.insertSettings(settings)
    }
}
