package com.example.platform.webview

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/** Shared hardened WebView configuration for generated or packaged local HTML previews. */
object SafeWebViewFactory {
    fun create(context: Context): WebView = WebView(context).apply {
        settings.javaScriptEnabled = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
        }
    }
}
