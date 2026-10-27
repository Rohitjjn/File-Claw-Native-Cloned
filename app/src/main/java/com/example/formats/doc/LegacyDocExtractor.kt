package com.example.formats.doc

import com.example.formats.legacyoffice.Ole2CompoundFile
import com.example.formats.legacyoffice.OleTextRecovery
import java.io.File

/**
 * Best-effort text recovery for legacy Word 97-2003 (.doc) documents.
 *
 * The binary FIB/CLX piece table is not fully reconstructed; both UTF-16LE and
 * compressed 8-bit text runs are scanned with a bounded shared recovery policy.
 * This is a read-only text preview, not a formatted Word renderer.
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
        return try {
            Ole2CompoundFile.open(file).use { cfb ->
                val docBytes = cfb.openStream("WordDocument", maxBytes = 30_000_000)
                    ?: return ExtractedDoc(file.name, "", emptyList())
                extractFromWordDocumentStream(docBytes, file.name)
            }
        } catch (_: Throwable) {
            ExtractedDoc(file.name, "", emptyList())
        }
    }

    fun extractFromWordDocumentStream(bytes: ByteArray, fileName: String): ExtractedDoc {
        if (bytes.size < 512) return ExtractedDoc(fileName, "", emptyList())
        val utf16 = OleTextRecovery.fromUtf16Le(bytes, startOffset = 512)
        val singleByte = OleTextRecovery.fromSingleByte(bytes, startOffset = 512)
        val best = listOf(utf16, singleByte).maxByOrNull { OleTextRecovery.qualityScore(it) }
            ?: OleTextRecovery.RecoveredText(emptyList(), "")
        return ExtractedDoc(fileName, best.text, best.paragraphs)
    }
}
