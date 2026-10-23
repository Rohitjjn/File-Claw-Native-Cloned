package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val id: Int = 1,
    val theme: String = "Light", // "Light", "Dark", "System"
    val fontSize: String = "Medium", // "Small", "Medium", "Large"
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = true,
    val autoSaveDrafts: Boolean = true,
    val defaultToEditOnOpen: Boolean = false,
    val tabSize: Int = 4, // 2 or 4
    val defaultEncoding: String = "Auto", // "Auto", "UTF-8", "UTF-16", "ASCII", "ISO-8859-1"
    val historyLimit: Int = 20,
    val notificationFileOpen: Boolean = true,
    val notificationSaveComplete: Boolean = true,
    val notificationLowStorage: Boolean = true
)
