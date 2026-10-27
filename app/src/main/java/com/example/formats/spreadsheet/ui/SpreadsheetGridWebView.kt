package com.example.formats.spreadsheet.ui

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
import com.example.formats.spreadsheet.SpreadsheetGridPayload
import com.example.platform.webview.MimeAwareAssetsPathHandler
import java.io.ByteArrayInputStream
import kotlin.math.abs

/**
 * Shared bounded spreadsheet grid used by CSV/TSV and XLSX previews.
 *
 * Unlike the previous inline viewer, no Base64 sheet is embedded in an HTML
 * string or passed through `evaluateJavascript()`. The page, script and UTF-8
 * JSON payload are all served through the fixed local asset-loader origin. Only
 * visible cells are materialised in the DOM, so a 20,000-row sheet does not
 * create two million nodes. Editing emits one bounded cell change at a time to
 * the caller; the existing ViewModel still owns the save decision.
 */
@Composable
fun SpreadsheetGridWebView(
    fileEntity: RecentFileEntity,
    rows: List<List<String>>,
    editable: Boolean,
    sheetName: String?,
    notice: String?,
    isNightMode: Boolean,
    searchQuery: String,
    searchTriggerNext: Int,
    searchTriggerPrev: Int,
    onSearchResultCount: (Int, Int, Boolean) -> Unit,
    zoomFactor: Float,
    onZoomChanged: (Float) -> Unit,
    onCellChanged: (Int, Int, String) -> Unit,
    onCopyText: (String) -> Unit,
    onSingleTap: () -> Unit,
    onWebViewCreated: (android.webkit.WebView?) -> Unit,
    onReady: () -> Unit = {},
    modifier: Modifier = Modifier
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

    // Build once per document/retry. Later cell edits live in the caller's draft
    // and are sent as one-cell deltas, so the payload is never re-copied.
    val payload = remember(fileEntity.path, reloadKey) {
        SpreadsheetGridPayload.build(rows, editable, sheetName, notice)
    }
    val payloadBytes = remember(payload) { payload.json.toByteArray(Charsets.UTF_8) }

    val latestReady = rememberUpdatedState(onReady)
    val latestSearchCount = rememberUpdatedState(onSearchResultCount)
    val latestZoomChanged = rememberUpdatedState(onZoomChanged)
    val latestCellChanged = rememberUpdatedState(onCellChanged)
    val latestCopyText = rememberUpdatedState(onCopyText)
    val latestSingleTap = rememberUpdatedState(onSingleTap)
    val latestWebViewCreated = rememberUpdatedState(onWebViewCreated)
    val latestNightMode = rememberUpdatedState(isNightMode)

    fun retry() {
        loadError = null
        isReady = false
        issuedSearch = InViewerSearchCommandPolicy.Issued()
        lastPageReportedZoom = -1f
        reloadKey++
    }

    LaunchedEffect(webViewRef, isReady, isNightMode) {
        if (webViewRef != null && isReady) {
            webViewRef?.evaluateJavascript("setDarkMode(${latestNightMode.value});", null)
        }
    }

    LaunchedEffect(webViewRef, isReady, zoomFactor) {
        val webView = webViewRef
        if (webView != null && isReady && abs(zoomFactor - lastPageReportedZoom) > 0.004f) {
            webView.evaluateJavascript("setZoom($zoomFactor, false);", null)
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
                    kotlinx.coroutines.delay(120)
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

    LaunchedEffect(fileEntity.path, reloadKey, isReady, loadError) {
        if (!isReady && loadError == null) {
            kotlinx.coroutines.delay(20_000L)
            if (!isReady && loadError == null) {
                loadError = "The spreadsheet viewer did not finish loading. Please try again."
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(if (isNightMode) Color(0xFF151514) else Color(0xFFFAF9F5))) {
        if (loadError == null) {
            key(fileEntity.path, reloadKey) {
                AndroidView(
                    factory = { viewContext ->
                        val assetLoader = androidx.webkit.WebViewAssetLoader.Builder()
                            .addPathHandler("/assets/", MimeAwareAssetsPathHandler(context))
                            .addPathHandler("/spreadsheet-src/", object : androidx.webkit.WebViewAssetLoader.PathHandler {
                                override fun handle(path: String): android.webkit.WebResourceResponse? {
                                    if (path != "data.json") return null
                                    return android.webkit.WebResourceResponse(
                                        "application/json",
                                        "utf-8",
                                        200,
                                        "OK",
                                        mapOf(
                                            "Content-Length" to payloadBytes.size.toString(),
                                            "Cache-Control" to "no-store"
                                        ),
                                        ByteArrayInputStream(payloadBytes)
                                    )
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

                            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                            addJavascriptInterface(object : Any() {
                                @android.webkit.JavascriptInterface
                                fun onGridReady(rowCount: Int, columnCount: Int) {
                                    mainHandler.post {
                                        isReady = true
                                        latestReady.value()
                                    }
                                }

                                @android.webkit.JavascriptInterface
                                fun onGridError(error: String) {
                                    mainHandler.post {
                                        isReady = false
                                        loadError = error.take(180).ifBlank { "The spreadsheet viewer failed to load." }
                                    }
                                }

                                @android.webkit.JavascriptInterface
                                fun onCellChanged(row: Int, column: Int, text: String) {
                                    mainHandler.post {
                                        if (row < 0 || row >= SpreadsheetGridPayload.MAX_ROWS ||
                                            column < 0 || column >= SpreadsheetGridPayload.MAX_COLUMNS
                                        ) return@post
                                        latestCellChanged.value(
                                            row,
                                            column,
                                            text.take(SpreadsheetGridPayload.MAX_CELL_CHARS)
                                        )
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
                                fun onSelectionChanged(label: String, text: String) {
                                    // The selection is owned by the page; the bridge is
                                    // intentionally read-only so document scripts cannot
                                    // mutate Kotlin state through an arbitrary payload.
                                }

                                @android.webkit.JavascriptInterface
                                fun copyToClipboard(text: String) {
                                    mainHandler.post {
                                        latestCopyText.value(text.take(SpreadsheetGridPayload.MAX_CELL_CHARS))
                                    }
                                }

                                @android.webkit.JavascriptInterface
                                fun onGridTap() {
                                    mainHandler.post { latestSingleTap.value() }
                                }
                            }, "Android")

                            webViewClient = object : android.webkit.WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?
                                ): Boolean = OfficeWebViewSecurity.shouldBlockNavigation(request)

                                override fun shouldInterceptRequest(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?
                                ): android.webkit.WebResourceResponse? =
                                    OfficeWebViewSecurity.intercept(request, assetLoader)

                                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                    view?.evaluateJavascript("setDarkMode(${latestNightMode.value});", null)
                                }

                                override fun onReceivedError(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?,
                                    error: android.webkit.WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        view?.post {
                                            isReady = false
                                            loadError = "The spreadsheet viewer page could not be loaded."
                                        }
                                    }
                                }

                                override fun onRenderProcessGone(
                                    view: android.webkit.WebView,
                                    detail: android.webkit.RenderProcessGoneDetail
                                ): Boolean {
                                    view.post {
                                        isReady = false
                                        loadError = "The spreadsheet renderer stopped unexpectedly. Please try again."
                                    }
                                    return true
                                }
                            }
                            loadUrl("https://${OfficeWebViewSecurity.ASSET_HOST}/assets/spreadsheet/grid.html")
                        }
                    },
                    update = { webView ->
                        webViewRef = webView
                        latestWebViewCreated.value(webView)
                        webView.setBackgroundColor(if (isNightMode) 0xFF151514.toInt() else 0xFFFAF9F5.toInt())
                    },
                    onRelease = { webView ->
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
                com.example.ui.component.PremiumLoadingIndicator(text = "Loading Spreadsheet…")
            }
        }
    }
}
