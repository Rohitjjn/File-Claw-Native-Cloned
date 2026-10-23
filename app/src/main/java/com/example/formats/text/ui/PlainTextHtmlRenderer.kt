package com.example.formats.text.ui

import com.example.core.security.HtmlEscaping
import com.example.formats.common.ui.DocumentPreviewScript
import com.example.formats.markdown.FencedCodeParser
import com.example.formats.markdown.FencedCodeSegment

/** Makes a safe HTML rendering of a TXT file while keeping fenced code copyable as separate blocks. */
internal fun buildPlainTextPreviewHtml(
    content: String,
    isDark: Boolean,
    fontSizePx: Int,
    themeBgHex: String? = null,
    themeFgHex: String? = null
): String {
    val background = themeBgHex ?: if (isDark) "#151514" else "#faf9f5"
    val foreground = themeFgHex ?: if (isDark) "#d1cfc7" else "#3d3d3a"
    val codeBackground = if (isDark) "#1f1e1b" else "#efe9de"
    val codeHeader = if (isDark) "#252320" else "#e8e0d2"
    val border = if (isDark) "#383530" else "#e6dfd8"
    val buttonColor = if (isDark) "#c7c2b8" else "#6c6a64"

    val body = FencedCodeParser.split(content).joinToString(separator = "") { segment ->
        when (segment) {
            is FencedCodeSegment.Plain -> "<pre class=\"plain-text\">${HtmlEscaping.escape(segment.text)}</pre>"
            is FencedCodeSegment.Code -> {
                val safeLanguage = segment.language
                    .takeIf { it.isNotBlank() }
                    ?.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
                    ?.take(32)
                    .orEmpty()
                val languageClass = if (safeLanguage.isNotEmpty()) " language-$safeLanguage" else ""
                "<pre><code class=\"$languageClass\">${HtmlEscaping.escape(segment.text)}</code></pre>"
            }
        }
    }

    return """
        <!doctype html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1, minimum-scale=0.25, maximum-scale=5, user-scalable=yes">
          <style>
            * { box-sizing: border-box; }
            html, body { min-height: 100%; margin: 0; background: $background; color: $foreground; }
            body { padding: 16px; font: ${fontSizePx}px/1.6 ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; overflow-wrap: anywhere; }
            #document-content { width: 100%; }
            pre { margin: 0; padding: 0; white-space: pre-wrap; overflow-wrap: anywhere; }
            .plain-text { margin-bottom: 12px; }
            .code-block-container { margin: 0 0 16px; border: 1px solid $border; border-radius: 12px; overflow: hidden; background: $codeBackground; }
            .code-header { min-height: 40px; padding: 6px 12px; display: flex; align-items: center; justify-content: space-between; gap: 12px; background: $codeHeader; }
            .code-lang { color: $buttonColor; font-size: 11px; font-weight: 700; letter-spacing: .06em; text-transform: uppercase; }
            .copy-btn { padding: 6px 10px; border: 1px solid $border; border-radius: 8px; background: transparent; color: $buttonColor; font: 600 12px sans-serif; }
            .code-content { overflow-x: auto; padding: 14px; background: $codeBackground; }
            .code-content pre { white-space: pre; overflow-wrap: normal; }
            .search-highlight { background: rgba(204, 120, 92, 0.32); color: inherit; border-radius: 2px; }
            .search-highlight-active { background: #cc785c; color: #ffffff; outline: 2px solid rgba(232, 165, 90, 0.9); }
            .line-anchor-flash { background: rgba(204, 120, 92, 0.32); color: inherit; border-radius: 2px; outline: 2px solid #cc785c; transition: background 0.4s ease; }
            @media print { .code-header, .copy-btn { display: none !important; } .code-block-container { break-inside: avoid; } }
          </style>
          $DocumentPreviewScript
        </head>
        <body><main id="document-content">$body</main></body>
        </html>
    """.trimIndent()
}
