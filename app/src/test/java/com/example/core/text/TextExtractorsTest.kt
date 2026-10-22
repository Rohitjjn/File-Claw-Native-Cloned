package com.example.core.text

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class TextExtractorsTest {
    private lateinit var dir: File

    @Before fun setUp() { dir = Files.createTempDirectory("tx").toFile() }
    @After fun tearDown() { dir.deleteRecursively() }

    private fun zip(name: String, vararg entries: Pair<String, String>): File {
        val f = File(dir, name)
        ZipOutputStream(FileOutputStream(f)).use { z ->
            for ((n, c) in entries) { z.putNextEntry(ZipEntry(n)); z.write(c.toByteArray()); z.closeEntry() }
        }
        return f
    }

    // ---------------- RTF ----------------
    @Test fun rtfExtractsTextAndSkipsTables() {
        val rtf = "{\\rtf1\\ansi{\\fonttbl{\\f0 Arial;}}{\\colortbl;\\red0\\green0\\blue0;}" +
            "\\pard Hello \\b World\\b0\\par Second line\\tab tabbed\\par}"
        val t = RtfExtractor.extract(rtf)
        assertEquals("Hello World\nSecond line\ttabbed", t)
        assertFalse(t.contains("Arial"))
    }

    @Test fun rtfHandlesEscapesUnicodeAndDestinations() {
        assertEquals("caf\u00E9", RtfExtractor.extract("{\\rtf1 caf\\'e9}"))
        assertEquals("a\u20ACb", RtfExtractor.extract("{\\rtf1 a\\u8364?b}"))
        assertEquals("x\uD83Dy".length, RtfExtractor.extract("{\\rtf1 x\\u-10179?y}").length)
        assertEquals("keep", RtfExtractor.extract("{\\rtf1 {\\*\\generator Word;}keep}"))
        assertEquals("a{b}c\\d", RtfExtractor.extract("{\\rtf1 a\\{b\\}c\\\\d}"))
        assertEquals("\u2014\u2013\u2022\u2018\u2019\u201C\u201D", RtfExtractor.extract("{\\rtf1 \\emdash \\endash \\bullet \\lquote\\rquote \\ldblquote\\rdblquote}"))
        assertEquals("a\u00A0b", RtfExtractor.extract("{\\rtf1 a\\~b}"))
        assertEquals("a-b", RtfExtractor.extract("{\\rtf1 a\\_b}"))
        assertEquals("", RtfExtractor.extract("{\\rtf1 {\\pict\\wmetafile8 deadbeef}}"))
        assertEquals("line1\nline2", RtfExtractor.extract("{\\rtf1 line1\\line line2}"))
    }

    @Test fun rtfRespectsMaxCharsAndSurvivesGarbage() {
        assertTrue(RtfExtractor.extract("{\\rtf1 " + "word ".repeat(1000) + "}", maxChars = 50).length < 120)
        RtfExtractor.extract("{\\rtf1 \\")
        RtfExtractor.extract("}}}{{{\\'")
        RtfExtractor.extract("")
    }

    // ---------------- ODF ----------------
    private val odfBody = """<?xml version="1.0"?><office:document-content xmlns:office="o" xmlns:text="t" xmlns:table="tb" xmlns:draw="d">
        <office:body><office:text>
        <text:h>Title</text:h>
        <text:p>Hello<text:s text:c="2"/>World<text:tab/>Tab<text:line-break/>Next</text:p>
        <table:table><table:table-row><table:table-cell><text:p>A1</text:p></table:table-cell><table:table-cell><text:p>B1</text:p></table:table-cell></table:table-row></table:table>
        </office:text></office:body></office:document-content>"""

    @Test fun odfExtractsParagraphsSpacesAndTables() {
        val t = OdfTextExtractor.extract(zip("a.odt", "content.xml" to odfBody))
        assertTrue(t, t.startsWith("Title\nHello  World\tTab\nNext"))
        assertTrue(t, t.contains("A1 \tB1"))
    }

    @Test fun odfSlidesAreSeparated() {
        val xml = """<office:document-content xmlns:office="o" xmlns:text="t" xmlns:draw="d"><draw:page><text:p>One</text:p></draw:page><draw:page><text:p>Two</text:p></draw:page></office:document-content>"""
        val t = OdfTextExtractor.extract(zip("a.odp", "content.xml" to xml))
        assertTrue(t, t.contains("--- Slide ---"))
        assertTrue(t.contains("Two"))
    }

    @Test fun odfRejectsMissingContentAndDoctype() {
        try { OdfTextExtractor.extract(zip("b.odt", "other.xml" to "<a/>")); fail("expected") } catch (e: IllegalArgumentException) { /* ok */ }
        val xxe = """<?xml version="1.0"?><!DOCTYPE x [<!ENTITY e SYSTEM "file:///etc/passwd">]><office:document-content xmlns:office="o" xmlns:text="t"><text:p>&e;</text:p></office:document-content>"""
        try {
            val t = OdfTextExtractor.extract(zip("c.odt", "content.xml" to xxe))
            assertFalse(t.contains("root:"))
        } catch (e: Exception) {
            // refusing the document is also correct
        }
    }

    @Test fun odfStopsAtMaxChars() {
        val big = "<office:document-content xmlns:office=\"o\" xmlns:text=\"t\">" + "<text:p>${"x".repeat(100)}</text:p>".repeat(100) + "</office:document-content>"
        assertTrue(OdfTextExtractor.extract(zip("d.odt", "content.xml" to big), maxChars = 500).length < 1000)
    }

    // ---------------- EPUB ----------------
    private fun epub(): File = zip(
        "book.epub",
        "mimetype" to "application/epub+zip",
        "META-INF/container.xml" to """<container><rootfiles><rootfile full-path="OEBPS/content.opf"/></rootfiles></container>""",
        "OEBPS/content.opf" to """<package xmlns:dc="d"><metadata><dc:title>My Book</dc:title></metadata>
            <manifest><item id="c1" href="ch1.xhtml"/><item id="c2" href="ch2.xhtml#frag"/><item id="css" href="s.css"/></manifest>
            <spine><itemref idref="c1"/><itemref idref="c2"/></spine></package>""",
        "OEBPS/ch1.xhtml" to """<html><head><style>x{}</style></head><body><h2>One</h2><p onclick="evil()">Text <img src="a.png"/></p><script>alert(1)</script></body></html>""",
        "OEBPS/ch2.xhtml" to """<html><body><p>Two <a href="javascript:bad()">link</a></p></body></html>"""
    )

    @Test fun epubOpensSpineInOrder() {
        ZipFile(epub()).use { zf ->
            val b = EpubReader.open(zf)
            assertEquals("My Book", b.title)
            assertEquals(listOf("OEBPS/ch1.xhtml", "OEBPS/ch2.xhtml"), b.chapterHrefs)
        }
    }

    @Test fun epubHtmlIsSanitised() {
        val html = EpubReader.toHtml(epub())
        assertTrue(html.contains("<h1>My Book</h1>"))
        assertTrue(html.contains("One"))
        assertTrue(html.contains("Two"))
        assertFalse(html.contains("script"))
        assertFalse(html.contains("onclick"))
        assertFalse(html.contains("<img"))
        assertFalse(html.contains("javascript:"))
    }

    @Test fun epubTruncatesHugeBooksAndRejectsNonEpub() {
        val tiny = EpubReader.toHtml(epub(), maxChars = 5)
        assertTrue(tiny.contains("truncated"))
        try { ZipFile(zip("n.zip", "a.txt" to "x")).use { EpubReader.open(it) }; fail("expected") } catch (e: IllegalArgumentException) { /* ok */ }
        try {
            ZipFile(zip("n2.zip", "META-INF/container.xml" to "<container/>")).use { EpubReader.open(it) }
            fail("expected")
        } catch (e: IllegalArgumentException) { /* ok */ }
        try {
            ZipFile(zip("n3.zip", "META-INF/container.xml" to """<container><rootfile full-path="x.opf"/></container>""")).use { EpubReader.open(it) }
            fail("expected")
        } catch (e: IllegalArgumentException) { /* ok */ }
    }

    @Test fun sanitizeStripsActiveContentVariants() {
        val s = EpubReader.sanitize("<p onmouseover='x()'>a</p><iframe src=x></iframe><svg><script>1</script></svg><a href=\"data:text/html,x\">d</a><video>v</video>")
        assertFalse(s.contains("onmouseover"))
        assertFalse(s.contains("iframe"))
        assertFalse(s.contains("svg"))
        assertFalse(s.contains("data:"))
        assertFalse(s.contains("video"))
        assertTrue(s.contains("<p"))
    }
}
