package com.example.formats.xlsx

import java.io.File
import java.io.IOException
import java.io.OutputStream

/** Bounded, first-sheet view of an XLSX workbook. */
data class XlsxWorkbookData(
    val rows: List<List<String>>,
    val truncated: Boolean,
    val sheetCount: Int,
    val sheetName: String
)

object XlsxWorkbookLoader {
    // Lowered from 50,000 / 256 (12.8M possible cells): that worst case, run through
    // JSON + Base64 + a second copy inside the WebView for the x-spreadsheet grid
    // (CsvPreview.kt), could reach several hundred MB and is a very plausible cause of
    // "the app crashes as soon as I open a real Excel file" for any reasonably large,
    // dense sheet. These caps still comfortably cover the vast majority of real-world
    // spreadsheets; larger ones are truncated with a visible notice (same mechanism
    // already used for CSV), never silently and never by crashing.
    const val MAX_VIEW_ROWS = 20_000
    const val MAX_VIEW_COLUMNS = 100

    fun load(file: File): XlsxWorkbookData {
        if (!file.isFile) throw IOException("File not found")
        org.dhatim.fastexcel.reader.ReadableWorkbook(file).use { workbook ->
            val sheetCount = workbook.sheets.count().toInt()
            if (sheetCount == 0) return XlsxWorkbookData(emptyList(), false, 0, "")
            val sheet = workbook.firstSheet
            val rows = ArrayList<List<String>>()
            var truncated = false
            sheet.openStream().use { stream ->
                val iterator = stream.iterator()
                while (iterator.hasNext()) {
                    if (rows.size >= MAX_VIEW_ROWS) {
                        truncated = true
                        break
                    }
                    val values = ArrayList<String>()
                    for (cell in iterator.next()) {
                        if (values.size >= MAX_VIEW_COLUMNS) {
                            truncated = true
                            break
                        }
                        // A handful of cell types (certain date/number formats, formula
                        // errors) can throw out of fastexcel's own .text conversion;
                        // falling back to the raw stored value (or blank) keeps one
                        // unreadable cell from failing the whole file.
                        val text = runCatching { cell?.text }.getOrNull()
                            ?: runCatching { cell?.rawValue }.getOrNull()
                            ?: ""
                        values.add(text)
                    }
                    rows.add(values)
                }
            }
            return XlsxWorkbookData(rows, truncated, sheetCount, sheet.name ?: "")
        }
    }

    /**
     * Light-edit save: writes plain cell VALUES (as text; fastexcel infers number/date
     * types where the text parses as one) to a single-sheet workbook. No formulas,
     * formatting, column widths or merged cells are preserved -- this intentionally
     * matches the same "values only" tier CSV editing already offers, not a full
     * spreadsheet editor. Only ever called for workbooks that had exactly one sheet to
     * begin with (enforced by the caller), so nothing the viewer never showed can be
     * dropped by a save.
     */
    fun write(file: File, rows: List<List<String>>, sheetName: String) {
        val safeSheetName = sheetName.ifBlank { "Sheet1" }
        file.outputStream().use { output: OutputStream ->
            val workbook = org.dhatim.fastexcel.Workbook(output, "File Claw", "1.0")
            val worksheet = workbook.newWorksheet(safeSheetName)
            for ((rowIndex, row) in rows.withIndex()) {
                for ((colIndex, value) in row.withIndex()) {
                    if (value.isEmpty()) continue
                    worksheet.value(rowIndex, colIndex, value)
                }
            }
            workbook.finish()
        }
    }
}
