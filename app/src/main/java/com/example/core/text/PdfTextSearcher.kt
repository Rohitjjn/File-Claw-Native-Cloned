package com.example.core.text

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.InflaterInputStream

data class PdfSearchResult(
    val pageNumber: Int,
    val matchedText: String
)

/**
 * Pure Kotlin, zero-dependency PDF text searcher.
 * Parses PDF page content streams (including /FlateDecode compressed streams)
 * to locate occurrences of a search keyword by page number.
 */
object PdfTextSearcher {

    fun searchInPdf(file: File, query: String, maxResults: Int = 100): List<PdfSearchResult> {
        if (!file.exists() || query.isBlank()) return emptyList()
        return try {
            val bytes = file.readBytes()
            searchInBytes(bytes, query.trim(), maxResults)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun searchInBytes(bytes: ByteArray, query: String, maxResults: Int = 100): List<PdfSearchResult> {
        if (bytes.isEmpty() || query.isBlank()) return emptyList()

        val results = mutableListOf<PdfSearchResult>()
        val queryLower = query.lowercase()

        // 1. Locate all stream ... endstream blocks
        val streams = extractStreams(bytes)

        // 2. Extract text from each stream and check for matches
        var estimatedPage = 1
        for (stream in streams) {
            val text = extractTextFromStream(stream)
            if (text.contains(queryLower)) {
                // Find snippet
                val idx = text.indexOf(queryLower)
                val start = (idx - 20).coerceAtLeast(0)
                val end = (idx + query.length + 20).coerceAtMost(text.length)
                val snippet = text.substring(start, end).replace('\n', ' ').trim()

                results.add(PdfSearchResult(pageNumber = estimatedPage, matchedText = snippet))
                if (results.size >= maxResults) break
            }
            estimatedPage++
        }

        // If no stream-based match found, do a raw byte-scan fallback for uncompressed PDFs
        if (results.isEmpty()) {
            val rawText = String(bytes, Charsets.ISO_8859_1).lowercase()
            if (rawText.contains(queryLower)) {
                results.add(PdfSearchResult(pageNumber = 1, matchedText = query))
            }
        }

        return results
    }

    private fun extractStreams(bytes: ByteArray): List<ByteArray> {
        val streams = mutableListOf<ByteArray>()
        val streamTag = "stream".toByteArray(Charsets.US_ASCII)
        val endstreamTag = "endstream".toByteArray(Charsets.US_ASCII)

        var idx = 0
        while (idx < bytes.size - streamTag.size) {
            val streamPos = indexOf(bytes, streamTag, idx)
            if (streamPos == -1) break

            // Skip "stream" and subsequent newline (\r\n or \n)
            var start = streamPos + streamTag.size
            if (start < bytes.size && bytes[start] == '\r'.code.toByte()) start++
            if (start < bytes.size && bytes[start] == '\n'.code.toByte()) start++

            val endstreamPos = indexOf(bytes, endstreamTag, start)
            if (endstreamPos == -1) break

            // Stream length check: truncate trailing \r or \n before endstream
            var end = endstreamPos
            if (end > start && bytes[end - 1] == '\n'.code.toByte()) end--
            if (end > start && bytes[end - 1] == '\r'.code.toByte()) end--

            if (end > start) {
                val streamBytes = bytes.copyOfRange(start, end)
                // Attempt decompression (FlateDecode)
                val decompressed = tryDecompress(streamBytes)
                streams.add(decompressed ?: streamBytes)
            }

            idx = endstreamPos + endstreamTag.size
        }

        return streams
    }

    private fun tryDecompress(data: ByteArray): ByteArray? {
        return try {
            InflaterInputStream(ByteArrayInputStream(data)).use { inflater ->
                val buffer = ByteArray(4096)
                val out = ByteArrayOutputStream()
                var read: Int
                while (inflater.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
                out.toByteArray()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractTextFromStream(stream: ByteArray): String {
        val str = String(stream, Charsets.ISO_8859_1)
        val sb = java.lang.StringBuilder()

        // Extract strings inside (...)
        var i = 0
        while (i < str.length) {
            if (str[i] == '(') {
                val end = findMatchingParen(str, i)
                if (end != -1) {
                    val literal = str.substring(i + 1, end)
                    sb.append(unescapePdfString(literal)).append(" ")
                    i = end
                }
            } else if (str[i] == '<' && i + 1 < str.length && str[i + 1] != '<') {
                // Hex string <...>
                val end = str.indexOf('>', i)
                if (end != -1) {
                    val hex = str.substring(i + 1, end).trim()
                    sb.append(decodeHexString(hex)).append(" ")
                    i = end
                }
            }
            i++
        }
        return sb.toString().lowercase()
    }

    private fun findMatchingParen(str: String, start: Int): Int {
        var depth = 1
        var i = start + 1
        var escaped = false
        while (i < str.length) {
            val c = str[i]
            if (escaped) {
                escaped = false
            } else if (c == '\\') {
                escaped = true
            } else if (c == '(') {
                depth++
            } else if (c == ')') {
                depth--
                if (depth == 0) return i
            }
            i++
        }
        return -1
    }

    private fun unescapePdfString(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                val next = s[i + 1]
                when (next) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    '(', ')', '\\' -> sb.append(next)
                    else -> sb.append(next)
                }
                i += 2
                continue
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    private fun decodeHexString(hex: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < hex.length - 1) {
            val hexByte = hex.substring(i, i + 2)
            val code = hexByte.toIntOrNull(16)
            if (code != null && code in 32..126) {
                sb.append(code.toChar())
            }
            i += 2
        }
        return sb.toString()
    }

    private fun indexOf(source: ByteArray, target: ByteArray, fromIndex: Int): Int {
        if (target.isEmpty()) return fromIndex
        val max = source.size - target.size
        for (i in fromIndex..max) {
            var j = 0
            while (j < target.size && source[i + j] == target[j]) {
                j++
            }
            if (j == target.size) return i
        }
        return -1
    }
}
