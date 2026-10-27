package com.example.platform.webview

import android.content.Context
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader

/**
 * Serves packaged assets with an explicit standard JavaScript MIME type for `.js` and `.mjs`.
 * WebView's module loader applies strict MIME checks, so the platform's extension guess is not
 * relied on for bundled renderer scripts.
 */
class MimeAwareAssetsPathHandler(context: Context) : WebViewAssetLoader.PathHandler {
    private val assetsHandler = WebViewAssetLoader.AssetsPathHandler(context)

    override fun handle(path: String): WebResourceResponse? {
        // Asset paths come from a fixed local origin, but still reject traversal
        // and encoded separators before delegating to AndroidX's asset opener.
        if (path.isBlank() || path.startsWith("/") || path.contains("..") ||
            path.contains("\\") || path.contains("%2f", ignoreCase = true) ||
            path.contains("%5c", ignoreCase = true)
        ) return null
        val response = assetsHandler.handle(path) ?: return null
        LocalWebViewMimeTypes.overrideForAsset(path)?.let(response::setMimeType)
        return response
    }
}
