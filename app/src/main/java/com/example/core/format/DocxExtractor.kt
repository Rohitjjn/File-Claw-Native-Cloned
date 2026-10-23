package com.example.core.format

import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Pure Kotlin, 100% offline DOCX extractor.
 * Reads word/document.xml from the OOXML ZIP package and extracts clean text paragraphs.
 * Guarantees that File Claw can ALWAYS extract and display text from a DOCX even without WebView/JS.
 */
object DocxExtractor {

    data class ExtractedDocx(
        val title: String?,
        val paragraphs: List<String>,
        val plainText: String,
        val wordCount: Int,
        val paragraphCount: Int
    )

    fun extractText(file: File): ExtractedDocx {
        if (!file.exists() || !file.canRead()) {
            return ExtractedDocx(null, emptyList(), "", 0, 0)
        }
        return file.inputStream().use { extractText(it, file.name) }
    }

    fun extractText(bytes: ByteArray, fileName: String = "document.docx"): ExtractedDocx {
        return ByteArrayInputStream(bytes).use { extractText(it, fileName) }
    }

    fun extractText(inputStream: InputStream, fileName: String = "document.docx"): ExtractedDocx {
        val zip = ZipInputStream(inputStream)
        var documentXmlBytes: ByteArray? = null

        var entry = zip.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                documentXmlBytes = zip.readBytes()
                break
            }
            entry = zip.nextEntry
        }

        if (documentXmlBytes == null) {
            return ExtractedDocx(fileName, emptyList(), "", 0, 0)
        }

        val xmlString = String(documentXmlBytes, Charsets.UTF_8)
        val paragraphs = parseParagraphsFromXml(xmlString)
        val plainText = paragraphs.joinToString("\n\n")
        val words = plainText.split(Regex("\\s+")).filter { it.isNotBlank() }.size

        val title = paragraphs.firstOrNull { it.isNotBlank() } ?: fileName

        return ExtractedDocx(
            title = title,
            paragraphs = paragraphs,
            plainText = plainText,
            wordCount = words,
            paragraphCount = paragraphs.size
        )
    }

    private fun parseParagraphsFromXml(xml: String): List<String> {
        val result = mutableListOf<String>()
        // Match each <w:p>...</w:p> block
        val pRegex = Regex("<w:p(?:\\s[^>]*)?>(.*?)</w:p>", RegexOption.DOT_MATCHES_ALL)
        val tRegex = Regex("<w:t(?:\\s[^>]*)?>(.*?)</w:t>", RegexOption.DOT_MATCHES_ALL)
        val brRegex = Regex("<w:br(?:\\s[^>]*)?/>")
        val tabRegex = Regex("<w:tab(?:\\s[^>]*)?/>")

        val matches = pRegex.findAll(xml)
        for (m in matches) {
            val pContent = m.groupValues[1]
            val textBuilder = StringBuilder()

            // Handle line breaks and tabs inside paragraph
            val cleanContent = pContent
                .replace(brRegex, "\n")
                .replace(tabRegex, "\t")

            for (t in tRegex.findAll(cleanContent)) {
                var text = t.groupValues[1]
                // Unescape basic XML entities
                text = text
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'")
                textBuilder.append(text)
            }

            val pText = textBuilder.toString().trim()
            if (pText.isNotEmpty()) {
                result.add(pText)
            }
        }

        return result
    }
}
