package com.example.platform.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Shared FileProvider-based share flow with the existing path-only fallback. */
object FileShareHelper {
    fun share(context: Context, file: File, extension: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val mimeType = when (extension.lowercase()) {
                "pdf" -> "application/pdf"
                "csv" -> "text/csv"
                "zip" -> "application/zip"
                "png", "jpg", "jpeg", "webp" -> "image/*"
                "mp3", "wav", "m4a" -> "audio/*"
                "mp4", "mkv" -> "video/*"
                else -> "text/plain"
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share File"))
        } catch (error: Exception) {
            error.printStackTrace()
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "File Path: ${file.absolutePath}")
                }
                context.startActivity(Intent.createChooser(intent, "Share File Path"))
            } catch (fallbackError: Exception) {
                fallbackError.printStackTrace()
            }
        }
    }
}
