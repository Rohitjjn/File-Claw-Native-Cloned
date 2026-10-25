package com.example.formats.pdf.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentFileEntity
import java.io.File
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.example.formats.pdf.PdfRenderBudgetPolicy
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import com.example.formats.pdf.PdfAssetUriPolicy
import com.example.formats.pdf.PdfRangeResponder
import com.example.formats.pdf.PdfPageSearchMatch
import com.example.formats.pdf.PdfSearchSnapshot
import com.example.platform.webview.MimeAwareAssetsPathHandler

@Composable
fun PdfPreview(
    fileEntity: RecentFileEntity,
    // Loop 14: PDF view rotation in degrees clockwise (0/90/180/270). Pure viewer
    // state pushed into the already-loaded PDF.js document — the WebView is never
    // recreated for a rotate, so the authenticated session and the password state
    // survive. PDFs render in light mode only (the Loop 14 dark-mode removal).
    viewRotation: Int = 0,
    searchQuery: String = "",
    selectedMatchIndex: Int = 0,
    scrollToPage: Int?,
    // Loop 14: bumped per explicit jump so repeated identical page numbers still fire.
    scrollToken: Int = 0,
    onScrollToPageHandled: () -> Unit,
    onSearchSnapshot: (PdfSearchSnapshot, Boolean) -> Unit = { _, _ -> },
    onSearchError: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    onSingleTap: () -> Unit = {},
    onPageChanged: (Int, Int) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val pdfFile = remember(fileEntity.path) { File(fileEntity.path) }
    val bookmarkKey = remember(fileEntity.path) { "bookmark_${fileEntity.path.hashCode()}" }
    val bookmarkPrefs = remember(context) { context.getSharedPreferences("pdf_bookmarks", android.content.Context.MODE_PRIVATE) }
    val savedPage = remember(fileEntity.path) { bookmarkPrefs.getInt(bookmarkKey, 0) }
    val assetLoader = remember(context, fileEntity.path) {
        androidx.webkit.WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", MimeAwareAssetsPathHandler(context))
            // Range-aware responses are produced in shouldInterceptRequest (it has the
            // request headers); this handler is the header-less fallback for the same URL.
            .addPathHandler("/search-pdf/", object : androidx.webkit.WebViewAssetLoader.PathHandler {
                override fun handle(path: String): android.webkit.WebResourceResponse? {
                    if (path != "document.pdf") return null
                    return runCatching { PdfRangeResponder.respond(pdfFile, null) }.getOrNull()
                }
            })
            .build()
    }

    var webViewRef by remember(fileEntity.path) { mutableStateOf<android.webkit.WebView?>(null) }
    var isJavascriptReady by remember(fileEntity.path) { mutableStateOf(false) }
    var isPdfReady by remember(fileEntity.path) { mutableStateOf(false) }
    var requirePassword by remember(fileEntity.path) { mutableStateOf(false) }
    var passwordError by remember(fileEntity.path) { mutableStateOf(false) }
    var pdfPassword by remember(fileEntity.path) { mutableStateOf("") }
    var pdfErrorMessage by remember(fileEntity.path) { mutableStateOf<String?>(null) }
    // Loop 12 (Bug C): bumped to force a fresh WebView after a renderer crash. The
    // crashed instance can never be reused, so recovery replaces the whole view.
    var pdfReloadKey by remember(fileEntity.path) { mutableIntStateOf(0) }
    // Page-scroller state, fed by the reader's scroll tracking (page space, 0..1).
    var scrollerPage by remember(fileEntity.path) { mutableIntStateOf(1) }
    var scrollerTotal by remember(fileEntity.path) { mutableIntStateOf(0) }
    var scrollerPosition by remember(fileEntity.path) { mutableFloatStateOf(0f) }
    // -1 = size unknown (indeterminate spinner), otherwise 0..99.
    var loadProgress by remember(fileEntity.path) { mutableIntStateOf(-1) }

    // Settles pinch-zoom gestures: after the user lifts their fingers the visible pages
    // re-rasterize at the new resolution (Google Drive reader behavior).
    val zoomSettleHandler = remember(fileEntity.path) {
        android.os.Handler(android.os.Looper.getMainLooper())
    }
    DisposableEffect(fileEntity.path) {
        onDispose { zoomSettleHandler.removeCallbacksAndMessages(null) }
    }

    val latestSearchSnapshot = androidx.compose.runtime.rememberUpdatedState(onSearchSnapshot)
    val latestSearchError = androidx.compose.runtime.rememberUpdatedState(onSearchError)
    val latestPageChanged = androidx.compose.runtime.rememberUpdatedState(onPageChanged)
    val latestSingleTap = androidx.compose.runtime.rememberUpdatedState(onSingleTap)

    LaunchedEffect(webViewRef, isJavascriptReady, isPdfReady, searchQuery) {
        val webView = webViewRef
        if (webView != null && isJavascriptReady) {
            val query = org.json.JSONObject.quote(searchQuery)
            if (isPdfReady) webView.evaluateJavascript("searchPdf($query);", null)
        }
    }
    // Loop 14: rotation is viewer state pushed into the loaded PDF.js document.
    // This is deliberately NOT part of the WebView key: rotating must never
    // recreate the WebView, refetch the document or re-run the password flow —
    // the authenticated PDFDocumentProxy stays alive and pages re-render from it.
    LaunchedEffect(webViewRef, isPdfReady, viewRotation) {
        if (webViewRef != null && isPdfReady) {
            val normalized = com.example.formats.pdf.PdfRotationPolicy.normalize(viewRotation)
            webViewRef?.evaluateJavascript("if (window.setPageRotation) setPageRotation($normalized);", null)
        }
    }
    LaunchedEffect(webViewRef, isJavascriptReady, selectedMatchIndex) {
        if (isJavascriptReady) {
            webViewRef?.evaluateJavascript("showPdfSearchMatch($selectedMatchIndex);", null)
        }
    }
    LaunchedEffect(webViewRef, isPdfReady, scrollToPage, scrollToken) {
        if (isPdfReady && scrollToPage != null) {
            webViewRef?.evaluateJavascript("gotoPdfPage(${scrollToPage + 1});", null)
            onScrollToPageHandled()
        }
    }
    LaunchedEffect(webViewRef, isPdfReady, savedPage) {
        if (isPdfReady && savedPage > 0 && scrollToPage == null) {
            webViewRef?.evaluateJavascript("gotoPdfPage(${savedPage + 1});", null)
        }
    }

    // Loop 12 (Bug C): derive the TOTAL resident-canvas budget from heap signals
    // (isLowRamDevice / memoryClass — a heap cap, NOT physical RAM) and hand it to
    // the bundled reader, which divides it across the pages it keeps rendered. The
    // Loop 10 per-page number ignored that 3-5 canvases are resident at once inside
    // the prefetch window — the main reason heavy PDFs struggled.
    val pdfQualityBudgetBytes = remember(fileEntity.path) {
        val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        PdfRenderBudgetPolicy.totalCanvasBytes(
            isLowRamDevice = activityManager?.isLowRamDevice == true,
            memoryClassMb = activityManager?.memoryClass
        )
    }
    LaunchedEffect(webViewRef, isJavascriptReady, pdfQualityBudgetBytes) {
        if (webViewRef != null && isJavascriptReady) {
            webViewRef?.evaluateJavascript(
                "if (window.setPdfQualityBudget) setPdfQualityBudget($pdfQualityBudgetBytes);",
                null
            )
        }
    }

    // Last-resort watchdog. The reader has its own boot guard and open-strategy
    // fallbacks (about 10 s each); if nothing at all has come back after this long the
    // viewer reports a clear, retryable error instead of an endless spinner. It is not
    // armed while the password dialog is up (that is user time, not a stall).
    LaunchedEffect(pdfReloadKey, isPdfReady, pdfErrorMessage, requirePassword, fileEntity.path) {
        if (!isPdfReady && pdfErrorMessage == null && !requirePassword) {
            delay(PDF_OPEN_WATCHDOG_MS)
            if (!isPdfReady && pdfErrorMessage == null && !requirePassword) {
                android.util.Log.e("PdfJsReader", "Open watchdog fired for a ${pdfFile.length()} byte file")
                pdfErrorMessage = "The PDF viewer did not respond. Check that Android System WebView is up to date, then try again."
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F0F0)) // Loop 14: PDF is always light mode
    ) {
        if (pdfErrorMessage == null) {
            androidx.compose.runtime.key(fileEntity.path, pdfReloadKey) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { viewContext ->
                        android.webkit.WebView(viewContext).apply {
                            webViewRef = this
                            setBackgroundColor(0xFFF0F0F0.toInt())
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = false
                                allowFileAccess = false
                                allowContentAccess = false
                                builtInZoomControls = true
                                displayZoomControls = false
                                setSupportZoom(true)
                                useWideViewPort = true
                                loadWithOverviewMode = false
                                cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                            }
                            addJavascriptInterface(
                                PdfWebAppInterface(
                                    context = viewContext,
                                    onReady = { isJavascriptReady = true },
                                    onDocumentLoaded = { pageCount ->
                                        isPdfReady = true
                                        scrollerTotal = pageCount
                                        requirePassword = false
                                        passwordError = false
                                        pdfErrorMessage = null
                                        latestPageChanged.value(1, pageCount)
                                    },
                                    onPageChanged = { page, pageCount ->
                                        bookmarkPrefs.edit().putInt(bookmarkKey, (page - 1).coerceAtLeast(0)).apply()
                                        latestPageChanged.value(page, pageCount)
                                    },
                                    onScrollProgress = { page, pageCount, position ->
                                        scrollerPage = page
                                        scrollerTotal = pageCount
                                        scrollerPosition = position.coerceIn(0f, 1f)
                                    },
                                    onLoadProgress = { percent -> loadProgress = percent },
                                    onSearchSnapshot = { snapshot, searching -> latestSearchSnapshot.value(snapshot, searching) },
                                    onSearchError = { message -> latestSearchError.value(message) },
                                    onPasswordRequired = { reason ->
                                        passwordError = reason == 2
                                        requirePassword = true
                                    },
                                    onError = { message ->
                                        pdfErrorMessage = message
                                        isPdfReady = false
                                    },
                                    onSingleTap = { latestSingleTap.value() },
                                    onDoubleTap = {
                                        webViewRef?.evaluateJavascript("if (window.setPdfZoom) setPdfZoom(1.0);", null)
                                    }
                                ),
                                "Android"
                            )
                            webViewClient = object : androidx.webkit.WebViewClientCompat() {
                                override fun shouldOverrideUrlLoading(
                                    view: android.webkit.WebView,
                                    request: android.webkit.WebResourceRequest
                                ): Boolean = !PdfAssetUriPolicy.isBundledReaderUri(request.url)

                                @Suppress("DEPRECATION")
                                override fun shouldOverrideUrlLoading(
                                    view: android.webkit.WebView,
                                    url: String
                                ): Boolean = !PdfAssetUriPolicy.isBundledReaderUri(android.net.Uri.parse(url))

                                override fun onReceivedError(
                                    view: android.webkit.WebView,
                                    request: android.webkit.WebResourceRequest,
                                    error: androidx.webkit.WebResourceErrorCompat
                                ) {
                                    if (request.isForMainFrame) {
                                        android.util.Log.e("PdfJsReader", "Reader page failed to load: ${error.description}")
                                        pdfErrorMessage = "The PDF viewer page could not be loaded."
                                        isPdfReady = false
                                    }
                                }

                                override fun onReceivedHttpError(
                                    view: android.webkit.WebView,
                                    request: android.webkit.WebResourceRequest,
                                    errorResponse: android.webkit.WebResourceResponse
                                ) {
                                    // Never log document bytes or content; URL path + status only.
                                    android.util.Log.w(
                                        "PdfJsReader",
                                        "HTTP ${errorResponse.statusCode} for ${request.url.path}"
                                    )
                                }

                                override fun onScaleChanged(
                                    view: android.webkit.WebView,
                                    oldScale: Float,
                                    newScale: Float
                                ) {
                                    // During the pinch the WebView only magnifies the rasterized
                                    // bitmap; once the gesture settles we re-render the visible
                                    // pages at the new resolution for a crisp zoom (v1.3.0).
                                    super.onScaleChanged(view, oldScale, newScale)
                                    if (kotlin.math.abs(newScale - oldScale) < 0.01f) return
                                    zoomSettleHandler.removeCallbacksAndMessages(null)
                                    zoomSettleHandler.postDelayed({
                                        view.evaluateJavascript(
                                            "if (window.setPdfRenderScale) setPdfRenderScale($newScale);",
                                            null
                                        )
                                    }, 260)
                                }

                                // Loop 12 (Bug C): a dead renderer used to leave a permanently
                                // blank viewer and could take the app down with it. Report a
                                // handled failure and let the user rebuild the WebView.
                                override fun onRenderProcessGone(
                                    view: android.webkit.WebView,
                                    detail: android.webkit.RenderProcessGoneDetail
                                ): Boolean {
                                    android.util.Log.e(
                                        "PdfJsReader",
                                        "PDF renderer process gone (crashed=${detail.didCrash()})"
                                    )
                                    zoomSettleHandler.removeCallbacksAndMessages(null)
                                    pdfErrorMessage = "The PDF renderer stopped unexpectedly."
                                    isPdfReady = false
                                    isJavascriptReady = false
                                    return true
                                }

                                override fun shouldInterceptRequest(
                                    view: android.webkit.WebView,
                                    request: android.webkit.WebResourceRequest
                                ): android.webkit.WebResourceResponse? {
                                    val uri = request.url
                                    if (uri.path?.contains("document.pdf") == true) {
                                        val response = PdfRangeResponder.respond(pdfFile, request.requestHeaders)
                                        if (response != null) return response
                                    }
                                    return assetLoader.shouldInterceptRequest(request.url)
                                }

                                override fun shouldInterceptRequest(
                                    view: android.webkit.WebView,
                                    url: String
                                ): android.webkit.WebResourceResponse? {
                                    val uri = android.net.Uri.parse(url)
                                    if (uri.path?.contains("document.pdf") == true) {
                                        val response = PdfRangeResponder.respond(pdfFile, null)
                                        if (response != null) return response
                                    }
                                    return assetLoader.shouldInterceptRequest(uri)
                                }
                            }
                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                                    android.util.Log.d(
                                        "PdfJsReader",
                                        "${message?.message()} -- line ${message?.lineNumber()} ${message?.sourceId()}"
                                    )
                                    return true
                                }
                            }
                        }
                    },
                    update = { webView ->
                        webViewRef = webView
                        webView.setBackgroundColor(0xFFF0F0F0.toInt()) // Loop 14: always light
                        if (webView.tag != fileEntity.path) {
                            webView.tag = fileEntity.path
                            isJavascriptReady = false
                            isPdfReady = false
                            webView.loadUrl("https://appassets.androidplatform.net/assets/pdfjs/search.html")
                        }
                    },
                    // Loop 12 (Bug E): destroy the WebView when its node leaves
                    // composition so a closed viewer does not retain its render process.
                    onRelease = { webView ->
                        webViewRef = null
                        webView.destroy()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (!isPdfReady && pdfErrorMessage == null) {
            // Rounded loading indicator (replaces the old page-shaped skeleton). The
            // password dialog overlays it; the error state below replaces it entirely.
            PdfRoundedLoading(progress = loadProgress)
        }

        // Right-edge page scroller: always shows the page number, follows the reading
        // position 1 -> last page, and scrolls the document live while it is dragged.
        if (isPdfReady && pdfErrorMessage == null && scrollerTotal > 1) {
            PdfPageScrubber(
                page = scrollerPage.coerceIn(1, scrollerTotal),
                pageCount = scrollerTotal,
                position = scrollerPosition,
                onScrub = { position ->
                    webViewRef?.evaluateJavascript("if (window.scrubToPosition) scrubToPosition($position);", null)
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 10.dp)
            )
        }

        if (pdfErrorMessage != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "PDF Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Could not open PDF", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pdfErrorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onBackClick) { Text("Go Back") }
                    // Loop 12 (Bug C): rebuild the viewer from scratch — the only valid
                    // recovery after a renderer crash or a failed document load.
                    Button(onClick = {
                        pdfErrorMessage = null
                        isPdfReady = false
                        isJavascriptReady = false
                        requirePassword = false
                        passwordError = false
                        pdfReloadKey++
                    }) { Text("Try Again") }
                }
            }
        }

        if (requirePassword) {
            AlertDialog(
                onDismissRequest = onBackClick,
                title = { Text("Password Required") },
                text = {
                    Column {
                        Text("This PDF is password protected.", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pdfPassword,
                            onValueChange = { pdfPassword = it; passwordError = false },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            isError = passwordError,
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (passwordError) Text("Incorrect password", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        requirePassword = false
                        webViewRef?.evaluateJavascript("submitPdfPassword(${org.json.JSONObject.quote(pdfPassword)});", null)
                    }) { Text("Open") }
                },
                dismissButton = { TextButton(onClick = onBackClick) { Text("Cancel") } }
            )
        }
    }
}

private const val PDF_OPEN_WATCHDOG_MS = 45_000L

/**
 * Rounded loading indicator shown while the PDF opens (file read, worker start,
 * page-count, password step). A round-capped arc spins over a soft track; once the
 * reader reports transfer progress the percentage is shown too. It disappears as soon
 * as the document, the password dialog or an error takes over.
 */
@Composable
private fun PdfRoundedLoading(progress: Int, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pdf-rounded-loading")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pdf-loading-rotation"
    )
    val sweep by transition.animateFloat(
        initialValue = 70f,
        targetValue = 250f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pdf-loading-sweep"
    )
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F0F0)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .semantics { contentDescription = "Opening PDF" },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 6.dp.toPx()
                    val inset = strokePx / 2f
                    val arcSize = Size(size.width - strokePx, size.height - strokePx)
                    drawArc(
                        color = accent.copy(alpha = 0.16f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = accent,
                        startAngle = rotation,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
                if (progress in 0..99) {
                    Text(
                        text = "$progress%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4A4843)
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Opening PDF…",
                fontSize = 13.sp,
                color = Color(0xFF6C6A64)
            )
        }
    }
}

/**
 * Right-edge page scroller (Google-Drive style).
 *
 * - A pill handle on the right edge always shows the CURRENT page number and rides
 *   the reading position from the first page (top) to the last page (bottom).
 * - Dragging the handle scrolls the document LIVE (the reader jumps instantly, the
 *   pages passed over are not rendered); a "Page N of M" bubble shows where you are.
 *   Letting go leaves you on that page.
 * - Only the handle itself captures touches, so normal scrolling anywhere else on the
 *   right edge is never blocked. It dims when idle and lights up on scroll / touch.
 */
@Composable
private fun PdfPageScrubber(
    page: Int,
    pageCount: Int,
    position: Float,
    onScrub: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }
    var recentlyActive by remember { mutableStateOf(true) }
    var lastDispatchMs by remember { mutableLongStateOf(0L) }
    val latestPosition by rememberUpdatedState(position)
    val latestOnScrub by rememberUpdatedState(onScrub)
    val density = LocalDensity.current
    val total = pageCount.coerceAtLeast(1)

    // Light up on any reading-position change, fade after a short idle.
    LaunchedEffect(position, page, isDragging) {
        recentlyActive = true
        if (!isDragging) {
            delay(1800)
            recentlyActive = false
        }
    }
    val handleAlpha by animateFloatAsState(
        targetValue = if (isDragging || recentlyActive) 1f else 0.5f,
        animationSpec = tween(220),
        label = "pdf-scroller-alpha"
    )
    val shownPosition = if (isDragging) dragPosition else position.coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .width(120.dp)
            .semantics { contentDescription = "PDF page scroller. Page $page of $total." },
        contentAlignment = Alignment.TopEnd
    ) {
        val handleHeight = 36.dp
        val travelPx = with(density) { (maxHeight - handleHeight).coerceAtLeast(1.dp).toPx() }
        val handleOffsetY = (travelPx * shownPosition).toInt()

        // "Page N of M" bubble beside the handle while dragging.
        AnimatedVisibility(
            visible = isDragging,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(160)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(0, handleOffsetY) }
                .padding(end = 58.dp)
                .height(handleHeight)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.95f),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Page $page",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                    Text(
                        text = "of $total",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // The handle: page number pill, the only touch target of the scroller.
        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp),
            color = if (isDragging) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            shadowElevation = if (isDragging) 6.dp else 3.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(0, handleOffsetY) }
                .alpha(handleAlpha)
                .width(54.dp)
                .height(handleHeight)
                .pointerInput(total) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            dragPosition = latestPosition.coerceIn(0f, 1f)
                            isDragging = true
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragPosition = (dragPosition + dragAmount / travelPx).coerceIn(0f, 1f)
                            val now = android.os.SystemClock.uptimeMillis()
                            if (now - lastDispatchMs >= 32L) {
                                lastDispatchMs = now
                                latestOnScrub(dragPosition)
                            }
                        },
                        onDragEnd = {
                            latestOnScrub(dragPosition)
                            isDragging = false
                        },
                        onDragCancel = { isDragging = false }
                    )
                }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = page.toString(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = if (isDragging) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private class PdfWebAppInterface(
    context: android.content.Context,
    private val onReady: () -> Unit,
    private val onDocumentLoaded: (Int) -> Unit,
    private val onPageChanged: (Int, Int) -> Unit,
    private val onScrollProgress: (Int, Int, Float) -> Unit,
    private val onLoadProgress: (Int) -> Unit,
    private val onSearchSnapshot: (PdfSearchSnapshot, Boolean) -> Unit,
    private val onSearchError: (String) -> Unit,
    private val onPasswordRequired: (Int) -> Unit,
    private val onError: (String) -> Unit,
    private val onSingleTap: () -> Unit,
    private val onDoubleTap: () -> Unit = {}
) {
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val appContext = context.applicationContext

    @android.webkit.JavascriptInterface
    fun onPdfSearchReady(@Suppress("UNUSED_PARAMETER") status: String) {
        mainHandler.post { onReady() }
    }

    @android.webkit.JavascriptInterface
    fun onPdfDocumentLoaded(pageCount: Int) {
        mainHandler.post { onDocumentLoaded(pageCount) }
    }

    @android.webkit.JavascriptInterface
    fun onPdfPageChanged(page: Int, pageCount: Int) {
        mainHandler.post { onPageChanged(page, pageCount) }
    }

    @android.webkit.JavascriptInterface
    fun onPdfScrollProgress(page: Int, pageCount: Int, position: Double) {
        mainHandler.post { onScrollProgress(page, pageCount, position.toFloat()) }
    }

    @android.webkit.JavascriptInterface
    fun onPdfLoadProgress(percent: Int) {
        mainHandler.post { onLoadProgress(percent) }
    }

    @android.webkit.JavascriptInterface
    fun onPdfSearchResults(payload: String) {
        runCatching {
            val json = org.json.JSONObject(payload)
            val jsonMatches = json.optJSONArray("matches") ?: org.json.JSONArray()
            val matches = buildList {
                for (index in 0 until jsonMatches.length()) {
                    val matchJson = jsonMatches.optJSONObject(index) ?: continue
                    add(PdfPageSearchMatch(matchJson.optInt("pageNumber")))
                }
            }
            val snapshot = PdfSearchSnapshot(
                count = json.optInt("count"),
                truncated = json.optBoolean("truncated"),
                matches = matches
            )
            val searching = json.optBoolean("searching")
            mainHandler.post { onSearchSnapshot(snapshot, searching) }
        }.onFailure { error ->
            android.util.Log.w("PdfJsReader", "Could not read search results", error)
        }
    }

    @android.webkit.JavascriptInterface
    fun onPdfSearchError(payload: String) {
        val message = runCatching { org.json.JSONObject(payload).optString("message") }
            .getOrDefault("PDF text search failed")
        mainHandler.post { onSearchError(message) }
    }

    @android.webkit.JavascriptInterface
    fun onPdfPasswordRequired(reason: Int) {
        mainHandler.post { onPasswordRequired(reason) }
    }

    @android.webkit.JavascriptInterface
    fun onPdfError(message: String) {
        mainHandler.post {
            onError(message.ifBlank { "This PDF could not be opened." })
            android.widget.Toast.makeText(appContext, "Could not open PDF", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    @android.webkit.JavascriptInterface
    fun onPdfSingleTap() {
        mainHandler.post { onSingleTap() }
    }

    @android.webkit.JavascriptInterface
    fun onPdfDoubleTap() {
        mainHandler.post { onDoubleTap() }
    }
}
