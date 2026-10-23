package com.example.formats.text.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentFileEntity
import com.example.ui.component.formatFileSize
import com.example.ui.component.formatElapsedTime
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import androidx.compose.ui.semantics.contentDescription

@Composable
fun CodePreview(
    codeText: String,
    fileEntity: RecentFileEntity,
    fontSizeSetting: String,
    wrapSetting: Boolean,
    showLineNumbers: Boolean,
    modifier: Modifier = Modifier,
    scrollToLine: Int? = null,
    onScrollToLineHandled: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var isAllCopied by remember { mutableStateOf(false) }
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    LaunchedEffect(isAllCopied) {
        if (isAllCopied) {
            kotlinx.coroutines.delay(1800)
            isAllCopied = false
        }
    }

    Column(modifier = modifier) {
        // Tag Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
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

            TextButton(
                onClick = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(codeText))
                    isAllCopied = true
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isAllCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = "Copy Entire Code",
                        modifier = Modifier.size(14.dp),
                        tint = if (isAllCopied) Color(0xFF427A5B) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAllCopied) "Copied!" else "Copy All",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAllCopied) Color(0xFF427A5B) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "WebView Render",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val htmlContent = remember(codeText, isDark, fontSizeSetting, wrapSetting, showLineNumbers) {
                val escapedCode = codeText.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                val bgColor = if (isDark) "#1e1e1e" else "#ffffff"
                val textColor = if (isDark) "#d4d4d4" else "#000000"
                val fSize = when (fontSizeSetting) {
                    "Small" -> "12px"
                    "Large" -> "18px"
                    else -> "14px"
                }
                val wWrap = if (wrapSetting) "pre-wrap" else "pre"
                val wWord = if (wrapSetting) "break-word" else "normal"
                val theme = if (isDark) "vs2015.min.css" else "vs.min.css"

                val lineNumbersCss = if (showLineNumbers) """
                    .hljs {
                        counter-reset: linenumber;
                    }
                    .hljs-line {
                        display: block;
                        counter-increment: linenumber;
                    }
                    .hljs-line::before {
                        content: counter(linenumber);
                        display: inline-block;
                        width: 3em;
                        margin-right: 1em;
                        text-align: right;
                        color: #888;
                        user-select: none;
                    }
                """.trimIndent() else ""

                val jsLineNumbers = """
                    const codeBlock = document.querySelector('code');
                    if (codeBlock) {
                        const lines = codeBlock.innerHTML.split('\n');
                        codeBlock.innerHTML = lines.map((line, idx) => '<span id="line-' + (idx + 1) + '" class="hljs-line">' + line + '</span>').join('\n');
                    }
                    window.scrollToLine = function(lineNum) {
                        const el = document.getElementById('line-' + lineNum);
                        if (el) {
                            el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                            el.style.backgroundColor = 'rgba(229, 169, 60, 0.4)';
                            el.style.borderRadius = '4px';
                            setTimeout(() => { el.style.backgroundColor = ''; }, 2500);
                        }
                    };
                """.trimIndent()

                """
                <!DOCTYPE html>
                <html>
                <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=0.25, maximum-scale=5.0, user-scalable=yes">
                <style>
                  body {
                    background-color: $bgColor;
                    color: $textColor;
                    margin: 0; padding: 16px;
                    font-size: $fSize;
                  }
                  pre {
                    margin: 0;
                    white-space: $wWrap;
                    word-wrap: $wWord;
                    font-family: monospace;
                  }
                  $lineNumbersCss
                </style>
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/$theme">
                <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
                </head>
                <body>
                <pre><code class="language-${fileEntity.extension.lowercase()}">$escapedCode</code></pre>
                <script>
                  hljs.highlightAll();
                  $jsLineNumbers
                </script>
                </body>
                </html>
                """.trimIndent()
            }

            var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }

            LaunchedEffect(scrollToLine) {
                if (scrollToLine != null) {
                    webViewRef?.evaluateJavascript("if (typeof window.scrollToLine === 'function') { window.scrollToLine($scrollToLine); }", null)
                    onScrollToLineHandled()
                }
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        webViewRef = this
                        settings.javaScriptEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                update = { webView ->
                    webViewRef = webView
                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            )
        }
    }
}
