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

/** Bounded base64 hand-off to the existing DOCX/PPTX WebView renderers. */
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
        if (request.file.length() > MAX_WEB_DOCUMENT_BYTES) return archiveHandler.open(request)
        val encoded = request.file.inputStream().buffered().use { input ->
            Base64.encodeToString(input.readBytes(), Base64.NO_WRAP)
        }
        return when (request.detection.spec.id) {
            "docx" -> FileContentState.DocxSuccess(encoded, request.fileEntity)
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
