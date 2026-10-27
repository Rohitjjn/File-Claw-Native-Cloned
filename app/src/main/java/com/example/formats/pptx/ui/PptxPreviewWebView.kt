package com.example.formats.pptx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.RecentFileEntity
import com.example.formats.common.search.InViewerSearchCommandPolicy
import com.example.formats.ooxml.OfficeWebViewSecurity
import com.example.platform.webview.MimeAwareAssetsPathHandler
import com.example.viewmodel.MainViewModel
import java.io.File
import kotlin.math.abs

/**
 * Offline PPTX preview over the existing bundled pptx-preview engine.
 *
 * The renderer is initialised only after Compose reports non-zero WebView
 * bounds, so slides are measured from the real viewport. Slide navigation,
 * search, zoom/pan, dark mode and an honest text-only recovery path are exposed
 * through the same minimal bridge used by the rest of the app. No macros,
 * remote resources, scripts from the presentation, or file-scheme access are
 * enabled.
 */
@Composable
fun PptxPreviewWebView(
    fileEntity: RecentFileEntity,
    isNightMode: Boolean,
    notice: String?,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onSlideChanged: (Int, Int) -> Unit = { _, _ -> },
    goToSlide: Int? = null,
    goToSlideToken: Int = 0,
    onGoToSlideHandled: () -> Unit = {},
    searchQuery: String = "",
    searchTriggerNext: Int = 0,
    searchTriggerPrev: Int = 0,
    onSearchResultCount: (Int, Int, Boolean) -> Unit = { _, _, _ -> },
    zoomFactor: Float = 1f,
    onZoomChanged: (Float) -> Unit = {},
    onWebViewCreated: (android.webkit.WebView?) -> Unit = {},
    onReady: () -> Unit = {},
    onSingleTap: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onRendererFallback: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var reloadKey by remember(fileEntity.path) { mutableIntStateOf(0) }
    var webViewRef by remember(fileEntity.path, reloadKey) { mutableStateOf<android.webkit.WebView?>(null) }
    var isReady by remember(fileEntity.path, reloadKey) { mutableStateOf(false) }
    var loadError by remember(fileEntity.path, reloadKey) { mutableStateOf<String?>(null) }
    var issuedSearch by remember(fileEntity.path, reloadKey) {
        mutableStateOf(InViewerSearchCommandPolicy.Issued())
    }
    var lastPageReportedZoom by remember(fileEntity.path, reloadKey) { mutableFloatStateOf(-1f) }
    val layoutListeners = remember {
        java.util.IdentityHashMap<android.webkit.WebView, android.view.ViewTreeObserver.OnGlobalLayoutListener>()
    }

    val latestSlideChanged = rememberUpdatedState(onSlideChanged)
    val latestSearchCount = rememberUpdatedState(onSearchResultCount)
    val latestZoomChanged = rememberUpdatedState(onZoomChanged)
    val latestWebViewCreated = rememberUpdatedState(onWebViewCreated)
    val latestReady = rememberUpdatedState(onReady)
    val latestSingleTap = rememberUpdatedState(onSingleTap)
    val latestDoubleTap = rememberUpdatedState(onDoubleTap)
    val latestFallback = rememberUpdatedState(onRendererFallback)
    val latestGoToHandled = rememberUpdatedState(onGoToSlideHandled)
    val latestNightMode = rememberUpdatedState(isNightMode)
    val latestNotice = rememberUpdatedState(notice)
    val latestZoomForPageLoad = rememberUpdatedState(zoomFactor)

    fun retry() {
        loadError = null
        isReady = false
        issuedSearch = InViewerSearchCommandPolicy.Issued()
        lastPageReportedZoom = -1f
        latestFallback.value(false)
        reloadKey++
    }

    val sizeMb = remember(fileEntity.path) {
        (File(fileEntity.path).length() / (1024L * 1024L)).coerceAtLeast(1L)
    }
    LaunchedEffect(fileEntity.path, reloadKey, isReady, loadError) {
        if (!isReady && loadError == null) {
            kotlinx.coroutines.delay((20_000L + sizeMb * 500L).coerceAtMost(90_000L))
            if (!isReady && loadError == null) {
                loadError = "The presentation did not finish loading. Check that Android System WebView is up to date, then try again."
            }
        }
    }

    LaunchedEffect(webViewRef, isReady, isNightMode, notice) {
        val webView = webViewRef
        if (webView != null && isReady) {
            webView.evaluateJavascript(
                "setDarkMode(${latestNightMode.value}); " +
                    "setNotice(${org.json.JSONObject.quote(latestNotice.value.orEmpty())});",
                null
            )
        }
    }

    LaunchedEffect(webViewRef, isReady, zoomFactor) {
        val webView = webViewRef
        if (webView != null && isReady && abs(zoomFactor - lastPageReportedZoom) > 0.004f) {
            webView.evaluateJavascript("setZoom($zoomFactor, false);", null)
        }
    }

    LaunchedEffect(webViewRef, isReady, goToSlide, goToSlideToken) {
        val webView = webViewRef
        val slide = goToSlide
        if (webView != null && isReady && slide != null) {
            webView.evaluateJavascript("goToSlide($slide);", null)
            latestGoToHandled.value()
        }
    }

    LaunchedEffect(webViewRef, isReady, searchQuery, searchTriggerNext, searchTriggerPrev) {
        val webView = webViewRef
        if (webView != null && isReady) {
            val plan = InViewerSearchCommandPolicy.plan(
                issuedSearch,
                searchQuery,
                searchTriggerNext,
                searchTriggerPrev
            )
            if (!plan.isEmpty) {
                if (plan.search && issuedSearch.query != null) {
                    kotlinx.coroutines.delay(150)
                    if (!kotlin.coroutines.coroutineContext.isActive) return@LaunchedEffect
                }
                val script = buildString {
                    if (plan.search) {
                        append("searchWord(${org.json.JSONObject.quote(searchQuery)});")
                    }
                    repeat(plan.nextSteps) { append("nextSearchMatch();") }
                    repeat(plan.prevSteps) { append("prevSearchMatch();") }
                }
                webView.evaluateJavascript(script, null)
                issuedSearch = plan.issued
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(if (isNightMode) Color(0xFF151514) else Color(0xFFFAF9F5))) {
        if (loadError == null) {
            key(fileEntity.path, reloadKey) {
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
                            latestWebViewCreated.value(this)
                            settings.apply {
                                OfficeWebViewSecurity.configure(this)
                                builtInZoomControls = false
                                displayZoomControls = false
                                setSupportZoom(false)
                                useWideViewPort = true
                                loadWithOverviewMode = false
                            }
                            setBackgroundColor(if (isNightMode) 0xFF151514.toInt() else 0xFFFAF9F5.toInt())

                            val host = this
                            var lastLayoutWidth = 0
                            var lastLayoutHeight = 0
                            val layoutListener = object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                                override fun onGlobalLayout() {
                                    val width = host.width
                                    val height = host.height
                                    if (width <= 0 || height <= 0 || (width == lastLayoutWidth && height == lastLayoutHeight)) return
                                    lastLayoutWidth = width
                                    lastLayoutHeight = height
                                    host.evaluateJavascript("notifyViewport($width, $height);", null)
                                }
                            }
                            viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
                            layoutListeners[host] = layoutListener

                            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                            addJavascriptInterface(object : Any() {
                                @android.webkit.JavascriptInterface
                                fun onDocumentLoaded(totalSlides: Int) {
                                    mainHandler.post {
                                        isReady = true
                                        latestReady.value()
                                        latestSlideChanged.value(1, totalSlides.coerceAtLeast(1))
                                    }
                                }

                                @android.webkit.JavascriptInterface
                                fun onSlideChanged(current: Int, total: Int) {
                                    mainHandler.post { latestSlideChanged.value(current, total) }
                                }

                                @android.webkit.JavascriptInterface
                                fun onDocumentError(error: String) {
                                    mainHandler.post {
                                        isReady = false
                                        loadError = error.take(180).ifBlank { "Could not open this presentation." }
                                    }
                                }

                                @android.webkit.JavascriptInterface
                                fun onSearchResultCount(count: Int, currentIndex: Int, truncated: Boolean) {
                                    mainHandler.post { latestSearchCount.value(count, currentIndex, truncated) }
                                }

                                @android.webkit.JavascriptInterface
                                fun onZoomChanged(newZoom: Double) {
                                    mainHandler.post {
                                        if (!newZoom.isFinite()) return@post
                                        val factor = newZoom.toFloat()
                                        lastPageReportedZoom = factor
                                        latestZoomChanged.value(factor)
                                    }
                                }

                                @android.webkit.JavascriptInterface
                                fun onDocumentTap() {
                                    mainHandler.post { latestSingleTap.value() }
                                }

                                @android.webkit.JavascriptInterface
                                fun onDocumentDoubleTap() {
                                    mainHandler.post { latestDoubleTap.value() }
                                }

                                @android.webkit.JavascriptInterface
                                fun onRendererFallback(isFallback: Boolean) {
                                    mainHandler.post { latestFallback.value(isFallback) }
                                }

                                @android.webkit.JavascriptInterface
                                fun exitWithoutSaving() {
                                    mainHandler.post { viewModel.closeCurrentFile() }
                                }
                            }, "Android")

                            webViewClient = object : androidx.webkit.WebViewClientCompat() {
                                override fun shouldOverrideUrlLoading(
                                    view: android.webkit.WebView,
                                    request: android.webkit.WebResourceRequest
                                ): Boolean = OfficeWebViewSecurity.shouldBlockNavigation(request)

                                override fun shouldInterceptRequest(
                                    view: android.webkit.WebView,
                                    request: android.webkit.WebResourceRequest
                                ): android.webkit.WebResourceResponse? =
                                    OfficeWebViewSecurity.intercept(request, pptxAssetLoader)

                                override fun onPageFinished(view: android.webkit.WebView, url: String) {
                                    val night = latestNightMode.value
                                    val noticeJson = org.json.JSONObject.quote(latestNotice.value.orEmpty())
                                    val zoom = latestZoomForPageLoad.value
                                    view.evaluateJavascript(
                                        "notifyViewport(${view.width}, ${view.height}); " +
                                            "setDarkMode($night); setNotice($noticeJson); " +
                                            "setZoom($zoom, false); loadPptx(null, $night);",
                                        null
                                    )
                                }

                                override fun onReceivedError(
                                    view: android.webkit.WebView,
                                    request: android.webkit.WebResourceRequest,
                                    error: androidx.webkit.WebResourceErrorCompat
                                ) {
                                    if (request.isForMainFrame) {
                                        view.post {
                                            isReady = false
                                            loadError = "The presentation viewer page could not be loaded."
                                        }
                                    }
                                }

                                override fun onRenderProcessGone(
                                    view: android.webkit.WebView,
                                    detail: android.webkit.RenderProcessGoneDetail
                                ): Boolean {
                                    view.post {
                                        isReady = false
                                        loadError = "The presentation renderer stopped unexpectedly. Please try again."
                                    }
                                    return true
                                }
                            }
                            loadUrl("https://${OfficeWebViewSecurity.ASSET_HOST}/assets/editor/pptx_editor.html")
                        }
                    },
                    update = { webView ->
                        webViewRef = webView
                        latestWebViewCreated.value(webView)
                        webView.setBackgroundColor(if (isNightMode) 0xFF151514.toInt() else 0xFFFAF9F5.toInt())
                    },
                    onRelease = { webView ->
                        layoutListeners.remove(webView)?.let { listener ->
                            webView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                        }
                        webViewRef = null
                        latestWebViewCreated.value(null)
                        webView.destroy()
                    },
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
                Spacer(Modifier.height(12.dp))
                Button(onClick = { retry() }) { Text("Retry") }
            }
        }

        if (!isReady && loadError == null) {
            Box(
                modifier = Modifier.fillMaxSize().background(if (isNightMode) Color(0xFF151514) else Color(0xFFFAF9F5)),
                contentAlignment = Alignment.Center
            ) {
                com.example.ui.component.PremiumLoadingIndicator(text = "Loading Presentation…")
            }
        }
    }
}
