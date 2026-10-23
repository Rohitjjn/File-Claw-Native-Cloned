package com.example.feature.browser

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.io.BatchFileOperations
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class FileNode(val file: File, val depth: Int, val isExpanded: Boolean = false)

enum class PendingBatchMode {
    COPY,
    MOVE
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileTreeComponent(
    rootDir: File,
    onFileClick: (File) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel? = null
) {
    val coroutineScope = rememberCoroutineScope()

    var expandedPaths by rememberSaveable { mutableStateOf(setOf<String>()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var flatNodes by remember { mutableStateOf<List<FileNode>>(emptyList()) }
    val listState = rememberLazyListState()

    // Batch Selection States
    var isBatchMode by remember { mutableStateOf(false) }
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }
    var pendingMode by remember { mutableStateOf<PendingBatchMode?>(null) }
    var pendingFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showFolderPickerDialog by remember { mutableStateOf(false) }
    var reloadTrigger by remember { mutableStateOf(0) }
    var selectedDestinationDir by remember { mutableStateOf<File?>(null) }

    // Scan directory tree
    LaunchedEffect(rootDir, expandedPaths, searchQuery, reloadTrigger) {
        withContext(Dispatchers.IO) {
            val nodes = mutableListOf<FileNode>()
            fun traverse(dir: File, depth: Int) {
                val files = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                files?.forEach { f ->
                    val matchesSearch = searchQuery.isEmpty() || f.name.contains(searchQuery, ignoreCase = true)
                    if (matchesSearch || (f.isDirectory && expandedPaths.contains(f.absolutePath))) {
                        if (matchesSearch) nodes.add(FileNode(f, depth, expandedPaths.contains(f.absolutePath)))
                        if (f.isDirectory && (expandedPaths.contains(f.absolutePath) || searchQuery.isNotEmpty())) {
                            traverse(f, depth + 1)
                        }
                    }
                }
            }

            val children = rootDir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            children?.forEach { f ->
                val matchesSearch = searchQuery.isEmpty() || f.name.contains(searchQuery, ignoreCase = true)
                if (matchesSearch) nodes.add(FileNode(f, 0, expandedPaths.contains(f.absolutePath)))
                if (f.isDirectory && (expandedPaths.contains(f.absolutePath) || searchQuery.isNotEmpty())) {
                    traverse(f, if (matchesSearch) 1 else 0)
                }
            }
            flatNodes = nodes
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("file_tree_component")) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: Either Search Bar or Batch Selection Header
            if (isBatchMode) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                isBatchMode = false
                                selectedPaths = emptySet()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel batch selection",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${selectedPaths.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    selectedPaths = if (selectedPaths.size == flatNodes.size) {
                                        emptySet()
                                    } else {
                                        flatNodes.map { it.file.absolutePath }.toSet()
                                    }
                                }
                            ) {
                                Text(
                                    text = if (selectedPaths.size == flatNodes.size && flatNodes.isNotEmpty()) "Deselect All" else "Select All",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search files...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { isBatchMode = true },
                        modifier = Modifier.testTag("batch_select_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Checklist,
                            contentDescription = "Batch Selection Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // File Tree List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = listState,
                contentPadding = PaddingValues(bottom = if (isBatchMode || pendingMode != null) 96.dp else 16.dp)
            ) {
                items(flatNodes, key = { it.file.absolutePath }) { node ->
                    val isSelected = selectedPaths.contains(node.file.absolutePath)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (isBatchMode) {
                                        selectedPaths = if (isSelected) {
                                            selectedPaths - node.file.absolutePath
                                        } else {
                                            selectedPaths + node.file.absolutePath
                                        }
                                    } else {
                                        if (node.file.isDirectory) {
                                            expandedPaths = if (expandedPaths.contains(node.file.absolutePath)) {
                                                expandedPaths - node.file.absolutePath
                                            } else {
                                                expandedPaths + node.file.absolutePath
                                            }
                                        } else {
                                            onFileClick(node.file)
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (!isBatchMode) {
                                        isBatchMode = true
                                        selectedPaths = setOf(node.file.absolutePath)
                                    } else {
                                        selectedPaths = if (isSelected) {
                                            selectedPaths - node.file.absolutePath
                                        } else {
                                            selectedPaths + node.file.absolutePath
                                        }
                                    }
                                }
                            )
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .padding(
                                start = (node.depth * 16 + 8).dp,
                                top = 8.dp,
                                bottom = 8.dp,
                                end = 12.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Checkbox when in batch mode
                        if (isBatchMode) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedPaths = if (checked) {
                                        selectedPaths + node.file.absolutePath
                                    } else {
                                        selectedPaths - node.file.absolutePath
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(end = 4.dp)
                            )
                        }

                        // Icon with folder expand click target
                        if (node.file.isDirectory) {
                            IconButton(
                                onClick = {
                                    expandedPaths = if (expandedPaths.contains(node.file.absolutePath)) {
                                        expandedPaths - node.file.absolutePath
                                    } else {
                                        expandedPaths + node.file.absolutePath
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (node.isExpanded || searchQuery.isNotEmpty()) Icons.Default.FolderOpen else Icons.Default.Folder,
                                    contentDescription = if (node.isExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(28.dp)
                                    .padding(4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = node.file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (node.file.isDirectory) FontWeight.Medium else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!node.file.isDirectory && node.file.length() > 0) {
                                val sizeStr = remember(node.file) { formatFileSize(node.file.length()) }
                                Text(
                                    text = sizeStr,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                if (flatNodes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matching files found" else "Empty directory",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Action Bar: In Batch Mode (Copy, Move, Delete)
        AnimatedVisibility(
            visible = isBatchMode && selectedPaths.isNotEmpty(),
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
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // COPY BUTTON
                    TextButton(
                        onClick = {
                            val files = selectedPaths.map { File(it) }.filter { it.exists() }
                            if (files.isNotEmpty()) {
                                pendingFiles = files
                                pendingMode = PendingBatchMode.COPY
                                isBatchMode = false
                                selectedPaths = emptySet()
                            }
                        },
                        modifier = Modifier.testTag("batch_copy_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Copy", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // MOVE BUTTON
                    TextButton(
                        onClick = {
                            val files = selectedPaths.map { File(it) }.filter { it.exists() }
                            if (files.isNotEmpty()) {
                                pendingFiles = files
                                pendingMode = PendingBatchMode.MOVE
                                isBatchMode = false
                                selectedPaths = emptySet()
                            }
                        },
                        modifier = Modifier.testTag("batch_move_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.DriveFileMove, contentDescription = "Move", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Move", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // DELETE BUTTON
                    TextButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("batch_delete_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Delete", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Bottom Action Bar: Pending Paste / Move Mode
        AnimatedVisibility(
            visible = pendingMode != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val mode = pendingMode
            val targetDir = selectedDestinationDir ?: rootDir

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (mode == PendingBatchMode.COPY) Icons.Outlined.ContentCopy else Icons.Outlined.DriveFileMove,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${if (mode == PendingBatchMode.COPY) "Copying" else "Moving"} ${pendingFiles.size} item(s)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Target: ${targetDir.name}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(onClick = {
                            pendingMode = null
                            pendingFiles = emptyList()
                            selectedDestinationDir = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { showFolderPickerDialog = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Change Folder")
                        }

                        Button(
                            onClick = {
                                val action = mode
                                val filesToProcess = pendingFiles
                                val dest = targetDir
                                coroutineScope.launch {
                                    if (action == PendingBatchMode.COPY) {
                                        if (viewModel != null) {
                                            viewModel.copyBatchFiles(filesToProcess, dest) {
                                                reloadTrigger++
                                            }
                                        } else {
                                            withContext(Dispatchers.IO) {
                                                BatchFileOperations.copyFiles(filesToProcess, dest)
                                            }
                                            reloadTrigger++
                                        }
                                    } else if (action == PendingBatchMode.MOVE) {
                                        if (viewModel != null) {
                                            viewModel.moveBatchFiles(filesToProcess, dest) {
                                                reloadTrigger++
                                            }
                                        } else {
                                            withContext(Dispatchers.IO) {
                                                BatchFileOperations.moveFiles(filesToProcess, dest)
                                            }
                                            reloadTrigger++
                                        }
                                    }
                                    pendingMode = null
                                    pendingFiles = emptyList()
                                    selectedDestinationDir = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("paste_confirm_button")
                        ) {
                            Text(if (mode == PendingBatchMode.COPY) "Paste Here" else "Move Here")
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        val count = selectedPaths.size
        val sampleNames = selectedPaths.take(3).map { File(it).name }
        val remainingCount = count - sampleNames.size

        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete $count item(s)?") },
            text = {
                Column {
                    Text("Are you sure you want to delete these files? This operation permanently deletes them from disk.")
                    Spacer(modifier = Modifier.height(12.dp))
                    sampleNames.forEach { name ->
                        Text("• $name", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (remainingCount > 0) {
                        Text("...and $remainingCount more", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val filesToDelete = selectedPaths.map { File(it) }.filter { it.exists() }
                        showDeleteConfirmDialog = false
                        isBatchMode = false
                        selectedPaths = emptySet()

                        coroutineScope.launch {
                            if (viewModel != null) {
                                viewModel.deleteBatchFiles(filesToDelete) {
                                    reloadTrigger++
                                }
                            } else {
                                withContext(Dispatchers.IO) {
                                    BatchFileOperations.deleteFiles(filesToDelete)
                                }
                                reloadTrigger++
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Folder Picker Dialog
    if (showFolderPickerDialog) {
        FolderPickerDialog(
            root = rootDir,
            onDismiss = { showFolderPickerDialog = false },
            onFolderSelected = { folder ->
                selectedDestinationDir = folder
                showFolderPickerDialog = false
            }
        )
    }
}

@Composable
private fun FolderPickerDialog(
    root: File,
    onDismiss: () -> Unit,
    onFolderSelected: (File) -> Unit
) {
    var currentDir by remember { mutableStateOf(root) }
    val folders = remember(currentDir) {
        currentDir.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.sortedBy { it.name.lowercase() } ?: emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Destination Folder") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                Text(
                    text = currentDir.absolutePath,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (currentDir.parentFile != null && currentDir != root) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentDir.parentFile?.let { currentDir = it } }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go up", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(".. (Parent Folder)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    HorizontalDivider()
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(folders) { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentDir = folder }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(folder.name, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (folders.isEmpty()) {
                        item {
                            Text("No subfolders here", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onFolderSelected(currentDir) }) {
                Text("Select This Folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
