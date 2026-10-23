package com.example.core.io

import com.example.core.format.TextHeuristics
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.Charset

data class DetectedEncoding(val charset: Charset, val bomLength: Int)

object TextEncoding {

    private val CP1252: Charset = try {
        Charset.forName("windows-1252")
    } catch (e: Exception) {
        Charsets.ISO_8859_1
    }

    /** Maps a settings value ("Auto", "UTF-8", ...) to a charset; null means "detect". */
    fun resolve(requested: String): Charset? {
        if (requested.equals("Auto", ignoreCase = true)) return null
        return try {
            Charset.forName(requested)
        } catch (e: Exception) {
            null
        }
    }

    /** Detects encoding from the first bytes of a file: BOM, then UTF-8 validity, then UTF-16 pattern. */
    fun detect(head: ByteArray, n: Int = head.size): DetectedEncoding {
        val len = minOf(n, head.size)
        fun at(i: Int) = head[i].toInt() and 0xFF
        if (len >= 4 && at(0) == 0xFF && at(1) == 0xFE && at(2) == 0 && at(3) == 0) return DetectedEncoding(Charset.forName("UTF-32LE"), 4)
        if (len >= 4 && at(0) == 0 && at(1) == 0 && at(2) == 0xFE && at(3) == 0xFF) return DetectedEncoding(Charset.forName("UTF-32BE"), 4)
        if (len >= 3 && at(0) == 0xEF && at(1) == 0xBB && at(2) == 0xBF) return DetectedEncoding(Charsets.UTF_8, 3)
        if (len >= 2 && at(0) == 0xFE && at(1) == 0xFF) return DetectedEncoding(Charsets.UTF_16BE, 2)
        if (len >= 2 && at(0) == 0xFF && at(1) == 0xFE) return DetectedEncoding(Charsets.UTF_16LE, 2)
        if (len >= 4) {
            var evenNul = 0
            var oddNul = 0
            for (i in 0 until len / 2) {
                if (head[2 * i].toInt() == 0) evenNul++
                if (head[2 * i + 1].toInt() == 0) oddNul++
            }
            val pairs = len / 2
            if (oddNul > pairs * 0.3 && evenNul == 0) return DetectedEncoding(Charsets.UTF_16LE, 0)
            if (evenNul > pairs * 0.3 && oddNul == 0) return DetectedEncoding(Charsets.UTF_16BE, 0)
        }
        return if (TextHeuristics.isValidUtf8(head, len)) DetectedEncoding(Charsets.UTF_8, 0) else DetectedEncoding(CP1252, 0)
    }

    fun bomBytes(charset: Charset): ByteArray = when (charset.name().uppercase()) {
        "UTF-8" -> byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        "UTF-16LE" -> byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        "UTF-16BE" -> byteArrayOf(0xFE.toByte(), 0xFF.toByte())
        else -> ByteArray(0)
    }
}

/**
 * Result of loading a text file for display or editing.
 * [truncated] is a separate flag: the text itself never contains a "[truncated]" marker, so
 * saving what was loaded cannot silently destroy the rest of the file.
 */
data class TextLoadResult(
    val text: String,
    val charset: Charset,
    val hadBom: Boolean,
    val truncated: Boolean,
    val totalBytes: Long
) {
    /** Editing a truncated view would overwrite the file with a fragment, so it is never allowed. */
    val editable: Boolean get() = !truncated
}

class TextLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

object TextLoader {

    const val DEFAULT_MAX_CHARS = 2_000_000
    private const val HEAD_BYTES = 8192

    /** Loads at most [maxChars] characters. Throws [TextLoadException] instead of returning error text. */
    fun load(file: File, requested: String = "Auto", maxChars: Int = DEFAULT_MAX_CHARS): TextLoadResult {
        if (!file.isFile) throw TextLoadException("Not a readable file")
        try {
            val total = file.length()
            val head = ByteArray(HEAD_BYTES)
            var n = 0
            file.inputStream().use { input ->
                while (n < head.size) {
                    val r = input.read(head, n, head.size - n)
                    if (r < 0) break
                    n += r
                }
            }
            val detected = TextEncoding.detect(head, n)
            val explicit = TextEncoding.resolve(requested)
            val explicitMatchesDetected = explicit == detected.charset || when (explicit?.name()?.uppercase()) {
                "UTF-16" -> detected.charset.name().uppercase().startsWith("UTF-16")
                "UTF-32" -> detected.charset.name().uppercase().startsWith("UTF-32")
                else -> false
            }
            val explicitName = explicit?.name()?.uppercase()
            val charset = when {
                explicit == null || explicitMatchesDetected -> detected.charset
                explicitName == "UTF-16" && detected.charset.name().uppercase().startsWith("UTF-16") -> detected.charset
                explicitName == "UTF-16" -> Charset.forName("UTF-16BE")
                explicitName == "UTF-32" && detected.charset.name().uppercase().startsWith("UTF-32") -> detected.charset
                explicitName == "UTF-32" -> Charset.forName("UTF-32BE")
                else -> explicit ?: detected.charset
            }
            val hadBom = detected.bomLength > 0 && (explicit == null || explicitMatchesDetected)

            val sb = StringBuilder(minOf(maxChars, (total.coerceAtMost(Int.MAX_VALUE.toLong())).toInt() + 16))
            var truncated = false
            InputStreamReader(file.inputStream().buffered(64 * 1024), charset).use { reader ->
                val buf = CharArray(16 * 1024)
                while (true) {
                    val r = reader.read(buf)
                    if (r < 0) break
                    val room = maxChars - sb.length
                    if (r > room) {
                        sb.append(buf, 0, room)
                        truncated = true
                        break
                    }
                    sb.append(buf, 0, r)
                }
            }
            if (sb.isNotEmpty() && sb[0] == '\uFEFF') sb.deleteCharAt(0)
            if (truncated && sb.isNotEmpty() && Character.isHighSurrogate(sb[sb.length - 1])) sb.setLength(sb.length - 1)
            return TextLoadResult(sb.toString(), charset, hadBom, truncated, total)
        } catch (e: TextLoadException) {
            throw e
        } catch (e: Exception) {
            throw TextLoadException("Could not read file: ${e.javaClass.simpleName}", e)
        }
    }
}

/** Atomic text saving that keeps the original charset and BOM. */
object TextSaver {
    fun save(file: File, text: String, charset: Charset = Charsets.UTF_8, writeBom: Boolean = false) {
        val body = text.toByteArray(charset)
        AtomicFileWriter.write(file) { out ->
            if (writeBom) out.write(TextEncoding.bomBytes(charset))
            out.write(body)
        }
    }
}

/** One page of a very large text file, aligned to character and (when possible) line boundaries. */
data class TextWindow(val text: String, val startByte: Long, val endByte: Long, val hasMore: Boolean, val hasPrevious: Boolean)

/**
 * Reads a bounded byte range of a huge file without loading the rest. UTF-8 and single-byte text
 * are line-aligned when possible; UTF-16/UTF-32 windows retain code-unit boundaries. Match offsets
 * may begin mid-line so a search result can be placed at the start of the returned window.
 */
object TextWindowReader {

    fun read(file: File, startByte: Long, maxBytes: Int, charset: Charset = Charsets.UTF_8): TextWindow {
        require(startByte >= 0 && maxBytes > 0) { "bad window" }
        val total = file.length()
        if (startByte >= total) return TextWindow("", total, total, hasMore = false, hasPrevious = total > 0)

        val charsetName = charset.name().uppercase()
        val unitBytes = when (charsetName) {
            "UTF-16", "UTF-16LE", "UTF-16BE" -> 2
            "UTF-32", "UTF-32LE", "UTF-32BE" -> 4
            else -> 1
        }
        val alignedStart = if (unitBytes > 1) startByte - startByte % unitBytes else startByte
        val from = (startByte - alignedStart).toInt()
        val requested = maxOf(maxBytes, unitBytes).toLong() + unitBytes + 4
        val want = minOf(requested, total - alignedStart, Int.MAX_VALUE.toLong()).toInt()
        val buf = ByteArray(want)
        java.io.RandomAccessFile(file, "r").use { raf ->
            raf.seek(alignedStart)
            raf.readFully(buf)
        }

        if (unitBytes == 1 && charsetName == "UTF-8") {
            var skipped = 0
            while (from + skipped < buf.size && skipped < 4 && startByte > 0 && (buf[from + skipped].toInt() and 0xC0) == 0x80) skipped++
            // Search hits should begin at code-point boundaries; retain a guard for arbitrary callers.
            if (skipped > 0) return read(file, startByte + skipped, maxBytes, charset)
        }

        var to = minOf(buf.size.toLong(), from.toLong() + maxOf(maxBytes, unitBytes)).toInt()
        if (unitBytes > 1) {
            to = from + ((to - from) / unitBytes) * unitBytes
            if (to == from && buf.size >= from + unitBytes) to = from + unitBytes
        }
        val reachedEof = alignedStart + to >= total
        if (!reachedEof) {
            val newline = findLastNewline(buf, from, to, unitBytes, charsetName)
            if (newline >= from) {
                to = newline + unitBytes
            } else if (unitBytes == 1 && charsetName == "UTF-8") {
                var sequenceStart = to - 1
                while (sequenceStart > from && (buf[sequenceStart].toInt() and 0xC0) == 0x80) sequenceStart--
                val lead = buf[sequenceStart].toInt() and 0xFF
                val expectedLength = when {
                    (lead and 0x80) == 0 -> 1
                    (lead and 0xE0) == 0xC0 -> 2
                    (lead and 0xF0) == 0xE0 -> 3
                    (lead and 0xF8) == 0xF0 -> 4
                    else -> 1
                }
                if (to - sequenceStart < expectedLength) to = sequenceStart
            }
        }

        if (unitBytes == 2 && to - from >= 2) {
            val decoded = String(buf, from, to - from, charset)
            if (decoded.isNotEmpty() && Character.isHighSurrogate(decoded.last())) to -= 2
        }
        if (to <= from && buf.size > from) {
            if (unitBytes == 1 && charsetName == "UTF-8") {
                val lead = buf[from].toInt() and 0xFF
                val characterLength = when {
                    (lead and 0x80) == 0 -> 1
                    (lead and 0xE0) == 0xC0 -> 2
                    (lead and 0xF0) == 0xE0 -> 3
                    (lead and 0xF8) == 0xF0 -> 4
                    else -> 1
                }
                if (from + characterLength <= buf.size) to = from + characterLength
            } else if (buf.size >= from + unitBytes) {
                to = from + unitBytes
            }
        }

        val text = String(buf, from, to - from, charset)
        val start = alignedStart + from
        val end = alignedStart + to
        return TextWindow(text, start, end, hasMore = end < total, hasPrevious = start > 0)
    }

    private fun findLastNewline(buf: ByteArray, from: Int, to: Int, unitBytes: Int, charsetName: String): Int {
        var index = to - unitBytes
        while (index >= from) {
            val isNewline = when (unitBytes) {
                1 -> buf[index] == '\n'.code.toByte()
                2 -> if (charsetName.endsWith("LE")) {
                    buf[index] == '\n'.code.toByte() && index + 1 < to && buf[index + 1] == 0.toByte()
                } else {
                    buf[index] == 0.toByte() && index + 1 < to && buf[index + 1] == '\n'.code.toByte()
                }
                else -> if (charsetName.endsWith("LE")) {
                    buf[index] == '\n'.code.toByte() && buf[index + 1] == 0.toByte() && buf[index + 2] == 0.toByte() && buf[index + 3] == 0.toByte()
                } else {
                    buf[index] == 0.toByte() && buf[index + 1] == 0.toByte() && buf[index + 2] == 0.toByte() && buf[index + 3] == '\n'.code.toByte()
                }
            }
            if (isNewline) return index
            index -= unitBytes
        }
        return -1
    }
}
