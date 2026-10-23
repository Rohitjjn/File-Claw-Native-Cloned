package com.example.platform.webview

import android.net.Uri
import android.webkit.WebView
import java.io.File

/** Loads a local file only when its URL or script setting changes, not on every recomposition. */
object LocalWebViewFileLoader {
    fun loadOnce(webView: WebView, path: String, allowScripts: Boolean) {
        webView.settings.javaScriptEnabled = allowScripts
        val url = Uri.fromFile(File(path)).toString()
        val key = "$url|$allowScripts"
        if (webView.tag != key) {
            webView.tag = key
            webView.loadUrl(url)
        }
    }
}
