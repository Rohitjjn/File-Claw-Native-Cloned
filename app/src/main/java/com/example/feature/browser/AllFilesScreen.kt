package com.example.feature.browser

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.component.ClaudeAppBar
import com.example.ui.component.ClaudeCard
import com.example.ui.component.FileIcon
import com.example.ui.component.FileExtensionBadge
import com.example.ui.component.StaggeredAnimatedItem
import com.example.ui.component.premiumLoadingPulse
import com.example.viewmodel.MainViewModel
import java.io.File
import androidx.compose.foundation.lazy.rememberLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllFilesScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onNavigateToPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val files by viewModel.recentFilesState.collectAsState()
    val favoritePaths by viewModel.favoritePaths.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFileState by viewModel.currentFileState.collectAsState()
    val loadingFilePathState = viewModel.loadingFilePath.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(false) }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var deleteFromDiskChecked by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    // Batch Selection Mode State
    var isBatchMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importFileFromUri(it)
        }
    }

    Scaffold(
        topBar = {
            if (isBatchMode) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .statusBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                isBatchMode = false
                                selectedIds = emptySet()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close batch selection")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${selectedIds.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    selectedIds = if (selectedIds.size == files.size) {
                                        emptySet()
                                    } else {
                                        files.map { it.id }.toSet()
                                    }
                                }
                            ) {
                                Text(
                                    text = if (selectedIds.size == files.size && files.isNotEmpty()) "Deselect All" else "Select All",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            } else if (isSearchActive) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .statusBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        IconButton(
                            onClick = { isSearchActive = false; viewModel.updateSearchQuery("") },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Search", modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Search files...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                        )
                                    }
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { viewModel.updateSearchQuery(it) },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(searchFocusRequester)
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.updateSearchQuery("") },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    LaunchedEffect(Unit) {
                        searchFocusRequester.requestFocus()
                    }
                }
            } else {
                ClaudeAppBar(
                    title = "Recent History",
                    onNavIconClick = onBackClick,
                    navIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                imageVector = if (isGridView) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                                contentDescription = "Toggle Grid or List View",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { isBatchMode = true },
                            modifier = Modifier.testTag("all_files_batch_select_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Checklist,
                                contentDescription = "Batch Select",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { showConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.testTag("all_files_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val targetFiles by remember(files, showFavoritesOnly, favoritePaths) {
                derivedStateOf {
                    if (showFavoritesOnly) files.filter { favoritePaths.contains(it.path) } else files
                }
            }
            val favCount by remember(files, favoritePaths) {
                derivedStateOf { files.count { favoritePaths.contains(it.path) } }
            }

            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = if (isBatchMode && selectedIds.isNotEmpty()) 120.dp else 40.dp
                )
            ) {
                // Filter Tabs (All vs Favorites)
                if (files.isNotEmpty()) {
                    item(contentType = "FilterChips") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                        ) {
                            FilterChip(
                                selected = !showFavoritesOnly,
                                onClick = { showFavoritesOnly = false },
                                label = { Text("All Files (${files.size})", fontSize = 12.sp) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            FilterChip(
                                selected = showFavoritesOnly,
                                onClick = { showFavoritesOnly = true },
                                label = { Text("★ Favorites ($favCount)", fontSize = 12.sp) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE5A93C).copy(alpha = 0.18f),
                                    selectedLabelColor = Color(0xFFD49228)
                                )
                            )
                        }
                    }
                }

                if (targetFiles.isEmpty()) {
                    item(contentType = "EmptyState") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (showFavoritesOnly) Icons.Outlined.StarOutline else Icons.Outlined.LibraryBooks,
                                    contentDescription = null,
                                    tint = if (showFavoritesOnly) Color(0xFFE5A93C).copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                                    modifier = Modifier.size(68.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (showFavoritesOnly) "No favorites starred yet." else "Your trace is clean. No files tracked.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                } else if (isGridView) {
                    // 2-Column Grid Layout with Staggered Spring Animation
                    val rows = targetFiles.chunked(2)
                    itemsIndexed(rows, key = { _, row -> row.joinToString("-") { it.id.toString() } }, contentType = { _, _ -> "GridRow" }) { rowIndex, rowFiles ->
                        StaggeredAnimatedItem(index = rowIndex) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                            for (file in rowFiles) {
                                val isSelected = selectedIds.contains(file.id)
                                val isFav = favoritePaths.contains(file.path)
                                Box(modifier = Modifier.weight(1f)) {
                                    ClaudeCard(
                                        isSelected = isSelected,
                                        onClick = {
                                            if (isBatchMode) {
                                                selectedIds = if (isSelected) selectedIds - file.id else selectedIds + file.id
                                            } else {
                                                viewModel.openFile(file)
                                            }
                                        }
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                FileExtensionBadge(extension = file.extension)
                                                if (isBatchMode) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = { checked ->
                                                            selectedIds = if (checked) selectedIds + file.id else selectedIds - file.id
                                                        },
                                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                } else {
                                                    IconButton(
                                                        onClick = { viewModel.toggleFavorite(file.path) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                                            contentDescription = "Toggle favorite",
                                                            tint = if (isFav) Color(0xFFE5A93C) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val isLoading by remember(file.path) { androidx.compose.runtime.derivedStateOf { loadingFilePathState.value == file.path } }
                                                FileIcon(
                                                    extension = file.extension,
                                                    size = 46,
                                                    isSample = file.isSample,
                                                    modifier = Modifier.premiumLoadingPulse(isLoading)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = file.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            val formattedSize = remember(file.size) { formatLocalFileSize(file.size) }
                                            Text(
                                                text = formattedSize,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                            )
                                        }
                                    }
                                }
                            }
                            if (rowFiles.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(targetFiles, key = { _, it -> it.id }, contentType = { _, _ -> "FileItem" }) { fileIndex, file ->
                    val isSelected = selectedIds.contains(file.id)
                    val isFav = favoritePaths.contains(file.path)

                    StaggeredAnimatedItem(index = fileIndex) {
                        ClaudeCard(
                            onClick = {
                                if (isBatchMode) {
                                    selectedIds = if (isSelected) selectedIds - file.id else selectedIds + file.id
                                } else {
                                    viewModel.openFile(file)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isBatchMode) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedIds = if (checked) selectedIds + file.id else selectedIds - file.id
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }

                                    val isLoading by remember(file.path) { androidx.compose.runtime.derivedStateOf { loadingFilePathState.value == file.path } }
                                    FileIcon(
                                        extension = file.extension,
                                        size = 42,
                                        isSample = file.isSample,
                                        modifier = Modifier.premiumLoadingPulse(isLoading)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = file.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val formattedSize = remember(file.size) { formatLocalFileSize(file.size) }
                                        val formattedTime = remember(file.lastOpened) { formatLocalElapsedTime(file.lastOpened) }
                                        Text(
                                            text = "${file.extension.uppercase()}  •  $formattedSize  •  $formattedTime",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!isBatchMode) {
                                        IconButton(
                                            onClick = { viewModel.toggleFavorite(file.path) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                                contentDescription = "Toggle favorite",
                                                tint = if (isFav) Color(0xFFE5A93C) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteRecentFile(file) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = "Remove from history",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Open Preview",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }

            // Bottom Batch Action Bar
            AnimatedVisibility(
                visible = isBatchMode && selectedIds.isNotEmpty(),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedIds.size} files selected",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row {
                            TextButton(
                                onClick = {
                                    val selectedEntities = files.filter { selectedIds.contains(it.id) }
                                    viewModel.deleteRecentFilesBatch(selectedEntities, deleteFromDisk = false)
                                    isBatchMode = false
                                    selectedIds = emptySet()
                                }
                            ) {
                                Text("Remove History", fontSize = 13.sp)
                            }

                            Button(
                                onClick = { showBatchDeleteDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Batch Delete Dialog
    if (showBatchDeleteDialog) {
        val selectedEntities = files.filter { selectedIds.contains(it.id) }

        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("Delete ${selectedEntities.size} File(s)?") },
            text = {
                Column {
                    Text("Select how you want to remove the selected files:")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteFromDiskChecked = !deleteFromDiskChecked }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = deleteFromDiskChecked,
                            onCheckedChange = { deleteFromDiskChecked = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Also delete permanently from device storage", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("Warning: cannot be undone", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecentFilesBatch(selectedEntities, deleteFromDisk = deleteFromDiskChecked)
                        showBatchDeleteDialog = false
                        isBatchMode = false
                        selectedIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (deleteFromDiskChecked) "Delete from Disk" else "Remove from History")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(text = "Clear History?") },
            text = { Text(text = "Are you sure you want to delete all files from your recent history? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearRecentFiles()
                        showConfirmDialog = false
                    }
                ) {
                    Text(text = "Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

// Helpers local to screen
private fun formatLocalFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.1f MB", mb)
}

private fun formatLocalElapsedTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    if (diff < 60 * 1000) return "just now"
    val mins = diff / (60 * 1000)
    if (mins < 60) return if (mins == 1L) "1 min ago" else "$mins mins ago"
    val hours = mins / 60
    if (hours < 24) return if (hours == 1L) "1 hour ago" else "$hours hours ago"
    val days = hours / 24
    return if (days == 1L) "1 day ago" else "$days days ago"
}
