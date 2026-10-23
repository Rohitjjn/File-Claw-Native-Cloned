package com.example.utils

import android.webkit.WebResourceResponse
import com.example.core.io.HttpRange
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.RandomAccessFile

/**
 * Handles HTTP Range requests for PDF documents served to pdf.js in WebView.
 * When pdf.js sends `Range: bytes=X-Y`, this responder returns HTTP 206 Partial Content
 * with only the requested bytes, enabling fast rendering of huge (150MB+) PDFs.
 */
object PdfRangeResponder {

    private class BoundedFileInputStream(
        file: File,
        start: Long,
        private val length: Long
    ) : InputStream() {
        private val raf = RandomAccessFile(file, "r")
        private var remaining = length

        init {
            if (start > 0) raf.seek(start)
        }

        override fun read(): Int {
            if (remaining <= 0) return -1
            val b = raf.read()
            if (b != -1) remaining--
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (remaining <= 0) return -1
            val toRead = minOf(len.toLong(), remaining).toInt()
            val read = raf.read(b, off, toRead)
            if (read > 0) remaining -= read
            return read
        }

        override fun available(): Int = minOf(remaining, Int.MAX_VALUE.toLong()).toInt()

        override fun close() {
            raf.close()
        }
    }

    fun respond(file: File, requestHeaders: Map<String, String>?): WebResourceResponse? {
        if (!file.isFile || file.length() == 0L) return null
        val totalLength = file.length()

        val rangeHeader = requestHeaders?.entries?.firstOrNull {
            it.key.equals("Range", ignoreCase = true)
        }?.value

        if (!rangeHeader.isNullOrBlank()) {
            val range = HttpRange.parse(rangeHeader, totalLength)
            if (range != null) {
                val stream = BoundedFileInputStream(file, range.start, range.length)
                val headers = mapOf(
                    "Content-Type" to "application/pdf",
                    "Accept-Ranges" to "bytes",
                    "Content-Range" to range.contentRangeHeader,
                    "Content-Length" to range.length.toString(),
                    "Access-Control-Allow-Origin" to "*",
                    "Access-Control-Expose-Headers" to "Content-Range, Accept-Ranges, Content-Length"
                )
                return WebResourceResponse(
                    "application/pdf",
                    "binary",
                    206,
                    "Partial Content",
                    headers,
                    stream
                )
            } else {
                // Unsatisfiable range
                val headers = mapOf(
                    "Content-Range" to "bytes */$totalLength",
                    "Accept-Ranges" to "bytes"
                )
                return WebResourceResponse(
                    "text/plain",
                    "UTF-8",
                    416,
                    "Range Not Satisfiable",
                    headers,
                    ByteArrayInputStream("Range Not Satisfiable".toByteArray())
                )
            }
        }

        // Full document (status 200) with Accept-Ranges declared
        val headers = mapOf(
            "Content-Type" to "application/pdf",
            "Accept-Ranges" to "bytes",
            "Content-Length" to totalLength.toString(),
            "Access-Control-Allow-Origin" to "*",
            "Access-Control-Expose-Headers" to "Accept-Ranges, Content-Length"
        )
        return WebResourceResponse(
            "application/pdf",
            "binary",
            200,
            "OK",
            headers,
            FileInputStream(file)
        )
    }
}
