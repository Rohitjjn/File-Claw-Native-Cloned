package com.example.formats.ooxml

import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatOpenRequest
import com.example.domain.preview.FormatPrintPlan
import com.example.domain.preview.FormatPrintRequest
import com.example.formats.archive.ArchiveFormatHandler
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities

/**
 * Hand-off to the existing DOCX/PPTX WebView renderers.
 *
 * Both are now thin pass-throughs: [com.example.formats.docx.ui.DocxPreviewWebView] and
 * [com.example.formats.pptx.ui.PptxPreviewWebView] stream the file itself through
 * `WebViewAssetLoader` ("/docx-src/document.docx", "/pptx-src/presentation.pptx") and
 * never read this state's `base64Data` — both are always opened with the source argument
 * null/ignored. This handler used to read the whole file, Base64-encode it (~2.7x heap)
 * and park a multi-megabyte String here anyway, on every single open, for nothing; large
 * files were also turned away into the archive-tree view at a flat 25 MB even though the
 * streaming renderers handle files well beyond that (PPTX used to additionally pass that
 * giant string through a single `evaluateJavascript()` call, which is what made large
 * presentations the most likely of the two formats to fail outright -- see
 * PptxPreviewWebView.kt). Both formats now only confirm the file is readable and never
 * materialize the extra copy, so large DOCX/PPTX files open as fast as small ones and are
 * no longer size-limited here.
 */
class OoxmlDocumentHandler(
    private val archiveHandler: ArchiveFormatHandler
) : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = mapOf(
        "docx" to capabilities(FormatCapability.PREVIEW, FormatCapability.PRINT),
        "pptx" to capabilities(FormatCapability.PREVIEW, FormatCapability.PRINT)
    )
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState {
        if (!request.file.canRead() || request.file.length() <= 0L) {
            return archiveHandler.open(request)
        }
        return when (request.detection.spec.id) {
            "docx" -> FileContentState.DocxSuccess("", request.fileEntity)
            "pptx" -> FileContentState.PptxSuccess("", request.fileEntity)
            else -> FileContentState.Error("Unsupported OOXML document type.")
        }
    }

    override fun preparePrint(request: FormatPrintRequest): FormatPrintPlan =
        FormatPrintPlan.WebDocument(
            file = request.file,
            title = request.title,
            rendererId = if (request.mimeType.contains("presentation")) "pptx" else "docx"
        )

    companion object {
        val FORMAT_IDS = setOf("docx", "pptx")
    }
}
