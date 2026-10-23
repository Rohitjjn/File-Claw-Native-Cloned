package com.example.ui.screens

import android.graphics.Typeface
import android.util.Base64
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.core.model3d.Mesh
import com.example.data.RecentFileEntity
import com.example.services.SqliteBrowser
import com.example.services.SqlitePage
import java.io.File
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun humanSize(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024L * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

/** One-line banner for read-only / truncated / converted views. */
@Composable
fun NoticeBanner(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// SQLite
// ---------------------------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------------------------
// Fonts (TTF / OTF / TTC) via the platform Typeface API
// ---------------------------------------------------------------------------------------------

@Composable
fun FontViewer(fileEntity: RecentFileEntity, modifier: Modifier = Modifier) {
    val textColor = MaterialTheme.colorScheme.onBackground.toArgb()
    val face = remember(fileEntity.path) { runCatching { Typeface.createFromFile(File(fileEntity.path)) }.getOrNull() }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            text = "${fileEntity.name} \u2022 ${humanSize(fileEntity.size)}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        if (face == null) {
            NoticeBanner("This font file could not be loaded.", Modifier.padding(top = 12.dp))
            return@Column
        }
        val samples = listOf(
            12f to "The quick brown fox jumps over the lazy dog",
            18f to "Sphinx of black quartz, judge my vow",
            28f to "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            28f to "abcdefghijklmnopqrstuvwxyz",
            28f to "0123456789 !?&@#%()[]{}",
            48f to "Aa Bb Cc",
            72f to "Gg"
        )
        AndroidView(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            factory = { ctx ->
                LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    for ((size, sample) in samples) {
                        addView(TextView(ctx).apply {
                            text = sample
                            textSize = size
                            setTextColor(textColor)
                            typeface = face
                            setPadding(0, 8, 0, 8)
                        })
                    }
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// 3D wireframe (STL / OBJ)
// ---------------------------------------------------------------------------------------------

@Composable
fun Model3dViewer(mesh: Mesh, label: String, modifier: Modifier = Modifier) {
    var rotX by remember { mutableStateOf(-0.5f) }
    var rotY by remember { mutableStateOf(0.6f) }
    var zoom by remember { mutableStateOf(1f) }
    val bounds = remember(mesh) { mesh.bounds() }
    val wire = MaterialTheme.colorScheme.primary
    val maxDrawn = 25_000
    val stride = remember(mesh) { max(1, mesh.triangleCount / maxDrawn) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, z, _ ->
                    rotY += pan.x * 0.01f
                    rotX += pan.y * 0.01f
                    zoom = (zoom * z).coerceIn(0.2f, 10f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = (bounds[0] + bounds[3]) / 2f
            val cy = (bounds[1] + bounds[4]) / 2f
            val cz = (bounds[2] + bounds[5]) / 2f
            val extent = max(bounds[3] - bounds[0], max(bounds[4] - bounds[1], bounds[5] - bounds[2])).coerceAtLeast(1e-6f)
            val scale = (minOf(size.width, size.height) / extent) * 0.8f * zoom
            val sx = sin(rotX); val cxr = cos(rotX)
            val sy = sin(rotY); val cyr = cos(rotY)
            val mid = Offset(size.width / 2f, size.height / 2f)
            val p = mesh.positions
            val path = Path()

            fun project(i: Int): Offset {
                val x = p[i] - cx
                val y = p[i + 1] - cy
                val z = p[i + 2] - cz
                // rotate around Y, then X
                val x1 = x * cyr + z * sy
                val z1 = -x * sy + z * cyr
                val y1 = y * cxr - z1 * sx
                return Offset(mid.x + x1 * scale, mid.y - y1 * scale)
            }

            var t = 0
            while (t < mesh.triangleCount) {
                val b = t * 9
                val a = project(b)
                val c = project(b + 3)
                val d = project(b + 6)
                path.moveTo(a.x, a.y)
                path.lineTo(c.x, c.y)
                path.lineTo(d.x, d.y)
                path.close()
                t += stride
            }
            drawPath(path, wire.copy(alpha = 0.85f), style = Stroke(width = 1f))
        }
        Text(
            text = buildString {
                append(label).append(" \u2022 ").append(mesh.totalTriangles).append(" triangles")
                if (stride > 1 || mesh.truncated) append(" (showing a sample)")
                append("\nDrag to rotate \u2022 pinch to zoom")
            },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// SVG and generated-HTML (EPUB) viewers: JavaScript off, no file or content access
// ---------------------------------------------------------------------------------------------

private fun hardenedWebView(ctx: android.content.Context): WebView = WebView(ctx).apply {
    settings.javaScriptEnabled = false
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.builtInZoomControls = true
    settings.displayZoomControls = false
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    webViewClient = object : android.webkit.WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean = true
    }
}

@Composable
fun SvgViewer(fileEntity: RecentFileEntity, modifier: Modifier = Modifier) {
    val maxBytes = 5L * 1024 * 1024
    val file = File(fileEntity.path)
    if (file.length() > maxBytes) {
        NoticeBanner("This SVG is larger than ${humanSize(maxBytes)} and was not rendered.", modifier)
        return
    }
    val isDark = MaterialTheme.colorScheme.background.let { it.red + it.green + it.blue < 1.5f }
    val page = remember(fileEntity.path, isDark) {
        // An <img> never runs scripts embedded in the SVG, unlike inlining the markup.
        val b64 = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        val bg = if (isDark) "#22201d" else "#ffffff"
        "<html><body style=\"margin:0;background:$bg;display:flex;align-items:center;justify-content:center;min-height:100vh\">" +
            "<img style=\"max-width:100%;max-height:100vh\" src=\"data:image/svg+xml;base64,$b64\"/></body></html>"
    }
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx -> hardenedWebView(ctx) },
        update = { it.loadDataWithBaseURL(null, page, "text/html", "UTF-8", null) }
    )
}

@Composable
fun HtmlStringViewer(html: String, modifier: Modifier = Modifier) {
    val isDark = MaterialTheme.colorScheme.background.let { it.red + it.green + it.blue < 1.5f }
    val page = remember(html, isDark) {
        val bg = if (isDark) "#181715" else "#faf9f5"
        val fg = if (isDark) "#ece9e2" else "#1f1e1d"
        "<html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><style>" +
            "body{background:$bg;color:$fg;font-family:serif;line-height:1.65;font-size:17px;max-width:42em;margin:0 auto;padding:16px}" +
            "h1,h2,h3{font-family:sans-serif;line-height:1.25}hr{border:0;border-top:1px solid #8884;margin:2em 0}a{color:inherit}" +
            "</style></head><body>$html</body></html>"
    }
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx -> hardenedWebView(ctx) },
        update = { it.loadDataWithBaseURL(null, page, "text/html", "UTF-8", null) }
    )
}
