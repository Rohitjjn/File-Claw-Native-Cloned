package com.example.core.io

/**
 * RFC 7233 / 9110 HTTP Range header parser and validator for partial content responses.
 * Enables streaming large files (e.g. 150MB+ PDFs) on demand without loading the whole
 * file into memory upfront.
 */
object HttpRange {

    data class RangeSpec(
        val start: Long,
        val end: Long,
        val total: Long
    ) {
        val length: Long get() = (end - start + 1).coerceAtLeast(0)
        val contentRangeHeader: String get() = "bytes $start-$end/$total"
    }

    /**
     * Parses an HTTP `Range: bytes=...` header against the known total file size.
     * Returns a valid [RangeSpec] or null if the header is absent, malformed, or unsatisfiable.
     */
    fun parse(rangeHeader: String?, totalLength: Long): RangeSpec? {
        if (rangeHeader.isNullOrBlank() || totalLength <= 0) return null
        val trimmed = rangeHeader.trim()
        if (!trimmed.startsWith("bytes=", ignoreCase = true)) return null

        val specsString = trimmed.substring("bytes=".length).trim()
        // If multiple byte ranges are requested (e.g. "0-499, 1000-1499"), take the first one
        val firstSpec = specsString.split(',').firstOrNull()?.trim() ?: return null

        val dashIndex = firstSpec.indexOf('-')
        if (dashIndex == -1) return null

        val startStr = firstSpec.substring(0, dashIndex).trim()
        val endStr = firstSpec.substring(dashIndex + 1).trim()

        return try {
            when {
                // Suffix range: "bytes=-500" -> last 500 bytes of the file
                startStr.isEmpty() && endStr.isNotEmpty() -> {
                    val suffixLength = endStr.toLong()
                    if (suffixLength <= 0) return null
                    val actualLength = minOf(suffixLength, totalLength)
                    val start = totalLength - actualLength
                    val end = totalLength - 1
                    RangeSpec(start = start, end = end, total = totalLength)
                }
                // Open-ended range: "bytes=500-" -> from byte 500 to end of file
                startStr.isNotEmpty() && endStr.isEmpty() -> {
                    val start = startStr.toLong()
                    if (start < 0 || start >= totalLength) return null
                    RangeSpec(start = start, end = totalLength - 1, total = totalLength)
                }
                // Closed range: "bytes=0-499"
                startStr.isNotEmpty() && endStr.isNotEmpty() -> {
                    val start = startStr.toLong()
                    val end = endStr.toLong()
                    if (start < 0 || start > end || start >= totalLength) return null
                    val clampedEnd = minOf(end, totalLength - 1)
                    RangeSpec(start = start, end = clampedEnd, total = totalLength)
                }
                else -> null
            }
        } catch (_: NumberFormatException) {
            null
        }
    }
}
