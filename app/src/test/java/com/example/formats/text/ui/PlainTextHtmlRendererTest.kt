package com.example.formats.text.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlainTextHtmlRendererTest {
    @Test
    fun plainTextPreviewKeepsFencedCodeCopyableAndEscapesMarkup() {
        val html = buildPlainTextPreviewHtml(
            "before\n```kotlin\nprintln(\"<safe>\")\n```\nafter",
            isDark = false,
            fontSizePx = 15
        )

        assertTrue(html.contains("<pre class=\"plain-text\">before\n</pre>"))
        assertTrue(html.contains("<pre><code class=\" language-kotlin\">"))
        assertTrue(html.contains("println(&quot;&lt;safe&gt;&quot;)"))
        assertTrue(html.contains("Copy code"))
        assertTrue(html.contains("id=\"document-content\""))
        assertFalse(html.contains("</safe>"))
    }

    @Test
    fun plainTextPreviewDoesNotCreateCodeCopyButtonForUnfencedText() {
        val html = buildPlainTextPreviewHtml("ordinary text\nno fenced blocks", isDark = true, fontSizePx = 15)

        assertTrue(html.contains("<pre class=\"plain-text\">ordinary text\nno fenced blocks</pre>"))
        assertFalse(html.contains("<pre><code"))
    }
}
