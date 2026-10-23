package com.example.formats.text

import java.io.File
import java.io.InputStreamReader
import java.nio.charset.Charset

/** A search hit's byte position, suitable for opening a bounded [com.example.core.io.TextWindowReader] window. */
data class TextFileSearchMatch(val byteOffset: Long)

data class TextFileSearchResult(
    val matchCount: Int,
    val matches: List<TextFileSearchMatch>,
    val truncated: Boolean
)

/**
 * Case-insensitive, bounded-memory search for large text files. It scans the complete file but keeps
 * only the first [maxMatches] byte offsets so callers can navigate to matches without retaining the
 * whole document in memory.
 */
object TextFileSearchEngine {
    const val DEFAULT_MAX_MATCHES = 10_000
    const val MAX_QUERY_CHARS = 4_096
    private const val DEFAULT_BUFFER_SIZE = 64 * 1024

    fun search(
        file: File,
        query: String,
        charset: Charset = Charsets.UTF_8,
        maxMatches: Int = DEFAULT_MAX_MATCHES,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        shouldContinue: () -> Boolean = { true }
    ): TextFileSearchResult {
        require(file.isFile) { "Not a readable file" }
        require(maxMatches >= 0) { "maxMatches must not be negative" }
        require(bufferSize > 0) { "bufferSize must be positive" }

        val needle = query.trim()
        if (needle.isEmpty()) return TextFileSearchResult(0, emptyList(), truncated = false)
        require(needle.length <= MAX_QUERY_CHARS) { "Search query is too long (maximum $MAX_QUERY_CHARS characters)." }

        val matches = ArrayList<TextFileSearchMatch>(minOf(maxMatches, 256))
        val overlapLength = needle.length - 1
        val buffer = CharArray(bufferSize)
        var carry = ""
        var carryByteStart = 0L
        var accountedByteEnd = 0L
        var pendingHighSurrogate: Char? = null
        var totalMatches = 0
        // Byte offset just past the last counted match: candidates that start inside it
        // overlap an already-reported hit and must not be double-counted across chunks.
        var lastCountedMatchEndByte = -1L

        fun processChunk(chunk: String) {
            if (!shouldContinue()) throw java.util.concurrent.CancellationException("Text search cancelled")
            if (chunk.isEmpty()) return

            val combined = carry + chunk
            val baseByte = if (carry.isNotEmpty()) carryByteStart else accountedByteEnd
            var charCursor = 0
            var bytesBeforeCursor = 0L
            var matchAt = combined.indexOf(needle, startIndex = 0, ignoreCase = true)

            while (matchAt >= 0) {
                bytesBeforeCursor += combined.substring(charCursor, matchAt).toByteArray(charset).size
                val matchByteOffset = baseByte + bytesBeforeCursor
                val matchEnd = matchAt + needle.length
                val matchEndByteOffset = matchByteOffset + combined.substring(matchAt, matchEnd).toByteArray(charset).size
                if (matchEnd > carry.length && matchByteOffset >= lastCountedMatchEndByte) {
                    if (totalMatches < Int.MAX_VALUE) totalMatches++
                    if (matches.size < maxMatches) matches += TextFileSearchMatch(matchByteOffset)
                    lastCountedMatchEndByte = matchEndByteOffset
                }
                bytesBeforeCursor += combined.substring(matchAt, matchEnd).toByteArray(charset).size
                charCursor = matchEnd
                matchAt = combined.indexOf(needle, startIndex = charCursor, ignoreCase = true)
            }

            val newBytes = chunk.toByteArray(charset).size.toLong()
            accountedByteEnd += newBytes

            var keepChars = minOf(overlapLength, combined.length)
            val keepStart = combined.length - keepChars
            // Do not split a UTF-16 surrogate pair at the overlap boundary.
            if (keepStart > 0 && keepStart < combined.length && Character.isLowSurrogate(combined[keepStart])) {
                keepChars++
            }
            val actualKeepStart = combined.length - keepChars
            carry = if (keepChars == 0) "" else combined.substring(actualKeepStart)
            carryByteStart = baseByte + combined.substring(0, actualKeepStart).toByteArray(charset).size
        }

        InputStreamReader(file.inputStream().buffered(bufferSize), charset).use { reader ->
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break

                val chunkBuilder = StringBuilder(count + 1)
                pendingHighSurrogate?.let { chunkBuilder.append(it) }
                pendingHighSurrogate = null
                chunkBuilder.append(buffer, 0, count)
                if (chunkBuilder.isNotEmpty() && Character.isHighSurrogate(chunkBuilder[chunkBuilder.length - 1])) {
                    pendingHighSurrogate = chunkBuilder[chunkBuilder.length - 1]
                    chunkBuilder.setLength(chunkBuilder.length - 1)
                }
                processChunk(chunkBuilder.toString())
            }
        }

        pendingHighSurrogate?.let { processChunk(it.toString()) }
        return TextFileSearchResult(
            matchCount = totalMatches,
            matches = matches,
            truncated = totalMatches > matches.size
        )
    }
}
