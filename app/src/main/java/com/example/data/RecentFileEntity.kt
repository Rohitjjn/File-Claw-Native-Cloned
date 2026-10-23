package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "recent_files",
    indices = [
        Index(value = ["lastOpened"], orders = [Index.Order.DESC]),
        Index(value = ["path"])
    ]
)
data class RecentFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val path: String,
    val name: String,
    val size: Long,
    val extension: String,
    val lastOpened: Long,
    val isSample: Boolean = false,
    val parentZipPath: String? = null,
    val zipEntryPath: String? = null,
    val sourceUri: String? = null
)
