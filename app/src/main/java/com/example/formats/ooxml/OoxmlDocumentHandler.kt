package com.example.formats.ooxml

import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatOpenRequest
import com.example.domain.preview.FormatPrintPlan
import com.example.domain.preview.FormatPrintRequest
import com.example.formats.archive.ArchiveFormatHandler
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities
import android.util.Base64

/**
 * Hand-off to the existing DOCX/PPTX WebView renderers.
 *
 * DOCX is a thin pass-through: [com.example.formats.docx.ui.DocxPreviewWebView] streams
 * the file itself through `WebViewAssetLoader` ("/docx-src/document.docx") and never
 * reads this state's `base64Data` — it is always opened with `loadDocx(null, ...)`. This
 * handler used to read the whole file, Base64-encode it (~2.7x heap) and park a
 * multi-megabyte String here anyway, on every single open, for nothing; large files
 * were also turned away into the archive-tree view at a flat 25 MB even though the
 * streaming renderer handles files well beyond that. DOCX now only confirms the file
 * is readable and never materializes the extra copy, so big DOCX files open as fast as
 * small ones and are no longer size-limited here.
 *
 * PPTX still needs the base64 string (its renderer receives the file over the
 * JavaScript bridge), so it keeps the bounded, size-capped path.
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
        if (request.detection.spec.id == "docx") {
            if (!request.file.canRead() || request.file.length() <= 0L) {
                return archiveHandler.open(request)
            }
            return FileContentState.DocxSuccess("", request.fileEntity)
        }
        if (request.file.length() > MAX_WEB_DOCUMENT_BYTES) return archiveHandler.open(request)
        val encoded = request.file.inputStream().buffered().use { input ->
            Base64.encodeToString(input.readBytes(), Base64.NO_WRAP)
        }
        return when (request.detection.spec.id) {
            "pptx" -> FileContentState.PptxSuccess(encoded, request.fileEntity)
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
        const val MAX_WEB_DOCUMENT_BYTES = 25L * 1024 * 1024
        val FORMAT_IDS = setOf("docx", "pptx")
    }
}
