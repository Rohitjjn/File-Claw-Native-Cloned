package com.example.formats.svg.ui

import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.RecentFileEntity
import com.example.formats.common.ui.formatHumanBytes
import com.example.platform.webview.SafeWebViewFactory
import com.example.ui.component.NoticeBanner
import java.io.File

@Composable
fun SvgViewer(fileEntity: RecentFileEntity, modifier: Modifier = Modifier) {
    val maxBytes = 5L * 1024 * 1024
    val file = File(fileEntity.path)
    if (file.length() > maxBytes) {
        NoticeBanner("This SVG is larger than ${formatHumanBytes(maxBytes)} and was not rendered.", modifier)
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
        factory = { ctx -> SafeWebViewFactory.create(ctx) },
        update = { it.loadDataWithBaseURL(null, page, "text/html", "UTF-8", null) }
    )
}
