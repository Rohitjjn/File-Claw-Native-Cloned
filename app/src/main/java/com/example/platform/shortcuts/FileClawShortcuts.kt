package com.example.platform.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R

/** Registers the existing Home-screen shortcuts without mixing shortcut policy into navigation UI. */
object FileClawShortcuts {
    const val ACTION_LAST_OPENED = "com.example.action.LAST_OPENED"
    const val ACTION_HISTORY = "com.example.action.HISTORY"
    const val ACTION_SETTINGS = "com.example.action.SETTINGS"

    fun register(context: Context) {
        try {
            val shortcuts = listOf(
                shortcut(
                    context,
                    id = "shortcut_last_opened",
                    shortLabel = "Last Opened File",
                    longLabel = "Open the last opened file",
                    icon = R.drawable.ic_shortcut_last_opened,
                    action = ACTION_LAST_OPENED
                ),
                shortcut(
                    context,
                    id = "shortcut_history",
                    shortLabel = "History",
                    longLabel = "View recent files history",
                    icon = R.drawable.ic_shortcut_history,
                    action = ACTION_HISTORY
                ),
                shortcut(
                    context,
                    id = "shortcut_settings",
                    shortLabel = "Settings",
                    longLabel = "Open app settings",
                    icon = R.drawable.ic_shortcut_settings,
                    action = ACTION_SETTINGS
                )
            )
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    private fun shortcut(
        context: Context,
        id: String,
        shortLabel: String,
        longLabel: String,
        icon: Int,
        action: String
    ): ShortcutInfoCompat = ShortcutInfoCompat.Builder(context, id)
        .setShortLabel(shortLabel)
        .setLongLabel(longLabel)
        .setIcon(IconCompat.createWithResource(context, icon))
        .setIntent(
            Intent(context, MainActivity::class.java).apply {
                this.action = action
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        )
        .build()
}
