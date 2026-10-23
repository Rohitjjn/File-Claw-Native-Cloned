package com.example.formats.csv

import java.io.Reader
import java.io.Writer

data class DelimitedResult(val rows: List<List<String>>, val truncated: Boolean, val delimiter: Char)

/**
 * RFC 4180 parser with a configurable delimiter. Handles quoted fields, doubled quotes, and
 * CR / LF / CRLF line breaks (also inside quotes). Row and column caps stop huge files from
 * exhausting memory; when a cap is hit [DelimitedResult.truncated] is set so callers can
 * refuse to save the partial data back over the original.
 */
object DelimitedParser {

    const val DEFAULT_MAX_ROWS = 200_000
    const val DEFAULT_MAX_COLS = 512

    fun delimiterFor(extension: String): Char = when (extension.lowercase()) {
        "tsv", "tab" -> '\t'
        "psv" -> '|'
        else -> ','
    }

    /** Picks the delimiter (comma, semicolon, tab, pipe) that splits the first lines most consistently. */
    fun sniffDelimiter(sample: String, fallback: Char = ','): Char {
        val lines = sample.lineSequence().filter { it.isNotBlank() }.take(20).toList()
        if (lines.isEmpty()) return fallback
        var best = fallback
        var bestScore = 0
        for (d in charArrayOf(',', ';', '\t', '|')) {
            val counts = lines.map { l -> countOutsideQuotes(l, d) }
            val first = counts.first()
            if (first == 0) continue
            val consistent = counts.count { it == first }
            val score = consistent * 100 + minOf(first, 99)
            if (consistent >= (lines.size + 1) / 2 && score > bestScore) {
                best = d
                bestScore = score
            }
        }
        return best
    }

    private fun countOutsideQuotes(line: String, d: Char): Int {
        var q = false
        var n = 0
        for (c in line) {
            if (c == '"') q = !q else if (c == d && !q) n++
        }
        return n
    }

    fun parse(
        reader: Reader,
        delimiter: Char = ',',
        maxRows: Int = DEFAULT_MAX_ROWS,
        maxCols: Int = DEFAULT_MAX_COLS
    ): DelimitedResult {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var wasQuoted = false
        var truncated = false
        var pendingCr = false
        var anyData = false
        var stopped = false

        fun endCell() {
            if (row.size < maxCols) row.add(cell.toString()) else truncated = true
            cell.setLength(0)
            wasQuoted = false
        }

        fun endRow(): Boolean {
            if (rows.size >= maxRows) {
                truncated = true
                return false
            }
            endCell()
            rows.add(row)
            row = ArrayList()
            anyData = false
            return true
        }

        val buf = CharArray(16 * 1024)
        loop@ while (true) {
            val n = reader.read(buf)
            if (n < 0) break
            for (i in 0 until n) {
                val c = buf[i]
                if (pendingCr) {
                    pendingCr = false
                    if (c == '\n') continue
                }
                if (inQuotes) {
                    if (c == '"') {
                        inQuotes = false
                        // A following quote re-opens; handled below via lookbehind flag.
                        wasQuoted = true
                    } else {
                        cell.append(c)
                    }
                    continue
                }
                when {
                    c == '"' && cell.isEmpty() && !wasQuoted -> { inQuotes = true; anyData = true }
                    c == '"' && wasQuoted -> { cell.append('"'); inQuotes = true }
                    c == delimiter -> { endCell(); anyData = true }
                    c == '\n' -> if (!endRow()) { stopped = true; break@loop }
                    c == '\r' -> { pendingCr = true; if (!endRow()) { stopped = true; break@loop } }
                    else -> { cell.append(c); anyData = true }
                }
            }
        }
        if (!stopped && (anyData || cell.isNotEmpty() || row.isNotEmpty())) {
            if (rows.size >= maxRows) {
                truncated = true
            } else {
                endCell()
                rows.add(row)
            }
        }
        return DelimitedResult(rows, truncated, delimiter)
    }
}

object DelimitedWriter {

    fun write(rows: List<List<String>>, out: Writer, delimiter: Char = ',', lineEnd: String = "\n") {
        for (row in rows) {
            for ((ci, v) in row.withIndex()) {
                if (ci > 0) out.write(delimiter.code)
                out.write(quote(v, delimiter))
            }
            out.write(lineEnd)
        }
        out.flush()
    }

    fun quote(v: String, delimiter: Char): String {
        val needs = v.any { it == delimiter || it == '"' || it == '\n' || it == '\r' } ||
            v.startsWith(" ") || v.endsWith(" ")
        return if (needs) "\"" + v.replace("\"", "\"\"") + "\"" else v
    }
}
