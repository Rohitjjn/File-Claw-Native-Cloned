package com.example.formats.xlsx

import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatOpenRequest
import com.example.domain.preview.FormatOperationResult
import com.example.domain.preview.FormatSaveRequest
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities
import java.io.IOException

/**
 * XLSX preview handler, with a light-edit save for single-sheet workbooks.
 *
 * Issue 2 fix ("Excel file crashes the app as soon as it opens"): [open] used to let any
 * failure from [XlsxWorkbookLoader] propagate uncaught past this point. The view-model's
 * own try/catch around the whole open pipeline only catches Exception and
 * OutOfMemoryError specifically; a handful of other crash classes this kind of library
 * can realistically throw on a real device (e.g. LinkageError/NoClassDefFoundError if a
 * transitive XML dependency ever fails to resolve, StackOverflowError on a pathologically
 * nested file, or a plain RuntimeException from the parser on a malformed workbook) are
 * not Exception subtypes and were never caught anywhere, crashing the whole app instead
 * of showing an error screen. Every failure here is now caught and turned into a normal,
 * retryable FileContentState.Error.
 *
 * Multi-sheet workbooks stay view-only (only the first sheet is ever shown, so saving
 * could silently drop the others); a single-sheet workbook can be lightly edited and saved
 * back, matching CSV's "values only" editing tier -- no formulas, styling, column widths
 * or merged cells are preserved.
 */
class XlsxFormatHandler : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = FORMAT_IDS.associateWith {
        capabilities(FormatCapability.PREVIEW, FormatCapability.EDIT, FormatCapability.SAVE)
    }
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState {
        val workbook = try {
            XlsxWorkbookLoader.load(request.file)
        } catch (error: OutOfMemoryError) {
            return FileContentState.Error(
                "This Excel file is too large to preview on this device (ran out of memory)."
            )
        } catch (error: IOException) {
            return FileContentState.Error(error.message ?: "Could not read this Excel file.")
        } catch (error: Throwable) {
            // Deliberately broad: any unexpected failure from the third-party reader
            // becomes a recoverable error screen instead of taking the whole app down.
            return FileContentState.Error(
                "Could not open this Excel file" + (error.message?.let { ": $it" } ?: ".")
            )
        }

        val singleSheet = workbook.sheetCount <= 1
        val reason = when {
            workbook.sheetCount > 1 ->
                "Showing sheet \"${workbook.sheetName}\" (1 of ${workbook.sheetCount}). " +
                    "Editing is disabled for multi-sheet workbooks so the other sheets are never lost."
            workbook.truncated ->
                "This sheet is larger than the preview can show in full, so editing is disabled."
            else -> null
        }
        return FileContentState.CsvSuccess(
            rows = workbook.rows,
            file = request.fileEntity,
            truncated = workbook.truncated,
            readOnlyReason = reason,
            table = null,
            spreadsheetEditable = singleSheet && !workbook.truncated,
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
            FormatOperationResult.Failed(error.message ?: "Could not save this Excel file")
        }
    }

    companion object {
        val FORMAT_IDS = setOf("xlsx")
    }
}
