package com.example.formats.markdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.RecentFileEntity
import java.io.File
import com.example.ui.component.ViewerSyntaxTheme
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.example.feature.preview.model.ViewerScrollRequest
import com.example.formats.common.ui.DocumentPreviewScript
import com.example.formats.common.ui.SearchableDocumentWebView

@Composable
fun MarkdownPreview(
    markdownText: String,
    fileEntity: RecentFileEntity,
    fontSizeSetting: String,
    isNightMode: Boolean,
    searchQuery: String = "",
    searchTriggerNext: Int = 0,
    searchTriggerPrev: Int = 0,
    onSearchResultCount: (Int, Int, Boolean) -> Unit = { _, _, _ -> },
    onSingleTap: () -> Unit = {},
    syntaxTheme: ViewerSyntaxTheme = ViewerSyntaxTheme.AUTO,
    viewerFontSizeSp: Int = 16,
    zoomPercent: Int = 100,
    scrollRequest: ViewerScrollRequest? = null,
    onScrollRequestHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var generatedHtml by remember(markdownText, isNightMode, viewerFontSizeSp, syntaxTheme) { mutableStateOf<String?>(null) }

    LaunchedEffect(markdownText, isNightMode, viewerFontSizeSp, syntaxTheme) {
        val htmlContent = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val parser = org.intellij.markdown.parser.MarkdownParser(org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor())
            val parsedTree = parser.buildMarkdownTreeFromString(markdownText)
            val rawHtml = com.example.core.security.HtmlSanitizer.sanitize(
                org.intellij.markdown.html.HtmlGenerator(markdownText, parsedTree, org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor()).generateHtml(),
                keepImages = true
            )

            // Viewer Appearance drives the base font size; falls back to the settings mapping.
            val fontSize = "${viewerFontSizeSp.coerceIn(11, 22)}px"

            // Claude Theme Colors (EXACT from File Claw - MD Preview Theme Fix Prompt)
            // A chosen syntax theme overrides the base canvas/ink/accent tokens; AUTO keeps
            // the light/dark palette derived from the app theme.
            val useThemeOverride = !syntaxTheme.isAuto
            val bgColor = if (useThemeOverride) syntaxTheme.hexBg else if (isNightMode) "#151514" else "#faf9f5" // canvas
            val textPrimary = if (useThemeOverride) syntaxTheme.hexText else if (isNightMode) "#f5f5f3" else "#141413" // ink
            val textBody = if (useThemeOverride) syntaxTheme.hexText else if (isNightMode) "#d1cfc7" else "#3d3d3a" // body
            val textBodyStrong = if (useThemeOverride) syntaxTheme.hexText else if (isNightMode) "#faf9f5" else "#252523" // body_strong
            val textSecondary = if (useThemeOverride) syntaxTheme.hexText else if (isNightMode) "#a39f93" else "#6c6a64" // muted
            val textSecondarySoft = if (useThemeOverride) syntaxTheme.hexText else if (isNightMode) "#8e8779" else "#8e8b82" // muted_soft
            val accent = if (useThemeOverride) syntaxTheme.hexAccent else if (isNightMode) "#e08567" else "#cc785c" // primary (coral)
            val accentActive = if (useThemeOverride) syntaxTheme.hexAccent else if (isNightMode) "#f09a7d" else "#a9583e" // primary_active
            val border = if (isNightMode) "#2d2b28" else "#e6dfd8" // hairline
            val borderSoft = if (isNightMode) "#211f1d" else "#ebe6df" // hairline_soft
            val cardBg = if (isNightMode) "#2a2723" else "#efe9de" // surface_card
            val quoteBg = if (isNightMode) "#1c1a18" else "#f5f0e8" // surface_soft
            val inlineCodeBg = if (isNightMode) "#2d2823" else "#e8e0d2" // surface_cream_strong
            val codeBlockOuterBg = if (isNightMode) "#0d0d0c" else "#181715" // surface_dark
            val codeBlockInnerBg = if (isNightMode) "#161513" else "#1f1e1b" // surface_dark_soft
            val codeBlockHeaderBg = if (isNightMode) "#1d1b19" else "#252320" // surface_dark_elevated
            val codeBlockText = if (isNightMode) "#f5f5f3" else "#faf9f5" // on_dark
            val codeBlockButtons = if (isNightMode) "#a09d96" else "#8e8b82" // on_dark_soft

            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=0.25, maximum-scale=5.0, user-scalable=yes">
                <style>
                    :root {
                        --bg-color: $bgColor;
                        --text-primary: $textPrimary;
                        --text-body: $textBody;
                        --text-body-strong: $textBodyStrong;
                        --text-secondary: $textSecondary;
                        --text-secondary-soft: $textSecondarySoft;
                        --accent: $accent;
                        --accent-active: $accentActive;
                        --border: $border;
                        --border-soft: $borderSoft;
                        --card-bg: $cardBg;
                        --quote-bg: $quoteBg;
                        --inline-code-bg: $inlineCodeBg;
                        --code-block-outer: $codeBlockOuterBg;
                        --code-block-inner: $codeBlockInnerBg;
                        --code-block-header: $codeBlockHeaderBg;
                        --code-text: $codeBlockText;
                        --code-buttons: $codeBlockButtons;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        background-color: var(--bg-color);
                        color: var(--text-body);
                        font-size: $fontSize;
                        line-height: 1.55;
                        padding: 16px;
                        margin: 0;
                        word-wrap: break-word;
                    }
                    h1, h2, h3, h4, h5, h6 {
                        color: var(--text-primary);
                        margin-top: 24px;
                        margin-bottom: 16px;
                        font-weight: 700;
                    }
                    h1 {
                        font-size: 28px;
                        border-bottom: 2px solid var(--border);
                        padding-bottom: 8px;
                    }
                    h2 { font-size: 22px; }
                    h3 { font-size: 18px; font-weight: 600; }
                    p {
                        margin-top: 0;
                        margin-bottom: 16px;
                    }
                    li {
                        margin: 8px 0;
                    }
                    ul, ol {
                        padding-left: 24px;
                    }
                    a {
                        color: var(--accent);
                        text-decoration: none;
                    }
                    a:hover {
                        color: var(--accent-active);
                        text-decoration: underline;
                    }
                    code {
                        font-family: "JetBrains Mono", ui-monospace, Consolas, Monaco, monospace;
                        background-color: var(--inline-code-bg);
                        color: var(--accent);
                        padding: 2px 6px;
                        border-radius: 4px;
                        font-size: 14px;
                    }
                    pre {
                        background-color: transparent;
                        margin: 0;
                        padding: 0;
                        overflow: visible;
                    }
                    .code-block-container {
                        background-color: var(--code-block-outer);
                        border-radius: 12px;
                        margin-bottom: 16px;
                        overflow: hidden;
                        border: 1px solid var(--border);
                    }
                    .code-header {
                        background-color: var(--code-block-header);
                        padding: 8px 16px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                    }
                    .code-lang {
                        color: var(--text-secondary-soft);
                        font-size: 12px;
                        font-family: monospace;
                        text-transform: uppercase;
                    }
                    .copy-btn {
                        background: transparent;
                        border: 1px solid var(--code-buttons);
                        color: var(--text-secondary-soft);
                        padding: 4px 12px;
                        border-radius: 6px;
                        font-size: 12px;
                        cursor: pointer;
                        transition: background 0.2s;
                    }
                    .copy-btn:hover {
                        background: rgba(255, 255, 255, 0.1);
                    }
                    .code-content {
                        background-color: var(--code-block-inner);
                        padding: 16px;
                        overflow-x: auto;
                    }
                    .code-content code {
                        background-color: transparent;
                        color: var(--code-text);
                        padding: 0;
                        font-size: 14px;
                        line-height: 1.6;
                    }
                    blockquote {
                        margin: 0 0 16px 0;
                        padding: 12px 20px;
                        color: var(--text-body);
                        border-left: 4px solid var(--accent);
                        background-color: var(--quote-bg);
                        border-radius: 0 8px 8px 0;
                    }
                    table {
                        border-collapse: collapse;
                        width: 100%;
                        margin-bottom: 16px;
                        display: block;
                        overflow-x: auto;
                        border: 1px solid var(--border);
                    }
                    th, td {
                        border: 1px solid var(--border);
                        padding: 12px;
                    }
                    th {
                        background-color: var(--quote-bg);
                        font-weight: 600;
                        color: var(--text-body-strong);
                    }
                    tr:nth-child(even) {
                        background-color: var(--bg-color);
                    }
                    tr:nth-child(odd) {
                        background-color: var(--bg-color);
                    }
                    hr {
                        border: 0;
                        border-top: 2px solid var(--border);
                        margin: 24px 0;
                    }
                    img {
                        max-width: 100%;
                        border-radius: 8px;
                    }
                    .search-highlight { background: rgba(204, 120, 92, 0.32); color: inherit; border-radius: 2px; }
                    .search-highlight-active { background: #cc785c; color: #ffffff; outline: 2px solid rgba(232, 165, 90, 0.9); }
                    .line-anchor-flash { background: rgba(204, 120, 92, 0.32); color: inherit; border-radius: 2px; outline: 2px solid #cc785c; transition: background 0.4s ease; }
                    @media print { .code-header, .copy-btn { display: none !important; } .code-block-container { break-inside: avoid; } }
                </style>
                $DocumentPreviewScript
            </head>
            <body>
                <main id="document-content">$rawHtml</main>
            </body>
            </html>
            """.trimIndent()
        }
        generatedHtml = htmlContent
    }

    SearchableDocumentWebView(
        html = generatedHtml,
        backgroundColor = if (syntaxTheme.isAuto) {
            if (isNightMode) 0xFF151514.toInt() else 0xFFfaf9f5.toInt()
        } else {
            runCatching {
                android.graphics.Color.parseColor(syntaxTheme.hexBg)
            }.getOrDefault(if (isNightMode) 0xFF151514.toInt() else 0xFFfaf9f5.toInt())
        },
        searchQuery = searchQuery,
        searchTriggerNext = searchTriggerNext,
        searchTriggerPrev = searchTriggerPrev,
        onSearchResultCount = onSearchResultCount,
        onSingleTap = onSingleTap,
        zoomPercent = zoomPercent,
        scrollRequest = scrollRequest,
        onScrollRequestHandled = onScrollRequestHandled,
        loadingMessage = "Preparing Document...",
        modifier = modifier
    )
}
