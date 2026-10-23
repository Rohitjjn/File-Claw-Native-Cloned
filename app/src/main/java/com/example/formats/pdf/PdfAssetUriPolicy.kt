package com.example.formats.pdf

import android.net.Uri

/** Accepts only the bundled PDF.js URLs and the single in-app PDF byte endpoint. */
object PdfAssetUriPolicy {
    fun isBundledReaderUri(uri: Uri): Boolean {
        val path = uri.path.orEmpty()
        return uri.scheme == "https" &&
            uri.host == "appassets.androidplatform.net" &&
            uri.port in setOf(-1, 443) &&
            uri.userInfo == null &&
            (path.startsWith("/assets/pdfjs/") || path == "/search-pdf/document.pdf")
    }
}
