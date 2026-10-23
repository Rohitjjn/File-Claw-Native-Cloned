package com.example.platform.webview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalWebViewMimeTypesTest {
    @Test
    fun javascriptAssetsUseTheStandardMimeType() {
        assertEquals("text/javascript", LocalWebViewMimeTypes.overrideForAsset("editor/docx-preview.min.js"))
        assertEquals("text/javascript", LocalWebViewMimeTypes.overrideForAsset("pdfjs/pdf.worker.min.mjs"))
    }

    @Test
    fun scriptExtensionMatchingIsCaseInsensitive() {
        assertEquals("text/javascript", LocalWebViewMimeTypes.overrideForAsset("pdfjs/PDF.MIN.MJS"))
    }

    @Test
    fun nonJavaScriptAssetsKeepTheDefaultMimeType() {
        assertNull(LocalWebViewMimeTypes.overrideForAsset("pdfjs/cmaps/UniJIS-UTF16-H.cmap"))
        assertNull(LocalWebViewMimeTypes.overrideForAsset("editor/style.css"))
    }
}
