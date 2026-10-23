package com.example.formats.doc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LegacyDocExtractorTest {

    @Test
    fun extractFromWordDocumentStream_recoversTextParagraphs() {
        val stream = java.io.ByteArrayOutputStream()

        // 512 bytes FIB header
        val header = ByteArray(512)
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        bb.putShort(0xA5EC.toShort()) // WordDocument magic
        bb.position(24)
        bb.putInt(100) // ccpText
        stream.write(header)

        // Text body in UTF-16LE
        val p1 = "First executive summary paragraph.\r"
        val p2 = "Second paragraph with key findings.\r"

        for (ch in p1) {
            stream.write(ch.code and 0xFF)
            stream.write((ch.code shr 8) and 0xFF)
        }
        for (ch in p2) {
            stream.write(ch.code and 0xFF)
            stream.write((ch.code shr 8) and 0xFF)
        }

        val extracted = LegacyDocExtractor.extractFromWordDocumentStream(stream.toByteArray(), "memo.doc")

        assertEquals("memo.doc", extracted.title)
        assertTrue(extracted.paragraphs.isNotEmpty())
        assertTrue("Should extract first paragraph", extracted.text.contains("First executive summary paragraph"))
        assertTrue("Should extract second paragraph", extracted.text.contains("Second paragraph with key findings"))
    }
}
