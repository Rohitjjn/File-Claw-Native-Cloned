package com.example.platform.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Android storage permission checks and system-settings launch policy. */
object StoragePermissionPolicy {
    fun requiresManageAllFilesAccess(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun hasRequiredPermissions(context: Context): Boolean = if (requiresManageAllFilesAccess()) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    fun standardPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    fun openManageAllFilesSettings(activity: Activity) {
        if (!requiresManageAllFilesAccess()) return
        try {
            activity.startActivity(
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
            )
        } catch (error: Exception) {
            try {
                activity.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            } catch (fallbackError: Exception) {
                fallbackError.printStackTrace()
            }
        }
    }
}
