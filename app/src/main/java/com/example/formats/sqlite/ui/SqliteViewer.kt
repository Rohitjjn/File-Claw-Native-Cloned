package com.example.formats.sqlite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import kotlin.math.max
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentFileEntity
import com.example.formats.sqlite.SqliteBrowser
import com.example.formats.sqlite.SqlitePage
import com.example.ui.component.NoticeBanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SqliteViewer(fileEntity: RecentFileEntity, modifier: Modifier = Modifier) {
    val browser = remember(fileEntity.path) { runCatching { SqliteBrowser(fileEntity.path) } }
    DisposableEffect(browser) { onDispose { browser.getOrNull()?.close() } }

    var tables by remember { mutableStateOf<List<String>>(emptyList()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf(0) }
    var data by remember { mutableStateOf<SqlitePage?>(null) }
    var error by remember { mutableStateOf<String?>(browser.exceptionOrNull()?.let { "Not a readable SQLite database" }) }

    LaunchedEffect(browser) {
        val b = browser.getOrNull() ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val t = b.tables()
                tables = t
                selected = t.firstOrNull()
                if (t.isEmpty()) error = "This database has no tables."
            } catch (e: Exception) {
                error = "Could not read the database schema."
            }
        }
    }

    LaunchedEffect(selected, page) {
        val b = browser.getOrNull() ?: return@LaunchedEffect
        val table = selected ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                data = b.page(table, page * SqliteBrowser.PAGE_SIZE, SqliteBrowser.PAGE_SIZE)
            } catch (e: Exception) {
                error = "Could not read table \"$table\"."
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (error != null) {
            NoticeBanner(error!!)
        }
        if (tables.isNotEmpty()) {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tables) { name ->
                    val active = name == selected
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.clickable {
                            if (!active) {
                                selected = name
                                page = 0
                                data = null
                            }
                        }
                    ) {
                        Text(
                            text = name,
                            fontSize = 13.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        val d = data
        if (d != null) {
            val pages = max(1L, (d.totalRows + SqliteBrowser.PAGE_SIZE - 1) / SqliteBrowser.PAGE_SIZE)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "${d.totalRows} rows \u2022 ${d.columns.size} columns \u2022 page ${page + 1} of $pages",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { if (page > 0) page-- }, enabled = page > 0) { Text("Prev") }
                TextButton(onClick = { if (page + 1 < pages) page++ }, enabled = page + 1 < pages) { Text("Next") }
            }
            val cellWidth = 150.dp
            val hScroll = rememberScrollState()
            Box(modifier = Modifier.weight(1f).fillMaxWidth().horizontalScroll(hScroll)) {
                LazyColumn(modifier = Modifier.width(cellWidth * d.columns.size.coerceAtLeast(1))) {
                    item {
                        Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))) {
                            for (col in d.columns) {
                                Text(
                                    text = col,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(cellWidth).padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    items(d.rows) { row ->
                        Row {
                            for (cell in row) {
                                Text(
                                    text = cell,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (cell == "NULL") MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.width(cellWidth).padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
