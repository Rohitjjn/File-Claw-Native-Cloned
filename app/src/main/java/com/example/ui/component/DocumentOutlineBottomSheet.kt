package com.example.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Toc
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class HeadingItem(
    val level: Int,
    val title: String,
    val lineNumber: Int
)

object DocumentOutlineParser {
    /**
     * Parses document headings from markdown/text lines.
     * Recognizes #, ##, ###, ####, etc., or standard title underlines (===, ---).
     */
    fun parseHeadings(content: String): List<HeadingItem> {
        val lines = content.lines()
        val headings = mutableListOf<HeadingItem>()

        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#")) {
                val level = line.takeWhile { it == '#' }.length
                if (level in 1..6 && line.length > level && line[level] == ' ') {
                    val title = line.substring(level).trim()
                    if (title.isNotEmpty()) {
                        headings.add(HeadingItem(level = level, title = title, lineNumber = i + 1))
                    }
                }
            } else if (i + 1 < lines.size) {
                // Check setext-style underline
                val nextLine = lines[i + 1].trim()
                if (nextLine.length >= 3 && nextLine.all { it == '=' }) {
                    headings.add(HeadingItem(level = 1, title = line, lineNumber = i + 1))
                } else if (nextLine.length >= 3 && nextLine.all { it == '-' }) {
                    headings.add(HeadingItem(level = 2, title = line, lineNumber = i + 1))
                }
            }
        }
        return headings
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentOutlineBottomSheet(
    documentContent: String,
    documentTitle: String,
    onDismissRequest: () -> Unit,
    onSelectHeading: (lineNumber: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val headings = remember(documentContent) {
        DocumentOutlineParser.parseHeadings(documentContent)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        },
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.testTag("document_outline_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Toc,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Table of Contents",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = documentTitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close outline",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (headings.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "No Headings Found",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add markdown headings (# Heading 1, ## Heading 2) to generate an interactive outline.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Text(
                    text = "${headings.size} sections in document",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(headings) { item ->
                        val indentDp = ((item.level - 1) * 16).dp
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (item.level == 1) 0.5f else 0.25f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = if (item.level == 1) 0.35f else 0.15f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = indentDp)
                                .clickable {
                                    onSelectHeading(item.lineNumber)
                                    onDismissRequest()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when (item.level) {
                                            1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            2 -> Color(0xFFE5A93C).copy(alpha = 0.2f)
                                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                        }
                                    ) {
                                        Text(
                                            text = "H${item.level}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (item.level) {
                                                1 -> MaterialTheme.colorScheme.primary
                                                2 -> Color(0xFFE5A93C)
                                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            },
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = item.title,
                                        fontSize = if (item.level == 1) 14.sp else 13.sp,
                                        fontWeight = if (item.level == 1) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = "L${item.lineNumber}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
