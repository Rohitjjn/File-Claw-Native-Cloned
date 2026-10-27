package com.example.formats.xlsx

import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatOpenRequest
import com.example.domain.preview.FormatOperationResult
import com.example.domain.preview.FormatSaveRequest
import com.example.formats.ooxml.OoxmlPackageInspector
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities

/**
 * XLSX preview handler with a values-only light-edit save for safe workbooks.
 *
 * The package is validated before the third-party parser starts. Editing is
 * offered only for a complete, bounded, single-sheet, non-macro workbook;
 * saving a partial view, a template, or a macro-enabled workbook could
 * otherwise silently discard content the viewer never showed.
 */
class XlsxFormatHandler : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = FORMAT_IDS.associateWith {
        capabilities(FormatCapability.PREVIEW, FormatCapability.EDIT, FormatCapability.SAVE)
    }
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState {
        val inspection = try {
            OoxmlPackageInspector.inspect(request.file)
        } catch (error: OoxmlPackageInspector.InvalidPackageException) {
            return FileContentState.Error(error.message ?: "Could not open this Excel file.")
        }
        if (inspection.family != OoxmlPackageInspector.Family.EXCEL) {
            return FileContentState.Error("The file content does not match its Excel workbook type.")
        }

        val workbook = try {
            XlsxWorkbookLoader.load(request.file)
        } catch (error: OutOfMemoryError) {
            return FileContentState.Error("This Excel file is too large to preview on this device (ran out of memory).")
        } catch (_: Exception) {
            return FileContentState.Error("Could not read this Excel file. It may be damaged or unsupported.")
        } catch (_: Throwable) {
            // Deliberately broad: a third-party parser failure becomes a retryable
            // error screen rather than taking down the app process.
            return FileContentState.Error("Could not open this Excel file.")
        }

        val variantIsReadOnly = inspection.hasMacros ||
            inspection.variant == OoxmlPackageInspector.Variant.TEMPLATE ||
            inspection.variant == OoxmlPackageInspector.Variant.MACRO_TEMPLATE
        val singleSheet = workbook.sheetCount <= 1
        val reasons = mutableListOf<String>()
        inspection.notice?.let(reasons::add)
        if (workbook.sheetCount > 1) {
            reasons += "Showing sheet \"${workbook.sheetName}\" (1 of ${workbook.sheetCount}). " +
                "Other sheets are not shown, so editing is disabled."
        }
        if (workbook.truncated) {
            reasons += "This sheet exceeds the safe preview limit, so editing is disabled."
        }
        if (variantIsReadOnly) {
            reasons += "This workbook variant is read-only because a values-only save would discard its package features."
        }

        return FileContentState.CsvSuccess(
            rows = workbook.rows,
            file = request.fileEntity,
            truncated = workbook.truncated,
            readOnlyReason = reasons.joinToString(" ").takeIf { it.isNotBlank() },
            table = null,
            spreadsheetEditable = singleSheet && !workbook.truncated && !variantIsReadOnly,
            sheetName = workbook.sheetName
        )
    }

    override suspend fun save(request: FormatSaveRequest): FormatOperationResult {
        if (request !is FormatSaveRequest.Spreadsheet) {
            return FormatOperationResult.Unsupported("Excel files require a spreadsheet save request.")
        }
        return try {
            XlsxWorkbookLoader.write(request.file, request.rows, request.sheetName)
            FormatOperationResult.Completed
        } catch (error: OutOfMemoryError) {
            FormatOperationResult.Failed("Ran out of memory while saving this Excel file.")
        } catch (error: Throwable) {
            FormatOperationResult.Failed(error.message?.take(180) ?: "Could not save this Excel file.")
        }
    }

    companion object {
        val FORMAT_IDS = setOf("xlsx")
    }
}
