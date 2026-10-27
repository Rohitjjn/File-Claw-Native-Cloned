package com.example.formats.ppt

import com.example.formats.legacyoffice.Ole2CompoundFile
import com.example.formats.legacyoffice.OleTextRecovery
import java.io.File

/**
 * Bounded text recovery for legacy PowerPoint 97-2003 (.ppt) files.
 *
 * It walks the PowerPoint Document record tree and reads only `TextCharsAtom`
 * and `TextBytesAtom` payloads. Layout, images, charts and animations are not
 * reconstructed. This is intentionally a read-only text preview, not a slide
 * renderer; unsupported legacy features are documented in `docs/FILE_FORMATS.md`.
 */
object LegacyPptExtractor {

    data class ExtractedPpt(
        val title: String,
        val text: String,
        val paragraphs: List<String>
    )

    private const val TEXT_CHARS_ATOM = 0x0FA0
    private const val TEXT_BYTES_ATOM = 0x0FA8
    private const val MAX_RECORDS = 200_000
    private const val MAX_PARAGRAPHS = 50_000
    private const val MAX_STREAM_BYTES = 30_000_000

    fun extract(file: File): ExtractedPpt {
        if (!Ole2CompoundFile.isOle2File(file)) {
            return ExtractedPpt(file.name, "", emptyList())
        }
        return try {
            Ole2CompoundFile.open(file).use { cfb ->
                val stream = cfb.openStream("PowerPoint Document", maxBytes = MAX_STREAM_BYTES)
                    ?: return ExtractedPpt(file.name, "", emptyList())
                val paragraphs = extractFromPowerPointStream(stream)
                ExtractedPpt(file.name, paragraphs.joinToString("\n\n"), paragraphs)
            }
        } catch (_: Throwable) {
            ExtractedPpt(file.name, "", emptyList())
        }
    }

    fun extractFromPowerPointStream(bytes: ByteArray): List<String> {
        val paragraphs = mutableListOf<String>()
        var position = 0
        var records = 0
        while (position + 8 <= bytes.size && records++ < MAX_RECORDS && paragraphs.size < MAX_PARAGRAPHS) {
            val header = u16(bytes, position)
            val type = u16(bytes, position + 2)
            val length = u32(bytes, position + 4)
            val payloadStart = position + 8
            if (length < 0L || length > (bytes.size - payloadStart).toLong()) break
            val payloadEnd = payloadStart + length.toInt()
            when (type) {
                TEXT_CHARS_ATOM -> paragraphs += OleTextRecovery
                    .fromUtf16Le(bytes, payloadStart, payloadEnd)
                    .paragraphs
                TEXT_BYTES_ATOM -> paragraphs += OleTextRecovery
                    .fromSingleByte(bytes, payloadStart, payloadEnd)
                    .paragraphs
            }
            val isContainer = (header and 0x000F) == 0x000F
            position = if (isContainer) payloadStart else payloadEnd
        }
        return paragraphs.distinct().take(MAX_PARAGRAPHS)
    }

    private fun u16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun u32(bytes: ByteArray, offset: Int): Long =
        (bytes[offset].toLong() and 0xFFL) or
            ((bytes[offset + 1].toLong() and 0xFFL) shl 8) or
            ((bytes[offset + 2].toLong() and 0xFFL) shl 16) or
            ((bytes[offset + 3].toLong() and 0xFFL) shl 24)
}
