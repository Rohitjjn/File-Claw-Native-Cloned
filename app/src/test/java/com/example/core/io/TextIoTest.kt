package com.example.core.io

import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class TextIoTest {
    private lateinit var dir: File

    @Before fun setUp() { dir = Files.createTempDirectory("tio").toFile() }
    @After fun tearDown() { dir.deleteRecursively() }

    private fun file(name: String, bytes: ByteArray) = File(dir, name).also { it.writeBytes(bytes) }
    private fun b(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test fun detectsBomsAndHeuristics() {
        assertEquals("UTF-8", TextEncoding.detect(b(0xEF, 0xBB, 0xBF, 0x41)).charset.name())
        assertEquals(3, TextEncoding.detect(b(0xEF, 0xBB, 0xBF, 0x41)).bomLength)
        assertEquals("UTF-16LE", TextEncoding.detect(b(0xFF, 0xFE, 0x41, 0x00)).charset.name())
        assertEquals("UTF-16BE", TextEncoding.detect(b(0xFE, 0xFF, 0x00, 0x41)).charset.name())
        assertEquals("UTF-32LE", TextEncoding.detect(b(0xFF, 0xFE, 0, 0, 0x41, 0, 0, 0)).charset.name())
        assertEquals("UTF-32BE", TextEncoding.detect(b(0, 0, 0xFE, 0xFF, 0, 0, 0, 0x41)).charset.name())
        assertEquals("UTF-16LE", TextEncoding.detect("hello world".toByteArray(Charsets.UTF_16LE)).charset.name())
        assertEquals(0, TextEncoding.detect("hello world".toByteArray(Charsets.UTF_16LE)).bomLength)
        assertEquals("UTF-16BE", TextEncoding.detect("hello world".toByteArray(Charsets.UTF_16BE)).charset.name())
        assertEquals("UTF-8", TextEncoding.detect("héllo".toByteArray()).charset.name())
        assertEquals("windows-1252", TextEncoding.detect(b(0x63, 0x61, 0x66, 0xE9, 0x20, 0x62)).charset.name())
    }

    @Test fun resolveRequestedEncoding() {
        assertNull(TextEncoding.resolve("Auto"))
        assertEquals("UTF-16", TextEncoding.resolve("UTF-16")?.name())
        assertNull(TextEncoding.resolve("NOT-A-CHARSET"))
        assertEquals(3, TextEncoding.bomBytes(Charsets.UTF_8).size)
        assertEquals(2, TextEncoding.bomBytes(Charsets.UTF_16LE).size)
        assertEquals(2, TextEncoding.bomBytes(Charsets.UTF_16BE).size)
        assertEquals(0, TextEncoding.bomBytes(Charsets.ISO_8859_1).size)
    }

    @Test fun loadsSmallUtf8AndStripsBom() {
        val f = file("a.txt", b(0xEF, 0xBB, 0xBF) + "hi ✓".toByteArray())
        val r = TextLoader.load(f)
        assertEquals("hi ✓", r.text)
        assertTrue(r.hadBom)
        assertFalse(r.truncated)
        assertTrue(r.editable)
        assertEquals(f.length(), r.totalBytes)
    }

    @Test fun truncationIsAFlagNotAMarkerInTheText() {
        val f = file("big.txt", "abcdefghij".repeat(100).toByteArray())
        val r = TextLoader.load(f, maxChars = 250)
        assertEquals(250, r.text.length)
        assertTrue(r.truncated)
        assertFalse(r.editable)
        assertFalse(r.text.contains("truncated"))
    }

    @Test fun truncationNeverSplitsSurrogatePair() {
        val f = file("emoji.txt", "😀".repeat(10).toByteArray())
        val r = TextLoader.load(f, maxChars = 5)
        assertTrue(r.truncated)
        assertFalse(Character.isHighSurrogate(r.text.last()))
    }

    @Test fun exactlyAtLimitIsNotTruncated() {
        val f = file("x.txt", "a".repeat(100).toByteArray())
        assertFalse(TextLoader.load(f, maxChars = 100).truncated)
    }

    @Test fun explicitEncodingOverridesDetection() {
        val f = file("l1.txt", "caf\u00E9".toByteArray(Charsets.ISO_8859_1))
        assertEquals("café", TextLoader.load(f, "ISO-8859-1").text)
        val u16 = file("u16.txt", "héllo".toByteArray(Charsets.UTF_16LE))
        assertEquals("héllo", TextLoader.load(u16).text)
        val u16Bom = file("u16-bom.txt", byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "héllo".toByteArray(Charsets.UTF_16LE))
        val genericUtf16 = TextLoader.load(u16Bom, "UTF-16")
        assertEquals("héllo", genericUtf16.text)
        assertEquals("UTF-16LE", genericUtf16.charset.name())
        assertTrue(genericUtf16.hadBom)
    }

    @Test fun missingFileThrowsInsteadOfReturningErrorText() {
        try {
            TextLoader.load(File(dir, "nope.txt"))
            fail("expected exception")
        } catch (e: TextLoadException) {
            assertTrue(e.message!!.isNotEmpty())
        }
    }

    @Test fun emptyFileLoadsAsEmptyText() {
        val r = TextLoader.load(file("e.txt", ByteArray(0)))
        assertEquals("", r.text)
        assertFalse(r.truncated)
    }

    @Test fun saverRoundTripsAndPreservesBom() {
        val f = File(dir, "out.txt")
        TextSaver.save(f, "héllo", Charsets.UTF_8, writeBom = true)
        val bytes = f.readBytes()
        assertEquals(0xEF.toByte(), bytes[0])
        val back = TextLoader.load(f)
        assertEquals("héllo", back.text)
        assertTrue(back.hadBom)
        TextSaver.save(f, "plain")
        assertEquals("plain", f.readText())
        TextSaver.save(f, "hi", Charsets.UTF_16LE, writeBom = true)
        assertEquals("hi", TextLoader.load(f).text)
    }

    @Test fun atomicWriterLeavesOriginalIntactOnFailure() {
        val f = file("keep.txt", "ORIGINAL".toByteArray())
        try {
            AtomicFileWriter.write(f) { out ->
                out.write("partial".toByteArray())
                throw IOException("disk full")
            }
            fail("expected IOException")
        } catch (e: IOException) {
            assertEquals("disk full", e.message)
        }
        assertEquals("ORIGINAL", f.readText())
        assertTrue("no temp files left", dir.listFiles()!!.none { it.name.endsWith(".tmp") })
    }

    @Test fun atomicWriterReplacesAndCreates() {
        val f = File(dir, "new.bin")
        AtomicFileWriter.writeBytes(f, byteArrayOf(1, 2, 3))
        assertArrayEquals(byteArrayOf(1, 2, 3), f.readBytes())
        AtomicFileWriter.writeBytes(f, byteArrayOf(9))
        assertArrayEquals(byteArrayOf(9), f.readBytes())
        assertEquals(1, dir.listFiles()!!.size)
    }

    @Test fun atomicWriterRejectsMissingDirectory() {
        try {
            AtomicFileWriter.writeBytes(File(dir, "no/such/dir/x.txt"), byteArrayOf(1))
            fail("expected IOException")
        } catch (e: IOException) {
            // expected
        }
    }

    @Test fun windowReaderPagesWithoutSplittingLinesOrCharacters() {
        val lines = (1..200).joinToString("\n") { "line $it é✓" } + "\n"
        val f = file("w.txt", lines.toByteArray())
        val sb = StringBuilder()
        var pos = 0L
        var pages = 0
        while (true) {
            val w = TextWindowReader.read(f, pos, 500)
            sb.append(w.text)
            pages++
            if (pages > 1) assertTrue(w.hasPrevious)
            if (!w.hasMore) break
            assertTrue(w.endByte > pos)
            pos = w.endByte
        }
        assertTrue(pages > 5)
        assertEquals(lines, sb.toString())
    }

    @Test fun windowReaderEdgeCases() {
        val f = file("s.txt", "short".toByteArray())
        val w = TextWindowReader.read(f, 0, 1000)
        assertEquals("short", w.text)
        assertFalse(w.hasMore)
        assertFalse(w.hasPrevious)
        val past = TextWindowReader.read(f, 100, 10)
        assertEquals("", past.text)
        assertTrue(past.hasPrevious)
        // start in the middle of a multi-byte character
        val g = file("m.txt", "ééééé".toByteArray())
        val mid = TextWindowReader.read(g, 1, 4)
        assertFalse(mid.text.contains('\uFFFD'))
        // a window with no newline still never cuts a UTF-8 character
        val noNl = file("n.txt", "é".repeat(100).toByteArray())
        assertFalse(TextWindowReader.read(noNl, 0, 51).text.contains('\uFFFD'))
    }

    @Test fun windowReaderCanOpenUtf16AtSearchHitOffset() {
        val charset = Charset.forName("UTF-16LE")
        val prefix = "Alpha\n"
        val text = "$prefix BETA\nGamma"
        val file = file("utf16-window.txt", byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + text.toByteArray(charset))
        val betaOffset = 2L + "$prefix ".toByteArray(charset).size

        val window = TextWindowReader.read(file, betaOffset, 16, charset)

        assertTrue(window.text.startsWith("BETA"))
        assertFalse(window.text.contains('\uFFFD'))
        assertTrue(window.startByte > 0)
    }

    @Test(expected = IllegalArgumentException::class) fun windowReaderRejectsBadArgs() {
        TextWindowReader.read(File(dir, "x"), -1, 10)
    }
}
