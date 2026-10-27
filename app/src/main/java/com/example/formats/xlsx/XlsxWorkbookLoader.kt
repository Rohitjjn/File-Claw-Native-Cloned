package com.example.formats.xlsx

import com.example.core.io.AtomicFileWriter
import com.example.formats.spreadsheet.SpreadsheetGridPayload
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
    /**
     * The shared grid budget is stricter than the old 50,000 x 256 caps. A dense
     * sheet at those old caps could create millions of strings, then duplicate
     * them through JSON/Base64/WebView and exhaust an Android process heap.
     */
    val MAX_VIEW_ROWS get() = SpreadsheetGridPayload.MAX_ROWS
    val MAX_VIEW_COLUMNS get() = SpreadsheetGridPayload.MAX_COLUMNS

    fun load(file: File): XlsxWorkbookData {
        if (!file.isFile) throw IOException("File not found")
        org.dhatim.fastexcel.reader.ReadableWorkbook(file).use { workbook ->
            val sheetCount = workbook.sheets.count().toInt()
            if (sheetCount == 0) return XlsxWorkbookData(emptyList(), false, 0, "")
            val sheet = workbook.firstSheet
            val rows = ArrayList<List<String>>()
            var truncated = false
            var totalChars = 0L
            sheet.openStream().use { stream ->
                val iterator = stream.iterator()
                outer@ while (iterator.hasNext()) {
                    if (rows.size >= MAX_VIEW_ROWS) {
                        truncated = true
                        break
                    }
                    val values = ArrayList<String>()
                    val cells = iterator.next()
                    for (cell in cells) {
                        if (values.size >= MAX_VIEW_COLUMNS) {
                            truncated = true
                            break
                        }
                        // Some date/number/formula-error cells can throw from the
                        // reader's text conversion. One bad cell must not abort the file.
                        val rawText = runCatching { cell?.text }.getOrNull()
                            ?: runCatching { cell?.rawValue }.getOrNull()
                            ?: ""
                        val (safeText, cellTruncated) = SpreadsheetGridPayload.clampCell(
                            rawText,
                            SpreadsheetGridPayload.MAX_TOTAL_CHARS - totalChars
                        )
                        truncated = truncated || cellTruncated
                        values.add(safeText)
                        totalChars += safeText.length
                        if (totalChars >= SpreadsheetGridPayload.MAX_TOTAL_CHARS) {
                            truncated = true
                            rows.add(values)
                            break@outer
                        }
                    }
                    rows.add(values)
                }
            }
            return XlsxWorkbookData(rows, truncated, sheetCount, sheet.name ?: "")
        }
    }

    /**
     * Light-edit save: plain cell VALUES only (fastexcel infers numeric/date
     * types where possible). Formulas, styles, widths, merged cells, macros,
     * templates and additional sheets are intentionally not preserved. The
     * caller only offers this path for a bounded, single-sheet workbook.
     *
     * The output is written atomically: a failed save never truncates the
     * original workbook.
     */
    fun write(file: File, rows: List<List<String>>, sheetName: String) {
        val safeRows = SpreadsheetGridPayload.clampRows(rows)
        require(!safeRows.truncated) { "The edited sheet exceeds the safe light-edit limit." }
        AtomicFileWriter.write(file) { output: OutputStream ->
            val workbook = org.dhatim.fastexcel.Workbook(output, "File Claw", "1.0")
            val worksheet = workbook.newWorksheet(SpreadsheetGridPayload.safeSheetName(sheetName))
            for ((rowIndex, row) in safeRows.rows.withIndex()) {
                for ((colIndex, value) in row.withIndex()) {
                    if (value.isEmpty()) continue
                    worksheet.value(rowIndex, colIndex, value)
                }
            }
            workbook.finish()
        }
    }
}
