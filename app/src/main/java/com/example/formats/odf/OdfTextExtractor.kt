package com.example.formats.odf

import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

/** Plain-text extraction for OpenDocument (odt / ods / odp) via SAX over content.xml. */
object OdfTextExtractor {

    private const val MAX_XML_BYTES = 64L * 1024 * 1024

    fun extract(file: File, maxChars: Int = 5_000_000): String {
        ZipFile(file).use { zf ->
            val entry = zf.getEntry("content.xml") ?: throw IllegalArgumentException("content.xml missing")
            if (entry.size > MAX_XML_BYTES) throw IllegalArgumentException("content.xml too large")
            zf.getInputStream(entry).use { input ->
                val factory = SAXParserFactory.newInstance()
                factory.isNamespaceAware = false
                // Hardening: refuse DTDs and external entities (XXE, entity-expansion bombs).
                runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { factory.setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
                val handler = Handler(maxChars)
                try {
                    factory.newSAXParser().parse(InputSource(input), handler)
                } catch (e: StopParsing) {
                    // reached maxChars
                }
                return handler.result()
            }
        }
    }

    private class StopParsing : RuntimeException()

    private class Handler(val max: Int) : DefaultHandler() {
        private val sb = StringBuilder()
        private var inText = 0
        private var cellOpen = false

        fun result(): String = sb.toString().replace(Regex("\n{3,}"), "\n\n").trim()

        override fun startElement(uri: String?, localName: String?, qName: String, attrs: Attributes) {
            when (qName) {
                "text:p", "text:h" -> inText++
                "text:tab" -> sb.append('\t')
                "text:line-break" -> sb.append('\n')
                "text:s" -> sb.append(" ".repeat((attrs.getValue("text:c")?.toIntOrNull() ?: 1).coerceIn(1, 200)))
                "table:table-cell" -> cellOpen = true
                "draw:page" -> if (sb.isNotEmpty()) sb.append("\n\n--- Slide ---\n")
            }
        }

        override fun endElement(uri: String?, localName: String?, qName: String) {
            when (qName) {
                "text:p", "text:h" -> { inText--; sb.append(if (cellOpen) ' ' else '\n') }
                "table:table-cell" -> { cellOpen = false; sb.append('\t') }
                "table:table-row", "table:table" -> sb.append('\n')
            }
            if (sb.length > max) throw StopParsing()
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            if (inText > 0) sb.append(ch, start, length)
        }
    }
}
