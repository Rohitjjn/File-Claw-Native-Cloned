package com.example.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream

class PdfTextSearcherTest {

    @Test
    fun testSearchInUncompressedPdfStream() {
        val pdfContent = """
            %PDF-1.4
            1 0 obj
            << /Length 40 >>
            stream
            BT
            /F1 12 Tf
            (Hello from FileClaw PDF Search) Tj
            ET
            endstream
            endobj
            trailer
            << /Root 1 0 R >>
            %%EOF
        """.trimIndent()

        val results = PdfTextSearcher.searchInBytes(pdfContent.toByteArray(), "fileclaw")
        assertEquals(1, results.size)
        assertEquals(1, results[0].pageNumber)
        assertTrue(results[0].matchedText.contains("fileclaw", ignoreCase = true))
    }

    @Test
    fun testSearchInFlateCompressedPdfStream() {
        val rawStream = "BT /F1 12 Tf (Confidential Financial Report 2026) Tj ET".toByteArray()
        val bos = ByteArrayOutputStream()
        DeflaterOutputStream(bos).use { it.write(rawStream) }
        val compressed = bos.toByteArray()

        val prefix = "%PDF-1.4\n1 0 obj\n<< /Filter /FlateDecode >>\nstream\n".toByteArray()
        val suffix = "\nendstream\nendobj\n%%EOF".toByteArray()

        val fullPdf = prefix + compressed + suffix

        val results = PdfTextSearcher.searchInBytes(fullPdf, "confidential")
        assertEquals(1, results.size)
        assertTrue(results[0].matchedText.contains("confidential", ignoreCase = true))
    }

    @Test
    fun testSearchWithNoMatchesReturnsEmpty() {
        val pdfContent = "%PDF-1.4\nstream\n(Just some ordinary text)\nendstream\n%%EOF"
        val results = PdfTextSearcher.searchInBytes(pdfContent.toByteArray(), "nonexistent_query")
        assertTrue(results.isEmpty())
    }
}
