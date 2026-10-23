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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
    // Loop 14: right-edge page scrubber inputs. Values come from the visible-page
    // observer in the caller; jumps are delegated back through onScrubJump.
    scrubberPage: Int = 1,
    scrubberPageCount: Int = 0,
    onScrubJump: (Int) -> Unit = {},
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
            .addPathHandler("/search-pdf/", object : androidx.webkit.WebViewAssetLoader.PathHandler {
                override fun handle(path: String): android.webkit.WebResourceResponse? {
                    if (path != "document.pdf" || !pdfFile.isFile) return null
                    return runCatching {
                        android.webkit.WebResourceResponse(
                            "application/pdf",
                            "binary",
                            200,
                            "OK",
                            mapOf("Content-Length" to pdfFile.length().toString(), "Accept-Ranges" to "none"),
                            java.io.FileInputStream(pdfFile)
                        )
                    }.getOrNull()
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
                                        requirePassword = false
                                        passwordError = false
                                        pdfErrorMessage = null
                                        latestPageChanged.value(1, pageCount)
                                    },
                                    onPageChanged = { page, pageCount ->
                                        bookmarkPrefs.edit().putInt(bookmarkKey, (page - 1).coerceAtLeast(0)).apply()
                                        latestPageChanged.value(page, pageCount)
                                    },
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
            // Loop 14: skeleton loading — neutral page-shaped placeholders with a soft
            // shimmer replace the spinner-only screen during document open/metadata/
            // password initialization. The password dialog overlays this skeleton; the
            // error state below replaces it entirely.
            PdfOpeningSkeleton()
        }

        // Loop 14: right-edge page scrubber. Only rendered once the document is ready
        // and there is more than one page; the visible-page observer keeps it in sync
        // (page events flow up through onPageChanged), jumps flow down through the
        // shared gotoPdfPage pipeline (no eager rendering of the pages passed over).
        if (isPdfReady && pdfErrorMessage == null && scrubberPageCount > 1) {
            PdfPageScrubber(
                currentPage = scrubberPage.coerceIn(1, scrubberPageCount),
                pageCount = scrubberPageCount,
                onJumpRequest = onScrubJump,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .navigationBarsPadding()
                    .padding(end = 2.dp)
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

/**
 * Loop 14: neutral skeleton placeholders shown while the PDF opens (file read,
 * worker boot, metadata/page-count/password initialization). Page-shaped cards
 * with a soft shimmer — never a spinner-only screen — and they disappear the
 * moment the real content, the password dialog or an error state takes over.
 */
@Composable
private fun PdfOpeningSkeleton(modifier: Modifier = Modifier) {
    val shimmer = rememberInfiniteTransition(label = "pdf-skeleton")
    val alpha by shimmer.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pdf-skeleton-alpha"
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Preparing document…",
            fontSize = 12.sp,
            color = Color(0xFF6C6A64),
            modifier = Modifier.padding(start = 2.dp)
        )
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    // Stable dimensions/aspect ratio: A4-ish portrait cards on the light
                    // reader canvas, matching the real pages that replace them.
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = alpha))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF191919).copy(alpha = 0.10f),
                        shape = RoundedCornerShape(10.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.45f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFD8D4CC).copy(alpha = alpha))
                    )
                    repeat(4 - (index % 2)) {
                        Box(
                            Modifier
                                .fillMaxWidth(if (it == 3) 0.6f else 1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE4E0D8).copy(alpha = alpha))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Loop 14: right-edge draggable page scrubber. A slim rail with a grabbable
 * thumb sits over the page-margin area (never over body text), shows
 * `current / total` in a bubble while dragging, and maps the drag fraction to a
 * valid 1-based page via [PdfRotationPolicy.pageFromFraction]. Navigation fires
 * on release through [onJumpRequest] — the pages passed over are NOT eagerly
 * rendered, the shared gotoPdfPage pipeline simply scrolls to the target.
 */
@Composable
private fun PdfPageScrubber(
    currentPage: Int,
    pageCount: Int,
    onJumpRequest: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val pageTotal = pageCount.coerceAtLeast(1)
    val fraction = if (isDragging) dragFraction else
        com.example.formats.pdf.PdfRotationPolicy.fractionForPage(currentPage, pageTotal)
    val displayPage = if (isDragging) {
        com.example.formats.pdf.PdfRotationPolicy.pageFromFraction(dragFraction, pageTotal)
    } else currentPage.coerceIn(1, pageTotal)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(220.dp)
            .semantics {
                contentDescription = "PDF page scroller. Page $displayPage of $pageTotal."
            },
        contentAlignment = Alignment.TopEnd
    ) {
        val handleHeight = 52.dp
        val usableHeight = (maxHeight - handleHeight).coerceAtLeast(0.dp)
        val handleY = usableHeight * fraction.coerceIn(0f, 1f)

        // Floating Page Bubble shown prominently to the left of the pill handle when touching/dragging
        AnimatedVisibility(
            visible = isDragging,
            enter = fadeIn(tween(140)) + slideInVertically(tween(140)) { it / 2 },
            exit = fadeOut(tween(140)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = handleY - 6.dp)
                .padding(end = 46.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.94f),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Page $displayPage",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                    Text(
                        text = "of $pageTotal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // Floating Pill Handle on the right edge
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
            color = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.50f)),
            shadowElevation = if (isDragging) 6.dp else 2.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = handleY)
                .width(if (isDragging) 36.dp else 28.dp)
                .height(handleHeight)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(if (isDragging) 14.dp else 10.dp)
                                .height(2.dp)
                                .background(
                                    if (isDragging) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }
            }
        }

        // Touch capture area along the edge
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(56.dp)
                .align(Alignment.TopEnd)
                .pointerInput(pageTotal, maxHeight) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val available = (size.height - with(density) { handleHeight.toPx() }).coerceAtLeast(1f)
                            dragFraction = ((offset.y - with(density) { handleHeight.toPx() / 2f }) / available)
                                .coerceIn(0f, 1f)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val available = (size.height - with(density) { handleHeight.toPx() }).coerceAtLeast(1f)
                            dragFraction = ((change.position.y - with(density) { handleHeight.toPx() / 2f }) / available)
                                .coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            val targetPage = com.example.formats.pdf.PdfRotationPolicy
                                .pageFromFraction(dragFraction, pageTotal)
                            isDragging = false
                            onJumpRequest(targetPage)
                        },
                        onDragCancel = { isDragging = false }
                    )
                }
        )
    }
}

private class PdfWebAppInterface(
    context: android.content.Context,
    private val onReady: () -> Unit,
    private val onDocumentLoaded: (Int) -> Unit,
    private val onPageChanged: (Int, Int) -> Unit,
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
