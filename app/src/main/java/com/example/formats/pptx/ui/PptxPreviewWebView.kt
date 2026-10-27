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
import com.example.platform.webview.MimeAwareAssetsPathHandler
import java.io.File

/**
 * Issue 3 fix ("PPT file doesn't open properly"): this previously loaded the editor page
 * with the legacy `file:///android_asset/` scheme and pushed the WHOLE presentation into
 * the page as one giant Base64 string embedded in a single `evaluateJavascript()` call
 * (`"loadPptx('$base64Pptx', ...)"`) -- the exact pattern already identified and replaced
 * for DOCX (see DocxPreviewWebView.kt) because it multiplies memory use, and very large
 * inline script strings are unreliable across WebView versions. PPTX files in particular
 * tend to be large (embedded images), making this the most likely of the three formats to
 * hit that ceiling. It also never set up a WebViewAssetLoader, so `allowFileAccess = false`
 * combined with the bare `file://` origin left the page unable to reliably load its own
 * sibling resources on some WebView versions. This now mirrors the PDF/DOCX readers:
 * the editor page and the presentation's bytes are both served through
 * WebViewAssetLoader over the `https://appassets.androidplatform.net/` origin, and the
 * presentation is streamed in directly as an ArrayBuffer (`fetch('/pptx-src/...')`)
 * instead of being Base64-encoded anywhere.
 */
@Composable
fun PptxPreviewWebView(
    fileEntity: RecentFileEntity,
    isNightMode: Boolean,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var isWebViewLoading by remember(fileEntity.path) { mutableStateOf(true) }
    var webViewRef by remember(fileEntity.path) { mutableStateOf<android.webkit.WebView?>(null) }
    var loadError by remember(fileEntity.path) { mutableStateOf<String?>(null) }
    var reloadKey by remember(fileEntity.path) { mutableIntStateOf(0) }

    val context = LocalContext.current

    // If the renderer never calls back at all (page/script failed to load, large file
    // still decompressing), surface a retryable error instead of spinning forever.
    val sizeMb = remember(fileEntity.path) { (File(fileEntity.path).length() / (1024L * 1024L)).coerceAtLeast(1L) }
    LaunchedEffect(fileEntity.path, reloadKey, isWebViewLoading) {
        if (isWebViewLoading) {
            kotlinx.coroutines.delay((20_000L + sizeMb * 500L).coerceAtMost(90_000L))
            if (isWebViewLoading) {
                isWebViewLoading = false
                loadError = "The presentation did not finish loading. Check that Android System WebView is up to date, then try again."
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(if (isNightMode) Color(0xFF151514) else Color(0xFFfaf9f5))) {
        if (loadError == null) {
            androidx.compose.runtime.key(fileEntity.path, reloadKey) {
                AndroidView(
                    factory = { viewContext ->
                        val pptxAssetLoader = androidx.webkit.WebViewAssetLoader.Builder()
                            .addPathHandler("/assets/", MimeAwareAssetsPathHandler(context))
                            .addPathHandler("/pptx-src/", object : androidx.webkit.WebViewAssetLoader.PathHandler {
                                override fun handle(path: String): android.webkit.WebResourceResponse? {
                                    if (path != "presentation.pptx") return null
                                    val documentFile = File(fileEntity.path)
                                    if (!documentFile.isFile) return null
                                    return runCatching {
                                        android.webkit.WebResourceResponse(
                                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                            null,
                                            200,
                                            "OK",
                                            mapOf("Content-Length" to documentFile.length().toString()),
                                            java.io.FileInputStream(documentFile)
                                        )
                                    }.getOrNull()
                                }
                            })
                            .build()
                        android.webkit.WebView(viewContext).apply {
                            webViewRef = this
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                // The asset loader serves both the editor page and the
                                // presentation's bytes; no raw file scheme access is needed.
                                allowFileAccess = false
                                allowContentAccess = false
                                @Suppress("DEPRECATION")
                                allowFileAccessFromFileURLs = false
                                @Suppress("DEPRECATION")
                                allowUniversalAccessFromFileURLs = false
                                builtInZoomControls = true
                                displayZoomControls = false
                                setSupportZoom(true)
                                useWideViewPort = true
                                loadWithOverviewMode = true
                            }
                            setBackgroundColor(if (isNightMode) 0xFF151514.toInt() else 0xFFfaf9f5.toInt())

                            addJavascriptInterface(object : Any() {
                                @android.webkit.JavascriptInterface
                                fun onDocumentLoaded(totalSlides: Int) {
                                    post { isWebViewLoading = false }
                                }

                                @android.webkit.JavascriptInterface
                                fun onDocumentError(error: String) {
                                    post {
                                        isWebViewLoading = false
                                        loadError = error.ifBlank { "Could not open this presentation." }
                                    }
                                }

                                @android.webkit.JavascriptInterface
                                fun exitWithoutSaving() {
                                    post { viewModel.closeCurrentFile() }
                                }
                            }, "Android")

                            // WebViewClientCompat (not the plain framework WebViewClient) is
                            // required here: it is the only base class whose
                            // onReceivedError overload takes WebResourceErrorCompat, which
                            // is what makes that override actually override something
                            // instead of silently becoming a dead, never-called method
                            // (same reasoning as the PDF reader's WebViewClient).
                            webViewClient = object : androidx.webkit.WebViewClientCompat() {
                                override fun shouldInterceptRequest(
                                    view: android.webkit.WebView,
                                    request: android.webkit.WebResourceRequest
                                ): android.webkit.WebResourceResponse? {
                                    return pptxAssetLoader.shouldInterceptRequest(request.url)
                                }

                                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                    view?.evaluateJavascript("loadPptx(null, $isNightMode);", null)
                                }

                                override fun onReceivedError(
                                    view: android.webkit.WebView,
                                    request: android.webkit.WebResourceRequest,
                                    error: androidx.webkit.WebResourceErrorCompat
                                ) {
                                    if (request.isForMainFrame) {
                                        post {
                                            isWebViewLoading = false
                                            loadError = "The presentation viewer page could not be loaded."
                                        }
                                    }
                                }
                            }
                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                    android.util.Log.d(
                                        "WebViewPptx",
                                        "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}"
                                    )
                                    return super.onConsoleMessage(consoleMessage)
                                }
                            }
                            loadUrl("https://appassets.androidplatform.net/assets/editor/pptx_editor.html")
                        }
                    },
                    update = { /* no-op */ },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(loadError.orEmpty(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = {
                    loadError = null
                    isWebViewLoading = true
                    reloadKey++
                }) { Text("Retry") }
            }
        }

        if (isWebViewLoading && loadError == null) {
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
