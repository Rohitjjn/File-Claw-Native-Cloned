package com.example.platform.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.core.format.FormatRegistry
import java.io.File

/** Shared FileProvider-based share flow with the existing path-only fallback. */
object FileShareHelper {
    fun share(context: Context, file: File, extension: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            // Reuse the single format registry so OOXML variants and legacy
            // Office files are shared with their real MIME type rather than
            // being labelled as plain text.
            val mimeType = FormatRegistry.byExtension(extension)?.mime
                ?: "application/octet-stream"
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
