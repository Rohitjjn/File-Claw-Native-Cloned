package com.example.platform.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

/** Shared Android clipboard actions and the existing user feedback. */
object ClipboardHelper {
    fun copyText(
        context: Context,
        title: String,
        text: String,
        feedback: String = "Text copied to clipboard"
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(title, text))
        Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show()
    }

    fun showTextOperationError(context: Context, error: Throwable) {
        val message = error.message ?: "Could not prepare the complete file"
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
