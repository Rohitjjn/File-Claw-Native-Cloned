package com.example.formats.pptx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.RecentFileEntity
import com.example.viewmodel.MainViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PptxPreviewWebView(
    base64Pptx: String,
    fileEntity: RecentFileEntity,
    isNightMode: Boolean,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var isWebViewLoading by remember(base64Pptx) { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current

    Box(modifier = modifier.fillMaxSize().background(if (isNightMode) Color(0xFF151514) else Color(0xFFfaf9f5))) {
        AndroidView(
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    webViewRef = this
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = false
                        allowContentAccess = false
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }
                    setBackgroundColor(if (isNightMode) 0xFF151514.toInt() else 0xFFfaf9f5.toInt())

                    addJavascriptInterface(object : Any() {
                        @android.webkit.JavascriptInterface
                        fun onDocumentLoaded(totalPages: Int) {
                            post {
                                isWebViewLoading = false
                            }
                        }
                        @android.webkit.JavascriptInterface
                        fun onDocumentError(error: String) {
                            post {
                                isWebViewLoading = false
                                android.widget.Toast.makeText(context, "Error: $error", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                        @android.webkit.JavascriptInterface
                        fun exitWithoutSaving() {
                            post {
                                viewModel.closeCurrentFile()
                            }
                        }
                    }, "Android")

                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                            view?.evaluateJavascript("loadPptx('$base64Pptx', $isNightMode);", null)
                        }
                        override fun onReceivedError(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            android.util.Log.e("WebViewPptx", "Error: ${error?.description}")
                        }
                        override fun onReceivedHttpError(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            android.util.Log.e("WebViewPptx", "HTTP Error: ${errorResponse?.statusCode}")
                        }
                    }
                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                            android.util.Log.d("WebViewPptx", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                            return super.onConsoleMessage(consoleMessage)
                        }
                    }
                    loadUrl("file:///android_asset/editor/pptx_editor.html")
                }
            },
            update = { webView ->
                // No-op
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isWebViewLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isNightMode) Color(0xFF151514) else Color(0xFFfaf9f5)),
                contentAlignment = Alignment.Center
            ) {
                com.example.ui.component.PremiumLoadingIndicator(text = "Loading Presentation...")
            }
        }
    }
}
