package com.example.core.security

/** Escapes untrusted plain text before embedding it in generated HTML. */
object HtmlEscaping {
    fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
