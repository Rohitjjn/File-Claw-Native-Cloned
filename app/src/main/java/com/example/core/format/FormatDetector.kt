package com.example.core.format

import java.io.File
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile

enum class Confidence { HIGH, MEDIUM, LOW }

/**
 * Result of format detection.
 *
 * @property mismatch true when the file extension claims a different format than the content.
 */
data class Detection(
    val spec: FormatSpec,
    val confidence: Confidence,
    val reason: String,
    val mismatch: Boolean = false
) {
    val kind: FileKind get() = spec.kind
}

/** A signature hit. [strong] hits (binary magic) override the file extension; weak hits do not. */
internal data class MagicHit(val id: String, val strong: Boolean)

object TextHeuristics {

    /** True when [sample] looks like human-readable text (UTF-8/16/32 or a single-byte code page). */
    fun looksLikeText(sample: ByteArray, length: Int = sample.size): Boolean {
        val n = minOf(length, sample.size)
        if (n == 0) return true
        if (hasBom(sample, n)) return true
        var nul = 0
        var control = 0
        for (i in 0 until n) {
            val b = sample[i].toInt() and 0xFF
            if (b == 0) nul++
            else if (b < 0x20 && b != 0x09 && b != 0x0A && b != 0x0D && b != 0x0C && b != 0x1B) control++
        }
        if (nul > 0) return looksLikeUtf16(sample, n)
        val ratio = control.toDouble() / n
        return if (isValidUtf8(sample, n)) ratio < 0.02 else ratio < 0.05
    }

    private fun hasBom(b: ByteArray, n: Int): Boolean =
        (n >= 3 && b[0] == 0xEF.toByte() && b[1] == 0xBB.toByte() && b[2] == 0xBF.toByte()) ||
            (n >= 2 && b[0] == 0xFE.toByte() && b[1] == 0xFF.toByte()) ||
            (n >= 2 && b[0] == 0xFF.toByte() && b[1] == 0xFE.toByte())

    /** UTF-16 without BOM: ASCII-range text has a NUL in every other byte. */
    private fun looksLikeUtf16(b: ByteArray, n: Int): Boolean {
        if (n < 4) return false
        var evenNul = 0
        var oddNul = 0
        val pairs = n / 2
        for (i in 0 until pairs) {
            if (b[2 * i].toInt() == 0) evenNul++
            if (b[2 * i + 1].toInt() == 0) oddNul++
        }
        val big = evenNul > pairs * 0.3 && oddNul == 0
        val little = oddNul > pairs * 0.3 && evenNul == 0
        return big || little
    }

    /** Strict UTF-8 validation that tolerates a multi-byte sequence cut off at the end of the sample. */
    fun isValidUtf8(b: ByteArray, length: Int = b.size): Boolean {
        var i = 0
        val n = minOf(length, b.size)
        while (i < n) {
            val c = b[i].toInt() and 0xFF
            val need = when {
                c < 0x80 -> 0
                c in 0xC2..0xDF -> 1
                c in 0xE0..0xEF -> 2
                c in 0xF0..0xF4 -> 3
                else -> return false
            }
            if (need > 0 && i + need >= n) return trailingTruncatedOk(b, i, n)
            for (k in 1..need) {
                if ((b[i + k].toInt() and 0xC0) != 0x80) return false
            }
            i += need + 1
        }
        return true
    }

    private fun trailingTruncatedOk(b: ByteArray, start: Int, n: Int): Boolean {
        // Remaining bytes (if any) must all be continuation bytes.
        for (k in start + 1 until n) if ((b[k].toInt() and 0xC0) != 0x80) return false
        return true
    }
}

internal object MagicSniffer {

    private fun startsWith(b: ByteArray, n: Int, vararg sig: Int, offset: Int = 0): Boolean {
        if (n < offset + sig.size) return false
        for (i in sig.indices) if ((b[offset + i].toInt() and 0xFF) != sig[i]) return false
        return true
    }

    private fun ascii(b: ByteArray, n: Int, text: String, offset: Int = 0): Boolean {
        if (n < offset + text.length) return false
        for (i in text.indices) if ((b[offset + i].toInt() and 0xFF).toChar() != text[i]) return false
        return true
    }

    private fun containsAscii(b: ByteArray, n: Int, needle: String, limit: Int): Boolean {
        val end = minOf(n, limit) - needle.length
        var i = 0
        while (i <= end) {
            var ok = true
            for (j in needle.indices) {
                if ((b[i + j].toInt() and 0xFF).toChar() != needle[j]) { ok = false; break }
            }
            if (ok) return true
            i++
        }
        return false
    }

    fun sniff(b: ByteArray, n: Int, totalLength: Long): MagicHit? {
        if (n < 2) return null
        // ---- Documents / containers ------------------------------------------------------
        if (ascii(b, n, "%PDF-") || containsAscii(b, n, "%PDF-", 1024)) return MagicHit("pdf", true)
        if (startsWith(b, n, 0x50, 0x4B, 0x03, 0x04) || startsWith(b, n, 0x50, 0x4B, 0x05, 0x06) ||
            startsWith(b, n, 0x50, 0x4B, 0x07, 0x08)
        ) return MagicHit("zip", true)
        if (startsWith(b, n, 0x1F, 0x8B)) return MagicHit("gz", true)
        if (ascii(b, n, "BZh") && n >= 4 && b[3].toInt().toChar() in '1'..'9') return MagicHit("bz2", true)
        if (startsWith(b, n, 0xFD, 0x37, 0x7A, 0x58, 0x5A, 0x00)) return MagicHit("xz", true)
        if (startsWith(b, n, 0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C)) return MagicHit("7z", true)
        if (ascii(b, n, "Rar!\u001A\u0007")) return MagicHit("rar", true)
        if (ascii(b, n, "ustar", 257)) return MagicHit("tar", true)
        if (startsWith(b, n, 0x28, 0xB5, 0x2F, 0xFD)) return MagicHit("archive-other", true)
        if (startsWith(b, n, 0x04, 0x22, 0x4D, 0x18)) return MagicHit("archive-other", true)
        if (ascii(b, n, "!<arch>\n")) return MagicHit("ar", true)
        if (ascii(b, n, "070701") || ascii(b, n, "070707")) return MagicHit("cpio", true)
        if (startsWith(b, n, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1)) return MagicHit("ole2", true)
        if (ascii(b, n, "{\\rtf")) return MagicHit("rtf", true)
        if (ascii(b, n, "SQLite format 3\u0000")) return MagicHit("sqlite", true)
        if (ascii(b, n, "PAR1")) return MagicHit("data", true)

        // ---- Images ----------------------------------------------------------------------
        if (startsWith(b, n, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return MagicHit("png", true)
        if (startsWith(b, n, 0xFF, 0xD8, 0xFF)) return MagicHit("jpeg", true)
        if (ascii(b, n, "GIF87a") || ascii(b, n, "GIF89a")) return MagicHit("gif", true)
        if (ascii(b, n, "RIFF") && ascii(b, n, "WEBP", 8)) return MagicHit("webp", true)
        if (ascii(b, n, "BM") && n >= 10 && b[6].toInt() == 0 && b[7].toInt() == 0 && b[8].toInt() == 0 && b[9].toInt() == 0) {
            return MagicHit("bmp", true)
        }
        if (startsWith(b, n, 0x00, 0x00, 0x01, 0x00) && n >= 6 && (b[4].toInt() and 0xFF) in 1..64 && b[5].toInt() == 0) {
            return MagicHit("ico", true)
        }
        if (startsWith(b, n, 0x49, 0x49, 0x2A, 0x00) || startsWith(b, n, 0x4D, 0x4D, 0x00, 0x2A)) return MagicHit("tiff", true)
        if (ascii(b, n, "8BPS")) return MagicHit("psd", true)

        // ---- ISO base media (MP4 / MOV / M4A / HEIC / AVIF) -------------------------------
        if (ascii(b, n, "ftyp", 4) && n >= 12) {
            val brand = String(b, 8, 4, Charsets.ISO_8859_1)
            return when {
                brand.startsWith("heic") || brand.startsWith("heix") || brand.startsWith("mif1") ||
                    brand.startsWith("msf1") || brand.startsWith("avif") || brand.startsWith("avis") -> MagicHit("heif", true)
                brand.startsWith("M4A") || brand.startsWith("M4B") -> MagicHit("m4a", true)
                brand.startsWith("qt") -> MagicHit("video-other", true)
                brand.startsWith("3g") -> MagicHit("3gp", true)
                else -> MagicHit("mp4", true)
            }
        }
        if (startsWith(b, n, 0x1A, 0x45, 0xDF, 0xA3)) {
            return if (containsAscii(b, n, "webm", 64)) MagicHit("webm", true) else MagicHit("mkv", true)
        }

        // ---- Audio -----------------------------------------------------------------------
        if (ascii(b, n, "OggS")) return MagicHit("ogg", true)
        if (ascii(b, n, "fLaC")) return MagicHit("flac", true)
        if (ascii(b, n, "ID3")) return MagicHit("mp3", true)
        if (ascii(b, n, "RIFF") && ascii(b, n, "WAVE", 8)) return MagicHit("wav", true)
        if (ascii(b, n, "RIFF") && ascii(b, n, "AVI ", 8)) return MagicHit("video-other", true)
        if (ascii(b, n, "MThd")) return MagicHit("midi", true)
        if ((b[0].toInt() and 0xFF) == 0xFF && (b[1].toInt() and 0xE0) == 0xE0 && (b[1].toInt() and 0x06) != 0) {
            return MagicHit("mp3", false)
        }

        // ---- Fonts -----------------------------------------------------------------------
        if (startsWith(b, n, 0x00, 0x01, 0x00, 0x00) || ascii(b, n, "OTTO") || ascii(b, n, "true") || ascii(b, n, "ttcf")) {
            return MagicHit("ttf", true)
        }
        if (ascii(b, n, "wOFF") || ascii(b, n, "wOF2")) return MagicHit("woff", true)

        // ---- 3D / CAD --------------------------------------------------------------------
        if (ascii(b, n, "glTF")) return MagicHit("model-other", true)
        if (ascii(b, n, "ply\n") || ascii(b, n, "ply\r")) return MagicHit("model-other", true)
        if (ascii(b, n, "AC10") && n >= 6 && b[4].toInt().toChar().isDigit()) return MagicHit("model-other", true)
        if (isBinaryStl(b, n, totalLength)) return MagicHit("stl", true)
        if (ascii(b, n, "solid") && containsAscii(b, n, "facet", 1024)) return MagicHit("stl", false)

        // ---- Executables / misc binary ---------------------------------------------------
        if (startsWith(b, n, 0x7F, 0x45, 0x4C, 0x46)) return MagicHit("exe", true)
        if (ascii(b, n, "MZ")) return MagicHit("exe", true)
        if (startsWith(b, n, 0xCA, 0xFE, 0xBA, 0xBE)) return MagicHit("exe", true)
        if (ascii(b, n, "dex\n")) return MagicHit("exe", true)
        if (startsWith(b, n, 0x00, 0x61, 0x73, 0x6D)) return MagicHit("exe", true)
        if (ascii(b, n, "bplist00")) return MagicHit("data", true)
        if (ascii(b, n, "\u0093NUMPY")) return MagicHit("data", true)

        // ---- Weak (text) signatures -------------------------------------------------------
        val head = headAsText(b, n)
        val trimmed = head.trimStart('\uFEFF', ' ', '\t', '\r', '\n')
        val lower = trimmed.take(512).lowercase()
        if (lower.startsWith("<svg") || (lower.startsWith("<?xml") && lower.contains("<svg"))) return MagicHit("svg", false)
        if (lower.startsWith("<!doctype html") || lower.startsWith("<html")) return MagicHit("html", false)
        if (lower.startsWith("<?xml")) return MagicHit("xml", false)
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) return MagicHit("json", false)
        if (trimmed.startsWith("-----BEGIN")) return MagicHit("markup-text", false)
        return null
    }

    private fun headAsText(b: ByteArray, n: Int): String = String(b, 0, minOf(n, 600), Charsets.ISO_8859_1)

    /** Binary STL: 80-byte header, uint32 triangle count, then 50 bytes per triangle. */
    private fun isBinaryStl(b: ByteArray, n: Int, totalLength: Long): Boolean {
        if (n < 84 || totalLength < 84) return false
        val count = (b[80].toLong() and 0xFF) or ((b[81].toLong() and 0xFF) shl 8) or
            ((b[82].toLong() and 0xFF) shl 16) or ((b[83].toLong() and 0xFF) shl 24)
        if (count <= 0) return false
        return totalLength == 84L + count * 50L
    }
}

object FormatDetector {

    const val HEADER_BYTES = 8192

    /** Extensions that carry no format information, so content decides. */
    private val uninformative = setOf("", "bin", "dat", "tmp", "download", "unknown")

    /** Binary container formats whose extension is only trusted when the signature is present. */
    private val signatureExpected = setOf(
        "pdf", "png", "jpeg", "gif", "webp", "zip", "zip-app", "docx", "xlsx", "pptx",
        "odt", "ods", "odp", "epub", "7z", "gz", "bz2", "xz", "tgz", "tbz2", "txz", "rar", "tar"
    )

    /** SQLite is identified by signature only: `.db` is far too generic to trust. */
    private val signatureRequired = setOf("sqlite")

    private val textualKinds = setOf(
        FileKind.TEXT, FileKind.MARKDOWN, FileKind.HTML, FileKind.SVG, FileKind.DELIMITED
    )

    private val families: List<Set<String>> = listOf(
        setOf("zip", "zip-app", "docx", "xlsx", "pptx", "odt", "ods", "odp", "epub"),
        setOf("gz", "tgz"), setOf("bz2", "tbz2"), setOf("xz", "txz"),
        setOf("text", "json", "xml", "yaml", "config", "markup-text", "markdown", "html", "svg", "csv")
    )

    /** Detect from a file on disk. Reads at most [HEADER_BYTES] plus the ZIP central directory when needed. */
    fun detect(file: File, displayName: String = file.name): Detection {
        val header = ByteArray(HEADER_BYTES)
        var n = 0
        try {
            file.inputStream().use { input ->
                while (n < header.size) {
                    val r = input.read(header, n, header.size - n)
                    if (r < 0) break
                    n += r
                }
            }
        } catch (e: IOException) {
            return Detection(FormatRegistry.unknownBinary, Confidence.LOW, "unreadable: ${e.javaClass.simpleName}")
        }
        val hdr = header.copyOf(n)
        val hit = MagicSniffer.sniff(hdr, n, file.length())
        var zipEntries: List<String>? = null
        var zipMime: String? = null
        if (hit?.id == "zip") {
            val info = inspectZip(file)
            zipEntries = info.first
            zipMime = info.second
        }
        val gzipIsTar = hit?.id == "gz" && gzipContainsTar(file)
        return detect(hdr, file.length(), displayName, zipEntries, zipMime, gzipIsTar)
    }

    /** Pure detection from already-read bytes. [zipEntries] and [zipMimetype] refine ZIP-based formats. */
    fun detect(
        header: ByteArray,
        totalLength: Long,
        fileName: String,
        zipEntries: List<String>? = null,
        zipMimetype: String? = null,
        gzipIsTar: Boolean = false
    ): Detection {
        val ext = FormatRegistry.extensionOf(fileName)
        val extSpec = FormatRegistry.byExtension(ext)
        val hit = MagicSniffer.sniff(header, header.size, totalLength)
        val informativeExt = extSpec != null && ext !in uninformative

        // 1. Strong binary signature always wins over the extension.
        if (hit != null && hit.strong) {
            val spec = refine(hit.id, ext, extSpec, zipEntries, zipMimetype, gzipIsTar)
            if (spec != null) {
                val mismatch = informativeExt && !sameFormat(extSpec!!, spec)
                return Detection(spec, Confidence.HIGH, "magic:${hit.id}", mismatch)
            }
        }

        // 2. Trust a meaningful extension, with sanity checks against the content.
        if (informativeExt && extSpec!!.id !in signatureRequired) {
            if (extSpec.id in signatureExpected) {
                return if (header.isNotEmpty() && TextHeuristics.looksLikeText(header)) {
                    Detection(FormatRegistry.plainText, Confidence.LOW, "ext:$ext but signature missing; content is text", true)
                } else {
                    Detection(extSpec, Confidence.LOW, "ext:$ext but signature missing", true)
                }
            }
            if (extSpec.kind in textualKinds && header.isNotEmpty() && !TextHeuristics.looksLikeText(header)) {
                return Detection(FormatRegistry.unknownBinary, Confidence.MEDIUM, "ext:$ext but content is binary", true)
            }
            return Detection(extSpec, Confidence.MEDIUM, "ext:$ext")
        }

        // 3. Weak (text) signature: only used when the extension says nothing.
        if (hit != null) {
            val spec = refine(hit.id, ext, extSpec, zipEntries, zipMimetype, gzipIsTar)
            if (spec != null) return Detection(spec, Confidence.MEDIUM, "magic:${hit.id}")
        }

        // 4. Content heuristics.
        return if (TextHeuristics.looksLikeText(header)) {
            Detection(FormatRegistry.plainText, Confidence.LOW, "content looks like text")
        } else {
            Detection(FormatRegistry.unknownBinary, Confidence.LOW, "unrecognised binary content")
        }
    }

    private fun sameFormat(a: FormatSpec, b: FormatSpec): Boolean =
        a.id == b.id || families.any { a.id in it && b.id in it }

    /** Maps a raw magic id to the final spec, using the extension / ZIP contents to disambiguate. */
    private fun refine(
        id: String,
        ext: String,
        extSpec: FormatSpec?,
        zipEntries: List<String>?,
        zipMimetype: String?,
        gzipIsTar: Boolean
    ): FormatSpec? {
        val r = FormatRegistry
        return when (id) {
            "zip" -> refineZip(extSpec, zipEntries, zipMimetype)
            "gz" -> if (gzipIsTar || ext == "tgz" || ext == "tar.gz") r.byId("tgz") else r.byId("gz")
            "bz2" -> if (ext == "tbz2" || ext == "tbz" || ext == "tar.bz2") r.byId("tbz2") else r.byId("bz2")
            "xz" -> if (ext == "txz" || ext == "tar.xz") r.byId("txz") else r.byId("xz")
            "exe", "data", "model-other", "archive-other", "ogg" -> if (extSpec?.id == id) extSpec else r.byId(id)
            "markup-text", "json", "xml" -> extSpec ?: r.byId(id)
            else -> r.byId(id)
        }
    }

    private fun refineZip(extSpec: FormatSpec?, entries: List<String>?, mimetype: String?): FormatSpec? {
        val r = FormatRegistry
        if (entries != null) {
            val set = entries.toHashSet()
            when {
                "word/document.xml" in set -> return r.byId("docx")
                "xl/workbook.xml" in set -> return r.byId("xlsx")
                "ppt/presentation.xml" in set -> return r.byId("pptx")
                mimetype == "application/epub+zip" -> return r.byId("epub")
                mimetype == "application/vnd.oasis.opendocument.text" -> return r.byId("odt")
                mimetype == "application/vnd.oasis.opendocument.spreadsheet" -> return r.byId("ods")
                mimetype == "application/vnd.oasis.opendocument.presentation" -> return r.byId("odp")
                "AndroidManifest.xml" in set && entries.any { it.startsWith("classes") && it.endsWith(".dex") } -> return r.byId("zip-app")
                "META-INF/MANIFEST.MF" in set -> return r.byId("zip-app")
            }
        }
        // A ZIP-family extension agrees with a ZIP container.
        if (extSpec != null && families[0].contains(extSpec.id)) return extSpec
        return r.byId("zip")
    }

    /** Returns up to 256 entry names plus the content of a leading `mimetype` entry (ODF / EPUB). */
    private fun inspectZip(file: File): Pair<List<String>?, String?> {
        return try {
            ZipFile(file).use { zf ->
                val names = ArrayList<String>()
                var mime: String? = null
                val en = zf.entries()
                while (en.hasMoreElements() && names.size < 256) {
                    val e = en.nextElement()
                    names.add(e.name)
                    if (e.name == "mimetype" && e.size in 1..256) {
                        mime = zf.getInputStream(e).use { String(readUpTo(it, 256), Charsets.US_ASCII).trim() }
                    }
                }
                Pair(names, mime)
            }
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    /** Bounded read; `InputStream.readNBytes` is not available below Android API 33. */
    private fun readUpTo(input: java.io.InputStream, max: Int): ByteArray {
        val buf = ByteArray(max)
        var n = 0
        while (n < max) {
            val r = input.read(buf, n, max - n)
            if (r < 0) break
            n += r
        }
        return buf.copyOf(n)
    }

    private fun gzipContainsTar(file: File): Boolean {
        return try {
            GZIPInputStream(file.inputStream().buffered()).use { gz ->
                val buf = ByteArray(512)
                var n = 0
                while (n < buf.size) {
                    val r = gz.read(buf, n, buf.size - n)
                    if (r < 0) break
                    n += r
                }
                n >= 262 && String(buf, 257, 5, Charsets.ISO_8859_1) == "ustar"
            }
        } catch (e: Exception) {
            false
        }
    }
}
