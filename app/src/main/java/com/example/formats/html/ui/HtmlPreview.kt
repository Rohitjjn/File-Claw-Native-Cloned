package com.example.formats.html.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.RecentFileEntity
import java.io.File
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.core.security.HtmlEscaping
import com.example.platform.webview.LocalWebViewFileLoader

@Composable
fun WebViewPreview(
    fileEntity: RecentFileEntity,
    modifier: Modifier = Modifier,
    allowScripts: Boolean = false,
    searchQuery: String = "",
    onSingleTap: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background == com.example.ui.theme.ClaudeOnyx
    val bgColor = if (isDark) com.example.ui.theme.ClaudeOnyx else Color.White

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        val context = LocalContext.current
        val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

        AndroidView(
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    // HTML files are untrusted: scripts start off (see allowScripts), no bridge object is
                    // exposed to them, and navigation away from local files is blocked.
                    val htmlPage = fileEntity.extension.lowercase().let { it == "html" || it == "htm" }
                    settings.javaScriptEnabled = !htmlPage
                    settings.domStorageEnabled = !htmlPage
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true

                    // file:///android_asset is always readable; real file access is only for local HTML.
                    settings.allowFileAccess = htmlPage
                    settings.allowContentAccess = false

                    if (!htmlPage) {
                        addJavascriptInterface(object {
                            @android.webkit.JavascriptInterface
                            fun performSingleTap() {
                                mainHandler.post {
                                    onSingleTap()
                                }
                            }
                        }, "Android")
                    }

                    setBackgroundColor(if (isDark) 0xFF181715.toInt() else 0xFFFAF9F5.toInt())
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: android.webkit.WebView,
                            request: android.webkit.WebResourceRequest
                        ): Boolean = request.url.scheme != "file"
                    }
                    webChromeClient = android.webkit.WebChromeClient()
                }
            },
            update = { webView ->
                if (searchQuery.isNotEmpty()) {
                    webView.findAllAsync(searchQuery)
                } else {
                    webView.clearMatches()
                }
                val ext = fileEntity.extension.lowercase()
                if (ext == "html" || ext == "htm") {
                    LocalWebViewFileLoader.loadOnce(webView, fileEntity.path, allowScripts)
                } else {
                    // PDF, DOCX, TXT, and Markdown have dedicated preview routes; this fallback handles other text formats.
                    try {
                        val content = java.io.File(fileEntity.path).readText()
                        val htmlContent = """
                            <html>
                            <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=0.25, maximum-scale=5.0, user-scalable=yes">
                            <style>
                                body {
                                    background-color: ${if (isDark) "#181715" else "#FAF9F5"};
                                    color: ${if (isDark) "#FAF9F5" else "#141413"};
                                    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
                                    padding: 16px;
                                    white-space: pre-wrap;
                                    word-wrap: break-word;
                                    line-height: 1.6;
                                    font-size: 14px;
                                    margin: 0;
                                    -webkit-text-size-adjust: none;
                                }
                            </style>
                            </head>
                            <body>${HtmlEscaping.escape(content)}</body>
                            </html>
                            """.trimIndent()
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    } catch (e: Exception) {
                        LocalWebViewFileLoader.loadOnce(webView, fileEntity.path, allowScripts)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
