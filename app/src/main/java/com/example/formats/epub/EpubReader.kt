package com.example.formats.epub

import java.io.File
import com.example.core.security.HtmlSanitizer
import java.util.zip.ZipFile
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

data class EpubBook(val title: String, val chapterHrefs: List<String>)

/** Minimal EPUB reader: container.xml -> OPF -> spine -> chapter HTML. Scripts, images and remote content are dropped. */
object EpubReader {

    private fun factory(): SAXParserFactory {
        val f = SAXParserFactory.newInstance()
        runCatching { f.setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { f.setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        runCatching { f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
        return f
    }

    fun open(zf: ZipFile): EpubBook {
        val container = zf.getEntry("META-INF/container.xml") ?: throw IllegalArgumentException("not an EPUB")
        var opfPath: String? = null
        zf.getInputStream(container).use { s ->
            factory().newSAXParser().parse(InputSource(s), object : DefaultHandler() {
                override fun startElement(u: String?, l: String?, q: String, a: Attributes) {
                    if (q.endsWith("rootfile") && opfPath == null) opfPath = a.getValue("full-path")
                }
            })
        }
        val opf = opfPath ?: throw IllegalArgumentException("OPF not found")
        val opfEntry = zf.getEntry(opf) ?: throw IllegalArgumentException("OPF entry missing")
        val manifest = HashMap<String, String>()
        val spine = ArrayList<String>()
        val title = StringBuilder()
        zf.getInputStream(opfEntry).use { s ->
            factory().newSAXParser().parse(InputSource(s), object : DefaultHandler() {
                var inTitle = false
                override fun startElement(u: String?, l: String?, q: String, a: Attributes) {
                    when {
                        q.endsWith("itemref") -> a.getValue("idref")?.let { spine.add(it) }
                        q.endsWith("item") -> {
                            val id = a.getValue("id")
                            val href = a.getValue("href")
                            if (id != null && href != null) manifest[id] = href
                        }
                        q == "dc:title" -> inTitle = true
                    }
                }
                override fun endElement(u: String?, l: String?, q: String) { if (q == "dc:title") inTitle = false }
                override fun characters(ch: CharArray, st: Int, len: Int) {
                    if (inTitle && title.length < 200) title.append(ch, st, len)
                }
            })
        }
        val dir = if (opf.contains('/')) opf.substringBeforeLast('/') + "/" else ""
        val hrefs = spine.mapNotNull { manifest[it] }.map { dir + it.substringBefore('#') }
        return EpubBook(title.toString().trim(), hrefs)
    }

    /** Renders the whole book as simple, script-free HTML capped at roughly [maxChars] characters. */
    fun toHtml(file: File, maxChars: Int = 3_000_000): String {
        ZipFile(file).use { zf ->
            val book = open(zf)
            val sb = StringBuilder()
            sb.append("<h1>").append(escape(book.title.ifEmpty { file.nameWithoutExtension })).append("</h1>")
            var total = 0
            for (href in book.chapterHrefs) {
                val entry = zf.getEntry(href) ?: continue
                if (entry.size > 16L * 1024 * 1024) continue
                val raw = zf.getInputStream(entry).use { String(it.readBytes(), Charsets.UTF_8) }
                val body = Regex("<body[^>]*>(.*)</body>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                    .find(raw)?.groupValues?.get(1) ?: raw
                val clean = sanitize(body)
                total += clean.length
                sb.append("<section>").append(clean).append("</section><hr>")
                if (total > maxChars) { sb.append("<p><em>[Book truncated for preview]</em></p>"); break }
            }
            return sb.toString()
        }
    }

    /** Removes active content and media; keeps basic structure tags. */
    fun sanitize(html: String): String = HtmlSanitizer.sanitize(html, keepImages = false)

    private fun escape(t: String) = t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
