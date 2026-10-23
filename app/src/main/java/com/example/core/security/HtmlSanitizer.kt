package com.example.core.security

/**
 * Removes active content from untrusted HTML (Markdown raw HTML, EPUB chapters) before it is shown
 * in a WebView that has JavaScript enabled. This is defence in depth on top of the manifest, which
 * declares no INTERNET permission, so nothing can be exfiltrated even if something slips through.
 */
object HtmlSanitizer {

    private val dotAll = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)

    private val pairedActive = Regex("<(script|style|iframe|object|embed|svg|audio|video|form|math)[^>]*>.*?</\\1\\s*>", dotAll)
    private val strayActive = Regex("<(script|style|iframe|object|embed|link|meta|base|form|input|button|source|frame|frameset)[^>]*/?>", RegexOption.IGNORE_CASE)
    private val closeStray = Regex("</(script|style|iframe|object|embed|form|svg|audio|video)\\s*>", RegexOption.IGNORE_CASE)
    private val imageTags = Regex("<(img|image)[^>]*/?>", RegexOption.IGNORE_CASE)
    private val eventHandlers = Regex("\\son[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", RegexOption.IGNORE_CASE)
    private val dangerousUrlStrict = Regex("(href|src|xlink:href|action|formaction)\\s*=\\s*(\"\\s*(javascript|data|vbscript):[^\"]*\"|'\\s*(javascript|data|vbscript):[^']*')", RegexOption.IGNORE_CASE)
    private val dangerousUrlKeepImages = Regex("(href|src|xlink:href|action|formaction)\\s*=\\s*(\"\\s*(javascript|vbscript):[^\"]*\"|'\\s*(javascript|vbscript):[^']*'|\"\\s*data:(?!image/(png|jpe?g|gif|webp)[;,])[^\"]*\"|'\\s*data:(?!image/(png|jpe?g|gif|webp)[;,])[^']*')", RegexOption.IGNORE_CASE)

    /** @param keepImages keep img tags and safe data-URI image sources; EPUB uses false, Markdown true. */
    fun sanitize(html: String, keepImages: Boolean = false): String {
        var s = html
        s = s.replace(pairedActive, "")
        s = s.replace(strayActive, "")
        s = s.replace(closeStray, "")
        if (!keepImages) s = s.replace(imageTags, "")
        s = s.replace(eventHandlers, "")
        s = s.replace(if (keepImages) dangerousUrlKeepImages else dangerousUrlStrict, "")
        return s
    }
}
