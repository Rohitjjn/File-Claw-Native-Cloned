package com.example.formats.epub

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

class EpubReaderTest {
    private lateinit var dir: File

    @Before fun setUp() { dir = Files.createTempDirectory("format-test").toFile() }
    @After fun tearDown() { dir.deleteRecursively() }

    private fun zip(name: String, vararg entries: Pair<String, String>): File {
        val file = File(dir, name)
        ZipOutputStream(FileOutputStream(file)).use { output ->
            for ((entryName, content) in entries) {
                output.putNextEntry(ZipEntry(entryName))
                output.write(content.toByteArray())
                output.closeEntry()
            }
        }
        return file
    }
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
