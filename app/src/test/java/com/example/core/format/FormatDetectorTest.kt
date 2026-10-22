package com.example.core.format

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FormatDetectorTest {

    private lateinit var dir: File

    @Before fun setUp() { dir = Files.createTempDirectory("fd").toFile() }
    @After fun tearDown() { dir.deleteRecursively() }

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }
    private fun pad(b: ByteArray, n: Int = 64) = b.copyOf(maxOf(n, b.size))
    private fun detect(name: String, header: ByteArray, len: Long = header.size.toLong()) =
        FormatDetector.detect(header, len, name)

    private fun zip(name: String, vararg entries: Pair<String, String>): File {
        val f = File(dir, name)
        ZipOutputStream(FileOutputStream(f)).use { z ->
            for ((n, c) in entries) { z.putNextEntry(ZipEntry(n)); z.write(c.toByteArray()); z.closeEntry() }
        }
        return f
    }

    @Test fun pdfWithJunkPrefix() {
        val h = "junk\n%PDF-1.7\n".toByteArray()
        assertEquals("pdf", detect("x.bin", h).spec.id)
    }

    @Test fun commonImageSignatures() {
        assertEquals("png", detect("a", pad(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))).spec.id)
        assertEquals("jpeg", detect("a", pad(bytes(0xFF, 0xD8, 0xFF, 0xE0))).spec.id)
        assertEquals("gif", detect("a", "GIF89a....".toByteArray()).spec.id)
        assertEquals("webp", detect("a", "RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".toByteArray(Charsets.ISO_8859_1)).spec.id)
        assertEquals("bmp", detect("a", pad(byteArrayOf('B'.code.toByte(), 'M'.code.toByte(), 1, 2, 3, 4, 0, 0, 0, 0))).spec.id)
        assertEquals("ico", detect("a", pad(bytes(0, 0, 1, 0, 2, 0))).spec.id)
        assertEquals("tiff", detect("a", pad(bytes(0x49, 0x49, 0x2A, 0x00))).spec.id)
        assertEquals("psd", detect("a", "8BPS....".toByteArray()).spec.id)
    }

    @Test fun mediaSignatures() {
        fun ftyp(brand: String) = (String(bytes(0, 0, 0, 0x18)) + "ftyp" + brand + "\u0000\u0000\u0000\u0000").toByteArray(Charsets.ISO_8859_1)
        assertEquals("mp4", detect("a", ftyp("isom")).spec.id)
        assertEquals("heif", detect("a", ftyp("heic")).spec.id)
        assertEquals("heif", detect("a", ftyp("avif")).spec.id)
        assertEquals("m4a", detect("a", ftyp("M4A ")).spec.id)
        assertEquals("video-other", detect("a", ftyp("qt  ")).spec.id)
        assertEquals("3gp", detect("a", ftyp("3gp4")).spec.id)
        assertEquals("mkv", detect("a", pad(bytes(0x1A, 0x45, 0xDF, 0xA3))).spec.id)
        assertEquals("webm", detect("a", pad(bytes(0x1A, 0x45, 0xDF, 0xA3, 0x42, 0x82, 0x84) + "webm".toByteArray())).spec.id)
        assertEquals("ogg", detect("a", "OggS....".toByteArray()).spec.id)
        assertEquals("flac", detect("a", "fLaC....".toByteArray()).spec.id)
        assertEquals("mp3", detect("a", "ID3\u0003....".toByteArray()).spec.id)
        assertEquals("wav", detect("a", "RIFF\u0000\u0000\u0000\u0000WAVEfmt ".toByteArray(Charsets.ISO_8859_1)).spec.id)
        assertEquals("video-other", detect("a", "RIFF\u0000\u0000\u0000\u0000AVI LIST".toByteArray(Charsets.ISO_8859_1)).spec.id)
        assertEquals("midi", detect("a", "MThd....".toByteArray()).spec.id)
        assertEquals("mp3", detect("noext", pad(bytes(0xFF, 0xFB, 0x90, 0x00))).spec.id)
    }

    @Test fun archiveAndCompressionSignatures() {
        assertEquals("gz", detect("a", pad(bytes(0x1F, 0x8B, 8))).spec.id)
        assertEquals("bz2", detect("a", "BZh9....".toByteArray()).spec.id)
        assertEquals("xz", detect("a", pad(bytes(0xFD, 0x37, 0x7A, 0x58, 0x5A, 0x00))).spec.id)
        assertEquals("7z", detect("a", pad(bytes(0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C))).spec.id)
        assertEquals("rar", detect("a", pad(bytes(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00))).spec.id)
        assertEquals("archive-other", detect("a", pad(bytes(0x28, 0xB5, 0x2F, 0xFD))).spec.id)
        assertEquals("ar", detect("a", "!<arch>\nfoo".toByteArray()).spec.id)
        assertEquals("cpio", detect("a", "070701....".toByteArray()).spec.id)
        val tar = ByteArray(512); "ustar".toByteArray().copyInto(tar, 257)
        assertEquals("tar", detect("a", tar).spec.id)
        assertEquals("tgz", detect("x.tgz", pad(bytes(0x1F, 0x8B, 8))).spec.id)
        assertEquals("tbz2", detect("x.tar.bz2", "BZh9....".toByteArray()).spec.id)
        assertEquals("txz", detect("x.tar.xz", pad(bytes(0xFD, 0x37, 0x7A, 0x58, 0x5A, 0x00))).spec.id)
    }

    @Test fun otherBinarySignatures() {
        assertEquals("ole2", detect("x.doc", pad(bytes(0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1))).spec.id)
        assertEquals("rtf", detect("x", "{\\rtf1\\ansi hi}".toByteArray()).spec.id)
        assertEquals("sqlite", detect("x", ("SQLite format 3\u0000" + "x".repeat(40)).toByteArray(Charsets.ISO_8859_1)).spec.id)
        assertEquals("data", detect("x.parquet", "PAR1....".toByteArray()).spec.id)
        assertEquals("ttf", detect("x", pad(bytes(0, 1, 0, 0, 0, 0xF))).spec.id)
        assertEquals("ttf", detect("x", "OTTO....".toByteArray()).spec.id)
        assertEquals("woff", detect("x", "wOF2....".toByteArray()).spec.id)
        assertEquals("exe", detect("x.dll", "MZ......".toByteArray()).spec.id)
        assertEquals("exe", detect("x", pad(bytes(0x7F, 0x45, 0x4C, 0x46))).spec.id)
        assertEquals("exe", detect("x", pad(bytes(0xCA, 0xFE, 0xBA, 0xBE))).spec.id)
        assertEquals("exe", detect("x", "dex\n035\u0000".toByteArray()).spec.id)
        assertEquals("exe", detect("x", pad(bytes(0, 0x61, 0x73, 0x6D))).spec.id)
        assertEquals("data", detect("x", "bplist00....".toByteArray()).spec.id)
        assertEquals("data", detect("x", "\u0093NUMPY....".toByteArray(Charsets.ISO_8859_1)).spec.id)
        assertEquals("model-other", detect("x", "glTF....".toByteArray()).spec.id)
        assertEquals("model-other", detect("x", "ply\nformat".toByteArray()).spec.id)
        assertEquals("model-other", detect("x.dwg", "AC1032....".toByteArray()).spec.id)
    }

    @Test fun stlDetectionBinaryByLengthAndAsciiByContent() {
        val h = ByteArray(84)
        h[80] = 2  // two triangles
        assertEquals("stl", detect("model.bin", h, 84L + 100L).spec.id)
        assertEquals("data", detect("model.bin", h, 84L + 99L).spec.id)
        val ascii = "solid cube\n facet normal 0 0 1\n outer loop".toByteArray()
        val d = detect("cube.stl", ascii)
        assertEquals("stl", d.spec.id)
        assertEquals("stl", detect("noext", ascii).spec.id)
    }

    @Test fun zipFamilyRefinement() {
        assertEquals("docx", FormatDetector.detect(zip("a.zip", "word/document.xml" to "<x/>")).spec.id)
        assertEquals("xlsx", FormatDetector.detect(zip("b.bin", "xl/workbook.xml" to "<x/>")).spec.id)
        assertEquals("pptx", FormatDetector.detect(zip("c.bin", "ppt/presentation.xml" to "<x/>")).spec.id)
        assertEquals("epub", FormatDetector.detect(zip("d.bin", "mimetype" to "application/epub+zip")).spec.id)
        assertEquals("odt", FormatDetector.detect(zip("e.bin", "mimetype" to "application/vnd.oasis.opendocument.text")).spec.id)
        assertEquals("ods", FormatDetector.detect(zip("f.bin", "mimetype" to "application/vnd.oasis.opendocument.spreadsheet")).spec.id)
        assertEquals("odp", FormatDetector.detect(zip("g.bin", "mimetype" to "application/vnd.oasis.opendocument.presentation")).spec.id)
        assertEquals("zip-app", FormatDetector.detect(zip("h.bin", "AndroidManifest.xml" to "x", "classes.dex" to "x")).spec.id)
        assertEquals("zip-app", FormatDetector.detect(zip("i.bin", "META-INF/MANIFEST.MF" to "x")).spec.id)
        assertEquals("zip", FormatDetector.detect(zip("j.bin", "readme.txt" to "hi")).spec.id)
        assertEquals("zip-app", FormatDetector.detect(zip("k.jar", "readme.txt" to "hi")).spec.id)
    }

    @Test fun renamedDocxStillDetectedByContentAndNotFlaggedWhenExtensionAgrees() {
        val real = zip("report.docx", "word/document.xml" to "<x/>")
        val d = FormatDetector.detect(real)
        assertEquals("docx", d.spec.id)
        assertFalse(d.mismatch)
        assertEquals(Confidence.HIGH, d.confidence)
        val renamed = zip("report.zip", "word/document.xml" to "<x/>")
        assertEquals("docx", FormatDetector.detect(renamed).spec.id)
    }

    @Test fun gzipOfTarIsDetectedAsTgz() {
        val tarBytes = ByteArray(1024); "ustar".toByteArray().copyInto(tarBytes, 257)
        val f = File(dir, "bundle.gz")
        GZIPOutputStream(FileOutputStream(f)).use { it.write(tarBytes) }
        assertEquals("tgz", FormatDetector.detect(f).spec.id)
        val plain = File(dir, "plain.gz")
        GZIPOutputStream(FileOutputStream(plain)).use { it.write("hello".toByteArray()) }
        assertEquals("gz", FormatDetector.detect(plain).spec.id)
    }

    @Test fun contentBeatsMisleadingExtension() {
        val png = pad(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        val d = detect("photo.jpg", png)
        assertEquals("png", d.spec.id)
        assertTrue(d.mismatch)
        val agree = detect("photo.png", png)
        assertFalse(agree.mismatch)
    }

    @Test fun binaryHiddenBehindTextExtensionIsFlagged() {
        val d = detect("notes.txt", ByteArray(64) { if (it % 3 == 0) 0 else 7 })
        assertEquals(FileKind.BINARY, d.kind)
        assertTrue(d.mismatch)
    }

    @Test fun signatureExpectedFormatsFallBackToTextOrFlagMismatch() {
        val textAsPdf = detect("fake.pdf", "just some words".toByteArray())
        assertEquals(FileKind.TEXT, textAsPdf.kind)
        assertTrue(textAsPdf.mismatch)
        val garbageAsPng = detect("bad.png", ByteArray(64) { 0 })
        assertEquals("png", garbageAsPng.spec.id)
        assertEquals(Confidence.LOW, garbageAsPng.confidence)
        assertTrue(garbageAsPng.mismatch)
    }

    @Test fun dbExtensionWithoutSqliteSignatureIsNotTrusted() {
        assertEquals(FileKind.TEXT, detect("notes.db", "plain text".toByteArray()).kind)
        assertEquals(FileKind.BINARY, detect("blob.db", ByteArray(64) { 0 }).kind)
    }

    @Test fun extensionOnlyAndUninformativeExtensions() {
        assertEquals("code-jvm", detect("Main.kt", "fun main() {}".toByteArray()).spec.id)
        assertEquals(Confidence.MEDIUM, detect("Main.kt", "fun main() {}".toByteArray()).confidence)
        assertEquals("svg", detect("noext", "<svg xmlns='x'/>".toByteArray()).spec.id)
        assertEquals("svg", detect("a.bin", "<?xml version='1.0'?><svg/>".toByteArray()).spec.id)
        assertEquals("html", detect("noext", "<!DOCTYPE html><html>".toByteArray()).spec.id)
        assertEquals("xml", detect("noext", "<?xml version='1.0'?><a/>".toByteArray()).spec.id)
        assertEquals("json", detect("noext", "  {\"a\":1}".toByteArray()).spec.id)
        assertEquals("markup-text", detect("key", "-----BEGIN CERTIFICATE-----".toByteArray()).spec.id)
        assertEquals("text", detect("Makefile", "all:\n\techo".toByteArray()).spec.id)
        assertEquals(FileKind.BINARY, detect("mystery", ByteArray(100) { (it * 7).toByte() }).kind)
        assertEquals(FileKind.TEXT, detect("empty", ByteArray(0)).kind)
    }

    @Test fun txtStaysTextEvenIfContentLooksLikeHtml() {
        assertEquals("text", detect("page.txt", "<html><body>hi</body></html>".toByteArray()).spec.id)
    }

    @Test fun unreadableFileIsLowConfidenceBinary() {
        val d = FormatDetector.detect(File(dir, "missing.bin"))
        assertEquals(Confidence.LOW, d.confidence)
        assertEquals(FileKind.BINARY, d.kind)
    }

    @Test fun textHeuristics() {
        assertTrue(TextHeuristics.looksLikeText("héllo wörld".toByteArray()))
        assertTrue(TextHeuristics.looksLikeText("caf\u00E9".toByteArray(Charsets.ISO_8859_1)))
        assertTrue(TextHeuristics.looksLikeText("hi there".toByteArray(Charsets.UTF_16LE)))
        assertTrue(TextHeuristics.looksLikeText("hi there".toByteArray(Charsets.UTF_16BE)))
        assertTrue(TextHeuristics.looksLikeText(bytes(0xEF, 0xBB, 0xBF, 0x41)))
        assertFalse(TextHeuristics.looksLikeText(ByteArray(10) { 0 }))
        assertFalse(TextHeuristics.looksLikeText(ByteArray(50) { 1 }))
        assertTrue(TextHeuristics.isValidUtf8(bytes(0xE2, 0x82)))      // truncated sequence at end of sample
        assertFalse(TextHeuristics.isValidUtf8(bytes(0xE2, 0x41, 0x41)))
        assertFalse(TextHeuristics.isValidUtf8(bytes(0xFF)))
        assertFalse(TextHeuristics.isValidUtf8(bytes(0xC3, 0x28)))
    }
}
