package com.example.formats.archive.ui

import com.example.core.archive.ArchiveNode
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentFileEntity
import com.example.ui.component.ClaudeCard
import com.example.ui.component.formatFileSize
import com.example.ui.component.formatElapsedTime
import java.io.File
import androidx.compose.ui.semantics.contentDescription

@Composable
fun ZipPreview(
    zipRoot: ArchiveNode,
    fileEntity: RecentFileEntity,
    expandedPaths: Set<String>,
    onToggleExpand: (String) -> Unit,
    onZipEntryClick: (ArchiveNode, String) -> Unit,
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

data class NodeWithDepth(val node: ArchiveNode, val depth: Int)

fun countTotalFiles(root: ArchiveNode): Int {
    var count = 0
    fun traverse(node: ArchiveNode) {
        if (!node.isDirectory) {
            count++
        }
        node.children.forEach { traverse(it) }
    }
    traverse(root)
    return count
}

fun buildNodesList(
    node: ArchiveNode,
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
