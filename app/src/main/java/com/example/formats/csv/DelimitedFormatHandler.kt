package com.example.formats.csv

import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatOpenRequest
import com.example.core.format.FormatRegistry
import com.example.domain.preview.FormatOperationResult
import com.example.domain.preview.FormatSaveRequest
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities
import java.nio.charset.Charset

/** CSV/TSV/PSV loading and encoding-preserving atomic saves. */
class DelimitedFormatHandler : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = FORMAT_IDS.associateWith {
        capabilities(FormatCapability.PREVIEW, FormatCapability.EDIT, FormatCapability.SAVE)
    }
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState {
        val table = DelimitedTableLoader.load(
            request.file,
            extension = FormatRegistry.extensionOf(request.file.name).ifEmpty { request.fileEntity.extension.lowercase() },
            encoding = request.encoding
        )
        return FileContentState.CsvSuccess(
            rows = table.rows,
            file = request.fileEntity,
            truncated = table.truncated,
            readOnlyReason = null,
            table = table
        )
    }

    override suspend fun save(request: FormatSaveRequest): FormatOperationResult {
        if (request !is FormatSaveRequest.Delimited) {
            return FormatOperationResult.Unsupported("Delimited files require a table save request.")
        }
        return try {
            val data = DelimitedTableData(
                rows = request.rows,
                truncated = false,
                delimiter = request.delimiter,
                charset = Charset.forName(request.charsetName),
                hadBom = request.hadBom,
                lineEnd = request.lineEnd
            )
            DelimitedTableLoader.save(request.file, request.rows, data)
            FormatOperationResult.Completed
        } catch (error: Exception) {
            FormatOperationResult.Failed(error.message ?: "Could not save delimited file")
        }
    }

    companion object {
        val FORMAT_IDS = setOf("csv")
    }
}
