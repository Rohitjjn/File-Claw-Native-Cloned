package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.FormatLineSpacing
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.automirrored.outlined.Toc
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.component.DocumentOutlineBottomSheet
import com.example.ui.component.AppearanceBottomSheet
import com.example.ui.component.FilePropertiesBottomSheet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.io.TextLoader
import com.example.core.io.TextWindowReader
import com.example.core.text.TextFileSearchEngine
import com.example.core.text.TextFileSearchMatch
import com.example.data.RecentFileEntity
import com.example.services.FileManager
import com.example.ui.component.ClaudeAppBar
import com.example.ui.component.ClaudeCard
import com.example.ui.component.FileIcon
import com.example.ui.component.formatFileSize
import com.example.ui.component.formatElapsedTime
import com.example.viewmodel.MainViewModel
import java.io.File
import coil.compose.AsyncImage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.example.ui.component.DocumentOutlineParser
import com.example.ui.component.ViewerSyntaxTheme
import com.example.ui.component.ViewerAppearancePrefs
import com.example.core.format.DocxZoomPolicy
import com.example.core.format.PdfRenderBudgetPolicy
import com.example.core.format.PinchGestureGate
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import android.media.MediaPlayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Slider
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode

/** Where the in-document viewer should scroll next (outline tap / go-to-line). */
data class ViewerScrollRequest(
    val lineNumber: Int,
    val headingIndex: Int? = null
)

private const val MAX_TEXT_PRINT_CHARS = 2_000_000

private suspend fun readBoundedText(file: File, charsetName: String): Result<String> =
    withContext(Dispatchers.IO) {
        runCatching {
            val loaded = TextLoader.load(file, charsetName, maxChars = MAX_TEXT_PRINT_CHARS)
            check(!loaded.truncated) { "This file exceeds the bounded $MAX_TEXT_PRINT_CHARS-character limit for this operation." }
            loaded.text
        }
    }

private fun copyTextToClipboard(context: android.content.Context, title: String, text: String) {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText(title, text))
    android.widget.Toast.makeText(context, "Text copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
}

private fun showTextOperationError(context: android.content.Context, error: Throwable) {
    android.widget.Toast.makeText(
        context,
        error.message ?: "Could not prepare the complete file",
        android.widget.Toast.LENGTH_LONG
    ).show()
}

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
    var largeTextSearchMatches by remember(activeFile?.path) { mutableStateOf<List<TextFileSearchMatch>>(emptyList()) }
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
            val seedContent = (fileState as? MainViewModel.FileContentState.TextSuccess)?.content.orEmpty()
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
        if (fileState is MainViewModel.FileContentState.Idle && !restoreAttempted) {
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
        (fileState as? MainViewModel.FileContentState.TextSuccess)?.truncated == true

    LaunchedEffect(activeFile?.path, fileState) {
        val textState = fileState as? MainViewModel.FileContentState.TextSuccess
        val extension = activeFile?.extension?.lowercase()
        fullPreviewText = if (textState != null && extension in setOf("txt", "md")) textState.content else null
    }

    LaunchedEffect(activeFile?.path, inViewerSearchQuery, isLargeTextSearchable, fileState) {
        val textState = fileState as? MainViewModel.FileContentState.TextSuccess
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
            val charset = java.nio.charset.Charset.forName(textState.charsetName)
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                TextFileSearchEngine.search(
                    java.io.File(textState.file.path),
                    query,
                    charset = charset,
                    shouldContinue = { searchContext.isActive }
                )
            }
            largeTextSearchMatches = result.matches
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

        val textState = fileState as? MainViewModel.FileContentState.TextSuccess ?: return@LaunchedEffect
        val match = largeTextSearchMatches.getOrNull(currentMatchIndex) ?: return@LaunchedEffect
        try {
            val charset = java.nio.charset.Charset.forName(textState.charsetName)
            val window = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                TextWindowReader.read(
                    java.io.File(textState.file.path),
                    match.byteOffset,
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
        if (fileState is MainViewModel.FileContentState.PdfSuccess) {
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
        if (fileState is MainViewModel.FileContentState.PdfSuccess) {
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
                            if (fileState is MainViewModel.FileContentState.PdfSuccess) {
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
                        isPdfSearch = fileState is MainViewModel.FileContentState.PdfSuccess,
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
                                fileState is MainViewModel.FileContentState.PdfSuccess -> pdfSearchSnapshot.matches.size
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
                                                shareFile(context, File(fileEntity.path), fileEntity.extension)
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
                                                    openPdfInBrowser(context, java.io.File(fileEntity.path))
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
                                                    com.example.utils.FilePrintHelper.printPdf(context, java.io.File(entity.path), entity.name)
                                                }
                                                ext in setOf("docx", "doc") -> {
                                                    val renderedWebView = docxPreviewWebView
                                                    if (docxPreviewReady && renderedWebView != null) {
                                                        com.example.utils.FilePrintHelper.printWebView(context, renderedWebView, entity.name)
                                                    } else {
                                                        com.example.utils.FilePrintHelper.printDocx(context, java.io.File(entity.path), entity.name)
                                                    }
                                                }
                                                ext == "md" || ext == "txt" -> {
                                                    val textState = fileState as? MainViewModel.FileContentState.TextSuccess
                                                    printScope.launch {
                                                        val result = if (textState != null && !textState.truncated) {
                                                            Result.success(textState.content)
                                                        } else {
                                                            readBoundedText(
                                                                java.io.File(entity.path),
                                                                textState?.charsetName ?: "UTF-8"
                                                            )
                                                        }
                                                        result.onSuccess { fullContent ->
                                                            if (ext == "md") {
                                                                com.example.utils.FilePrintHelper.printMarkdown(context, fullContent, entity.name)
                                                            } else {
                                                                com.example.utils.FilePrintHelper.printTextOrMarkdown(context, fullContent, entity.name)
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
                                                    com.example.utils.FilePrintHelper.printImage(context, java.io.File(entity.path), entity.name)
                                                }
                                                else -> {
                                                    // The four document formats have bounded/dedicated branches above; preserve legacy fallback for other types.
                                                    val content = (fileState as? MainViewModel.FileContentState.TextSuccess)?.content
                                                        ?: runCatching { java.io.File(entity.path).readText() }.getOrNull() ?: ""
                                                    com.example.utils.FilePrintHelper.printTextOrMarkdown(context, content, entity.name)
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
            val isPdf = fileState is MainViewModel.FileContentState.PdfSuccess
            // Gate on the real state: legacy .doc files open in the hex viewer and must not
            // show DOCX zoom controls.
            val isDocx = fileState is MainViewModel.FileContentState.DocxSuccess
            val isImage = fileState is MainViewModel.FileContentState.ImageSuccess
            val isText = fileState is MainViewModel.FileContentState.TextSuccess ||
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
                                                    com.example.core.format.ScreenOrientationPolicy.toggleOrientation(
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
                                                com.example.utils.FilePrintHelper.printPdf(context, java.io.File(it.path), it.name)
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
                                        onClick = { activeFile?.let { shareFile(context, File(it.path), it.extension) } },
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
                                                    com.example.utils.FilePrintHelper.printWebView(context, renderedWebView, it.name)
                                                } else {
                                                    com.example.utils.FilePrintHelper.printDocx(context, java.io.File(it.path), it.name)
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
                                        onClick = { activeFile?.let { shareFile(context, File(it.path), it.extension) } },
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
                                                com.example.utils.FilePrintHelper.printImage(context, java.io.File(it.path), it.name)
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
                                        onClick = { activeFile?.let { shareFile(context, File(it.path), it.extension) } },
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
                                            val textState = fileState as? MainViewModel.FileContentState.TextSuccess
                                            if (entity != null) {
                                                printScope.launch {
                                                    val result = if (textState != null && !textState.truncated) {
                                                        Result.success(textState.content)
                                                    } else {
                                                        readBoundedText(
                                                            java.io.File(entity.path),
                                                            textState?.charsetName ?: "UTF-8"
                                                        )
                                                    }
                                                    result.onSuccess { textToPrint ->
                                                        if (ext == "md") {
                                                            com.example.utils.FilePrintHelper.printMarkdown(context, textToPrint, entity.name)
                                                        } else {
                                                            com.example.utils.FilePrintHelper.printTextOrMarkdown(context, textToPrint, entity.name)
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
                                            val textState = fileState as? MainViewModel.FileContentState.TextSuccess
                                            val entity = activeFile
                                            if (textState != null && !textState.truncated) {
                                                copyTextToClipboard(context, entity?.name ?: "File Content", textState.content)
                                            } else if (textState != null && entity != null) {
                                                printScope.launch {
                                                    readBoundedText(java.io.File(entity.path), textState.charsetName)
                                                        .onSuccess { copyTextToClipboard(context, entity.name, it) }
                                                        .onFailure { showTextOperationError(context, it) }
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

                                    val isEditable = (fileState as? MainViewModel.FileContentState.TextSuccess)?.editable == true
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
                                        onClick = { activeFile?.let { shareFile(context, File(it.path), it.extension) } },
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
                is MainViewModel.FileContentState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        com.example.ui.component.PremiumLoadingIndicator(text = "Loading file...")
                    }
                }
                is MainViewModel.FileContentState.Error -> {
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
                is MainViewModel.FileContentState.ArchivePasswordRequired -> {
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
                is MainViewModel.FileContentState.TextSuccess -> {
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
                is MainViewModel.FileContentState.CsvSuccess -> {
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
                is MainViewModel.FileContentState.ZipSuccess -> {
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
                is MainViewModel.FileContentState.ImageSuccess -> {
                    ImagePreview(
                        fileEntity = state.file,
                        modifier = Modifier.fillMaxSize(),
                        onTap = { isBarsVisible = !isBarsVisible }
                    )
                }
                is MainViewModel.FileContentState.PdfSuccess -> {
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
                is MainViewModel.FileContentState.DocxSuccess -> {
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
                            docxZoomFactor = com.example.core.format.DocxZoomPolicy.applyPinch(docxZoomFactor, delta)
                        },
                        onPinchSettled = {
                            docxPinchActive = false
                        },
                        onDoubleTap = {
                            docxZoomFactor = com.example.core.format.DocxZoomPolicy.toggleDoubleTap(docxZoomFactor)
                        },
                        onRendererFallback = { isFallback -> isDocxRenderFallback = isFallback }
                    )
                }
                is MainViewModel.FileContentState.PptxSuccess -> {
                    PptxPreviewWebView(
                        base64Pptx = state.base64Data,
                        fileEntity = state.file,
                        isNightMode = isDocxNightMode,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is MainViewModel.FileContentState.MediaSuccess -> {
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
                is MainViewModel.FileContentState.BinarySuccess -> {
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
                is MainViewModel.FileContentState.SvgSuccess -> SvgViewer(state.file, Modifier.fillMaxSize())
                is MainViewModel.FileContentState.SqliteSuccess -> SqliteViewer(state.file, Modifier.fillMaxSize())
                is MainViewModel.FileContentState.FontSuccess -> FontViewer(state.file, Modifier.fillMaxSize())
                is MainViewModel.FileContentState.Model3dSuccess -> Model3dViewer(state.mesh, state.label, Modifier.fillMaxSize())
                is MainViewModel.FileContentState.HtmlSuccess -> HtmlStringViewer(state.html, Modifier.fillMaxSize())
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
        com.example.ui.component.FilePropertiesBottomSheet(
            fileEntity = activeFile,
            onDismissRequest = { showPropertiesDialog = false }
        )
    }

    if (showJumpToLineDialog) {
        var lineInput by remember { mutableStateOf("") }
        val textLinesCount = remember(fileState) {
            val content = (fileState as? MainViewModel.FileContentState.TextSuccess)?.content
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
                            val outlineContent = (fileState as? MainViewModel.FileContentState.TextSuccess)?.let { fullPreviewText ?: it.content }.orEmpty()
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
                            docxZoomFactor = com.example.core.format.DocxZoomPolicy.coerce(it)
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
        val outlineContent = (fileState as? MainViewModel.FileContentState.TextSuccess)
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

@Composable
fun PropertyItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}


// Helper: Fast and robust code syntax highlighter
@Composable
fun MarkdownPreview(
    markdownText: String,
    fileEntity: RecentFileEntity,
    fontSizeSetting: String,
    isNightMode: Boolean,
    searchQuery: String = "",
    searchTriggerNext: Int = 0,
    searchTriggerPrev: Int = 0,
    onSearchResultCount: (Int, Int, Boolean) -> Unit = { _, _, _ -> },
    onSingleTap: () -> Unit = {},
    syntaxTheme: ViewerSyntaxTheme = ViewerSyntaxTheme.AUTO,
    viewerFontSizeSp: Int = 16,
    zoomPercent: Int = 100,
    scrollRequest: ViewerScrollRequest? = null,
    onScrollRequestHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var generatedHtml by remember(markdownText, isNightMode, viewerFontSizeSp, syntaxTheme) { mutableStateOf<String?>(null) }

    LaunchedEffect(markdownText, isNightMode, viewerFontSizeSp, syntaxTheme) {
        val htmlContent = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val parser = org.intellij.markdown.parser.MarkdownParser(org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor())
            val parsedTree = parser.buildMarkdownTreeFromString(markdownText)
            val rawHtml = com.example.core.text.HtmlSanitizer.sanitize(
                org.intellij.markdown.html.HtmlGenerator(markdownText, parsedTree, org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor()).generateHtml(),
                keepImages = true
            )

            // Viewer Appearance drives the base font size; falls back to the settings mapping.
            val fontSize = "${viewerFontSizeSp.coerceIn(11, 22)}px"

            // Claude Theme Colors (EXACT from File Claw - MD Preview Theme Fix Prompt)
            // A chosen syntax theme overrides the base canvas/ink/accent tokens; AUTO keeps
            // the light/dark palette derived from the app theme.
            val useThemeOverride = !syntaxTheme.isAuto
            val bgColor = if (useThemeOverride) syntaxTheme.hexBg else if (isNightMode) "#151514" else "#faf9f5" // canvas
            val textPrimary = if (useThemeOverride) syntaxTheme.hexText else if (isNightMode) "#f5f5f3" else "#141413" // ink
            val textBody = if (useThemeOverride) syntaxTheme.hexText else if (isNightMode) "#d1cfc7" else "#3d3d3a" // body
            val textBodyStrong = if (useThemeOverride) syntaxTheme.hexText else if (isNightMode) "#faf9f5" else "#252523" // body_strong
            val textSecondary = if (useThemeOverride) syntaxTheme.hexText else if (isNightMode) "#a39f93" else "#6c6a64" // muted
            val textSecondarySoft = if (useThemeOverride) syntaxTheme.hexText else if (isNightMode) "#8e8779" else "#8e8b82" // muted_soft
            val accent = if (useThemeOverride) syntaxTheme.hexAccent else if (isNightMode) "#e08567" else "#cc785c" // primary (coral)
            val accentActive = if (useThemeOverride) syntaxTheme.hexAccent else if (isNightMode) "#f09a7d" else "#a9583e" // primary_active
            val border = if (isNightMode) "#2d2b28" else "#e6dfd8" // hairline
            val borderSoft = if (isNightMode) "#211f1d" else "#ebe6df" // hairline_soft
            val cardBg = if (isNightMode) "#2a2723" else "#efe9de" // surface_card
            val quoteBg = if (isNightMode) "#1c1a18" else "#f5f0e8" // surface_soft
            val inlineCodeBg = if (isNightMode) "#2d2823" else "#e8e0d2" // surface_cream_strong
            val codeBlockOuterBg = if (isNightMode) "#0d0d0c" else "#181715" // surface_dark
            val codeBlockInnerBg = if (isNightMode) "#161513" else "#1f1e1b" // surface_dark_soft
            val codeBlockHeaderBg = if (isNightMode) "#1d1b19" else "#252320" // surface_dark_elevated
            val codeBlockText = if (isNightMode) "#f5f5f3" else "#faf9f5" // on_dark
            val codeBlockButtons = if (isNightMode) "#a09d96" else "#8e8b82" // on_dark_soft

            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=0.25, maximum-scale=5.0, user-scalable=yes">
                <style>
                    :root {
                        --bg-color: $bgColor;
                        --text-primary: $textPrimary;
                        --text-body: $textBody;
                        --text-body-strong: $textBodyStrong;
                        --text-secondary: $textSecondary;
                        --text-secondary-soft: $textSecondarySoft;
                        --accent: $accent;
                        --accent-active: $accentActive;
                        --border: $border;
                        --border-soft: $borderSoft;
                        --card-bg: $cardBg;
                        --quote-bg: $quoteBg;
                        --inline-code-bg: $inlineCodeBg;
                        --code-block-outer: $codeBlockOuterBg;
                        --code-block-inner: $codeBlockInnerBg;
                        --code-block-header: $codeBlockHeaderBg;
                        --code-text: $codeBlockText;
                        --code-buttons: $codeBlockButtons;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        background-color: var(--bg-color);
                        color: var(--text-body);
                        font-size: $fontSize;
                        line-height: 1.55;
                        padding: 16px;
                        margin: 0;
                        word-wrap: break-word;
                    }
                    h1, h2, h3, h4, h5, h6 {
                        color: var(--text-primary);
                        margin-top: 24px;
                        margin-bottom: 16px;
                        font-weight: 700;
                    }
                    h1 {
                        font-size: 28px;
                        border-bottom: 2px solid var(--border);
                        padding-bottom: 8px;
                    }
                    h2 { font-size: 22px; }
                    h3 { font-size: 18px; font-weight: 600; }
                    p {
                        margin-top: 0;
                        margin-bottom: 16px;
                    }
                    li {
                        margin: 8px 0;
                    }
                    ul, ol {
                        padding-left: 24px;
                    }
                    a {
                        color: var(--accent);
                        text-decoration: none;
                    }
                    a:hover {
                        color: var(--accent-active);
                        text-decoration: underline;
                    }
                    code {
                        font-family: "JetBrains Mono", ui-monospace, Consolas, Monaco, monospace;
                        background-color: var(--inline-code-bg);
                        color: var(--accent);
                        padding: 2px 6px;
                        border-radius: 4px;
                        font-size: 14px;
                    }
                    pre {
                        background-color: transparent;
                        margin: 0;
                        padding: 0;
                        overflow: visible;
                    }
                    .code-block-container {
                        background-color: var(--code-block-outer);
                        border-radius: 12px;
                        margin-bottom: 16px;
                        overflow: hidden;
                        border: 1px solid var(--border);
                    }
                    .code-header {
                        background-color: var(--code-block-header);
                        padding: 8px 16px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                    }
                    .code-lang {
                        color: var(--text-secondary-soft);
                        font-size: 12px;
                        font-family: monospace;
                        text-transform: uppercase;
                    }
                    .copy-btn {
                        background: transparent;
                        border: 1px solid var(--code-buttons);
                        color: var(--text-secondary-soft);
                        padding: 4px 12px;
                        border-radius: 6px;
                        font-size: 12px;
                        cursor: pointer;
                        transition: background 0.2s;
                    }
                    .copy-btn:hover {
                        background: rgba(255, 255, 255, 0.1);
                    }
                    .code-content {
                        background-color: var(--code-block-inner);
                        padding: 16px;
                        overflow-x: auto;
                    }
                    .code-content code {
                        background-color: transparent;
                        color: var(--code-text);
                        padding: 0;
                        font-size: 14px;
                        line-height: 1.6;
                    }
                    blockquote {
                        margin: 0 0 16px 0;
                        padding: 12px 20px;
                        color: var(--text-body);
                        border-left: 4px solid var(--accent);
                        background-color: var(--quote-bg);
                        border-radius: 0 8px 8px 0;
                    }
                    table {
                        border-collapse: collapse;
                        width: 100%;
                        margin-bottom: 16px;
                        display: block;
                        overflow-x: auto;
                        border: 1px solid var(--border);
                    }
                    th, td {
                        border: 1px solid var(--border);
                        padding: 12px;
                    }
                    th {
                        background-color: var(--quote-bg);
                        font-weight: 600;
                        color: var(--text-body-strong);
                    }
                    tr:nth-child(even) {
                        background-color: var(--bg-color);
                    }
                    tr:nth-child(odd) {
                        background-color: var(--bg-color);
                    }
                    hr {
                        border: 0;
                        border-top: 2px solid var(--border);
                        margin: 24px 0;
                    }
                    img {
                        max-width: 100%;
                        border-radius: 8px;
                    }
                    .search-highlight { background: rgba(204, 120, 92, 0.32); color: inherit; border-radius: 2px; }
                    .search-highlight-active { background: #cc785c; color: #ffffff; outline: 2px solid rgba(232, 165, 90, 0.9); }
                    .line-anchor-flash { background: rgba(204, 120, 92, 0.32); color: inherit; border-radius: 2px; outline: 2px solid #cc785c; transition: background 0.4s ease; }
                    @media print { .code-header, .copy-btn { display: none !important; } .code-block-container { break-inside: avoid; } }
                </style>
                $DocumentPreviewScript
            </head>
            <body>
                <main id="document-content">$rawHtml</main>
            </body>
            </html>
            """.trimIndent()
        }
        generatedHtml = htmlContent
    }

    SearchableDocumentWebView(
        html = generatedHtml,
        backgroundColor = if (syntaxTheme.isAuto) {
            if (isNightMode) 0xFF151514.toInt() else 0xFFfaf9f5.toInt()
        } else {
            runCatching {
                android.graphics.Color.parseColor(syntaxTheme.hexBg)
            }.getOrDefault(if (isNightMode) 0xFF151514.toInt() else 0xFFfaf9f5.toInt())
        },
        searchQuery = searchQuery,
        searchTriggerNext = searchTriggerNext,
        searchTriggerPrev = searchTriggerPrev,
        onSearchResultCount = onSearchResultCount,
        onSingleTap = onSingleTap,
        zoomPercent = zoomPercent,
        scrollRequest = scrollRequest,
        onScrollRequestHandled = onScrollRequestHandled,
        loadingMessage = "Preparing Document...",
        modifier = modifier
    )
}

@Composable
private fun PlainTextPreview(
    content: String,
    isDarkMode: Boolean,
    fontSizeSetting: String,
    searchQuery: String,
    searchTriggerNext: Int,
    searchTriggerPrev: Int,
    onSearchResultCount: (Int, Int, Boolean) -> Unit,
    onSingleTap: () -> Unit,
    syntaxTheme: ViewerSyntaxTheme = ViewerSyntaxTheme.AUTO,
    viewerFontSizeSp: Int = 16,
    zoomPercent: Int = 100,
    scrollRequest: ViewerScrollRequest? = null,
    onScrollRequestHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var generatedHtml by remember(content, isDarkMode, viewerFontSizeSp, syntaxTheme) { mutableStateOf<String?>(null) }
    LaunchedEffect(content, isDarkMode, viewerFontSizeSp, syntaxTheme) {
        // Viewer Appearance drives the base font size; falls back to the settings mapping.
        val fontSizePx = viewerFontSizeSp.coerceIn(11, 22)
        generatedHtml = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val themeBg = if (syntaxTheme.isAuto) null else syntaxTheme.hexBg
            val themeFg = if (syntaxTheme.isAuto) null else syntaxTheme.hexText
            buildPlainTextPreviewHtml(content, isDarkMode, fontSizePx, themeBg, themeFg)
        }
    }
    SearchableDocumentWebView(
        html = generatedHtml,
        backgroundColor = if (syntaxTheme.isAuto) {
            if (isDarkMode) 0xFF151514.toInt() else 0xFFfaf9f5.toInt()
        } else {
            runCatching {
                android.graphics.Color.parseColor(syntaxTheme.hexBg)
            }.getOrDefault(if (isDarkMode) 0xFF151514.toInt() else 0xFFfaf9f5.toInt())
        },
        searchQuery = searchQuery,
        searchTriggerNext = searchTriggerNext,
        searchTriggerPrev = searchTriggerPrev,
        onSearchResultCount = onSearchResultCount,
        onSingleTap = onSingleTap,
        zoomPercent = zoomPercent,
        scrollRequest = scrollRequest,
        onScrollRequestHandled = onScrollRequestHandled,
        loadingMessage = "Preparing Text...",
        modifier = modifier
    )
}

@Composable
private fun SearchableDocumentWebView(
    html: String?,
    backgroundColor: Int,
    searchQuery: String,
    searchTriggerNext: Int,
    searchTriggerPrev: Int,
    onSearchResultCount: (Int, Int, Boolean) -> Unit,
    onSingleTap: () -> Unit,
    loadingMessage: String,
    zoomPercent: Int = 100,
    scrollRequest: ViewerScrollRequest? = null,
    onScrollRequestHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val latestSearchResultCallback = androidx.compose.runtime.rememberUpdatedState(onSearchResultCount)
    val latestSingleTapCallback = androidx.compose.runtime.rememberUpdatedState(onSingleTap)
    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }
    var isPageReady by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    // Ledger of what the generated document's search engine has already been told.
    // Loop 12 (Bug B): this used to be two ints plus an implicit "always search first"
    // rule, so every arrow press re-ran searchWord(), reset the renderer's index to 0
    // and advanced one step — the counter could never walk past 2 of N matches.
    var issuedSearch by remember { mutableStateOf(InViewerSearchCommandPolicy.Issued()) }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { viewContext ->
            android.webkit.WebView(viewContext).apply {
                webViewRef = this
                setBackgroundColor(backgroundColor)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = false
                    allowFileAccess = false
                    allowContentAccess = false
                    builtInZoomControls = true
                    displayZoomControls = false
                    setSupportZoom(true)
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                addJavascriptInterface(
                    WebAppInterface(
                        viewContext,
                        onSearchResultsChanged = { count, index, truncated -> latestSearchResultCallback.value(count, index, truncated) },
                        onTapRequested = { latestSingleTapCallback.value() }
                    ),
                    "Android"
                )
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.post {
                            isPageReady = true
                            isLoading = false
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: android.webkit.WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString().orEmpty()
                        return !(url.startsWith("about:") || url.startsWith("data:"))
                    }
                }
            }
        },
        update = { webView ->
            if (html != null && webView.tag != html) {
                webView.tag = html
                isPageReady = false
                isLoading = true
                webView.setBackgroundColor(backgroundColor)
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        },
        // Loop 12 (Bug E): regenerated HTML used to leak the previous WebView (and its
        // renderer process) until finalizer GC. Destroy it deterministically when the
        // node leaves composition (file switch / screen exit).
        onRelease = { webView ->
            webView.tag = null
            webView.destroy()
        },
        modifier = Modifier.fillMaxSize()
    )

    // Loop 12 (Bug B): the query step and the navigation step are now planned by
    // InViewerSearchCommandPolicy. Arrow presses issue ONLY next/prev deltas, so the
    // renderer walks the full navigable match list (0/1/2/7/100+ all reachable in both
    // directions with wrap-around). A changed query still searches exactly once, first.
    LaunchedEffect(webViewRef, isPageReady, searchQuery, searchTriggerNext, searchTriggerPrev) {
        val webView = webViewRef
        if (webView != null && isPageReady) {
            val plan = InViewerSearchCommandPolicy.plan(issuedSearch, searchQuery, searchTriggerNext, searchTriggerPrev)
            if (!plan.isEmpty) {
                if (plan.search && issuedSearch.query != null) {
                    // Debounce: typing re-highlights the whole DOM once the user pauses,
                    // instead of once per keystroke (big documents used to stutter).
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
            // Always advance the ledger — including the no-op case, so a trigger counter
            // reset (file change) is absorbed instead of replayed as steps later.
            issuedSearch = plan.issued
        }
    }

    // Browser-style zoom (25% - 500%) applied live, mirroring the DOCX setZoom pipeline.
    LaunchedEffect(webViewRef, isPageReady, zoomPercent) {
        val webView = webViewRef
        if (webView != null && isPageReady) {
            webView.evaluateJavascript("setViewerZoom($zoomPercent);", null)
        }
    }

    // Outline / go-to-line jumps. Prefer exact heading anchoring when available.
    LaunchedEffect(webViewRef, isPageReady, scrollRequest) {
        val webView = webViewRef
        if (webView != null && isPageReady && scrollRequest != null) {
            val script = if (scrollRequest.headingIndex != null) {
                "var __ok = scrollToHeadingIndex(${scrollRequest.headingIndex}); if (!__ok) { scrollToLine(${scrollRequest.lineNumber}); }"
            } else {
                "scrollToLine(${scrollRequest.lineNumber});"
            }
            webView.evaluateJavascript(script, null)
            onScrollRequestHandled()
        }
    }

    if (html == null || isLoading) {
        Box(
            modifier = modifier.fillMaxSize().background(Color(backgroundColor)),
            contentAlignment = Alignment.Center
        ) {
            com.example.ui.component.PremiumLoadingIndicator(text = loadingMessage)
        }
    }
}

class WebAppInterface(
    private val context: android.content.Context,
    private val onSearchResultsChanged: (Int, Int, Boolean) -> Unit = { _, _, _ -> },
    private val onTapRequested: () -> Unit = {}
) {
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    @android.webkit.JavascriptInterface
    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Code", text))
        mainHandler.post {
            android.widget.Toast.makeText(context, "Code block copied", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    @android.webkit.JavascriptInterface
    fun onSearchResultCount(count: Int, currentIndex: Int, truncated: Boolean) {
        mainHandler.post { onSearchResultsChanged(count, currentIndex, truncated) }
    }

    @android.webkit.JavascriptInterface
    fun onDocumentTap() {
        mainHandler.post { onTapRequested() }
    }
}

@Composable
fun CodePreview(
    codeText: String,
    fileEntity: RecentFileEntity,
    fontSizeSetting: String,
    wrapSetting: Boolean,
    showLineNumbers: Boolean,
    modifier: Modifier = Modifier,
    scrollToLine: Int? = null,
    onScrollToLineHandled: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var isAllCopied by remember { mutableStateOf(false) }
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    LaunchedEffect(isAllCopied) {
        if (isAllCopied) {
            kotlinx.coroutines.delay(1800)
            isAllCopied = false
        }
    }

    Column(modifier = modifier) {
        // Tag Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    text = fileEntity.extension.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = "${formatFileSize(fileEntity.size)} • Modified ${formatElapsedTime(fileEntity.lastOpened)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(codeText))
                    isAllCopied = true
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isAllCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = "Copy Entire Code",
                        modifier = Modifier.size(14.dp),
                        tint = if (isAllCopied) Color(0xFF427A5B) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAllCopied) "Copied!" else "Copy All",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAllCopied) Color(0xFF427A5B) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "WebView Render",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val htmlContent = remember(codeText, isDark, fontSizeSetting, wrapSetting, showLineNumbers) {
                val escapedCode = codeText.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                val bgColor = if (isDark) "#1e1e1e" else "#ffffff"
                val textColor = if (isDark) "#d4d4d4" else "#000000"
                val fSize = when (fontSizeSetting) {
                    "Small" -> "12px"
                    "Large" -> "18px"
                    else -> "14px"
                }
                val wWrap = if (wrapSetting) "pre-wrap" else "pre"
                val wWord = if (wrapSetting) "break-word" else "normal"
                val theme = if (isDark) "vs2015.min.css" else "vs.min.css"
                
                val lineNumbersCss = if (showLineNumbers) """
                    .hljs {
                        counter-reset: linenumber;
                    }
                    .hljs-line {
                        display: block;
                        counter-increment: linenumber;
                    }
                    .hljs-line::before {
                        content: counter(linenumber);
                        display: inline-block;
                        width: 3em;
                        margin-right: 1em;
                        text-align: right;
                        color: #888;
                        user-select: none;
                    }
                """.trimIndent() else ""
                
                val jsLineNumbers = """
                    const codeBlock = document.querySelector('code');
                    if (codeBlock) {
                        const lines = codeBlock.innerHTML.split('\n');
                        codeBlock.innerHTML = lines.map((line, idx) => '<span id="line-' + (idx + 1) + '" class="hljs-line">' + line + '</span>').join('\n');
                    }
                    window.scrollToLine = function(lineNum) {
                        const el = document.getElementById('line-' + lineNum);
                        if (el) {
                            el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                            el.style.backgroundColor = 'rgba(229, 169, 60, 0.4)';
                            el.style.borderRadius = '4px';
                            setTimeout(() => { el.style.backgroundColor = ''; }, 2500);
                        }
                    };
                """.trimIndent()

                """
                <!DOCTYPE html>
                <html>
                <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=0.25, maximum-scale=5.0, user-scalable=yes">
                <style>
                  body { 
                    background-color: $bgColor; 
                    color: $textColor;
                    margin: 0; padding: 16px;
                    font-size: $fSize;
                  }
                  pre {
                    margin: 0;
                    white-space: $wWrap;
                    word-wrap: $wWord;
                    font-family: monospace;
                  }
                  $lineNumbersCss
                </style>
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/$theme">
                <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
                </head>
                <body>
                <pre><code class="language-${fileEntity.extension.lowercase()}">$escapedCode</code></pre>
                <script>
                  hljs.highlightAll();
                  $jsLineNumbers
                </script>
                </body>
                </html>
                """.trimIndent()
            }

            var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }

            LaunchedEffect(scrollToLine) {
                if (scrollToLine != null) {
                    webViewRef?.evaluateJavascript("if (typeof window.scrollToLine === 'function') { window.scrollToLine($scrollToLine); }", null)
                    onScrollToLineHandled()
                }
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        webViewRef = this
                        settings.javaScriptEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                update = { webView ->
                    webViewRef = webView
                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            )
        }
    }
}

// 3. SPREADSHEET CSV SCREEN COMPONENT
@Composable
fun CsvPreview(
    csvRows: List<List<String>>,
    fileEntity: RecentFileEntity,
    viewModel: MainViewModel,
    editable: Boolean,
    notice: String?,
    onDiscardAndExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNightMode = androidx.compose.foundation.isSystemInDarkTheme()

    // Edits are held here and written only when the user taps Save. The old code overwrote the file on
    // every keystroke, which for .xlsx also flattened the workbook to one sheet of plain values.
    var pendingRows by remember { mutableStateOf<List<List<String>>?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    LaunchedEffect(csvRows) { pendingRows = null }

    androidx.activity.compose.BackHandler(enabled = pendingRows != null) { showDiscardDialog = true }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Unsaved changes") },
            text = { Text("Save your edits to ${fileEntity.name} before leaving?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRows?.let { viewModel.saveSpreadsheetFile(fileEntity, it) }
                    showDiscardDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        pendingRows = null
                        showDiscardDialog = false
                        onDiscardAndExit()
                    }) { Text("Discard") }
                }
            }
        )
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    text = fileEntity.extension.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = "${formatFileSize(fileEntity.size)} • Modified ${formatElapsedTime(fileEntity.lastOpened)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.weight(1f))
            val dirty = pendingRows
            if (!editable) {
                Text(
                    text = "View only",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
            } else if (dirty != null) {
                TextButton(onClick = { viewModel.saveSpreadsheetFile(fileEntity, dirty) }) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "Edit Mode",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (notice != null) NoticeBanner(notice)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Optimized Compose Excel View with x-spreadsheet via WebView
        val base64Data = remember(csvRows) {
            val jsonArray = org.json.JSONArray()
            for (row in csvRows) {
                val rowArray = org.json.JSONArray()
                for (cell in row) {
                    rowArray.put(cell)
                }
                jsonArray.put(rowArray)
            }
            android.util.Base64.encodeToString(jsonArray.toString().toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        // The page and scripts come from file:///android_asset/, which is always readable.
                        allowFileAccess = false
                        allowContentAccess = false
                        domStorageEnabled = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                    }

                    addJavascriptInterface(object : Any() {
                        @android.webkit.JavascriptInterface
                        fun onDataChanged(jsonStr: String) {
                            if (!editable) return
                            try {
                                val parser = org.json.JSONArray(jsonStr)
                                val newRows = mutableListOf<List<String>>()
                                for (i in 0 until parser.length()) {
                                    val rowArray = parser.getJSONArray(i)
                                    val row = mutableListOf<String>()
                                    for (j in 0 until rowArray.length()) {
                                        row.add(rowArray.getString(j))
                                    }
                                    newRows.add(row)
                                }
                                pendingRows = newRows
                            } catch (e: Exception) {
                                // Malformed payload from the page: keep the last good pending state.
                            }
                        }
                    }, "Android")

                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                            view?.evaluateJavascript("loadData('$base64Data');", null)
                        }
                    }

                    val bgColor = if (isNightMode) "#151514" else "#faf9f5"
                    val htmlContent = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes">
                            <link rel="stylesheet" href="file:///android_asset/xspreadsheet/xspreadsheet.css">
                            <script src="file:///android_asset/xspreadsheet/xspreadsheet.js"></script>
                            <style>
                                body, html { margin: 0; padding: 0; height: 100%; width: 100%; overflow: hidden; background-color: $bgColor; }
                                #x-spreadsheet-demo { height: 100%; width: 100%; }
                            </style>
                        </head>
                        <body>
                            <div id="x-spreadsheet-demo"></div>
                            <script>
                                var s = x_spreadsheet('#x-spreadsheet-demo', {
                                    mode: '${if (editable) "edit" else "read"}',
                                    showToolbar: ${if (editable) "true" else "false"},
                                    showGrid: true,
                                    view: {
                                        height: () => document.documentElement.clientHeight,
                                        width: () => document.documentElement.clientWidth,
                                    }
                                });
                                
                                function loadData(base64Str) {
                                    try {
                                        var jsonStr = decodeURIComponent(escape(window.atob(base64Str)));
                                        var rowsData = JSON.parse(jsonStr);
                                        var xRows = {};
                                        for(var i=0; i<rowsData.length; i++) {
                                            var row = rowsData[i];
                                            xRows[i] = { cells: {} };
                                            for(var j=0; j<row.length; j++) {
                                                xRows[i].cells[j] = { text: row[j] || "" };
                                            }
                                        }
                                        s.loadData([{
                                            name: 'Sheet1',
                                            rows: xRows
                                        }]);
                                        
                                        s.change(data => {
                                            var maxRow = 0;
                                            var maxCol = 0;
                                            for (var r in data.rows) {
                                                if (r === 'len') continue;
                                                var ri = parseInt(r);
                                                if (ri > maxRow) maxRow = ri;
                                                var row = data.rows[r];
                                                if (row && row.cells) {
                                                    for (var c in row.cells) {
                                                        var ci = parseInt(c);
                                                        if (ci > maxCol) maxCol = ci;
                                                    }
                                                }
                                            }
                                            var resultRows = [];
                                            for (var i = 0; i <= maxRow; i++) {
                                                var rowArr = [];
                                                for (var j = 0; j <= maxCol; j++) {
                                                    if (data.rows[i] && data.rows[i].cells && data.rows[i].cells[j]) {
                                                        rowArr.push(data.rows[i].cells[j].text || "");
                                                    } else {
                                                        rowArr.push("");
                                                    }
                                                }
                                                resultRows.push(rowArr);
                                            }
                                            Android.onDataChanged(JSON.stringify(resultRows));
                                        });
                                    } catch(e) {
                                        console.error(e);
                                    }
                                }
                            </script>
                        </body>
                        </html>
                    """.trimIndent()
                    loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
                }
            }
        )
    }
}

// 4. TREE-LISTING ARCHIVE PREVIEW COMPONENT
@Composable
fun ZipPreview(
    zipRoot: FileManager.ZipNode,
    fileEntity: RecentFileEntity,
    expandedPaths: Set<String>,
    onToggleExpand: (String) -> Unit,
    onZipEntryClick: (FileManager.ZipNode, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep list state
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    // Automatically expand the root ZIP node if not expanded
    LaunchedEffect(zipRoot) {
        if (!expandedPaths.contains("")) {
            onToggleExpand("")
        }
    }

    Column(modifier = modifier) {
        // ZIP info row banner
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    text = fileEntity.extension.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = "${formatFileSize(fileEntity.size)} • Modified ${formatElapsedTime(fileEntity.lastOpened)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Read-only",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold
            )
        }
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Nested Header card
            item(contentType = "ZipHeader") {
                ClaudeCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFBF0EC),
                            modifier = Modifier.size(45.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = zipRoot.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val totalFilesCount = countTotalFiles(zipRoot)
                            Text(
                                text = "$totalFilesCount files • ${formatFileSize(fileEntity.size)} uncompressed",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Recursive flat structure listing to handle animations correctly in LazyColumn
            val nodesList = mutableListOf<NodeWithDepth>()
            buildNodesList(zipRoot, 0, expandedPaths, nodesList)

            items(nodesList, key = { it.node.path + "_" + it.depth }, contentType = { "ZipNode" }) { (node, depth) ->
                val isExpanded = expandedPaths.contains(node.path)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItemPlacement()
                        .clickable {
                            if (node.isDirectory) {
                                onToggleExpand(node.path)
                            } else {
                                onZipEntryClick(node, "")
                            }
                        }
                        .padding(start = (depth * 20).dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (node.isDirectory) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = "Folder",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = node.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Spacer(modifier = Modifier.width(24.dp)) // indentation padding
                        Icon(
                            imageVector = Icons.Outlined.InsertDriveFile,
                            contentDescription = "File",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = node.name,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatFileSize(node.size),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class NodeWithDepth(val node: FileManager.ZipNode, val depth: Int)

fun countTotalFiles(root: FileManager.ZipNode): Int {
    var count = 0
    fun traverse(node: FileManager.ZipNode) {
        if (!node.isDirectory) {
            count++
        }
        node.children.forEach { traverse(it) }
    }
    traverse(root)
    return count
}

fun buildNodesList(
    node: FileManager.ZipNode,
    currentDepth: Int,
    expandedPaths: Set<String>,
    result: MutableList<NodeWithDepth>
) {
    // We omit the root node itself since it is already rendered in the beautiful header card!
    if (node.path.isNotEmpty()) {
        result.add(NodeWithDepth(node, currentDepth))
    }

    val isExpanded = expandedPaths.contains(node.path)
    if (isExpanded || node.path.isEmpty()) {
        node.children.forEach { child ->
            buildNodesList(child, if (node.path.isEmpty()) 0 else currentDepth + 1, expandedPaths, result)
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ImagePreview(
    fileEntity: RecentFileEntity,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {}
) {
    val parentDir = remember(fileEntity) { java.io.File(fileEntity.path).parentFile }
    val imageFiles = remember(parentDir) {
        parentDir?.listFiles()?.filter { 
            val ext = it.extension.lowercase()
            ext in listOf("png", "jpg", "jpeg", "gif", "bmp", "webp")
        }?.sortedBy { it.name } ?: listOf(java.io.File(fileEntity.path))
    }
    
    val initialIndex = remember(imageFiles, fileEntity) {
        val idx = imageFiles.indexOfFirst { it.absolutePath == fileEntity.path }
        if (idx == -1) 0 else idx
    }
    
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialIndex,
        pageCount = { imageFiles.size }
    )
    
    val currentFile = imageFiles.getOrNull(pagerState.currentPage) ?: java.io.File(fileEntity.path)
    
    // The history save requirement "history me sirf folder save kro or path me last previewed pictures jis per click krne per us folder ki last dekhi hue image lload ho"
    // Since MainViewModel automatically saves the file when openFile is called, we don't need to save every swipe to the history,
    // but the user says "path me last previewed pictures jis per click krne per us folder ki last dekhi hue image lload ho"
    // So maybe we can trigger a history update when the page settles, updating the same DB record? 
    // For now, let's just show the images. We can use a LaunchedEffect to update history silently if needed.
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != initialIndex) {
            val dao = com.example.data.AppDatabase.getDatabase(context).recentFileDao()
            val newFile = imageFiles[pagerState.currentPage]
            val existing = dao.getRecentFileByPath(fileEntity.path)
            if (existing != null) {
                dao.deleteRecentFileByPath(existing.path)
                dao.insertRecentFile(existing.copy(
                    path = newFile.absolutePath, 
                    name = newFile.name, 
                    extension = newFile.extension.lowercase(), 
                    size = newFile.length(),
                    lastOpened = System.currentTimeMillis(),
                    id = 0 // Auto-generate new id
                ))
            } else {
                dao.insertRecentFile(RecentFileEntity(
                    path = newFile.absolutePath,
                    name = newFile.name,
                    extension = newFile.extension.lowercase(),
                    size = newFile.length(),
                    lastOpened = System.currentTimeMillis()
                ))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val file = imageFiles[page]
            val zoomSpec = remember {
                me.saket.telephoto.zoomable.ZoomSpec(maxZoomFactor = 15f)
            }
            val zoomableState = me.saket.telephoto.zoomable.rememberZoomableState(zoomSpec = zoomSpec)
            val zoomableImageState = me.saket.telephoto.zoomable.rememberZoomableImageState(zoomableState = zoomableState)

            me.saket.telephoto.zoomable.coil.ZoomableAsyncImage(
                model = file,
                contentDescription = "Image preview",
                modifier = Modifier.fillMaxSize(),
                state = zoomableImageState,
                onClick = { onTap() }
            )
        }
    }
}


private fun isBundledPdfReaderUri(uri: android.net.Uri): Boolean {
    val path = uri.path.orEmpty()
    return uri.scheme == "https" &&
        uri.host == "appassets.androidplatform.net" &&
        uri.port in setOf(-1, 443) &&
        uri.userInfo == null &&
        (path.startsWith("/assets/pdfjs/") || path == "/search-pdf/document.pdf")
}

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
        val defaultAssetsHandler = androidx.webkit.WebViewAssetLoader.AssetsPathHandler(context)
        androidx.webkit.WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", object : androidx.webkit.WebViewAssetLoader.PathHandler {
                override fun handle(path: String): android.webkit.WebResourceResponse? {
                    if (path.endsWith(".mjs", ignoreCase = true)) {
                        return runCatching {
                            android.webkit.WebResourceResponse(
                                "application/javascript",
                                "UTF-8",
                                context.assets.open(path)
                            )
                        }.getOrNull()
                    }
                    return defaultAssetsHandler.handle(path)
                }
            })
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
            val normalized = com.example.core.format.PdfRotationPolicy.normalize(viewRotation)
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
                                ): Boolean = !isBundledPdfReaderUri(request.url)

                                @Suppress("DEPRECATION")
                                override fun shouldOverrideUrlLoading(
                                    view: android.webkit.WebView,
                                    url: String
                                ): Boolean = !isBundledPdfReaderUri(android.net.Uri.parse(url))

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
                                        val response = com.example.utils.PdfRangeResponder.respond(pdfFile, request.requestHeaders)
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
                                        val response = com.example.utils.PdfRangeResponder.respond(pdfFile, null)
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
        com.example.core.format.PdfRotationPolicy.fractionForPage(currentPage, pageTotal)
    val displayPage = if (isDragging) {
        com.example.core.format.PdfRotationPolicy.pageFromFraction(dragFraction, pageTotal)
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
                            val targetPage = com.example.core.format.PdfRotationPolicy
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

@Composable
fun AudioPlayer(
    fileEntity: RecentFileEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPos by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(1) }

    LaunchedEffect(fileEntity) {
        try {
            val mp = MediaPlayer().apply {
                setDataSource(fileEntity.path)
                prepare()
                setOnCompletionListener {
                    isPlaying = false
                }
            }
            mediaPlayer = mp
            duration = mp.duration
            
            while (true) {
                val mpActive = mediaPlayer
                if (mpActive != null) {
                    try {
                        if (mpActive.isPlaying) {
                            currentPos = mpActive.currentPosition
                        }
                    } catch (e: Exception) {
                        break
                    }
                } else {
                    break
                }
                delay(500)
            }
        } catch (e: Exception) {
            // handle error
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.apply {
                try {
                    if (isPlaying) {
                        stop()
                    }
                } catch (ignored: Exception) {}
                release()
            }
            mediaPlayer = null
        }
    }

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = fileEntity.name,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress bar / Slider
        Slider(
            value = currentPos.toFloat(),
            onValueChange = { newVal ->
                mediaPlayer?.seekTo(newVal.toInt())
                currentPos = newVal.toInt()
            },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatTime(currentPos), fontSize = 12.sp, color = Color.Gray)
            Text(text = formatTime(duration), fontSize = 12.sp, color = Color.Gray)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Play / Pause FAB
        FloatingActionButton(
            onClick = {
                val mp = mediaPlayer ?: return@FloatingActionButton
                try {
                    if (isPlaying) {
                        mp.pause()
                        isPlaying = false
                    } else {
                        mp.start()
                        isPlaying = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause"
            )
        }
    }
}

@Composable
fun VideoPlayer(
    fileEntity: RecentFileEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = fileEntity.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        AndroidView(
            factory = { ctx ->
                android.widget.VideoView(ctx).apply {
                    setVideoPath(fileEntity.path)
                    val controller = android.widget.MediaController(ctx)
                    controller.setAnchorView(this)
                    setMediaController(controller)
                    start()
                }
            },
            modifier = Modifier.fillMaxWidth().height(260.dp)
        )
    }
}

@Composable
fun HexViewer(
    hexRows: List<String>,
    asciiRows: List<String>,
    fileEntity: RecentFileEntity,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    text = "BINARY / HEX",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = "${formatFileSize(fileEntity.size)} • Modified ${formatElapsedTime(fileEntity.lastOpened)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E)) // Dark code editor styled background for hex contrast
                .padding(8.dp)
        ) {
            items(hexRows.size, key = { it }, contentType = { "HexRow" }) { index ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp).animateItemPlacement(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = hexRows[index],
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFD4D4D4)
                    )
                    Text(
                        text = "| ${if (index < asciiRows.size) asciiRows[index] else ""}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF9CDCFE),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

fun formatTime(ms: Int): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}

private fun shareFile(context: android.content.Context, file: File, extension: String) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = when (extension.lowercase()) {
            "pdf" -> "application/pdf"
            "csv" -> "text/csv"
            "zip" -> "application/zip"
            "png", "jpg", "jpeg", "webp" -> "image/*"
            "mp3", "wav", "m4a" -> "audio/*"
            "mp4", "mkv" -> "video/*"
            else -> "text/plain"
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share File"))
    } catch (e: Exception) {
        e.printStackTrace()
        // Reliable fallback sharing flow using plain file path text
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, "File Path: ${file.absolutePath}")
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share File Path"))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}

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
                    loadLocalFileOnce(webView, fileEntity.path, allowScripts)
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
                            <body>${escapeHtml(content)}</body>
                            </html>
                            """.trimIndent()
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    } catch (e: Exception) {
                        loadLocalFileOnce(webView, fileEntity.path, allowScripts)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** Loads a local file only when the file or script setting changes, instead of on every recomposition. */
private fun loadLocalFileOnce(webView: android.webkit.WebView, path: String, allowScripts: Boolean) {
    webView.settings.javaScriptEnabled = allowScripts
    val url = android.net.Uri.fromFile(java.io.File(path)).toString()
    val key = "$url|$allowScripts"
    if (webView.tag != key) {
        webView.tag = key
        webView.loadUrl(url)
    }
}

@Composable
fun DocxPreviewWebView(
    base64Docx: String,
    fileEntity: RecentFileEntity,
    isNightMode: Boolean,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onPageChanged: (Int, Int) -> Unit = { _, _ -> },
    scrollToPage: Int? = null,
    onScrollProgressHandled: () -> Unit = {},
    searchQuery: String = "",
    searchTriggerNext: Int = 0,
    searchTriggerPrev: Int = 0,
    onSearchResultCount: (Int, Int, Boolean) -> Unit = { _, _, _ -> },
    zoomFactor: Float = 1.0f,
    // Loop 12 (Bug A): true while a pinch gesture is physically in progress. While it
    // is set, zoom updates go to the cheap compositor-only preview path instead of
    // re-laying-out the whole document per frame; the settle transition commits once.
    pinchActive: Boolean = false,
    onWebViewCreated: (android.webkit.WebView?) -> Unit = {},
    onDocumentReady: () -> Unit = {},
    onSingleTap: () -> Unit = {},
    onZoomChanged: (Float) -> Unit = {},
    onPinchZoom: (Float) -> Unit = {},
    onPinchSettled: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onRendererFallback: (Boolean) -> Unit = {}
) {
    var isWebViewLoading by remember(base64Docx) { mutableStateOf(true) }
    var webViewRef by remember(base64Docx) { mutableStateOf<android.webkit.WebView?>(null) }
    var pureKotlinFallback by remember(base64Docx) { mutableStateOf<com.example.core.format.DocxExtractor.ExtractedDocx?>(null) }
    // Loop 14: the plain-text view is an honest RECOVERY state, never a normal
    // preview. The renderer failure reason is surfaced verbatim (no document
    // content, no sensitive data) and Retry rebuilds the WebView from scratch.
    var fallbackReason by remember(base64Docx) { mutableStateOf<String?>(null) }
    var docxReloadKey by remember(base64Docx) { mutableIntStateOf(0) }
    // Ledger of what the renderer's search engine has already been told (Loop 12, Bug
    // B): navigation triggers now issue ONLY next/prev deltas; re-issuing searchWord()
    // per arrow press used to reset the match index to 0 so the counter stuck at 2/N.
    var issuedSearch by remember(base64Docx) { mutableStateOf(InViewerSearchCommandPolicy.Issued()) }
    fun retryDocxRender() {
        pureKotlinFallback = null
        fallbackReason = null
        isWebViewLoading = true
        issuedSearch = InViewerSearchCommandPolicy.Issued()
        docxReloadKey++
    }
    // Display-frame throttle (~30/s) for the pinch preview path (Bug E: coalescing).
    var lastDisplayDispatchNanos by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    val context = LocalContext.current
    val latestSearchResultCallback = androidx.compose.runtime.rememberUpdatedState(onSearchResultCount)
    val latestPageChangedCallback = androidx.compose.runtime.rememberUpdatedState(onPageChanged)
    val latestDocumentReadyCallback = androidx.compose.runtime.rememberUpdatedState(onDocumentReady)
    val latestWebViewCreatedCallback = androidx.compose.runtime.rememberUpdatedState(onWebViewCreated)
    val latestSingleTapCallback = androidx.compose.runtime.rememberUpdatedState(onSingleTap)
    val latestZoomChangedCallback = androidx.compose.runtime.rememberUpdatedState(onZoomChanged)
    val latestPinchZoomCallback = androidx.compose.runtime.rememberUpdatedState(onPinchZoom)
    val latestPinchSettledCallback = androidx.compose.runtime.rememberUpdatedState(onPinchSettled)
    val latestDoubleTapCallback = androidx.compose.runtime.rememberUpdatedState(onDoubleTap)
    val latestRendererFallbackCallback = androidx.compose.runtime.rememberUpdatedState(onRendererFallback)
    // Fresh values for the WebViewClient closures created once in the factory: reading
    // them at callback time prevents a stale initial zoom/theme being re-applied after
    // the document finishes loading (Loop 10).
    val latestZoomForPageLoad = androidx.compose.runtime.rememberUpdatedState(zoomFactor)
    val latestNightModeForPageLoad = androidx.compose.runtime.rememberUpdatedState(isNightMode)

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
    LaunchedEffect(webViewRef, isWebViewLoading, isNightMode, pureKotlinFallback) {
        if (webViewRef != null && !isWebViewLoading && pureKotlinFallback == null) {
            webViewRef?.evaluateJavascript("setDarkMode($isNightMode);", null)
        }
    }
    // Loop 12 (Bug A): two-phase zoom pipeline.
    //  - pinchActive == true  -> setZoomDisplay(): compositor-only transform preview,
    //    throttled to ~30 dispatches/s so a large DOCX stops re-laying-out per frame.
    //  - pinchActive == false -> setZoom(): the authoritative commit (toolbar +/-,
    //    percentage dialog, page load, and the settle transition after a pinch).
    LaunchedEffect(webViewRef, isWebViewLoading, zoomFactor, pinchActive, pureKotlinFallback) {
        val webView = webViewRef
        if (webView != null && !isWebViewLoading && pureKotlinFallback == null) {
            if (pinchActive) {
                val now = android.os.SystemClock.elapsedRealtimeNanos()
                if (now - lastDisplayDispatchNanos < 33_000_000L) return@LaunchedEffect
                lastDisplayDispatchNanos = now
                webView.evaluateJavascript("setZoomDisplay($zoomFactor);", null)
            } else {
                lastDisplayDispatchNanos = 0L
                webView.evaluateJavascript("setZoom($zoomFactor);", null)
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
                            text = com.example.ui.screens.DocxTextRecovery.bannerHeadline(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        com.example.ui.screens.DocxTextRecovery.displayReason(fallbackReason)?.let { reason ->
                            Text(
                                text = reason,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = .75f),
                                maxLines = 2
                            )
                        }
                        com.example.ui.screens.DocxTextRecovery
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
            androidx.compose.runtime.key(base64Docx, docxReloadKey) {
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
                            .addPathHandler("/assets/", androidx.webkit.WebViewAssetLoader.AssetsPathHandler(context))
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
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                // The asset loader serves both the editor page and the
                                // document bytes; no raw file scheme access is needed.
                                allowFileAccess = false
                                allowContentAccess = false
                                @Suppress("DEPRECATION")
                                allowFileAccessFromFileURLs = false
                                @Suppress("DEPRECATION")
                                allowUniversalAccessFromFileURLs = false
                                builtInZoomControls = false
                                displayZoomControls = false
                                setSupportZoom(false)
                                useWideViewPort = true
                                loadWithOverviewMode = true
                            }
                            setBackgroundColor(if (isNightMode) 0xFF151514.toInt() else 0xFFfaf9f5.toInt())

                            val pageChangedCallback = latestPageChangedCallback
                            addJavascriptInterface(object : Any() {
                                private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

                                @android.webkit.JavascriptInterface
                                fun getDocxBase64(): String = base64Docx

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
                                    mainHandler.post { latestZoomChangedCallback.value(newZoom.toFloat()) }
                                }

                                @android.webkit.JavascriptInterface
                                fun onDocumentError(error: String) {
                                    mainHandler.post {
                                        isWebViewLoading = false
                                        android.util.Log.w("WebViewDocx", "Switching to text-only DOCX preview: $error")
                                        val extracted = runCatching {
                                            com.example.core.format.DocxExtractor.extractText(File(fileEntity.path))
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
                                    view?.evaluateJavascript(
                                        "setZoom($freshZoom); loadDocx(null, $freshNightMode);",
                                        null
                                    )
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?
                                ): Boolean {
                                    val uri = request?.url ?: return true
                                    val localEditorAsset = uri.scheme == "https" &&
                                        uri.host == "appassets.androidplatform.net" &&
                                        (uri.path?.startsWith("/assets/editor/") == true || uri.path?.startsWith("/docx-src/") == true)
                                    return !localEditorAsset
                                }

                                override fun shouldInterceptRequest(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?
                                ): android.webkit.WebResourceResponse? {
                                    return request?.url?.let { docxAssetLoader.shouldInterceptRequest(it) }
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
                                            com.example.core.format.DocxExtractor.extractText(File(fileEntity.path))
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
                                            com.example.core.format.DocxExtractor.extractText(File(fileEntity.path))
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


/** Unwrap themed/Compose context wrappers to reach the host Activity for orientation requests. */
private fun Context.findHostActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
