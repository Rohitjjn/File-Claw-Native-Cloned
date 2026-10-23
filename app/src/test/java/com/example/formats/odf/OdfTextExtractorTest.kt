package com.example.formats.odf

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class OdfTextExtractorTest {
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
}
