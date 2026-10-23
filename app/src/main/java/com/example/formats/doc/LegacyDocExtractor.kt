package com.example.formats.doc

import com.example.formats.legacyoffice.Ole2CompoundFile
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure Kotlin extractor for legacy Word 97–2003 (.doc) binary documents.
 * Extracts clean text paragraphs without external dependencies.
 */
object LegacyDocExtractor {

    data class ExtractedDoc(
        val title: String,
        val text: String,
        val paragraphs: List<String>
    )

    fun extract(file: File): ExtractedDoc {
        if (!Ole2CompoundFile.isOle2File(file)) {
            return ExtractedDoc(file.name, "", emptyList())
        }

        Ole2CompoundFile.open(file).use { cfb ->
            val docBytes = cfb.openStream("WordDocument")
                ?: return ExtractedDoc(file.name, "", emptyList())

            return extractFromWordDocumentStream(docBytes, file.name)
        }
    }

    fun extractFromWordDocumentStream(bytes: ByteArray, fileName: String): ExtractedDoc {
        if (bytes.size < 512) return ExtractedDoc(fileName, "", emptyList())

        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val wIdent = bb.short.toInt() and 0xFFFF
        // 0xA5EC or 0xA5DC is the WordDocument magic
        if (wIdent != 0xA5EC && wIdent != 0xA5DC) {
            // Still attempt heuristic text scan
        }

        // FibRgLw97:
        // Byte 24: ccpText (length of main document text in characters)
        var ccpText = if (bytes.size >= 80) bb.getInt(24) else 0
        if (ccpText <= 0 || ccpText > 5_000_000) {
            ccpText = minOf(bytes.size, 500_000)
        }

        // In Word 97-2003, text often resides in the main stream starting after the header
        // (offset 512 or 1024), or in the Table stream via piece table.
        // We perform a robust UTF-16LE / ASCII text extractor that finds text runs.
        val paragraphs = mutableListOf<String>()
        val currentParagraph = StringBuilder()

        // Scan for runs of printable characters (UTF-16LE pairs or ASCII)
        var i = 512
        while (i < bytes.size - 1) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = bytes[i + 1].toInt() and 0xFF

            if (b1 == 0 && (b0 in 0x20..0x7E || b0 in 0xA0..0xFF || b0 == 0x09 || b0 == 0x0A || b0 == 0x0D)) {
                // Printable Latin/ASCII UTF-16LE
                val ch = b0.toChar()
                if (ch == '\r' || ch == '\n' || b0 == 0x07) { // 0x07 is Word cell delimiter
                    if (currentParagraph.isNotBlank()) {
                        paragraphs.add(currentParagraph.toString().trim())
                        currentParagraph.clear()
                    }
                } else if (ch == '\t') {
                    currentParagraph.append("    ")
                } else {
                    currentParagraph.append(ch)
                }
                i += 2
            } else if (b0 in 0x20..0x7E && b1 in 0x20..0x7E) {
                // ASCII text block
                val ch = b0.toChar()
                if (ch == '\r' || ch == '\n') {
                    if (currentParagraph.isNotBlank()) {
                        paragraphs.add(currentParagraph.toString().trim())
                        currentParagraph.clear()
                    }
                } else {
                    currentParagraph.append(ch)
                }
                i += 1
            } else {
                if (currentParagraph.length > 80 && currentParagraph.isNotBlank()) {
                    paragraphs.add(currentParagraph.toString().trim())
                    currentParagraph.clear()
                }
                i += 2
            }
        }

        if (currentParagraph.isNotBlank()) {
            paragraphs.add(currentParagraph.toString().trim())
        }

        // Clean up extracted paragraphs: remove binary gibberish runs
        val cleanParagraphs = paragraphs.filter { p ->
            val printableRatio = p.count { it.isLetterOrDigit() || it.isWhitespace() || it in ".,;:!?'\"()-+/*[]{}@#$%&" }.toDouble() / p.length
            p.length >= 3 && printableRatio >= 0.85
        }

        val fullText = cleanParagraphs.joinToString("\n\n")
        return ExtractedDoc(fileName, fullText, cleanParagraphs)
    }
}
