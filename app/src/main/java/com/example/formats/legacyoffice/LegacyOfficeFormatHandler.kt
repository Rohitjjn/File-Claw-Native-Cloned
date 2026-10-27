package com.example.formats.legacyoffice

import com.example.core.format.FormatRegistry
import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatOpenRequest
import com.example.formats.binary.BinaryFormatHandler
import com.example.formats.doc.LegacyDocExtractor
import com.example.formats.ppt.LegacyPptExtractor
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities
import com.example.formats.xls.Biff8XlsParser

/** Legacy OLE2 Office support: bounded read-only XLS/DOC/PPT extraction, identity for the rest. */
class LegacyOfficeFormatHandler(
    private val binaryHandler: BinaryFormatHandler
) : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = mapOf(
        "ole2" to capabilities(
            FormatCapability.PREVIEW,
            readOnlyReason = "Legacy Office files are shown as bounded, read-only extractions."
        )
    )
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState {
        if (isEncryptedOfficePackage(request.file)) {
            return FileContentState.Error(
                "This Office file is encrypted or password-protected. Remove the password and try again."
            )
        }
        return when (FormatRegistry.extensionOf(request.file.name).ifEmpty { request.fileEntity.extension.lowercase() }) {
            "xls" -> {
                val parsed = runCatching { Biff8XlsParser.parse(request.file) }.getOrNull()
                if (parsed != null && parsed.primarySheetRows.isNotEmpty()) {
                    val sheetLabel = parsed.sheetNames.firstOrNull()?.let { "Sheet: $it • " }.orEmpty()
                    FileContentState.CsvSuccess(
                        rows = parsed.primarySheetRows,
                        file = request.fileEntity,
                        truncated = false,
                        readOnlyReason = "${sheetLabel}Legacy Excel (.xls) cells are shown as a read-only grid. Formulas, styling and extra sheets are not rendered."
                    )
                } else {
                    binaryHandler.identifyOnly(
                        request,
                        "Legacy Excel (.xls) binary file could not be parsed. Convert it to .xlsx for a richer preview."
                    )
                }
            }
            "doc" -> {
                val extracted = runCatching { LegacyDocExtractor.extract(request.file) }.getOrNull()
                if (extracted != null && extracted.paragraphs.isNotEmpty()) {
                    FileContentState.TextSuccess(
                        content = extracted.text,
                        file = request.fileEntity,
                        readOnlyReason = "Text recovered from legacy Word (.doc) binary file (read-only). Formatting, tables and images are not reconstructed."
                    )
                } else {
                    binaryHandler.identifyOnly(
                        request,
                        "Legacy Word (.doc) text could not be recovered. Convert it to .docx for a formatted preview."
                    )
                }
            }
            "ppt" -> {
                val extracted = runCatching { LegacyPptExtractor.extract(request.file) }.getOrNull()
                if (extracted != null && extracted.paragraphs.isNotEmpty()) {
                    FileContentState.TextSuccess(
                        content = extracted.text,
                        file = request.fileEntity,
                        readOnlyReason = "Text recovered from legacy PowerPoint (.ppt) binary file (read-only). Slides, images, charts and animations are not reconstructed."
                    )
                } else {
                    binaryHandler.identifyOnly(
                        request,
                        "Legacy PowerPoint (.ppt) text could not be recovered. Convert it to .pptx for a formatted preview."
                    )
                }
            }
            else -> binaryHandler.identifyOnly(
                request,
                "Legacy binary Office format. Convert it to .docx / .xlsx / .pptx to open it here."
            )
        }
    }

    private fun isEncryptedOfficePackage(file: java.io.File): Boolean = runCatching {
        if (!Ole2CompoundFile.isOle2File(file)) return false
        Ole2CompoundFile.open(file).use { compound -> compound.hasStream("EncryptedPackage") }
    }.getOrDefault(false)

    companion object {
        val FORMAT_IDS = setOf("ole2")
    }
}
