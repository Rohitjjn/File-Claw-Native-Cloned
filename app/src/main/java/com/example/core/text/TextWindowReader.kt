package com.example.core.text

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * Pure Kotlin bounded memory window reader for massive text and log files (up to 500 MB+).
 * Never loads the full file into memory; reads clean newline-aligned byte windows on demand.
 */
class TextWindowReader(val file: File) : AutoCloseable {

    private val raf: RandomAccessFile = RandomAccessFile(file, "r")
    val totalBytes: Long = file.length()

    data class TextWindow(
        val text: String,
        val startByteOffset: Long,
        val endByteOffset: Long,
        val totalBytes: Long,
        val lineCount: Int,
        val isStartOfFile: Boolean,
        val isEndOfFile: Boolean
    )

    /**
     * Reads a slice of text starting near [requestedOffset], aligning cleanly to newline boundaries.
     * [windowSizeBytes] defaults to 128 KB (~2,000–3,000 lines).
     */
    fun readWindow(
        requestedOffset: Long,
        windowSizeBytes: Int = 128 * 1024,
        charset: Charset = Charsets.UTF_8
    ): TextWindow {
        if (totalBytes <= 0) {
            return TextWindow("", 0L, 0L, 0L, 0, isStartOfFile = true, isEndOfFile = true)
        }

        val clampedOffset = requestedOffset.coerceIn(0L, totalBytes - 1)
        var actualStart = clampedOffset

        // If not starting at the very beginning of the file, seek forward to the next newline boundary
        if (actualStart > 0) {
            raf.seek(actualStart)
            val scanBuf = ByteArray(minOf(4096, (totalBytes - actualStart).toInt()))
            val readCount = raf.read(scanBuf)
            if (readCount > 0) {
                var foundNewline = -1
                for (i in 0 until readCount) {
                    if (scanBuf[i] == '\n'.code.toByte()) {
                        foundNewline = i
                        break
                    }
                }
                if (foundNewline >= 0) {
                    actualStart += (foundNewline + 1)
                }
            }
        }

        if (actualStart >= totalBytes) {
            actualStart = (totalBytes - minOf(totalBytes, windowSizeBytes.toLong())).coerceAtLeast(0L)
        }

        val bytesToRead = minOf(windowSizeBytes.toLong(), totalBytes - actualStart).toInt()
        val buffer = ByteArray(bytesToRead)
        raf.seek(actualStart)
        val readTotal = raf.read(buffer)

        if (readTotal <= 0) {
            return TextWindow("", actualStart, actualStart, totalBytes, 0, actualStart == 0L, true)
        }

        // If not at the end of the file, trim back to the last newline boundary
        var actualEnd = actualStart + readTotal
        var effectiveLength = readTotal
        if (actualEnd < totalBytes) {
            var lastNewline = -1
            for (i in readTotal - 1 downTo 0) {
                if (buffer[i] == '\n'.code.toByte()) {
                    lastNewline = i
                    break
                }
            }
            if (lastNewline >= 0) {
                effectiveLength = lastNewline + 1
                actualEnd = actualStart + effectiveLength
            }
        }

        val decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)

        val decoded = decoder.decode(ByteBuffer.wrap(buffer, 0, effectiveLength)).toString()
        val lineCount = decoded.count { it == '\n' } + if (decoded.isNotEmpty() && !decoded.endsWith('\n')) 1 else 0

        return TextWindow(
            text = decoded,
            startByteOffset = actualStart,
            endByteOffset = actualEnd,
            totalBytes = totalBytes,
            lineCount = lineCount,
            isStartOfFile = actualStart == 0L,
            isEndOfFile = actualEnd >= totalBytes
        )
    }

    /**
     * Estimates total lines in the file based on a sample of lines from the head.
     */
    fun estimateTotalLines(sampleBytes: Int = 64 * 1024): Long {
        if (totalBytes <= 0) return 0L
        val sampleSize = minOf(totalBytes, sampleBytes.toLong()).toInt()
        val sampleBuf = ByteArray(sampleSize)
        synchronized(raf) {
            raf.seek(0)
            raf.readFully(sampleBuf)
        }
        val newlines = sampleBuf.count { it == '\n'.code.toByte() }
        if (newlines == 0) return 1L
        val avgLineBytes = sampleSize.toDouble() / newlines
        return (totalBytes / avgLineBytes).toLong().coerceAtLeast(newlines.toLong())
    }

    /**
     * Translates a scroll fraction (0.0 to 1.0) into a seek byte offset.
     */
    fun byteOffsetFromFraction(fraction: Float): Long {
        val f = fraction.coerceIn(0.0f, 1.0f)
        return (f * totalBytes).toLong().coerceIn(0L, totalBytes)
    }

    override fun close() {
        try {
            raf.close()
        } catch (_: Exception) {}
    }
}
