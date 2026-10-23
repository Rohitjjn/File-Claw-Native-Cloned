package com.example.formats.pdf

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Opens a PDF with an installed external viewer using a scoped content URI. */
object PdfIntentOpener {
    fun open(context: Context, file: File) {
        if (!file.isFile || !file.canRead()) {
            android.widget.Toast.makeText(context, "PDF file is unavailable or cannot be read", android.widget.Toast.LENGTH_LONG).show()
            return
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Open PDF with"))
        } catch (_: android.content.ActivityNotFoundException) {
            android.widget.Toast.makeText(context, "No PDF viewer is installed", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
