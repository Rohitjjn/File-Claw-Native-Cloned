package com.example.formats.xlsx

import java.io.File
import java.io.IOException

/** Bounded, first-sheet view of an XLSX workbook; writes are intentionally unsupported. */
data class XlsxWorkbookData(
    val rows: List<List<String>>,
    val truncated: Boolean,
    val sheetCount: Int,
    val sheetName: String
)

object XlsxWorkbookLoader {
    const val MAX_VIEW_ROWS = 50_000
    const val MAX_VIEW_COLUMNS = 256

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
                        values.add(cell?.text ?: "")
                    }
                    rows.add(values)
                }
            }
            return XlsxWorkbookData(rows, truncated, sheetCount, sheet.name ?: "")
        }
    }
}
