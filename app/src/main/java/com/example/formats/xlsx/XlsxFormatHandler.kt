package com.example.formats.xlsx

import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatOpenRequest
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities

/** Read-only, first-sheet XLSX preview handler. */
class XlsxFormatHandler : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = FORMAT_IDS.associateWith {
        capabilities(
            FormatCapability.PREVIEW,
            readOnlyReason = "Excel workbooks are view-only to protect formulas and formatting."
        )
    }
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState {
        val workbook = XlsxWorkbookLoader.load(request.file)
        val reason = if (workbook.sheetCount > 1) {
            "Excel workbooks are view-only. Showing sheet \"${workbook.sheetName}\" (1 of ${workbook.sheetCount})."
        } else {
            "Excel workbooks are view-only to protect formulas and formatting."
        }
        return FileContentState.CsvSuccess(
            rows = workbook.rows,
            file = request.fileEntity,
            truncated = workbook.truncated,
            readOnlyReason = reason,
            table = null
        )
    }

    companion object {
        val FORMAT_IDS = setOf("xlsx")
    }
}
