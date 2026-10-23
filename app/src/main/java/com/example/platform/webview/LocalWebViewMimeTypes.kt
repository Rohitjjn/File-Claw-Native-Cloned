package com.example.platform.webview

/** MIME overrides required for locally served WebView assets. */
object LocalWebViewMimeTypes {
    const val JAVASCRIPT = "text/javascript"

    /** Returns the explicit JavaScript MIME type for script assets; other types use AndroidX's default. */
    fun overrideForAsset(path: String): String? = when (path.substringAfterLast('.', "").lowercase()) {
        "js", "mjs" -> JAVASCRIPT
        else -> null
    }
}
