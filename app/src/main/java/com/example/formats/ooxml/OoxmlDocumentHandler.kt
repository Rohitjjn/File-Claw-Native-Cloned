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
 * Both viewers stream the original package through the fixed `/docx-src/` or
 * `/pptx-src/` asset-loader routes; this state never carries a second Base64
 * copy. Before either renderer starts, the shared OOXML inspector validates the
 * untrusted package and identifies macro/template variants so they can be shown
 * as an honest read-only preview rather than an editable document.
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
        val inspection = try {
            OoxmlPackageInspector.inspect(request.file)
        } catch (error: OoxmlPackageInspector.InvalidPackageException) {
            return FileContentState.Error(error.message ?: "This Office file could not be opened.")
        }

        val formatId = request.detection.spec.id
        val expected = OoxmlPackageInspector.expectedFamily(formatId)
        if (expected == null || expected != inspection.family) {
            return FileContentState.Error("The file content does not match its Office document type.")
        }

        return when (formatId) {
            "docx" -> FileContentState.DocxSuccess("", request.fileEntity, inspection.notice)
            "pptx" -> FileContentState.PptxSuccess("", request.fileEntity, inspection.notice)
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
