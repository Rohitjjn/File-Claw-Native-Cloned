package com.example.formats.legacyoffice

import com.example.core.format.FormatRegistry
import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatOpenRequest
import com.example.formats.binary.BinaryFormatHandler
import com.example.formats.doc.LegacyDocExtractor
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities
import com.example.formats.xls.Biff8XlsParser

/** Legacy OLE2 Office support: bounded identity plus existing XLS/DOC extraction only. */
class LegacyOfficeFormatHandler(
    private val binaryHandler: BinaryFormatHandler
) : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = mapOf(
        "ole2" to capabilities(
            FormatCapability.PREVIEW,
            readOnlyReason = "Legacy Office content is shown as a read-only extraction."
        )
    )
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState {
        return when (FormatRegistry.extensionOf(request.file.name).ifEmpty { request.fileEntity.extension.lowercase() }) {
            "xls" -> {
                val parsed = runCatching { Biff8XlsParser.parse(request.file) }.getOrNull()
                if (parsed != null && parsed.primarySheetRows.isNotEmpty()) {
                    val sheetLabel = parsed.sheetNames.firstOrNull()?.let { "Sheet: $it • " }.orEmpty()
                    FileContentState.CsvSuccess(
                        rows = parsed.primarySheetRows,
                        file = request.fileEntity,
                        truncated = false,
                        readOnlyReason = "${sheetLabel}Legacy Excel (.xls) file is shown as read-only grid."
                    )
                } else {
                    binaryHandler.identifyOnly(
                        request,
                        "Legacy Excel (.xls) binary file could not be parsed."
                    )
                }
            }
            "doc" -> {
                val extracted = runCatching { LegacyDocExtractor.extract(request.file) }.getOrNull()
                if (extracted != null && extracted.paragraphs.isNotEmpty()) {
                    FileContentState.TextSuccess(
                        content = extracted.text,
                        file = request.fileEntity,
                        readOnlyReason = "Text recovered from legacy Word (.doc) binary file (read-only)."
                    )
                } else {
                    binaryHandler.identifyOnly(
                        request,
                        "Legacy Word (.doc) binary file could not be parsed."
                    )
                }
            }
            else -> binaryHandler.identifyOnly(
                request,
                "Legacy binary Office format. Convert it to .docx / .xlsx / .pptx to open it here."
            )
        }
    }

    companion object {
        val FORMAT_IDS = setOf("ole2")
    }
}
