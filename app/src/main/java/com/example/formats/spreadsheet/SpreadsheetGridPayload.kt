package com.example.formats.spreadsheet

/**
 * Bounded, JSON-safe payload for the shared spreadsheet WebView.
 *
 * The viewer used to inject the entire sheet as a Base64 string inside one
 * `evaluateJavascript()` call. Large workbooks could crash WebView before a
 * single cell appeared. This helper creates one bounded UTF-8 JSON document
 * served through `WebViewAssetLoader`; when data must be truncated, editing is
 * disabled so a partial view can never overwrite the original file.
 */
object SpreadsheetGridPayload {
    const val MAX_ROWS = 20_000
    const val MAX_COLUMNS = 100
    const val MAX_CELL_CHARS = 4_000
    const val MAX_TOTAL_CHARS = 8_000_000L
    const val MAX_SHEET_NAME_CHARS = 31
    const val MAX_PRINT_ROWS = 5_000
    const val MAX_PRINT_COLUMNS = 50

    data class ClampedRows(
        val rows: List<List<String>>,
        val truncated: Boolean
    )

    data class Payload(
        val rows: List<List<String>>,
        val editable: Boolean,
        val truncated: Boolean,
        val notice: String?,
        val json: String
    )

    fun safeSheetName(raw: String?): String {
        val cleaned = (raw ?: "Sheet1")
            .replace(Regex("[\\\\/?*\\[\\]:]"), " ")
            .trim()
            .take(MAX_SHEET_NAME_CHARS)
        return cleaned.ifBlank { "Sheet1" }
    }

    fun clampCell(value: String, remainingBudget: Long): Pair<String, Boolean> {
        if (value.length <= MAX_CELL_CHARS && value.length.toLong() <= remainingBudget) {
            return value to false
        }
        val allowed = minOf(MAX_CELL_CHARS.toLong(), remainingBudget.coerceAtLeast(0L)).toInt()
        return value.take(allowed) to true
    }

    fun clampRows(rows: List<List<String>>): ClampedRows {
        val output = ArrayList<List<String>>(minOf(rows.size, MAX_ROWS))
        var totalChars = 0L
        var truncated = rows.size > MAX_ROWS
        outer@ for (row in rows.take(MAX_ROWS)) {
            val safeRow = ArrayList<String>(minOf(row.size, MAX_COLUMNS))
            if (row.size > MAX_COLUMNS) truncated = true
            for (cell in row.take(MAX_COLUMNS)) {
                val (safeCell, cellTruncated) = clampCell(cell, MAX_TOTAL_CHARS - totalChars)
                truncated = truncated || cellTruncated
                safeRow.add(safeCell)
                totalChars += safeCell.length
                if (totalChars >= MAX_TOTAL_CHARS) {
                    truncated = true
                    output.add(safeRow)
                    break@outer
                }
            }
            output.add(safeRow)
        }
        return ClampedRows(output, truncated)
    }

    fun build(
        rows: List<List<String>>,
        editable: Boolean,
        sheetName: String?,
        notice: String?
    ): Payload {
        val clamped = clampRows(rows)
        val effectiveEditable = editable && !clamped.truncated
        val messages = mutableListOf<String>()
        notice?.takeIf { it.isNotBlank() }?.let(messages::add)
        if (clamped.truncated) {
            messages += "Large sheet: showing a bounded view. Editing is disabled so no unseen data can be lost."
        }
        val safeNotice = messages.joinToString(" ").takeIf { it.isNotBlank() }
        val json = buildJson(clamped.rows, effectiveEditable, safeSheetName(sheetName), safeNotice)
        return Payload(clamped.rows, effectiveEditable, clamped.truncated, safeNotice, json)
    }

    fun buildJson(
        rows: List<List<String>>,
        editable: Boolean,
        sheetName: String,
        notice: String?
    ): String = buildString {
        append('{')
        append("\"sheetName\":"); appendJsonString(sheetName); append(',')
        append("\"editable\":").append(if (editable) "true" else "false").append(',')
        append("\"notice\":")
        if (notice == null) append("null") else appendJsonString(notice)
        append(",\"rows\":[")
        rows.forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) append(',')
            append('[')
            row.forEachIndexed { cellIndex, cell ->
                if (cellIndex > 0) append(',')
                appendJsonString(cell)
            }
            append(']')
        }
        append("]}")
    }

    private fun Appendable.appendJsonString(value: String) {
        append('"')
        for (char in value) {
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char.code < 0x20) {
                    append("\\u")
                    append(char.code.toString(16).padStart(4, '0'))
                } else {
                    append(char)
                }
            }
        }
        append('"')
    }
}
