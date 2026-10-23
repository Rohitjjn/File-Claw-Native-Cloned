package com.example.feature.preview

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.FormatLineSpacing
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.automirrored.outlined.Toc
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.component.DocumentOutlineBottomSheet
import com.example.ui.component.AppearanceBottomSheet
import com.example.feature.fileinfo.FilePropertiesBottomSheet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.io.TextWindowReader
import com.example.domain.preview.FormatSearchHit
import com.example.domain.preview.FormatSearchRequest
import com.example.core.format.FormatDetector
import com.example.core.format.FormatRegistry
import com.example.formats.registry.search
import com.example.data.RecentFileEntity
import com.example.formats.epub.ui.HtmlStringViewer
import com.example.formats.font.ui.FontViewer
import com.example.formats.model3d.ui.Model3dViewer
import com.example.formats.sqlite.ui.SqliteViewer
import com.example.formats.svg.ui.SvgViewer
import com.example.ui.component.ClaudeAppBar
import com.example.ui.component.NoticeBanner
import com.example.ui.component.formatFileSize
import com.example.domain.preview.FileContentState
import com.example.viewmodel.MainViewModel
import com.example.platform.share.FileShareHelper
import com.example.platform.clipboard.ClipboardHelper
import com.example.feature.preview.model.ViewerScrollRequest
import com.example.formats.text.ui.PlainTextPreview
import com.example.formats.text.TextOperationReader
import com.example.formats.pptx.ui.PptxPreviewWebView
import com.example.formats.pdf.ui.PdfPreview
import com.example.formats.pdf.PdfSearchSnapshot
import com.example.formats.pdf.PdfIntentOpener
import com.example.formats.media.ui.VideoPlayer
import com.example.formats.media.ui.AudioPlayer
import com.example.formats.markdown.ui.MarkdownPreview
import com.example.formats.image.ui.ImagePreview
import com.example.formats.html.ui.WebViewPreview
import com.example.formats.docx.ui.DocxPreviewWebView
import com.example.formats.csv.ui.CsvPreview
import com.example.formats.common.search.InViewerSearchNavigation
import com.example.formats.binary.ui.HexViewer
import com.example.formats.archive.ui.ZipPreview
import java.io.File
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.FormatIndentIncrease
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import com.example.ui.component.DocumentOutlineParser
import com.example.ui.component.ViewerSyntaxTheme
import com.example.ui.component.ViewerAppearancePrefs
import com.example.formats.docx.DocxZoomPolicy
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Slider
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.ui.semantics.contentDescription

/**
 * Chrome-style find bar that replaces the normal app bar while in-document search is
 * active (Loop 10). One compact header row: close control, focused single-line query
 * field, live `current / total` match badge with prev/next arrows and a clear control.
 * The filename and overflow menu stay hidden until search is closed.
 */
@Composable
private fun ViewerFindBarHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onClear: () -> Unit,
    onKeyboardSearch: () -> Unit,
    focusRequester: FocusRequester,
    isPdfSearch: Boolean,
    isLargeTextSearchable: Boolean,
    isPdfSearchLoading: Boolean,
    isFullTextLoading: Boolean,
    pdfSearchError: String?,
    largeTextSearchError: String?,
    totalMatchesCount: Int,
    searchResultsTruncated: Boolean,
    currentMatchIndex: Int,
    pdfPageInfo: String,
    navigableMatchCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val glass = com.example.ui.theme.LocalGlass.current
    Surface(
        color = glass.fillStrong,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .height(48.dp)
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close search",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            "Find in document...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            maxLines = 1
                        )
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onKeyboardSearch() }),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                }
                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = fadeIn(tween(160)),
                    exit = fadeOut(tween(140))
                ) {
                    IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear query",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                SearchMatchBadgeRow(
                    isPdfSearch = isPdfSearch,
                    isLargeTextSearchable = isLargeTextSearchable,
                    isPdfSearchLoading = isPdfSearchLoading,
                    isFullTextLoading = isFullTextLoading,
                    pdfSearchError = pdfSearchError,
                    largeTextSearchError = largeTextSearchError,
                    totalMatchesCount = totalMatchesCount,
                    searchResultsTruncated = searchResultsTruncated,
                    currentMatchIndex = currentMatchIndex,
                    pdfPageInfo = pdfPageInfo,
                    navigableMatchCount = navigableMatchCount,
                    onPrevious = onPrevious,
                    onNext = onNext
                )
            }
            androidx.compose.material3.HorizontalDivider(color = glass.strokeEnd)
        }
    }
}

/** Themed match counter + prev/next controls for the in-document search overlay (v1.3.0). */
@Composable
private fun SearchMatchBadgeRow(
    isPdfSearch: Boolean,
    isLargeTextSearchable: Boolean,
    isPdfSearchLoading: Boolean,
    isFullTextLoading: Boolean,
    pdfSearchError: String?,
    largeTextSearchError: String?,
    totalMatchesCount: Int,
    searchResultsTruncated: Boolean,
    currentMatchIndex: Int,
    pdfPageInfo: String,
    navigableMatchCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val hasError = (pdfSearchError != null && isPdfSearch) ||
        (largeTextSearchError != null && isLargeTextSearchable)
    val matchLabel = when {
        isPdfSearchLoading -> "Searching…"
        isFullTextLoading -> "Searching full file…"
        hasError -> "Search unavailable"
        totalMatchesCount > 0 && searchResultsTruncated -> "${currentMatchIndex + 1}/$navigableMatchCount of $totalMatchesCount$pdfPageInfo"
        totalMatchesCount > 0 -> "${currentMatchIndex + 1}/$totalMatchesCount$pdfPageInfo"
        else -> "0 / 0"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = when {
                hasError -> MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                totalMatchesCount > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        ) {
            Text(
                text = matchLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    hasError -> MaterialTheme.colorScheme.error
                    totalMatchesCount > 0 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
        IconButton(
            onClick = onPrevious,
            enabled = totalMatchesCount > 0 && !isPdfSearchLoading,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match", modifier = Modifier.size(20.dp))
        }
        IconButton(
            onClick = onNext,
            enabled = totalMatchesCount > 0 && !isPdfSearchLoading,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match", modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePreviewScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onNavigateToEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fileState by viewModel.currentFileState.collectAsState()
    val expandedZipPaths by viewModel.expandedZipPaths.collectAsState()
    val settings by viewModel.settingsState.collectAsState()

    val activeFile = remember(fileState) { fileState.fileOrNull }
    val isDarkTheme = MaterialTheme.colorScheme.background == com.example.ui.theme.ClaudeOnyx

    var isBarsVisible by remember { mutableStateOf(true) }

    val isHtmlFile = remember(activeFile) {
        activeFile?.extension?.lowercase() in setOf("html", "htm")
    }

    var isWebRenderActive by remember(activeFile) {
        mutableStateOf(true)
    }

    // HTML files are untrusted input: scripts stay off until the user opts in for this file.
    var htmlScriptsEnabled by remember(activeFile) { mutableStateOf(false) }

    val canBeRenderedInWeb = remember(activeFile) {
        activeFile?.extension?.lowercase() in setOf("html", "htm", "js", "ts", "css", "xml", "txt", "json", "md")
    }

    var currentPdfPage by remember { mutableStateOf(1) }
    var totalPdfPages by remember { mutableStateOf(0) }
    var showPropertiesDialog by remember { mutableStateOf(false) }

    // Loop 14: PDF view rotation in 90-degree steps (toolbar Rotate button). Pure
    // viewer state — the PDF.js document session is never reloaded, so password
    // protected files are asked once per open session. PDFs always display in
    // light mode now, so the night-mode state is DOCX-only and keeps its
    // system-theme seed for documents that still support dark mode.
    var pdfViewRotation by remember(activeFile?.path) { mutableIntStateOf(0) }

    var isDocxNightMode by remember(activeFile?.path) { mutableStateOf(isDarkTheme) }
    var scrollToPage by remember { mutableStateOf<Int?>(null) }
    // Loop 14: bumped on every explicit jump (scrubber release, Go-to-Page dialog) so
    // re-navigating to the SAME page number still re-fires the WebView navigation
    // effect (a repeated identical Int would not).
    var pageJumpToken by remember { mutableIntStateOf(0) }
    var pendingLinkToOpen by remember { mutableStateOf<String?>(null) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showJumpToLineDialog by remember { mutableStateOf(false) }
    var showDocxZoomDialog by remember { mutableStateOf(false) }
    var showOutlineBottomSheet by remember { mutableStateOf(false) }
    var showAppearanceBottomSheet by remember { mutableStateOf(false) }
    val docxZoomSeedContext = LocalContext.current
    var docxZoomFactor by remember(activeFile?.path) {
        mutableFloatStateOf(
            DocxZoomPolicy.fromPercent(ViewerAppearancePrefs.getDocxZoomPercent(docxZoomSeedContext))
        )
    }
    var docxZoomInput by remember(activeFile?.path) { mutableStateOf(DocxZoomPolicy.defaultPercent().toString()) }
    // Loop 12 (Bug A): true between the first pinch frame and the gesture settle. The
    // DOCX zoom pipeline switches to the compositor-only preview path while it is set
    // and applies one authoritative CSS zoom when it flips back to false.
    var docxPinchActive by remember { mutableStateOf(false) }
    var searchTriggerNext by remember { mutableIntStateOf(0) }
    var searchTriggerPrev by remember { mutableIntStateOf(0) }

    // --- Inline edit mode state (MD/TXT): the preview screen itself becomes the editor ---
    val isEditMode by viewModel.isEditMode.collectAsState()
    var editTextValue by remember { mutableStateOf(TextFieldValue("")) }
    var editOriginalContent by remember { mutableStateOf("") }
    val editUndoStack = remember { TextUndoStack(50) }
    val editRedoStack = remember { TextUndoStack(50) }
    var showEditHistoryDialog by remember { mutableStateOf(false) }
    var showDiscardEditsDialog by remember { mutableStateOf(false) }
    var showSaveConfirmDialog by remember { mutableStateOf(false) }
    var showEditSaveAsDialog by remember { mutableStateOf(false) }
    val hasUnsavedEdits = isEditMode && editTextValue.text != editOriginalContent

    // --- Viewer appearance state (MD/TXT): theme, font size and browser-style zoom ---
    var viewerSyntaxTheme by remember { mutableStateOf(ViewerSyntaxTheme.AUTO) }
    var viewerFontSizeSp by remember { mutableIntStateOf(16) }
    var viewerZoomPercent by remember { mutableIntStateOf(100) }
    var pendingScrollRequest by remember { mutableStateOf<ViewerScrollRequest?>(null) }

    var isSearchInDocumentOpen by rememberSaveable { mutableStateOf(false) }
    var inViewerSearchQuery by rememberSaveable { mutableStateOf("") }
    var currentMatchIndex by remember { mutableIntStateOf(0) }
    var totalMatchesCount by remember { mutableIntStateOf(0) }
    var searchResultsTruncated by remember { mutableStateOf(false) }
    var isPdfSearchLoading by remember { mutableStateOf(false) }
    var pdfSearchError by remember { mutableStateOf<String?>(null) }
    var pdfSearchSnapshot by remember { mutableStateOf(PdfSearchSnapshot()) }
    var docxPreviewWebView by remember { mutableStateOf<android.webkit.WebView?>(null) }
    var docxPreviewReady by remember(activeFile?.path) { mutableStateOf(false) }
    var fullPreviewText by remember(activeFile?.path) { mutableStateOf<String?>(null) }
    var largeTextSearchMatches by remember(activeFile?.path) { mutableStateOf<List<FormatSearchHit>>(emptyList()) }
    var largeTextSearchCompletedQuery by remember(activeFile?.path) { mutableStateOf("") }
    var isLargeTextSearchLoading by remember(activeFile?.path) { mutableStateOf(false) }
    var largeTextSearchError by remember(activeFile?.path) { mutableStateOf<String?>(null) }
    // True when the DOCX renderer fell back to the pure-Kotlin text extractor: the
    // WebView zoom pipeline does not exist there, so dead zoom controls must be hidden.
    var isDocxRenderFallback by remember(activeFile?.path) { mutableStateOf(false) }

    LaunchedEffect(activeFile?.path) {
        isBarsVisible = true
        inViewerSearchQuery = ""
        isSearchInDocumentOpen = false
        currentMatchIndex = 0
        totalMatchesCount = 0
        searchResultsTruncated = false
        isPdfSearchLoading = false
        pdfSearchError = null
        pdfSearchSnapshot = PdfSearchSnapshot()
        docxPreviewReady = false
        docxPreviewWebView = null
        largeTextSearchMatches = emptyList()
        largeTextSearchCompletedQuery = ""
        isLargeTextSearchLoading = false
        largeTextSearchError = null
        fullPreviewText = null
        searchTriggerNext = 0
        searchTriggerPrev = 0
        isDocxRenderFallback = false
    }

    val context = LocalContext.current
    val printScope = rememberCoroutineScope()

    // Seed the inline editor with the currently displayed content every time edit mode starts.
    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            val seedContent = (fileState as? FileContentState.TextSuccess)?.content.orEmpty()
            editTextValue = TextFieldValue(seedContent)
            editOriginalContent = seedContent
            editUndoStack.clear()
            editRedoStack.clear()
        }
    }

    // Load persisted viewer appearance preferences once per screen instance.
    LaunchedEffect(Unit) {
        viewerSyntaxTheme = ViewerAppearancePrefs.getTheme(context)
        viewerFontSizeSp = ViewerAppearancePrefs.getFontSize(context)
        viewerZoomPercent = ViewerAppearancePrefs.getZoomPercent(context)
    }

    // Persist the DOCX zoom preference (debounced so pinch gestures never spam disk).
    val docxZoomPersistContext = LocalContext.current
    LaunchedEffect(docxZoomFactor) {
        kotlinx.coroutines.delay(400)
        ViewerAppearancePrefs.setDocxZoomPercent(docxZoomPersistContext, DocxZoomPolicy.toPercent(docxZoomFactor))
    }

    // Auto-focus the in-document search field (and raise the keyboard) when the overlay opens.
    val searchFieldFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(isSearchInDocumentOpen, isBarsVisible) {
        if (isSearchInDocumentOpen && isBarsVisible) {
            delay(220)
            searchFieldFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Restore previously opened file state on recreate
    // Attempted once per screen instance (process-death recovery). Without the flag, "Remove from history"
    // set the state to Idle and this effect immediately re-opened the same file.
    var restoreAttempted by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(fileState) {
        if (fileState is FileContentState.Idle && !restoreAttempted) {
            restoreAttempted = true
            val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val lastPath = prefs.getString("last_previewed_file_path", null)
            if (lastPath != null) {
                val file = java.io.File(lastPath)
                if (file.exists()) {
                    val entity = RecentFileEntity(
                        path = file.absolutePath,
                        name = file.name,
                        size = file.length(),
                        extension = file.extension.lowercase(),
                        lastOpened = System.currentTimeMillis()
                    )
                    viewModel.openFile(entity)
                }
            }
        }
    }

    val isLargeTextSearchable = activeFile?.extension?.lowercase() in setOf("txt", "md") &&
        (fileState as? FileContentState.TextSuccess)?.truncated == true

    LaunchedEffect(activeFile?.path, fileState) {
        val textState = fileState as? FileContentState.TextSuccess
        val extension = activeFile?.extension?.lowercase()
        fullPreviewText = if (textState != null && extension in setOf("txt", "md")) textState.content else null
    }

    LaunchedEffect(activeFile?.path, inViewerSearchQuery, isLargeTextSearchable, fileState) {
        val textState = fileState as? FileContentState.TextSuccess
        if (!isLargeTextSearchable || textState == null) {
            largeTextSearchMatches = emptyList()
            largeTextSearchCompletedQuery = ""
            isLargeTextSearchLoading = false
            largeTextSearchError = null
            return@LaunchedEffect
        }

        val query = inViewerSearchQuery.trim()
        if (query.isEmpty()) {
            largeTextSearchMatches = emptyList()
            largeTextSearchCompletedQuery = ""
            isLargeTextSearchLoading = false
            largeTextSearchError = null
            fullPreviewText = textState.content
            return@LaunchedEffect
        }

        largeTextSearchMatches = emptyList()
        largeTextSearchCompletedQuery = ""
        largeTextSearchError = null
        isLargeTextSearchLoading = true
        fullPreviewText = textState.content
        kotlinx.coroutines.delay(180)
        val searchContext = kotlin.coroutines.coroutineContext
        try {
            val searchFile = File(textState.file.path)
            val detection = FormatDetector.detect(searchFile, searchFile.name)
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                FormatRegistry.search(
                    detection,
                    FormatSearchRequest(
                        file = searchFile,
                        query = query,
                        charsetName = textState.charsetName,
                        shouldContinue = { searchContext.isActive }
                    )
                )
            }
            largeTextSearchMatches = result.hits
            largeTextSearchCompletedQuery = query
            totalMatchesCount = result.matchCount
            searchResultsTruncated = result.truncated
            currentMatchIndex = 0
            isLargeTextSearchLoading = false
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            largeTextSearchError = error.message ?: "Could not search the complete file"
            isLargeTextSearchLoading = false
            totalMatchesCount = 0
        }
    }

    LaunchedEffect(activeFile?.path, inViewerSearchQuery, largeTextSearchCompletedQuery, largeTextSearchMatches, currentMatchIndex) {
        if (!isLargeTextSearchable || inViewerSearchQuery.isBlank() ||
            largeTextSearchCompletedQuery != inViewerSearchQuery.trim() || largeTextSearchMatches.isEmpty()
        ) return@LaunchedEffect

        val textState = fileState as? FileContentState.TextSuccess ?: return@LaunchedEffect
        val match = largeTextSearchMatches.getOrNull(currentMatchIndex) ?: return@LaunchedEffect
        try {
            val charset = java.nio.charset.Charset.forName(textState.charsetName)
            val window = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                TextWindowReader.read(
                    java.io.File(textState.file.path),
                    match.offset,
                    maxBytes = 250_000,
                    charset = charset
                )
            }
            fullPreviewText = window.text
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            largeTextSearchError = error.message ?: "Could not open the selected search result"
        }
    }

    val isFullTextLoading = isLargeTextSearchable && inViewerSearchQuery.isNotBlank() && isLargeTextSearchLoading

    // --- In-document find pipeline (Loop 10): one shared owner for header + keyboard ---
    // Navigation lives in the screen scope so the header find bar and the IME Search
    // action provably call the same logic (the old lambdas were scoped inside the
    // floating overlay, which Bug 1 removes).
    val goToNextMatch = {
        if (fileState is FileContentState.PdfSuccess) {
            val next = InViewerSearchNavigation.nextIndex(currentMatchIndex, pdfSearchSnapshot.matches.size)
            if (next != null) {
                currentMatchIndex = next
                scrollToPage = pdfSearchSnapshot.matches[next].pageNumber - 1
            }
        } else if (isLargeTextSearchable) {
            val next = InViewerSearchNavigation.nextIndex(currentMatchIndex, largeTextSearchMatches.size)
            if (next != null) currentMatchIndex = next
        } else {
            searchTriggerNext++
        }
        Unit
    }
    val goToPrevMatch = {
        if (fileState is FileContentState.PdfSuccess) {
            val prev = InViewerSearchNavigation.prevIndex(currentMatchIndex, pdfSearchSnapshot.matches.size)
            if (prev != null) {
                currentMatchIndex = prev
                scrollToPage = pdfSearchSnapshot.matches[prev].pageNumber - 1
            }
        } else if (isLargeTextSearchable) {
            val prev = InViewerSearchNavigation.prevIndex(currentMatchIndex, largeTextSearchMatches.size)
            if (prev != null) currentMatchIndex = prev
        } else {
            searchTriggerPrev++
        }
        Unit
    }
    // Closing find mode hides the keyboard, restores the normal app bar and clears the
    // query so every engine's existing empty-query path removes stale highlights.
    val closeInViewerSearch = {
        keyboardController?.hide()
        isSearchInDocumentOpen = false
        inViewerSearchQuery = ""
        pdfSearchSnapshot = PdfSearchSnapshot()
        isPdfSearchLoading = false
        pdfSearchError = null
        largeTextSearchMatches = emptyList()
        largeTextSearchCompletedQuery = ""
        largeTextSearchError = null
        searchResultsTruncated = false
        totalMatchesCount = 0
        currentMatchIndex = 0
        Unit
    }
    val toggleInViewerSearch = {
        if (isSearchInDocumentOpen) {
            closeInViewerSearch()
        } else {
            // The find bar lives in the header: reveal the bars when opening from focus mode.
            isBarsVisible = true
            isSearchInDocumentOpen = true
        }
        Unit
    }

    val handleBack = {
        val parentPath = activeFile?.parentZipPath
        if (parentPath != null) {
            viewModel.openParentZip(parentPath)
        } else {
            onBackClick()
        }
    }

    androidx.activity.compose.BackHandler {
        if (isSearchInDocumentOpen) {
            // Deliberate Chrome-style behaviour: back closes find mode first.
            closeInViewerSearch()
        } else if (isEditMode) {
            if (hasUnsavedEdits) showDiscardEditsDialog = true else viewModel.exitEditMode()
        } else {
            handleBack()
        }
    }

    val activity = context as? android.app.Activity
    val originalWindowBars = remember(activity) {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            Triple(window.statusBarColor, window.navigationBarColor, controller.isAppearanceLightStatusBars to controller.isAppearanceLightNavigationBars)
        }
    }
    val originalSystemBarsBehavior = remember(activity) {
        activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView).systemBarsBehavior }
    }
    val originalDecorBackground = remember(activity) { activity?.window?.decorView?.background }
    val originalCutoutMode = remember(activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            activity?.window?.attributes?.layoutInDisplayCutoutMode
        } else null
    }
    SideEffect {
        val window = activity?.window
        if (window != null) {
            // The activity is edge-to-edge; keep the preview behind the system insets in focus mode.
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            // Loop 14: PDF always renders in light mode regardless of the in-app theme,
            // so the status-bar canvas follows the system theme for PDFs again.
            val isDarkCanvas = when (activeFile?.extension?.lowercase()) {
                "pdf" -> isDarkTheme
                "docx", "doc" -> isDocxNightMode
                else -> isDarkTheme
            }
            val canvasColor = when (activeFile?.extension?.lowercase()) {
                "pdf" -> android.graphics.Color.rgb(240, 240, 240)
                "docx", "doc" -> if (isDocxNightMode) android.graphics.Color.rgb(21, 21, 20) else android.graphics.Color.rgb(250, 249, 245)
                else -> if (isDarkTheme) android.graphics.Color.rgb(24, 23, 21) else android.graphics.Color.rgb(250, 249, 245)
            }
            window.decorView.setBackgroundColor(canvasColor)
            insetsController.isAppearanceLightStatusBars = !isDarkCanvas
            insetsController.isAppearanceLightNavigationBars = !isDarkCanvas
            if (!isBarsVisible) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    window.attributes = window.attributes.apply {
                        layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && originalCutoutMode != null) {
                    window.attributes = window.attributes.apply { layoutInDisplayCutoutMode = originalCutoutMode }
                }
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    DisposableEffect(activity) {
        onDispose {
            val window = activity?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                originalWindowBars?.let { (status, navigation, appearance) ->
                    window.statusBarColor = status
                    window.navigationBarColor = navigation
                    insetsController.isAppearanceLightStatusBars = appearance.first
                    insetsController.isAppearanceLightNavigationBars = appearance.second
                }
                if (originalSystemBarsBehavior != null) {
                    insetsController.systemBarsBehavior = originalSystemBarsBehavior
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && originalCutoutMode != null) {
                    window.attributes = window.attributes.apply { layoutInDisplayCutoutMode = originalCutoutMode }
                }
                window.decorView.background = originalDecorBackground
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            androidx.compose.animation.AnimatedVisibility(
                visible = isBarsVisible,
                enter = androidx.compose.animation.slideInVertically(
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    initialOffsetY = { -it }
                ) + androidx.compose.animation.fadeIn(tween(220)),
                exit = androidx.compose.animation.slideOutVertically(
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    targetOffsetY = { -it }
                ) + androidx.compose.animation.fadeOut(tween(220)),
                modifier = Modifier.statusBarsPadding()
            ) {
                if (isSearchInDocumentOpen) {
                    // Bug 1 (Loop 10): while find mode is active the search field owns the
                    // header row itself (Chrome-style), replacing the filename app bar —
                    // no second floating search surface below the header.
                    ViewerFindBarHeader(
                        query = inViewerSearchQuery,
                        onQueryChange = { value ->
                            inViewerSearchQuery = value
                            currentMatchIndex = 0
                            totalMatchesCount = 0
                            searchResultsTruncated = false
                            pdfSearchError = null
                            largeTextSearchMatches = emptyList()
                            largeTextSearchCompletedQuery = ""
                            largeTextSearchError = null
                            if (fileState is FileContentState.PdfSuccess) {
                                isPdfSearchLoading = value.isNotBlank()
                                pdfSearchSnapshot = PdfSearchSnapshot()
                            }
                        },
                        onClose = { closeInViewerSearch() },
                        onClear = {
                            inViewerSearchQuery = ""
                            currentMatchIndex = 0
                            totalMatchesCount = 0
                            searchResultsTruncated = false
                            pdfSearchSnapshot = PdfSearchSnapshot()
                            isPdfSearchLoading = false
                            pdfSearchError = null
                            largeTextSearchMatches = emptyList()
                            largeTextSearchCompletedQuery = ""
                            largeTextSearchError = null
                        },
                        onKeyboardSearch = { goToNextMatch() },
                        focusRequester = searchFieldFocusRequester,
                        isPdfSearch = fileState is FileContentState.PdfSuccess,
                        isLargeTextSearchable = isLargeTextSearchable,
                        isPdfSearchLoading = isPdfSearchLoading,
                        isFullTextLoading = isFullTextLoading,
                        pdfSearchError = pdfSearchError,
                        largeTextSearchError = largeTextSearchError,
                        totalMatchesCount = totalMatchesCount,
                        searchResultsTruncated = searchResultsTruncated,
                        currentMatchIndex = currentMatchIndex,
                        pdfPageInfo = pdfSearchSnapshot.matches.getOrNull(currentMatchIndex)?.pageNumber?.let { " (p.$it)" }.orEmpty(),
                        navigableMatchCount = run {
                            val extension = activeFile?.extension?.lowercase()
                            when {
                                fileState is FileContentState.PdfSuccess -> pdfSearchSnapshot.matches.size
                                isLargeTextSearchable -> largeTextSearchMatches.size
                                extension in setOf("docx", "doc") -> minOf(totalMatchesCount, 5_000)
                                extension in setOf("txt", "md") -> minOf(totalMatchesCount, 1_000)
                                else -> totalMatchesCount
                            }
                        },
                        onPrevious = { goToPrevMatch() },
                        onNext = { goToNextMatch() }
                    )
                    return@AnimatedVisibility
                }
                val titleText = fileState.fileOrNull?.name ?: "Preview"

                var showMenu by remember { mutableStateOf(false) }
                val context = LocalContext.current

                ClaudeAppBar(
                    title = titleText,
                    onNavIconClick = if (isEditMode) {
                        {
                            // X icon: discard changes with confirmation when dirty.
                            if (hasUnsavedEdits) showDiscardEditsDialog = true else viewModel.exitEditMode()
                        }
                    } else {
                        handleBack
                    },
                    navIcon = if (isEditMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    actions = {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Share file") },
                                    onClick = {
                                        showMenu = false
                                        if (!isEditMode) {
                                            activeFile?.let { fileEntity ->
                                                FileShareHelper.share(context, File(fileEntity.path), fileEntity.extension)
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share file"
                                        )
                                    }
                                )

                                if (isEditMode) {
                                    DropdownMenuItem(
                                        text = { Text("Save changes", fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            showMenu = false
                                            if (hasUnsavedEdits) showSaveConfirmDialog = true else viewModel.exitEditMode()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Save,
                                                contentDescription = "Save changes",
                                                tint = if (hasUnsavedEdits) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export modified copy…") },
                                        onClick = {
                                            showMenu = false
                                            showEditSaveAsDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.DriveFileMove,
                                                contentDescription = "Export modified copy"
                                            )
                                        }
                                    )
                                }

                                val isPdf = activeFile?.extension?.lowercase() == "pdf"
                                val isDocx = activeFile?.extension?.lowercase() in setOf("docx", "doc")
                                if (isPdf || isDocx) {
                                    DropdownMenuItem(
                                        text = { Text("Go to Page ($currentPdfPage / $totalPdfPages)") },
                                        onClick = {
                                            showMenu = false
                                            showJumpDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.InsertDriveFile,
                                                contentDescription = "Go to Page"
                                            )
                                        }
                                    )
                                    // Loop 14: the PDF night/light toggle was removed entirely —
                                    // PDFs always render in light mode. Dark mode remains for
                                    // DOCX/TXT/Markdown and other file types.
                                    if (!isPdf) {
                                        DropdownMenuItem(
                                            text = { Text(if (isDocxNightMode) "Light Mode" else "Dark Mode") },
                                            onClick = {
                                                showMenu = false
                                                isDocxNightMode = !isDocxNightMode
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (isDocxNightMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                                    contentDescription = "Toggle Dark Mode"
                                                 )
                                            }
                                        )
                                    }
                                    if (isPdf) {
                                        DropdownMenuItem(
                                            text = { Text("Open in System Viewer") },
                                            onClick = {
                                                showMenu = false
                                                activeFile?.let { fileEntity ->
                                                    PdfIntentOpener.open(context, java.io.File(fileEntity.path))
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Outlined.Language,
                                                    contentDescription = "Open in System Viewer"
                                                )
                                            }
                                        )
                                    }
                                }

                                val hasOutline = activeFile?.extension?.lowercase() in setOf("md", "markdown", "txt") && !isEditMode
                                if (hasOutline) {
                                    DropdownMenuItem(
                                        text = { Text("Document Outline") },
                                        onClick = {
                                            showMenu = false
                                            showOutlineBottomSheet = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.Toc,
                                                contentDescription = "Document Outline"
                                            )
                                        }
                                    )
                                }

                                val isTextFile = activeFile?.extension?.lowercase() in setOf("txt", "md", "json", "xml", "html", "htm", "js", "ts", "css", "log", "yaml", "yml", "ini", "conf", "sh", "py", "java", "kt", "c", "cpp", "h", "sql", "csv", "tsv")
                                if (isTextFile) {
                                    DropdownMenuItem(
                                        text = { Text("Viewer Appearance") },
                                        onClick = {
                                            showMenu = false
                                            showAppearanceBottomSheet = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Palette,
                                                contentDescription = "Viewer Appearance"
                                            )
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Go to Line...") },
                                        onClick = {
                                            showMenu = false
                                            showJumpToLineDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.FormatLineSpacing,
                                                contentDescription = "Go to Line"
                                            )
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text("Print / Export as PDF") },
                                    onClick = {
                                        showMenu = false
                                        activeFile?.let { entity ->
                                            val ext = entity.extension.lowercase()
                                            when {
                                                ext == "pdf" -> {
                                                    com.example.platform.print.FilePrintHelper.printPdf(context, java.io.File(entity.path), entity.name)
                                                }
                                                ext in setOf("docx", "doc") -> {
                                                    val renderedWebView = docxPreviewWebView
                                                    if (docxPreviewReady && renderedWebView != null) {
                                                        com.example.platform.print.FilePrintHelper.printWebView(context, renderedWebView, entity.name)
                                                    } else {
                                                        com.example.platform.print.FilePrintHelper.printDocx(context, java.io.File(entity.path), entity.name)
                                                    }
                                                }
                                                ext == "md" || ext == "txt" -> {
                                                    val textState = fileState as? FileContentState.TextSuccess
                                                    printScope.launch {
                                                        val result = if (textState != null && !textState.truncated) {
                                                            Result.success(textState.content)
                                                        } else {
                                                            TextOperationReader.readComplete(
                                                                java.io.File(entity.path),
                                                                textState?.charsetName ?: "UTF-8"
                                                            )
                                                        }
                                                        result.onSuccess { fullContent ->
                                                            if (ext == "md") {
                                                                com.example.platform.print.FilePrintHelper.printMarkdown(context, fullContent, entity.name)
                                                            } else {
                                                                com.example.platform.print.FilePrintHelper.printTextOrMarkdown(context, fullContent, entity.name)
                                                            }
                                                        }.onFailure { error ->
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                error.message ?: "Could not prepare this file for printing",
                                                                android.widget.Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    }
                                                }
                                                ext in setOf("png", "jpg", "jpeg", "webp", "gif") -> {
                                                    com.example.platform.print.FilePrintHelper.printImage(context, java.io.File(entity.path), entity.name)
                                                }
                                                else -> {
                                                    // The four document formats have bounded/dedicated branches above; preserve legacy fallback for other types.
                                                    val content = (fileState as? FileContentState.TextSuccess)?.content
                                                        ?: runCatching { java.io.File(entity.path).readText() }.getOrNull() ?: ""
                                                    com.example.platform.print.FilePrintHelper.printTextOrMarkdown(context, content, entity.name)
                                                }
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Print,
                                            contentDescription = "Print or Export as PDF"
                                        )
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Properties") },
                                    onClick = {
                                        showMenu = false
                                        showPropertiesDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = "Properties"
                                        )
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Remove from history", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        activeFile?.let { fileEntity ->
                                            viewModel.deleteRecentFile(fileEntity)
                                            handleBack()
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove from history",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            val isPdf = fileState is FileContentState.PdfSuccess
            // Gate on the real state: legacy .doc files open in the hex viewer and must not
            // show DOCX zoom controls.
            val isDocx = fileState is FileContentState.DocxSuccess
            val isImage = fileState is FileContentState.ImageSuccess
            val isText = fileState is FileContentState.TextSuccess ||
                (activeFile?.extension?.lowercase() in setOf("txt", "md", "json", "xml", "html", "htm", "js", "ts", "css", "log", "yaml", "yml", "ini", "conf", "sh", "py", "java", "kt", "c", "cpp", "h", "sql", "csv", "tsv"))

            val showFloatingBar = isPdf || isDocx || isImage || isText

            if (showFloatingBar) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isBarsVisible,
                    enter = androidx.compose.animation.slideInVertically(
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                        initialOffsetY = { it }
                    ) + androidx.compose.animation.fadeIn(tween(220)),
                    exit = androidx.compose.animation.slideOutVertically(
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                        targetOffsetY = { it }
                    ) + androidx.compose.animation.fadeOut(tween(220))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                // Bug 2 (Loop 10): while inline editing, lift the tray above
                                // the keyboard. safeDrawing.bottom is the UNION of navigation
                                // bar and IME insets, so the padding is correct in both states
                                // and never double-counts the gesture-nav area. Reading mode
                                // keeps the plain navigation-bar padding, unchanged.
                                if (isEditMode) {
                                    Modifier.windowInsetsPadding(
                                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                                    )
                                } else {
                                    Modifier.navigationBarsPadding()
                                }
                            )
                            .padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            shadowElevation = 8.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                if (isPdf) {
                                    // Page indicator / Jump button
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.clickable { showJumpDialog = true }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.MenuBook,
                                                contentDescription = "Page indicator",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "$currentPdfPage / ${totalPdfPages.coerceAtLeast(1)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Screen orientation control: rotate between portrait and sensor landscape
                                    IconButton(
                                        onClick = {
                                            val activity = context.findHostActivity()
                                            if (activity != null) {
                                                activity.requestedOrientation =
                                                    com.example.platform.lifecycle.ScreenOrientationPolicy.toggleOrientation(
                                                        activity.resources.configuration.orientation
                                                    )
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.RotateRight,
                                            contentDescription = "Switch portrait/landscape screen orientation",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Find in PDF (Loop 14: moved into the Night/Light toggle's
                                    // former slot; the PDF-only night mode was removed entirely)
                                    IconButton(
                                        onClick = { toggleInViewerSearch() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Search,
                                            contentDescription = "Find in document",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isSearchInDocumentOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Print / Save as PDF
                                    IconButton(
                                        onClick = {
                                            activeFile?.let {
                                                com.example.platform.print.FilePrintHelper.printPdf(context, java.io.File(it.path), it.name)
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Print,
                                            contentDescription = "Print or Save as PDF",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Share File
                                    IconButton(
                                        onClick = { activeFile?.let { FileShareHelper.share(context, File(it.path), it.extension) } },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Share,
                                            contentDescription = "Share PDF",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else if (isDocx) {
                                    // Loop 10: the zoom pipeline only exists in the WebView
                                    // renderer; hide the dead zoom controls when the pure-Kotlin
                                    // fallback renderer took over (its text preview has no zoom).
                                    if (!isDocxRenderFallback) {
                                    // DOCX Zoom Out Button
                                    IconButton(
                                        onClick = {
                                            docxZoomFactor = (docxZoomFactor - 0.25f).coerceAtLeast(0.25f)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.ZoomOut,
                                            contentDescription = "Zoom Out",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Zoom percentage chip
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.clickable {
                                            docxZoomInput = (docxZoomFactor * 100).toInt().toString()
                                            showDocxZoomDialog = true
                                        }
                                    ) {
                                        Text(
                                            text = "${(docxZoomFactor * 100).toInt()}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    // DOCX Zoom In Button
                                    IconButton(
                                        onClick = {
                                            docxZoomFactor = (docxZoomFactor + 0.25f).coerceAtMost(5.0f)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.ZoomIn,
                                            contentDescription = "Zoom In",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    }

                                    // Find in DOCX
                                    IconButton(
                                        onClick = { toggleInViewerSearch() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Search,
                                            contentDescription = "Find in DOCX",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isSearchInDocumentOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // DOCX Night Mode toggle
                                    IconButton(
                                        onClick = { isDocxNightMode = !isDocxNightMode },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isDocxNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = "Toggle night mode",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isDocxNightMode) Color(0xFFFFB74D) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Print DOCX
                                    IconButton(
                                        onClick = {
                                            activeFile?.let {
                                                val renderedWebView = docxPreviewWebView
                                                if (docxPreviewReady && renderedWebView != null) {
                                                    com.example.platform.print.FilePrintHelper.printWebView(context, renderedWebView, it.name)
                                                } else {
                                                    com.example.platform.print.FilePrintHelper.printDocx(context, java.io.File(it.path), it.name)
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Print,
                                            contentDescription = "Print or export as PDF",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Share DOCX
                                    IconButton(
                                        onClick = { activeFile?.let { FileShareHelper.share(context, File(it.path), it.extension) } },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Share,
                                            contentDescription = "Share document",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else if (isImage) {
                                    // Image counter
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Image,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Photo Viewer",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Print Image
                                    IconButton(
                                        onClick = {
                                            activeFile?.let {
                                                com.example.platform.print.FilePrintHelper.printImage(context, java.io.File(it.path), it.name)
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Print,
                                            contentDescription = "Print photo",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Share Image
                                    IconButton(
                                        onClick = { activeFile?.let { FileShareHelper.share(context, File(it.path), it.extension) } },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Share,
                                            contentDescription = "Share image",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else if (isText) {
                                    if (isEditMode) {
                                        // Inline editing tray — replaces the reading tray in place,
                                        // keeping the exact same floating pill so the screen never jumps.
                                        // Undo
                                        IconButton(
                                            onClick = {
                                                if (editUndoStack.isNotEmpty) {
                                                    val prevState = editUndoStack.pop()
                                                    if (prevState != null) {
                                                        editRedoStack.push(editTextValue)
                                                        editTextValue = prevState
                                                    }
                                                }
                                            },
                                            enabled = editUndoStack.isNotEmpty,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Undo,
                                                contentDescription = "Undo",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // Redo
                                        IconButton(
                                            onClick = {
                                                if (editRedoStack.isNotEmpty) {
                                                    val nextState = editRedoStack.pop()
                                                    if (nextState != null) {
                                                        editUndoStack.push(editTextValue)
                                                        editTextValue = nextState
                                                    }
                                                }
                                            },
                                            enabled = editRedoStack.isNotEmpty,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Redo,
                                                contentDescription = "Redo",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // Tab indent
                                        IconButton(
                                            onClick = {
                                                applyTabIndent(editTextValue.text, editTextValue.selection, settings.tabSize)?.let { result ->
                                                    editUndoStack.push(editTextValue)
                                                    editRedoStack.clear()
                                                    editTextValue = TextFieldValue(result.text, result.newSelection)
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.FormatIndentIncrease,
                                                contentDescription = "Insert tab indent",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // Markdown bold
                                        IconButton(
                                            onClick = {
                                                editUndoStack.push(editTextValue)
                                                editRedoStack.clear()
                                                applyMarkdownWrap(editTextValue.text, editTextValue.selection, "**").let { result ->
                                                    editTextValue = TextFieldValue(result.text, result.newSelection)
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.FormatBold,
                                                contentDescription = "Bold (markdown)",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // Markdown italic
                                        IconButton(
                                            onClick = {
                                                editUndoStack.push(editTextValue)
                                                editRedoStack.clear()
                                                applyMarkdownWrap(editTextValue.text, editTextValue.selection, "*").let { result ->
                                                    editTextValue = TextFieldValue(result.text, result.newSelection)
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.FormatItalic,
                                                contentDescription = "Italic (markdown)",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // Undo history viewer
                                        IconButton(
                                            onClick = { showEditHistoryDialog = true },
                                            enabled = editUndoStack.isNotEmpty,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.History,
                                                contentDescription = "Edit history",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // Save pill with confirmation
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable {
                                                if (hasUnsavedEdits) showSaveConfirmDialog = true else viewModel.exitEditMode()
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Save,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Save",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else {
                                    // Find in Text
                                    IconButton(
                                        onClick = { toggleInViewerSearch() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Search,
                                            contentDescription = "Find in document",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isSearchInDocumentOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Jump to line in Text/Code
                                    IconButton(
                                        onClick = { showJumpToLineDialog = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.FormatLineSpacing,
                                            contentDescription = "Jump to line",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Print / Export as PDF
                                    IconButton(
                                        onClick = {
                                            val ext = activeFile?.extension?.lowercase() ?: ""
                                            val entity = activeFile
                                            val textState = fileState as? FileContentState.TextSuccess
                                            if (entity != null) {
                                                printScope.launch {
                                                    val result = if (textState != null && !textState.truncated) {
                                                        Result.success(textState.content)
                                                    } else {
                                                        TextOperationReader.readComplete(
                                                            java.io.File(entity.path),
                                                            textState?.charsetName ?: "UTF-8"
                                                        )
                                                    }
                                                    result.onSuccess { textToPrint ->
                                                        if (ext == "md") {
                                                            com.example.platform.print.FilePrintHelper.printMarkdown(context, textToPrint, entity.name)
                                                        } else {
                                                            com.example.platform.print.FilePrintHelper.printTextOrMarkdown(context, textToPrint, entity.name)
                                                        }
                                                    }.onFailure { error ->
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            error.message ?: "Could not prepare this file for printing",
                                                            android.widget.Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Print,
                                            contentDescription = "Print or export as PDF",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Copy all text
                                    IconButton(
                                        onClick = {
                                            val textState = fileState as? FileContentState.TextSuccess
                                            val entity = activeFile
                                            if (textState != null && !textState.truncated) {
                                                ClipboardHelper.copyText(context, entity?.name ?: "File Content", textState.content)
                                            } else if (textState != null && entity != null) {
                                                printScope.launch {
                                                    TextOperationReader.readComplete(java.io.File(entity.path), textState.charsetName)
                                                        .onSuccess { ClipboardHelper.copyText(context, entity.name, it) }
                                                        .onFailure { ClipboardHelper.showTextOperationError(context, it) }
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.ContentCopy,
                                            contentDescription = "Copy all text",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    val isEditable = (fileState as? FileContentState.TextSuccess)?.editable == true
                                    if (isEditable) {
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable {
                                                // Inline edit: keep this same screen and morph it into
                                                // an editor instead of opening a separate editor route.
                                                if (!viewModel.enterEditMode()) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "This file can't be edited here",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Edit",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Share Text
                                    IconButton(
                                        onClick = { activeFile?.let { FileShareHelper.share(context, File(it.path), it.extension) } },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Share,
                                            contentDescription = "Share file",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.testTag("file_preview_screen")
    ) { innerPadding ->
        val usesReaderGestures = activeFile?.extension?.lowercase() in setOf("pdf", "doc", "docx", "txt", "md")
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!usesReaderGestures) {
                        Modifier.pointerInput(activeFile?.path) {
                            detectTapGestures(onDoubleTap = { isBarsVisible = !isBarsVisible })
                        }
                    } else Modifier
                )
                .background(MaterialTheme.colorScheme.background)
                // Always respect the animated scaffold insets: the padding shrinks frame-by-frame
                // while the bars slide away, so focus mode no longer snaps the content full-bleed.
                .padding(innerPadding)
        ) {
            // Subtle Floating Back & Exit Pill when !isBarsVisible (Focus mode)
            androidx.compose.animation.AnimatedVisibility(
                visible = !isBarsVisible,
                enter = androidx.compose.animation.fadeIn(tween(220)) +
                    androidx.compose.animation.slideInVertically(tween(260, easing = FastOutSlowInEasing)) { -it },
                exit = androidx.compose.animation.fadeOut(tween(220)) +
                    androidx.compose.animation.slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { -it },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = handleBack,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { isBarsVisible = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.FullscreenExit,
                                contentDescription = "Exit Focus Mode",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            when (val state = fileState) {
                is FileContentState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        com.example.ui.component.PremiumLoadingIndicator(text = "Loading file...")
                    }
                }
                is FileContentState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Preview Error",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                is FileContentState.ArchivePasswordRequired -> {
                    var inputPassword by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { onBackClick() },
                        title = { Text("Password Required") },
                        text = {
                            Column {
                                Text("This archive is password protected.", fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = inputPassword,
                                    onValueChange = { inputPassword = it },
                                    singleLine = true,
                                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    label = { Text("Password") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                viewModel.openFile(state.file, inputPassword)
                            }) {
                                Text("Open")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { onBackClick() }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                is FileContentState.TextSuccess -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val completeContent = fullPreviewText ?: state.content
                        val supportsFullFileSearch = state.file.extension.lowercase() in setOf("txt", "md")
                        val notice = state.readOnlyReason ?: when {
                            state.truncated && supportsFullFileSearch && isFullTextLoading -> "Searching the complete ${formatFileSize(state.totalBytes)} file…"
                            state.truncated && supportsFullFileSearch && largeTextSearchError != null -> "Full-file search failed: ${largeTextSearchError.orEmpty()}"
                            state.truncated && supportsFullFileSearch -> "Preview is bounded to protect memory; in-document search scans the complete ${formatFileSize(state.totalBytes)} file and opens matches in bounded excerpts (first 10,000 results are navigable)."
                            state.truncated -> "Preview is limited to the first ${state.content.length} characters to protect memory."
                            else -> null
                        }
                        if (notice != null && isBarsVisible) NoticeBanner(notice)
                        if (isHtmlFile && isWebRenderActive && !htmlScriptsEnabled) {
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 16.dp, end = 4.dp)
                                ) {
                                    Text(
                                        "Scripts are off for safety.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { htmlScriptsEnabled = true }) { Text("Run scripts") }
                                }
                            }
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (isEditMode) {
                                // Inline editing workspace: sits on the exact same surface as the
                                // preview canvas (no line-number gutter) so the screen keeps looking
                                // like the preview — the user simply starts typing in place.
                                InlineEditWorkspace(
                                    value = editTextValue,
                                    onValueChange = { newValue ->
                                        val textChanged = newValue.text != editTextValue.text
                                        if (textChanged) {
                                            editUndoStack.push(editTextValue)
                                            editRedoStack.clear()
                                        }
                                        editTextValue = newValue
                                    },
                                    settings = settings,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else when (state.file.extension.lowercase()) {
                                "md" -> MarkdownPreview(
                                    markdownText = completeContent,
                                    fileEntity = state.file,
                                    fontSizeSetting = settings.fontSize,
                                    isNightMode = isDarkTheme,
                                    searchQuery = inViewerSearchQuery,
                                    searchTriggerNext = searchTriggerNext,
                                    searchTriggerPrev = searchTriggerPrev,
                                    onSearchResultCount = { count, index, truncated ->
                                        if (!isLargeTextSearchable) {
                                            totalMatchesCount = count
                                            currentMatchIndex = if (count > 0) index.coerceAtLeast(0) else 0
                                            searchResultsTruncated = truncated
                                        }
                                    },
                                    onSingleTap = { isBarsVisible = !isBarsVisible },
                                    syntaxTheme = viewerSyntaxTheme,
                                    viewerFontSizeSp = viewerFontSizeSp,
                                    zoomPercent = viewerZoomPercent,
                                    scrollRequest = pendingScrollRequest,
                                    onScrollRequestHandled = { pendingScrollRequest = null },
                                    modifier = Modifier.fillMaxSize()
                                )
                                "txt" -> PlainTextPreview(
                                    content = completeContent,
                                    isDarkMode = isDarkTheme,
                                    fontSizeSetting = settings.fontSize,
                                    searchQuery = inViewerSearchQuery,
                                    searchTriggerNext = searchTriggerNext,
                                    searchTriggerPrev = searchTriggerPrev,
                                    onSearchResultCount = { count, index, truncated ->
                                        if (!isLargeTextSearchable) {
                                            totalMatchesCount = count
                                            currentMatchIndex = if (count > 0) index.coerceAtLeast(0) else 0
                                            searchResultsTruncated = truncated
                                        }
                                    },
                                    onSingleTap = { isBarsVisible = !isBarsVisible },
                                    syntaxTheme = viewerSyntaxTheme,
                                    viewerFontSizeSp = viewerFontSizeSp,
                                    zoomPercent = viewerZoomPercent,
                                    scrollRequest = pendingScrollRequest,
                                    onScrollRequestHandled = { pendingScrollRequest = null },
                                    modifier = Modifier.fillMaxSize()
                                )
                                else -> WebViewPreview(
                                    fileEntity = state.file,
                                    allowScripts = htmlScriptsEnabled,
                                    searchQuery = inViewerSearchQuery,
                                    onSingleTap = { isBarsVisible = !isBarsVisible },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
                is FileContentState.CsvSuccess -> {
                    CsvPreview(
                        csvRows = state.rows,
                        fileEntity = state.file,
                        viewModel = viewModel,
                        editable = state.editable,
                        notice = state.readOnlyReason
                            ?: if (state.truncated) "Large file: showing the first ${state.rows.size} rows. Read-only, so nothing is lost on save." else null,
                        onDiscardAndExit = { handleBack() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is FileContentState.ZipSuccess -> {
                    ZipPreview(
                        zipRoot = state.root,
                        fileEntity = state.file,
                        expandedPaths = expandedZipPaths,
                        onToggleExpand = { path ->
                            viewModel.toggleZipPathExpanded(path)
                        },
                        onZipEntryClick = { node, pwd ->
                            viewModel.openZipEntry(state.file, node, pwd)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is FileContentState.ImageSuccess -> {
                    ImagePreview(
                        fileEntity = state.file,
                        modifier = Modifier.fillMaxSize(),
                        onTap = { isBarsVisible = !isBarsVisible },
                        onPageChanged = { viewModel.recordImageFolderPage(state.file, it) }
                    )
                }
                is FileContentState.PdfSuccess -> {
                    PdfPreview(
                        fileEntity = state.file,
                        viewRotation = pdfViewRotation,
                        searchQuery = inViewerSearchQuery,
                        selectedMatchIndex = currentMatchIndex,
                        scrollToPage = scrollToPage,
                        scrollToken = pageJumpToken,
                        onScrollToPageHandled = { scrollToPage = null },
                        scrubberPage = currentPdfPage,
                        scrubberPageCount = totalPdfPages,
                        onScrubJump = { page ->
                            scrollToPage = page - 1
                            pageJumpToken++
                        },
                        onSearchSnapshot = { snapshot, searching ->
                            pdfSearchSnapshot = snapshot
                            totalMatchesCount = snapshot.count
                            searchResultsTruncated = snapshot.truncated
                            isPdfSearchLoading = searching
                            pdfSearchError = null
                            if (!searching) currentMatchIndex = 0
                        },
                        onSearchError = { message ->
                            pdfSearchError = message
                            isPdfSearchLoading = false
                        },
                        modifier = Modifier.fillMaxSize(),
                        onSingleTap = { isBarsVisible = !isBarsVisible },
                        onPageChanged = { page, total ->
                            currentPdfPage = page
                            totalPdfPages = total
                        },
                        onBackClick = handleBack
                    )
                }
                is FileContentState.DocxSuccess -> {
                    DocxPreviewWebView(
                        base64Docx = state.base64Data,
                        fileEntity = state.file,
                        isNightMode = isDocxNightMode,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                        onPageChanged = { page, total ->
                            currentPdfPage = page
                            totalPdfPages = total
                        },
                        scrollToPage = scrollToPage,
                        onScrollProgressHandled = { scrollToPage = null },
                        searchQuery = inViewerSearchQuery,
                        searchTriggerNext = searchTriggerNext,
                        searchTriggerPrev = searchTriggerPrev,
                        onSearchResultCount = { count, index, truncated ->
                            totalMatchesCount = count
                            currentMatchIndex = if (count > 0) index.coerceAtLeast(0) else 0
                            searchResultsTruncated = truncated
                        },
                        zoomFactor = docxZoomFactor,
                        pinchActive = docxPinchActive,
                        onWebViewCreated = { docxPreviewWebView = it },
                        onDocumentReady = { docxPreviewReady = true },
                        onSingleTap = { isBarsVisible = !isBarsVisible },
                        onZoomChanged = { factor -> docxZoomFactor = factor },
                        onPinchZoom = { delta ->
                            docxPinchActive = true
                            docxZoomFactor = com.example.formats.docx.DocxZoomPolicy.applyPinch(docxZoomFactor, delta)
                        },
                        onPinchSettled = {
                            docxPinchActive = false
                        },
                        onDoubleTap = {
                            docxZoomFactor = com.example.formats.docx.DocxZoomPolicy.toggleDoubleTap(docxZoomFactor)
                        },
                        onRendererFallback = { isFallback -> isDocxRenderFallback = isFallback }
                    )
                }
                is FileContentState.PptxSuccess -> {
                    PptxPreviewWebView(
                        base64Pptx = state.base64Data,
                        fileEntity = state.file,
                        isNightMode = isDocxNightMode,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is FileContentState.MediaSuccess -> {
                    if (state.isAudio) {
                        AudioPlayer(
                            fileEntity = state.file,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        VideoPlayer(
                            fileEntity = state.file,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                is FileContentState.BinarySuccess -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val header = listOfNotNull(state.label?.let { "Detected: $it" }, state.note).joinToString(" \u2022 ")
                        if (header.isNotEmpty()) NoticeBanner(header)
                        HexViewer(
                            hexRows = state.hexRows,
                            asciiRows = state.asciiRows,
                            fileEntity = state.file,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    }
                }
                is FileContentState.SvgSuccess -> SvgViewer(state.file, Modifier.fillMaxSize())
                is FileContentState.SqliteSuccess -> SqliteViewer(state.file, Modifier.fillMaxSize())
                is FileContentState.FontSuccess -> FontViewer(state.file, Modifier.fillMaxSize())
                is FileContentState.Model3dSuccess -> Model3dViewer(state.mesh, state.label, Modifier.fillMaxSize())
                is FileContentState.HtmlSuccess -> HtmlStringViewer(state.html, Modifier.fillMaxSize())
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No file selected", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    // Glassmorphism Modal Bottom Sheet for File Properties
    if (showPropertiesDialog && activeFile != null) {
        com.example.feature.fileinfo.FilePropertiesBottomSheet(
            fileEntity = activeFile,
            onDismissRequest = { showPropertiesDialog = false }
        )
    }

    if (showJumpToLineDialog) {
        var lineInput by remember { mutableStateOf("") }
        val textLinesCount = remember(fileState) {
            val content = (fileState as? FileContentState.TextSuccess)?.content
            content?.count { it == '\n' }?.plus(1) ?: 1000
        }
        AlertDialog(
            onDismissRequest = { showJumpToLineDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val lNum = lineInput.toIntOrNull()
                        if (lNum != null && lNum > 0) {
                            // MD files: jump by heading when the target line is a heading,
                            // otherwise fall back to approximate line scrolling.
                            val extLower = activeFile?.extension?.lowercase().orEmpty()
                            val outlineContent = (fileState as? FileContentState.TextSuccess)?.let { fullPreviewText ?: it.content }.orEmpty()
                            val headingIndex = if (extLower in setOf("md", "markdown")) {
                                DocumentOutlineParser.parseHeadings(outlineContent)
                                    .indexOfFirst { it.lineNumber == lNum }
                                    .takeIf { it >= 0 }
                            } else null
                            pendingScrollRequest = ViewerScrollRequest(lineNumber = lNum, headingIndex = headingIndex)
                        } else {
                            android.widget.Toast.makeText(context, "Invalid line number", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        showJumpToLineDialog = false
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpToLineDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Go to Line") },
            text = {
                Column {
                    Text("Enter line number (1 to $textLinesCount):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lineInput,
                        onValueChange = { lineInput = it.filter { char -> char.isDigit() } },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    if (showJumpDialog) {
        var pageInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val pNum = pageInput.toIntOrNull()
                        if (pNum != null && pNum in 1..totalPdfPages) {
                            scrollToPage = pNum - 1
                            pageJumpToken++
                        } else {
                            android.widget.Toast.makeText(context, "Invalid page number", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        showJumpDialog = false
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Go to Page") },
            text = {
                Column {
                    Text("Enter page number (1 to $totalPdfPages):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it.filter { char -> char.isDigit() } },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    if (showDocxZoomDialog) {
        AlertDialog(
            onDismissRequest = { showDocxZoomDialog = false },
            title = { Text("Document zoom") },
            text = {
                Column {
                    Text("${(docxZoomFactor * 100).toInt()}%", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = docxZoomInput,
                        onValueChange = { value -> docxZoomInput = value.filter { it.isDigit() }.take(3) },
                        singleLine = true,
                        label = { Text("Exact zoom (%)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Slider(
                        value = docxZoomFactor,
                        onValueChange = {
                            docxZoomFactor = com.example.formats.docx.DocxZoomPolicy.coerce(it)
                            docxZoomInput = (it * 100).toInt().toString()
                        },
                        // 0.05 stops so the 40% default is a reachable snap point.
                        valueRange = 0.25f..5.0f,
                        steps = 94
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("25%", style = MaterialTheme.typography.labelSmall)
                        Text("500%", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    docxZoomInput.toIntOrNull()?.let { percent ->
                        docxZoomFactor = percent.coerceIn(25, 500) / 100f
                    }
                    showDocxZoomDialog = false
                }) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = {
                    docxZoomFactor = 1.0f
                    docxZoomInput = "100"
                }) { Text("Reset 100%") }
            }
        )
    }

    if (pendingLinkToOpen != null) {
        AlertDialog(
            onDismissRequest = { pendingLinkToOpen = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingLinkToOpen?.let { url ->
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Failed to open link", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        pendingLinkToOpen = null
                    }
                ) {
                    Text("Open")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLinkToOpen = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Open Link") },
            text = { Text("Do you want to open this link in your browser?\n\n$pendingLinkToOpen") }
        )
    }

    // Document Outline bottom sheet (MD/TXT): lists every heading and jumps to it.
    if (showOutlineBottomSheet && activeFile != null && !isEditMode) {
        val outlineContent = (fileState as? FileContentState.TextSuccess)
            ?.let { fullPreviewText ?: it.content }.orEmpty()
        val isMarkdownFile = activeFile.extension.lowercase() in setOf("md", "markdown")
        DocumentOutlineBottomSheet(
            documentContent = outlineContent,
            documentTitle = activeFile.name,
            onDismissRequest = { showOutlineBottomSheet = false },
            onSelectHeading = { lineNumber ->
                val headingIndex = if (isMarkdownFile) {
                    DocumentOutlineParser.parseHeadings(outlineContent)
                        .indexOfFirst { it.lineNumber == lineNumber }
                        .takeIf { it >= 0 }
                } else null
                pendingScrollRequest = ViewerScrollRequest(lineNumber = lineNumber, headingIndex = headingIndex)
            }
        )
    }

    // Viewer Appearance bottom sheet (text files): theme, font size and browser-style zoom.
    if (showAppearanceBottomSheet) {
        AppearanceBottomSheet(
            currentTheme = viewerSyntaxTheme,
            currentFontSizeSp = viewerFontSizeSp,
            onThemeChanged = { viewerSyntaxTheme = it },
            onFontSizeChanged = { viewerFontSizeSp = it },
            currentZoomPercent = viewerZoomPercent,
            onZoomChanged = { viewerZoomPercent = it },
            onDismissRequest = { showAppearanceBottomSheet = false }
        )
    }

    // ---- Inline edit mode dialogs ------------------------------------------------

    // X (top bar) confirmation: discard unsaved changes?
    if (showDiscardEditsDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardEditsDialog = false },
            title = {
                Text(
                    "Discard changes?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = { Text("Your edits to this file have not been saved yet. Discard them?") },
            dismissButton = {
                TextButton(onClick = { showDiscardEditsDialog = false }) {
                    Text("Keep editing", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardEditsDialog = false
                        viewModel.exitEditMode()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Save confirmation: writes the draft through the charset-safe save pipeline.
    if (showSaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmDialog = false },
            title = {
                Text(
                    "Save changes?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = { Text("Save your edits to \"${activeFile?.name ?: "this file"}\"? A .bak backup of the original is kept automatically.") },
            dismissButton = {
                TextButton(onClick = { showSaveConfirmDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveConfirmDialog = false
                        val entity = activeFile
                        if (entity != null) {
                            viewModel.saveTextFile(entity, editTextValue.text)
                            editOriginalContent = editTextValue.text
                            viewModel.exitEditMode()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Undo history viewer for the inline editor.
    if (showEditHistoryDialog) {
        EditHistoryDialog(
            entries = editUndoStack.snapshot(),
            onDismissRequest = { showEditHistoryDialog = false },
            onRestore = { restored ->
                editUndoStack.push(editTextValue)
                editRedoStack.clear()
                editTextValue = restored
            }
        )
    }

    // Export the modified draft as a fresh copy without touching the original file.
    if (showEditSaveAsDialog && activeFile != null) {
        val defaultCopyName = remember(activeFile.name) {
            val dotIdx = activeFile.name.lastIndexOf('.')
            if (dotIdx > 0) {
                activeFile.name.substring(0, dotIdx) + "_copy." + activeFile.extension
            } else {
                activeFile.name + "_copy"
            }
        }
        var copyNameInput by remember { mutableStateOf(defaultCopyName) }
        AlertDialog(
            onDismissRequest = { showEditSaveAsDialog = false },
            title = {
                Text(
                    text = "Export Modified Copy",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Atomically save your edits into a new copy without overwriting the original file:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = copyNameInput,
                        onValueChange = { copyNameInput = it },
                        singleLine = true,
                        label = { Text("File Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditSaveAsDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (copyNameInput.isNotBlank()) {
                            viewModel.exportCopy(activeFile, copyNameInput.trim(), editTextValue.text)
                            showEditSaveAsDialog = false
                        }
                    }
                ) {
                    Text("Export Copy")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

/** Unwrap themed/Compose context wrappers to reach the host Activity for orientation requests. */
private fun Context.findHostActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
