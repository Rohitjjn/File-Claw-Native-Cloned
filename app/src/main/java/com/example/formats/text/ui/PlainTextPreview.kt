package com.example.formats.text.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.component.ViewerSyntaxTheme
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.example.feature.preview.model.ViewerScrollRequest
import com.example.formats.common.ui.SearchableDocumentWebView

@Composable
fun PlainTextPreview(
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
