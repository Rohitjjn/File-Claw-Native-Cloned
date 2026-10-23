package com.example.formats.csv

import com.example.core.io.AtomicFileWriter
import com.example.core.io.TextEncoding
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.Charset

/** Parsed delimited text plus metadata required to preserve its encoding and line endings on save. */
data class DelimitedTableData(
    val rows: List<List<String>>,
    val truncated: Boolean,
    val delimiter: Char,
    val charset: Charset,
    val hadBom: Boolean,
    val lineEnd: String
)

/** Bounded loading and atomic, encoding-preserving saving for CSV, TSV, and PSV files. */
object DelimitedTableLoader {
    const val MAX_VIEW_ROWS = 50_000
    const val MAX_VIEW_COLUMNS = 256

    fun load(file: File, extension: String, encoding: String): DelimitedTableData {
        if (!file.isFile) throw IOException("File not found")
        val head = ByteArray(8192)
        val n = file.inputStream().use { it.read(head) }.coerceAtLeast(0)
        val detected = TextEncoding.detect(head, n)
        val charset = TextEncoding.resolve(encoding) ?: detected.charset
        val hadBom = detected.bomLength > 0 && charset == detected.charset
        val sample = String(head, 0, n, charset)
        val ext = extension.lowercase()
        val base = DelimitedParser.delimiterFor(ext)
        val delimiter = if (ext == "csv") DelimitedParser.sniffDelimiter(sample, base) else base
        val lineEnd = if (sample.contains("\r\n")) "\r\n" else "\n"
        val result = InputStreamReader(file.inputStream().buffered(64 * 1024), charset).use { reader ->
            DelimitedParser.parse(reader, delimiter, MAX_VIEW_ROWS, MAX_VIEW_COLUMNS)
        }
        var rows = result.rows
        if (rows.isNotEmpty() && rows[0].isNotEmpty() && rows[0][0].startsWith("\uFEFF")) {
            val first = rows[0].toMutableList().also { it[0] = it[0].removePrefix("\uFEFF") }
            rows = listOf<List<String>>(first) + rows.drop(1)
        }
        return DelimitedTableData(rows, result.truncated, delimiter, charset, hadBom, lineEnd)
    }

    fun save(file: File, rows: List<List<String>>, data: DelimitedTableData) {
        AtomicFileWriter.write(file) { out ->
            if (data.hadBom) out.write(TextEncoding.bomBytes(data.charset))
            // AtomicFileWriter owns the stream and must fsync before closing it.
            val writer = OutputStreamWriter(out, data.charset)
            DelimitedWriter.write(rows, writer, data.delimiter, data.lineEnd)
        }
    }
}
