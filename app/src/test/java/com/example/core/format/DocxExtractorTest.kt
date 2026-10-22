package com.example.core.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocxExtractorTest {

    @Test
    fun testExtractTextFromDocxZip() {
        val bos = ByteArrayOutputStream()
        val zos = ZipOutputStream(bos)

        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:body>
                    <w:p>
                        <w:r><w:t>File Claw Offline Document</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r><w:t>This is a paragraph test for DOCX extraction &amp; viewing.</w:t></w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        zos.putNextEntry(ZipEntry("word/document.xml"))
        zos.write(xmlContent.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
        zos.close()

        val docxBytes = bos.toByteArray()
        val result = DocxExtractor.extractText(docxBytes, "test.docx")

        assertEquals("File Claw Offline Document", result.title)
        assertEquals(2, result.paragraphs.size)
        assertTrue(result.plainText.contains("File Claw Offline Document"))
        assertTrue(result.plainText.contains("This is a paragraph test for DOCX extraction & viewing."))
        assertTrue(result.wordCount > 5)
    }

    @Test
    fun testExtractTextEmptyZip() {
        val bos = ByteArrayOutputStream()
        val zos = ZipOutputStream(bos)
        zos.putNextEntry(ZipEntry("something_else.txt"))
        zos.write("hello".toByteArray())
        zos.closeEntry()
        zos.close()

        val result = DocxExtractor.extractText(bos.toByteArray(), "empty.docx")
        assertEquals(0, result.paragraphs.size)
        assertEquals("", result.plainText)
    }
}
