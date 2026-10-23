package com.example.platform.intent

import android.content.Intent
import android.net.Uri

/** Routes externally delivered document intents separately from app-shortcut intents. */
object IncomingIntentRouter {
    fun dispatch(
        intent: Intent,
        onExternalDocument: (Uri) -> Unit,
        onShortcut: (Intent) -> Unit
    ) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let(onExternalDocument)
        } else {
            onShortcut(intent)
        }
    }
}
