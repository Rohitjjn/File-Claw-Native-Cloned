package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {
    @Query("SELECT * FROM recent_files ORDER BY lastOpened DESC")
    fun getAllRecentFiles(): Flow<List<RecentFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(file: RecentFileEntity): Long

    @Query("DELETE FROM recent_files WHERE id = :id")
    suspend fun deleteRecentFileById(id: Int)

    @Query("DELETE FROM recent_files WHERE path = :path")
    suspend fun deleteRecentFileByPath(path: String)

    @Query("SELECT * FROM recent_files WHERE path = :path LIMIT 1")
    suspend fun getRecentFileByPath(path: String): RecentFileEntity?

    @Query("DELETE FROM recent_files")
    suspend fun clearAllRecentFiles()

    @Query("DELETE FROM recent_files WHERE isSample = 1")
    suspend fun deleteSampleFiles()

    @Query("SELECT COUNT(*) FROM recent_files")
    suspend fun getRecentFileCount(): Int

    @Query("SELECT * FROM recent_files ORDER BY lastOpened ASC LIMIT :limit")
    suspend fun getOldestFiles(limit: Int): List<RecentFileEntity>
}
