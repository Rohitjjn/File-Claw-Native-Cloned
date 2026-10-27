package com.example.formats.legacyoffice

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class OleTextRecoveryTest {

    @Test fun utf16RecoveryPreservesDevanagariArabicAndEmoji() {
        val text = "Executive summary with नमस्ते दुनिया and مرحبا بالعالم 🧠\rSecond paragraph with enough words."
        val bytes = ByteArray(512) + text.toByteArray(Charsets.UTF_16LE)
        val recovered = OleTextRecovery.fromUtf16Le(bytes, startOffset = 512)
        assertTrue(recovered.text.contains("Executive summary"))
        assertTrue(recovered.text.contains("नमस्ते दुनिया"))
        assertTrue(recovered.text.contains("مرحبا بالعالم"))
        assertTrue(recovered.text.contains("🧠"))
        assertTrue(recovered.text.contains("Second paragraph"))
    }

    @Test fun singleByteRecoveryHandlesCompressedLatinText() {
        val stream = ByteArrayOutputStream()
        repeat(128) { stream.write(0) }
        stream.write("Legacy compressed text with spaces\rAnother line".toByteArray(Charsets.ISO_8859_1))
        val recovered = OleTextRecovery.fromSingleByte(stream.toByteArray(), startOffset = 128)
        assertTrue(recovered.text.contains("Legacy compressed text"))
        assertTrue(recovered.text.contains("Another line"))
    }

    @Test fun binaryNoiseIsFilteredByQuality() {
        val noise = ByteArray(4096) { (it * 7).toByte() }
        val utf16Noise = OleTextRecovery.fromUtf16Le(noise)
        assertTrue(utf16Noise.paragraphs.size < 20)
    }
}
