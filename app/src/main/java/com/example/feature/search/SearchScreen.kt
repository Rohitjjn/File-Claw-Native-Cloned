package com.example.feature.search

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.search.FileSearchRanker
import com.example.ui.component.FileIcon
import com.example.ui.component.formatFileSize
import com.example.ui.component.premiumLoadingPulse
import com.example.ui.theme.ClaudeOrangeAccent
import com.example.viewmodel.MainViewModel
import java.io.File

import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val historyResults by viewModel.recentFilesState.collectAsState()
    val deviceResults by viewModel.deviceSearchResults.collectAsState()
    val deviceSearchTruncated by viewModel.deviceSearchTruncated.collectAsState()
    val isSearchingDevice by viewModel.isSearchingDevice.collectAsState()
    val isIndexing by viewModel.isIndexing.collectAsState()
    val searchTermsHistory by viewModel.searchTermsHistory.collectAsState()

    var showAllHistory by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
        viewModel.triggerStorageIndexRefresh()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.updateSearchQuery("")
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search files & storage...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    )
                                }
                                androidx.compose.foundation.text.BasicTextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        viewModel.updateSearchQuery(it)
                                        viewModel.searchLocalFiles(it)
                                    },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        if (searchQuery.isNotBlank()) viewModel.addSearchTermToHistory(searchQuery)
                                        keyboardController?.hide()
                                    }),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        viewModel.updateSearchQuery("")
                                        viewModel.searchLocalFiles("")
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear Input",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.testTag("search_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (searchQuery.isEmpty()) {
                if (searchTermsHistory.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TravelExplore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(96.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Search files & storage",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Find documents by name across your history and device folders",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                    ) {
                        item(contentType = "HistoryHeader") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "RECENT SEARCHES",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { viewModel.clearSearchTermsHistory() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Clear all",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                        items(searchTermsHistory, key = { it }) { term ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItemPlacement()
                                    .clickable {
                                        viewModel.updateSearchQuery(term)
                                        viewModel.searchLocalFiles(term)
                                        viewModel.addSearchTermToHistory(term)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "History",
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = term,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.removeSearchTermFromHistory(term) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                val limitCount = if (showAllHistory) historyResults.size else 3
                val displayedHistory by remember(historyResults, limitCount) {
                    derivedStateOf { historyResults.take(limitCount) }
                }

                LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. RECENT HISTORY MATCHES (signature terracotta scheme)
                    item(contentType = "HistoryHeader") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, bottom = 2.dp)
                        ) {
                            Text(
                                text = "RECENT HISTORY MATCHES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            if (historyResults.isNotEmpty()) {
                                Text(
                                    text = "${historyResults.size} found",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    if (historyResults.isEmpty()) {
                        item(contentType = "HistoryEmptyState") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No history files found matching \"$searchQuery\"",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {
                        items(displayedHistory, key = { "hist_" + it.id }, contentType = { "HistoryItem" }) { file ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItemPlacement()
                                    .clickable { viewModel.openFile(file) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FileIcon(extension = file.extension, size = 28, isSample = file.isSample)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        HighlightedFileName(name = file.name, query = searchQuery)
                                        val formattedSize = remember(file.size) { formatFileSize(file.size) }
                                        Text(
                                            text = "History • ${file.extension.uppercase()} • $formattedSize",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Bottom-right "Show More" expansion option for history matching list
                        if (historyResults.size > 3 && !showAllHistory) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = "Show More (${historyResults.size - 3} more)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clickable { showAllHistory = true }
                                            .padding(vertical = 4.dp, horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. DEVICE DIRECT FILES MATCHES (warm amber accent)
                    item(contentType = "DeviceHeader") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                        ) {
                            Text(
                                text = "DEVICE STORAGE SEARCH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ClaudeOrangeAccent,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isSearchingDevice || isIndexing) {
                                Text(
                                    text = if (isIndexing) "Building index... " else "Searching... ",
                                    fontSize = 11.sp,
                                    color = ClaudeOrangeAccent,
                                    modifier = Modifier.premiumLoadingPulse(true)
                                )
                            } else if (deviceResults.isNotEmpty()) {
                                Text(
                                    text = if (deviceSearchTruncated) "first ${deviceResults.size} of 150+" else "${deviceResults.size} found",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ClaudeOrangeAccent.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }

                    if (deviceResults.isEmpty()) {
                        item(contentType = "DeviceEmptyState") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isIndexing) "Building index... Please wait." else if (isSearchingDevice) "Scanning local directories..." else "No local device files found matching \"$searchQuery\"",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {
                        items(deviceResults, key = { "dev_" + it.absolutePath }, contentType = { "DeviceItem" }) { localFile ->
                            val isAlreadyInHistory = historyResults.any { it.path == localFile.absolutePath }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItemPlacement()
                                    .clickable {
                                        viewModel.importAndOpenFile(localFile)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val ext = localFile.name.substringAfterLast('.', "")
                                    FileIcon(
                                        extension = ext,
                                        size = 28,
                                        isSample = false,
                                        isRecent = isAlreadyInHistory
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        HighlightedFileName(name = localFile.name, query = searchQuery)
                                        Text(
                                            text = "Storage • ${localFile.parent ?: localFile.absolutePath}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * File name with the matched substring painted in the signature terracotta tone
 * and bold, so users can instantly see why a result matched.
 */
@Composable
private fun HighlightedFileName(name: String, query: String) {
    val highlightColor = MaterialTheme.colorScheme.primary
    val annotated = remember(name, query) {
        buildAnnotatedString {
            val range = FileSearchRanker.firstMatchRange(query, name)
            if (range == null) {
                append(name)
            } else {
                append(name.substring(0, range.start))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = highlightColor)) {
                    append(name.substring(range.start, range.end))
                }
                append(name.substring(range.end))
            }
        }
    }
    Text(
        text = annotated,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
