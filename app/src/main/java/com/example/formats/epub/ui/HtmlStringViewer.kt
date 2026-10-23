package com.example.formats.epub.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.viewinterop.AndroidView
import com.example.platform.webview.SafeWebViewFactory

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
        factory = { ctx -> SafeWebViewFactory.create(ctx) },
        update = { it.loadDataWithBaseURL(null, page, "text/html", "UTF-8", null) }
    )
}
