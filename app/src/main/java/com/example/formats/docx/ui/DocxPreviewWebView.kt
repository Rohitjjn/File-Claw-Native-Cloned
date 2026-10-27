package com.example.formats.docx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentFileEntity
import com.example.viewmodel.MainViewModel
import java.io.File
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.example.formats.common.search.InViewerSearchCommandPolicy
import com.example.formats.docx.DocxTextRecovery
import com.example.formats.ooxml.OfficeWebViewSecurity
import com.example.formats.text.ui.PlainTextPreview
import com.example.platform.webview.MimeAwareAssetsPathHandler

@Composable
fun DocxPreviewWebView(
    fileEntity: RecentFileEntity,
    isNightMode: Boolean,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    notice: String? = null,
    onPageChanged: (Int, Int) -> Unit = { _, _ -> },
    scrollToPage: Int? = null,
    onScrollProgressHandled: () -> Unit = {},
    searchQuery: String = "",
    searchTriggerNext: Int = 0,
    searchTriggerPrev: Int = 0,
    onSearchResultCount: (Int, Int, Boolean) -> Unit = { _, _, _ -> },
    zoomFactor: Float = 1.0f,
    onWebViewCreated: (android.webkit.WebView?) -> Unit = {},
    onDocumentReady: () -> Unit = {},
    onSingleTap: () -> Unit = {},
    onZoomChanged: (Float) -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onRendererFallback: (Boolean) -> Unit = {}
) {
    var isWebViewLoading by remember(fileEntity.path) { mutableStateOf(true) }
    var webViewRef by remember(fileEntity.path) { mutableStateOf<android.webkit.WebView?>(null) }
    var pureKotlinFallback by remember(fileEntity.path) { mutableStateOf<com.example.formats.docx.DocxExtractor.ExtractedDocx?>(null) }
    // Loop 14: the plain-text view is an honest RECOVERY state, never a normal
    // preview. The renderer failure reason is surfaced verbatim (no document
    // content, no sensitive data) and Retry rebuilds the WebView from scratch.
    var fallbackReason by remember(fileEntity.path) { mutableStateOf<String?>(null) }
    var docxReloadKey by remember(fileEntity.path) { mutableIntStateOf(0) }
    // Ledger of what the renderer's search engine has already been told (Loop 12, Bug
    // B): navigation triggers now issue ONLY next/prev deltas; re-issuing searchWord()
    // per arrow press used to reset the match index to 0 so the counter stuck at 2/N.
    var issuedSearch by remember(fileEntity.path) { mutableStateOf(InViewerSearchCommandPolicy.Issued()) }
    fun retryDocxRender() {
        pureKotlinFallback = null
        fallbackReason = null
        isWebViewLoading = true
        issuedSearch = InViewerSearchCommandPolicy.Issued()
        docxReloadKey++
    }
    // Last zoom the page itself reported. Kotlin only pushes a zoom value to the page
    // when it differs from this (toolbar +/-, dialog) so page -> Kotlin -> page echoes
    // can never re-anchor a gesture in progress.
    var lastPageReportedZoom by remember(fileEntity.path) { mutableFloatStateOf(-1f) }
    val context = LocalContext.current
    // Compose owns the real WebView bounds. Report each non-zero layout pass to
    // the page instead of letting it guess from a possibly stale DOM viewport.
    val layoutListeners = remember {
        java.util.IdentityHashMap<android.webkit.WebView, android.view.ViewTreeObserver.OnGlobalLayoutListener>()
    }
    val latestSearchResultCallback = androidx.compose.runtime.rememberUpdatedState(onSearchResultCount)
    val latestPageChangedCallback = androidx.compose.runtime.rememberUpdatedState(onPageChanged)
    val latestDocumentReadyCallback = androidx.compose.runtime.rememberUpdatedState(onDocumentReady)
    val latestWebViewCreatedCallback = androidx.compose.runtime.rememberUpdatedState(onWebViewCreated)
    val latestSingleTapCallback = androidx.compose.runtime.rememberUpdatedState(onSingleTap)
    val latestZoomChangedCallback = androidx.compose.runtime.rememberUpdatedState(onZoomChanged)
    val latestDoubleTapCallback = androidx.compose.runtime.rememberUpdatedState(onDoubleTap)
    val latestRendererFallbackCallback = androidx.compose.runtime.rememberUpdatedState(onRendererFallback)
    // Fresh values for the WebViewClient closures created once in the factory: reading
    // them at callback time prevents a stale initial zoom/theme being re-applied after
    // the document finishes loading (Loop 10).
    val latestZoomForPageLoad = androidx.compose.runtime.rememberUpdatedState(zoomFactor)
    val latestNightModeForPageLoad = androidx.compose.runtime.rememberUpdatedState(isNightMode)
    val latestNotice = androidx.compose.runtime.rememberUpdatedState(notice)

    // Loop 12 (Bug B): query changes search exactly once; arrow presses issue only
    // next/prev deltas, so all N matches stay reachable in both directions.
    LaunchedEffect(webViewRef, isWebViewLoading, searchQuery, searchTriggerNext, searchTriggerPrev, pureKotlinFallback) {
        val webView = webViewRef
        if (webView != null && !isWebViewLoading && pureKotlinFallback == null) {
            val plan = InViewerSearchCommandPolicy.plan(issuedSearch, searchQuery, searchTriggerNext, searchTriggerPrev)
            if (!plan.isEmpty) {
                if (plan.search && issuedSearch.query != null) {
                    // Debounce rapid typing before re-highlighting the whole DOM.
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
            }
            // Always advance the ledger — including no-op plans, so a trigger counter
            // reset (file change / find closed) is absorbed instead of replayed.
            issuedSearch = plan.issued
        }
    }
    LaunchedEffect(webViewRef, isWebViewLoading, isNightMode, notice, pureKotlinFallback) {
        if (webViewRef != null && !isWebViewLoading && pureKotlinFallback == null) {
            webViewRef?.evaluateJavascript(
                "setDarkMode($isNightMode); setNotice(${org.json.JSONObject.quote(notice.orEmpty())});",
                null
            )
        }
    }
    // Zoom is owned by the page's own gesture engine (pinch around the fingers, double
    // tap, fit-to-width baseline). Kotlin only pushes values it originated.
    LaunchedEffect(webViewRef, isWebViewLoading, zoomFactor, pureKotlinFallback) {
        val webView = webViewRef
        if (webView != null && !isWebViewLoading && pureKotlinFallback == null) {
            if (kotlin.math.abs(zoomFactor - lastPageReportedZoom) > 0.004f) {
                webView.evaluateJavascript("setZoom($zoomFactor);", null)
            }
        }
    }

    // Watchdog: a renderer that never answers must end in the labeled recovery state
    // (with Retry), not an endless "Rendering Document..." spinner. Large files get more
    // time. A real error from the page arrives long before this.
    LaunchedEffect(isWebViewLoading, docxReloadKey, fileEntity.path, pureKotlinFallback) {
        if (isWebViewLoading && pureKotlinFallback == null) {
            val sizeMb = (File(fileEntity.path).length() / (1024L * 1024L)).coerceAtLeast(1L)
            delay((25_000L + sizeMb * 1_000L).coerceAtMost(180_000L))
            if (isWebViewLoading && pureKotlinFallback == null) {
                isWebViewLoading = false
                fallbackReason = "The document renderer did not respond in time."
                pureKotlinFallback = runCatching {
                    com.example.formats.docx.DocxExtractor.extractText(File(fileEntity.path))
                }.getOrNull()
                latestRendererFallbackCallback.value(true)
            }
        }
    }
    LaunchedEffect(webViewRef, isWebViewLoading, scrollToPage, pureKotlinFallback) {
        if (webViewRef != null && !isWebViewLoading && pureKotlinFallback == null && scrollToPage != null) {
            webViewRef?.evaluateJavascript("scrollToPage(${scrollToPage + 1});", null)
            onScrollProgressHandled()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(if (isNightMode) Color(0xFF151514) else Color(0xFFfaf9f5))) {
        if (pureKotlinFallback != null) {
            val extracted = pureKotlinFallback!!
            Column(modifier = Modifier.fillMaxSize()) {
                // Loop 14: labeled recovery/error state — the extracted text must never
                // masquerade as a successful formatted preview (the old "Clean Document
                // View" banner looked like a feature, not a failure).
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = .5f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = DocxTextRecovery.bannerHeadline(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        DocxTextRecovery.displayReason(fallbackReason)?.let { reason ->
                            Text(
                                text = reason,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = .75f),
                                maxLines = 2
                            )
                        }
                        DocxTextRecovery
                            .statsLabel(extracted.wordCount, extracted.paragraphCount)?.let { stats ->
                                Text(
                                    text = stats,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = .75f)
                                )
                            }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                            // Retry rebuilds the WebView and re-runs the real renderer;
                            // the source document is never modified.
                            TextButton(
                                onClick = { retryDocxRender() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) { Text("Retry formatted view", fontSize = 12.sp) }
                        }
                    }
                }
                if (extracted.paragraphs.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No text content could be recovered from this DOCX file.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                    }
                } else {
                    PlainTextPreview(
                        content = extracted.paragraphs.joinToString("\n\n"),
                        isDarkMode = isNightMode,
                        fontSizeSetting = "Default",
                        searchQuery = searchQuery,
                        searchTriggerNext = searchTriggerNext,
                        searchTriggerPrev = searchTriggerPrev,
                        onSearchResultCount = onSearchResultCount,
                        onSingleTap = onSingleTap,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            androidx.compose.runtime.key(fileEntity.path, docxReloadKey) {
                AndroidView(
                    factory = { viewContext ->
                        // Loop 12 (Bug E): the editor page is now served through the
                        // WebViewAssetLoader (same mechanism as the PDF reader) so the
                        // DOCX bytes can be streamed to docx-preview as an ArrayBuffer
                        // instead of a ~1.33x-larger base64 String crossing the JS bridge
                        // (which peaked at ~5 simultaneous copies of large documents).
                        //
                        // Loop 14 REGRESSION FIX: the editor was previously registered under
                        // "/editor/" and loaded at /editor/docx_editor.html. WebViewAssetLoader
                        // hands each handler the path with the registered prefix STRIPPED, so
                        // AssetsPathHandler tried to open the asset "docx_editor.html" — which
                        // lives at "editor/docx_editor.html" — and every DOCX open failed the
                        // main-frame load, silently landing in the plain-text recovery view.
                        // Registering "/assets/" (the documented pattern, same as the PDF
                        // reader) makes /assets/editor/docx_editor.html resolve to the real
                        // asset editor/docx_editor.html. The /docx-src/ stream is unaffected:
                        // its custom handler receives the stripped suffix "document.docx",
                        // which is exactly what it matches on.
                        val docxAssetLoader = androidx.webkit.WebViewAssetLoader.Builder()
                            .addPathHandler("/assets/", MimeAwareAssetsPathHandler(context))
                            .addPathHandler("/docx-src/", object : androidx.webkit.WebViewAssetLoader.PathHandler {
                                override fun handle(path: String): android.webkit.WebResourceResponse? {
                                    if (path != "document.docx") return null
                                    val documentFile = File(fileEntity.path)
                                    if (!documentFile.isFile) return null
                                    return runCatching {
                                        android.webkit.WebResourceResponse(
                                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
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
                            latestWebViewCreatedCallback.value(this)
                            settings.apply {
                                OfficeWebViewSecurity.configure(this)
                                builtInZoomControls = false
                                displayZoomControls = false
                                setSupportZoom(false)
                                useWideViewPort = true
                                // Overview mode would let a wide page shrink the layout
                                // viewport (breaks fit-to-width + rotation).
                                loadWithOverviewMode = false
                            }
                            setBackgroundColor(if (isNightMode) 0xFF151514.toInt() else 0xFFfaf9f5.toInt())

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

                            val pageChangedCallback = latestPageChangedCallback
                            addJavascriptInterface(object : Any() {
                                private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

                                // Fallback transport (only used if fetch of /docx-src/ fails):
                                // the file in 1.5 MB base64 chunks, never one giant string.
                                @android.webkit.JavascriptInterface
                                fun getDocxSize(): Double = File(fileEntity.path).length().toDouble()

                                @android.webkit.JavascriptInterface
                                fun getDocxChunk(index: Int): String {
                                    val file = File(fileEntity.path)
                                    val chunkSize = 1_500_000L // multiple of 3 -> clean base64 joins
                                    val start = index.toLong() * chunkSize
                                    if (index < 0 || start >= file.length()) return ""
                                    return runCatching {
                                        java.io.RandomAccessFile(file, "r").use { raf ->
                                            raf.seek(start)
                                            val length = minOf(chunkSize, file.length() - start).toInt()
                                            val buffer = ByteArray(length)
                                            raf.readFully(buffer)
                                            android.util.Base64.encodeToString(buffer, android.util.Base64.NO_WRAP)
                                        }
                                    }.getOrDefault("")
                                }

                                @android.webkit.JavascriptInterface
                                fun onDocumentLoaded(totalPages: Int) {
                                    mainHandler.post {
                                        isWebViewLoading = false
                                        latestDocumentReadyCallback.value()
                                        pageChangedCallback.value(1, totalPages)
                                    }
                                }

                                @android.webkit.JavascriptInterface
                                fun onPageChanged(currentPage: Int, totalPages: Int) {
                                    mainHandler.post { pageChangedCallback.value(currentPage, totalPages) }
                                }

                                @android.webkit.JavascriptInterface
                                fun onSearchResultCount(count: Int, currentIndex: Int, truncated: Boolean) {
                                    mainHandler.post { latestSearchResultCallback.value(count, currentIndex, truncated) }
                                }

                                @android.webkit.JavascriptInterface
                                fun onDocumentTap() {
                                    mainHandler.post { latestSingleTapCallback.value() }
                                }

                                @android.webkit.JavascriptInterface
                                fun onDocumentDoubleTap() {
                                    mainHandler.post { latestDoubleTapCallback.value() }
                                }

                                @android.webkit.JavascriptInterface
                                fun onZoomChanged(newZoom: Double) {
                                    mainHandler.post {
                                        lastPageReportedZoom = newZoom.toFloat()
                                        latestZoomChangedCallback.value(newZoom.toFloat())
                                    }
                                }

                                @android.webkit.JavascriptInterface
                                fun onDocumentError(error: String) {
                                    mainHandler.post {
                                        isWebViewLoading = false
                                        android.util.Log.w("WebViewDocx", "Switching to text-only DOCX preview: $error")
                                        val extracted = runCatching {
                                            com.example.formats.docx.DocxExtractor.extractText(File(fileEntity.path))
                                        }.getOrNull()
                                        // Loop 14: always enter the labeled recovery state (even with
                                        // zero recovered paragraphs) so a failed render is never left
                                        // as a silent blank surface or a deceptively normal preview.
                                        fallbackReason = error.take(180)
                                        pureKotlinFallback = extracted
                                        latestRendererFallbackCallback.value(true)
                                    }
                                }

                                @android.webkit.JavascriptInterface
                                fun exitWithoutSaving() {
                                    mainHandler.post { viewModel.closeCurrentFile() }
                                }
                            }, "Android")

                            webViewClient = object : android.webkit.WebViewClient() {
                                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                    // Apply the CURRENT zoom factor (not a stale factory-time
                                    // capture) BEFORE loadDocx so the document never flashes at
                                    // 100% before settling on the preferred zoom. Loop 12: the
                                    // bytes are streamed by the page itself (loadDocx(null, ...));
                                    // the bridge base64 path is only the in-page fallback.
                                    val freshZoom = latestZoomForPageLoad.value
                                    val freshNightMode = latestNightModeForPageLoad.value
                                    // Seed the layout bridge from the current View bounds;
                                    // the global-layout listener keeps it current after Compose.
                                    view?.evaluateJavascript(
                                        "notifyViewport(${view.width}, ${view.height}); " +
                                            "setNotice(${org.json.JSONObject.quote(latestNotice.value.orEmpty())}); " +
                                            "setZoom($freshZoom); loadDocx(null, $freshNightMode);",
                                        null
                                    )
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?
                                ): Boolean {
                                    return OfficeWebViewSecurity.shouldBlockNavigation(request)
                                }

                                override fun shouldInterceptRequest(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?
                                ): android.webkit.WebResourceResponse? {
                                    return OfficeWebViewSecurity.intercept(request, docxAssetLoader)
                                }

                                override fun onReceivedError(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?,
                                    error: android.webkit.WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        android.util.Log.e("WebViewDocx", "Error: ${error?.description}")
                                        val extracted = runCatching {
                                            com.example.formats.docx.DocxExtractor.extractText(File(fileEntity.path))
                                        }.getOrNull()
                                        // Loop 14: honest recovery state with the failure reason
                                        // (the Loop 12 /editor/ registration 404 surfaced HERE).
                                        view?.post {
                                            fallbackReason = error?.description?.toString()?.take(180)
                                                ?: "The preview page could not be loaded."
                                            pureKotlinFallback = extracted
                                            latestRendererFallbackCallback.value(true)
                                            isWebViewLoading = false
                                        }
                                    }
                                }

                                // Loop 12 (Bug C/E): a WebView renderer crash must degrade
                                // to the text-only fallback instead of killing the app or
                                // leaving a dead blank surface. Returning true tells Android
                                // this app handled the loss.
                                override fun onRenderProcessGone(
                                    view: android.webkit.WebView,
                                    detail: android.webkit.RenderProcessGoneDetail
                                ): Boolean {
                                    android.util.Log.e(
                                        "WebViewDocx",
                                        "Renderer process gone (crashed=${detail.didCrash()}); switching to text-only preview"
                                    )
                                    view.post {
                                        val extracted = runCatching {
                                            com.example.formats.docx.DocxExtractor.extractText(File(fileEntity.path))
                                        }.getOrNull()
                                        fallbackReason = "The document renderer stopped unexpectedly."
                                        pureKotlinFallback = extracted
                                        latestRendererFallbackCallback.value(true)
                                        isWebViewLoading = false
                                    }
                                    return true
                                }

                                override fun onReceivedHttpError(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?,
                                    errorResponse: android.webkit.WebResourceResponse?
                                ) {
                                    super.onReceivedHttpError(view, request, errorResponse)
                                    android.util.Log.e("WebViewDocx", "HTTP Error: ${errorResponse?.statusCode}")
                                }
                            }
                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                    android.util.Log.d("WebViewDocx", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                                    return true
                                }
                            }
                            loadUrl("https://appassets.androidplatform.net/assets/editor/docx_editor.html")
                        }
                    },
                    update = { webView ->
                        webViewRef = webView
                        latestWebViewCreatedCallback.value(webView)
                        webView.setBackgroundColor(if (isNightMode) 0xFF151514.toInt() else 0xFFfaf9f5.toInt())
                    },
                    // Loop 12 (Bug E): destroying the WebView when its node leaves
                    // composition prevents stale instances (and their render processes)
                    // from retaining the file context after a file switch.
                    onRelease = { webView ->
                        layoutListeners.remove(webView)?.let { listener ->
                            webView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                        }
                        webViewRef = null
                        webView.destroy()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isWebViewLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(if (isNightMode) Color(0xFF151514) else Color(0xFFfaf9f5)),
                    contentAlignment = Alignment.Center
                ) {
                    com.example.ui.component.PremiumLoadingIndicator(text = "Rendering Document...")
                }
            }
        }
    }
}
