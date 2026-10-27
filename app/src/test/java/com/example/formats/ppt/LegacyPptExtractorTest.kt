package com.example.formats.ppt

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LegacyPptExtractorTest {

    private fun record(type: Int, payload: ByteArray, container: Boolean = false): ByteArray {
        val out = ByteArrayOutputStream()
        val header = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        header.putShort((if (container) 0x000F else 0x0000).toShort())
        header.putShort(type.toShort())
        out.write(header.array())
        val length = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        length.putInt(payload.size)
        out.write(length.array())
        out.write(payload)
        return out.toByteArray()
    }

    @Test fun extractsTextCharsAndTextBytesAtoms() {
        val stream = ByteArrayOutputStream()
        stream.write(record(0x0FF0, ByteArray(0), container = true))
        stream.write(record(0x0FA0, "Quarterly results with नमस्ते\r".toByteArray(Charsets.UTF_16LE)))
        stream.write(record(0x0FA8, "Legacy bytes atom text\r".toByteArray(Charsets.ISO_8859_1)))
        stream.write(record(0x03EF, "Ignored binary content".toByteArray()))

        val paragraphs = LegacyPptExtractor.extractFromPowerPointStream(stream.toByteArray()).joinToString("\n")
        assertTrue(paragraphs.contains("Quarterly results"))
        assertTrue(paragraphs.contains("नमस्ते"))
        assertTrue(paragraphs.contains("Legacy bytes atom text"))
        assertTrue(!paragraphs.contains("Ignored binary content"))
    }

    @Test fun malformedRecordLengthStopsWithoutThrowing() {
        val stream = ByteArrayOutputStream()
        stream.write(record(0x0FA0, "Valid text".toByteArray(Charsets.UTF_16LE)))
        stream.write(byteArrayOf(0, 0, 0xFA.toByte(), 0x0F, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x7F.toByte()))
        val paragraphs = LegacyPptExtractor.extractFromPowerPointStream(stream.toByteArray())
        assertTrue(paragraphs.isEmpty() || paragraphs.joinToString(" ").contains("Valid text"))
    }
}
