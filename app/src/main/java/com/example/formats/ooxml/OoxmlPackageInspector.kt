package com.example.formats.ooxml

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Bounded validation and identification for the existing OOXML preview handlers.
 *
 * The app's WebView renderers receive an untrusted ZIP package. This inspector is
 * deliberately run before JSZip/fastexcel so path traversal, duplicate parts,
 * decompression bombs, encrypted containers and unsafe XML declarations become a
 * normal error state rather than a renderer crash or an OOM. It does not register
 * a new format route: callers still use `FormatRegistry`'s detection result.
 */
object OoxmlPackageInspector {

    const val MAX_PACKAGE_BYTES = 96L * 1024L * 1024L
    const val MAX_TOTAL_UNCOMPRESSED_BYTES = 512L * 1024L * 1024L
    const val MAX_SINGLE_UNCOMPRESSED_BYTES = 128L * 1024L * 1024L
    const val MAX_ZIP_ENTRIES = 10_000
    const val MAX_XML_PART_BYTES = 2L * 1024L * 1024L
    const val MAX_XML_PARTS = 2_000
    const val XML_DOCTYPE_SCAN_BYTES = 64L * 1024L
    const val MAX_XML_SCAN_BYTES = 32L * 1024L * 1024L
    const val MAX_RELATIONSHIP_PARTS = 512
    // DEFLATE cannot exceed roughly 1,032:1, so a 2,000:1 threshold was
    // unreachable and would never reject a real ZIP bomb. 1,000:1 still allows
    // normal OOXML/media compression but blocks near-maximum expansion.
    const val MAX_COMPRESSION_RATIO = 1_000L
    const val MAX_NAME_LENGTH = 1_024
    const val MAX_PATH_DEPTH = 32

    enum class Family { WORD, EXCEL, POWERPOINT, UNKNOWN }

    enum class Variant { STANDARD, MACRO_ENABLED, TEMPLATE, MACRO_TEMPLATE, SHOW, ADD_IN, UNKNOWN }

    /**
     * Inspection phase a failure belongs to. Reported through [failureSink] so the app
     * can log where an untrusted package was rejected without any file path, file name,
     * document content or credential ever reaching the log.
     */
    enum class InspectionStage {
        PACKAGE_ACCESS,
        ENTRY_STRUCTURE,
        XML_SCAN,
        CONTENT_TYPES,
        RELATIONSHIPS,
        VARIANT_IDENTIFY
    }

    /** Structured, privacy-safe failure classification (stage + exception class only). */
    data class OfficeInspectionFailure(
        val stage: InspectionStage,
        val exceptionType: String,
        /** The fixed user-facing rejection reason for expected rejections; null otherwise. */
        val reason: String?
    )

    /**
     * Optional diagnostic hook. The inspector itself never logs; the application can
     * install a sink (for example `android.util.Log`) to receive one [OfficeInspectionFailure]
     * per rejected package. Only stage names, exception class names and the fixed
     * rejection messages are reported — never file paths, file names or content.
     */
    @Volatile
    var failureSink: ((OfficeInspectionFailure) -> Unit)? = null

    /** Per-call phase tracking; a fresh instance is used for every [inspect] call. */
    private class InspectionContext {
        var stage: InspectionStage = InspectionStage.PACKAGE_ACCESS
    }

    private fun reportFailure(ctx: InspectionContext, exceptionType: String, reason: String?) {
        try {
            failureSink?.invoke(OfficeInspectionFailure(ctx.stage, exceptionType, reason))
        } catch (_: Throwable) {
            // A diagnostic hook must never break inspection itself.
        }
    }

    data class PackageInfo(
        val family: Family,
        val variant: Variant,
        val mainContentType: String,
        val hasMacros: Boolean,
        val externalRelationshipCount: Int,
        val notice: String?
    )

    class InvalidPackageException(message: String) : IOException(message)

    private val OLE2_MAGIC = byteArrayOf(
        0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte(),
        0xA1.toByte(), 0xB1.toByte(), 0x1A.toByte(), 0xE1.toByte()
    )

    fun inspect(file: File): PackageInfo {
        val ctx = InspectionContext()
        if (!file.isFile || file.length() <= 0L) {
            reportFailure(ctx, InvalidPackageException::class.java.name, "The Office file is empty or unavailable.")
            throw InvalidPackageException("The Office file is empty or unavailable.")
        }
        ctx.stage = InspectionStage.PACKAGE_ACCESS
        if (file.length() > MAX_PACKAGE_BYTES) {
            reportFailure(ctx, InvalidPackageException::class.java.name, "This Office file is too large to preview safely.")
            throw InvalidPackageException("This Office file is too large to preview safely.")
        }
        if (hasOle2Magic(file)) {
            reportFailure(
                ctx,
                InvalidPackageException::class.java.name,
                "This Office file is encrypted or password-protected. Remove the password and try again."
            )
            throw InvalidPackageException(
                "This Office file is encrypted or password-protected. Remove the password and try again."
            )
        }

        return try {
            ZipFile(file).use { zip -> inspectZip(zip, ctx) }
        } catch (error: InvalidPackageException) {
            reportFailure(ctx, InvalidPackageException::class.java.name, error.message)
            throw error
        } catch (error: Throwable) {
            // Zip libraries can throw Errors on hostile archives. Turn every failure
            // into a recoverable preview error; never leak the file path or content.
            // Classify the failing stage and exception type so the true cause of an
            // unexpected failure is diagnosable from logs (type name only).
            reportFailure(ctx, error.javaClass.name, null)
            throw InvalidPackageException("This Office package is damaged or unsupported.")
        }
    }

    private fun hasOle2Magic(file: File): Boolean = try {
        RandomAccessFile(file, "r").use { raf ->
            if (raf.length() < OLE2_MAGIC.size) return false
            val header = ByteArray(OLE2_MAGIC.size)
            raf.readFully(header)
            header.contentEquals(OLE2_MAGIC)
        }
    } catch (_: IOException) {
        false
    }

    private fun inspectZip(zip: ZipFile, ctx: InspectionContext): PackageInfo {
        val entries = zip.entries.toList()
        ctx.stage = InspectionStage.ENTRY_STRUCTURE
        if (entries.isEmpty() || entries.size > MAX_ZIP_ENTRIES) {
            throw InvalidPackageException("This Office package has an unsupported number of parts.")
        }

        val names = HashSet<String>(entries.size * 2)
        var totalUncompressed = 0L
        var relationshipParts = 0

        for (entry in entries) {
            val name = entry.name
            if (isUnsafeEntryName(name)) {
                throw InvalidPackageException("This Office package contains an unsafe part name.")
            }
            if (!names.add(name)) {
                throw InvalidPackageException("This Office package contains duplicate parts.")
            }
            if (isSymlink(entry)) {
                throw InvalidPackageException("This Office package contains a symbolic link.")
            }
            if (entry.generalPurposeBit.usesEncryption()) {
                throw InvalidPackageException("Encrypted ZIP entries are not supported in Office previews.")
            }
            if (entry.isDirectory) continue

            val size = declaredOrMeasuredSize(zip, entry)
            if (size > MAX_SINGLE_UNCOMPRESSED_BYTES) {
                throw InvalidPackageException("One part of this Office file is too large to preview.")
            }
            totalUncompressed += size
            if (totalUncompressed > MAX_TOTAL_UNCOMPRESSED_BYTES) {
                throw InvalidPackageException("This Office file expands to too much data to preview.")
            }
            val compressed = entry.compressedSize
            if (size > 4L * 1024L * 1024L && compressed > 0L && size / compressed > MAX_COMPRESSION_RATIO) {
                throw InvalidPackageException("This Office package has an unsafe compression ratio.")
            }
            if (name.endsWith(".rels", ignoreCase = true)) {
                relationshipParts++
                if (relationshipParts > MAX_RELATIONSHIP_PARTS) {
                    throw InvalidPackageException("This Office package has too many relationship parts.")
                }
            }
        }

        ctx.stage = InspectionStage.XML_SCAN
        scanXmlParts(zip, names)
        ctx.stage = InspectionStage.CONTENT_TYPES
        val contentTypesXml = readPart(zip, "[Content_Types].xml", MAX_XML_PART_BYTES)
            ?: throw InvalidPackageException("This Office package is missing its content types.")
        val contentTypes = parseSecureXml(contentTypesXml)
        val overrides = contentTypes.getElementsByTagName("Override")

        val mainPart = when {
            names.contains("word/document.xml") -> "word/document.xml"
            names.contains("xl/workbook.xml") -> "xl/workbook.xml"
            names.contains("ppt/presentation.xml") -> "ppt/presentation.xml"
            else -> throw InvalidPackageException("This ZIP file is not a supported Word, Excel or PowerPoint document.")
        }
        val family = familyFor(mainPart)
        val mainContentType = (0 until overrides.length)
            .mapNotNull { overrides.item(it) as? Element }
            .firstOrNull { it.getAttribute("PartName").trimStart('/') == mainPart }
            ?.getAttribute("ContentType")
            .orEmpty()
        if (mainContentType.isBlank()) {
            throw InvalidPackageException("This Office package has an invalid main content type.")
        }
        if (!mainContentType.contains("openxmlformats-officedocument") &&
            !mainContentType.contains("ms-word") &&
            !mainContentType.contains("ms-excel") &&
            !mainContentType.contains("ms-powerpoint")
        ) {
            throw InvalidPackageException("This Office package has an unsupported main content type.")
        }

        val hasMacros = names.any { it.endsWith("vbaProject.bin", ignoreCase = true) } ||
            mainContentType.contains("macroEnabled", ignoreCase = true)
        ctx.stage = InspectionStage.VARIANT_IDENTIFY
        val variant = variantFor(mainContentType, hasMacros)
        ctx.stage = InspectionStage.RELATIONSHIPS
        val externalCount = countExternalRelationships(zip, names)

        val notes = mutableListOf<String>()
        if (hasMacros) notes += "Macros are never executed; this is a read-only preview."
        if (variant == Variant.TEMPLATE || variant == Variant.MACRO_TEMPLATE) {
            notes += "Template files are shown as read-only previews."
        }
        if (externalCount > 0) notes += "External links are blocked while previewing offline."

        return PackageInfo(
            family = family,
            variant = variant,
            mainContentType = mainContentType,
            hasMacros = hasMacros,
            externalRelationshipCount = externalCount,
            notice = notes.joinToString(" ").takeIf { it.isNotBlank() }
        )
    }

    private fun declaredOrMeasuredSize(zip: ZipFile, entry: ZipArchiveEntry): Long {
        val declared = entry.size
        if (declared >= 0L) return declared
        // Unknown central-directory size: measure with a hard cap, never trust it.
        var measured = 0L
        val buffer = ByteArray(16 * 1024)
        zip.getInputStream(entry).use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                measured += read
                if (measured > MAX_SINGLE_UNCOMPRESSED_BYTES) {
                    throw InvalidPackageException("One part of this Office file is too large to preview.")
                }
            }
        }
        return measured
    }

    private fun isSymlink(entry: ZipArchiveEntry): Boolean =
        (entry.unixMode and 0xF000) == 0xA000

    private fun countExternalRelationships(zip: ZipFile, names: Set<String>): Int {
        var count = 0
        for (name in names) {
            if (!name.endsWith(".rels", ignoreCase = true)) continue
            val xml = readPart(zip, name, MAX_XML_PART_BYTES) ?: continue
            val document = parseSecureXml(xml)
            val relationships = document.getElementsByTagName("Relationship")
            for (i in 0 until relationships.length) {
                val relationship = relationships.item(i) as? Element ?: continue
                if (relationship.getAttribute("TargetMode").equals("External", ignoreCase = true)) {
                    val target = relationship.getAttribute("Target").lowercase()
                    if (target.startsWith("file:") || target.startsWith("javascript:") ||
                        target.startsWith("data:") || target.startsWith("content:")
                    ) {
                        throw InvalidPackageException("This Office package contains an unsafe external relationship.")
                    }
                    count++
                }
            }
        }
        return count
    }

    /**
     * Browsers do not expand external entities from DOMParser, but a DOCTYPE in
     * any OOXML part is invalid and can still trigger parser-specific behavior.
     * Reject it from every XML/relationship part with a small prefix scan.
     */
    private fun scanXmlParts(zip: ZipFile, names: Set<String>) {
        var parts = 0
        var scanned = 0L
        for (name in names) {
            if (!name.endsWith(".xml", ignoreCase = true) &&
                !name.endsWith(".rels", ignoreCase = true)
            ) continue
            if (++parts > MAX_XML_PARTS) {
                throw InvalidPackageException("This Office package has too many XML parts.")
            }
            val prefix = readPrefix(zip, name, XML_DOCTYPE_SCAN_BYTES)
                ?: throw InvalidPackageException("An Office XML part is missing.")
            scanned += prefix.size
            if (scanned > MAX_XML_SCAN_BYTES) {
                throw InvalidPackageException("This Office package has too much XML to inspect safely.")
            }
            val utf8Text = String(prefix, Charsets.UTF_8).uppercase()
            // UTF-16 XML has NULs between ASCII characters; strip them for the
            // same signature check without fully parsing every part.
            val zeroStripped = String(prefix, Charsets.ISO_8859_1)
                .replace("\u0000", "")
                .uppercase()
            if (listOf(utf8Text, zeroStripped).any {
                    it.contains("<!DOCTYPE") || it.contains("<!ENTITY")
                }
            ) {
                throw InvalidPackageException("This Office package contains unsafe XML declarations.")
            }
        }
    }

    private fun readPrefix(zip: ZipFile, name: String, maxBytes: Long): ByteArray? {
        val entry = zip.getEntry(name) ?: return null
        if (entry.isDirectory) return null
        val out = ByteArrayOutputStream(minOf(maxBytes, 64L * 1024L).toInt())
        val buffer = ByteArray(8 * 1024)
        var total = 0L
        zip.getInputStream(entry).use { input ->
            while (total < maxBytes) {
                val read = input.read(buffer, 0, minOf(buffer.size.toLong(), maxBytes - total).toInt())
                if (read < 0) break
                out.write(buffer, 0, read)
                total += read
            }
        }
        return out.toByteArray()
    }

    private fun readPart(zip: ZipFile, name: String, maxBytes: Long): ByteArray? {
        val entry = zip.getEntry(name) ?: return null
        if (entry.isDirectory) return null
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        zip.getInputStream(entry).use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > maxBytes) {
                    throw InvalidPackageException("An Office XML part is too large to inspect.")
                }
                out.write(buffer, 0, read)
            }
        }
        return out.toByteArray()
    }

    /**
     * Hardened DOM parsing of one bounded metadata part, delegated to
     * [OfficeXmlHardening]. The previous inline configuration used JVM-only parser
     * features unconditionally (XInclude, FEATURE_SECURE_PROCESSING, Xerces feature
     * URLs), which always throw on Android's bundled parser and rejected every valid
     * Office package on real devices; see [OfficeXmlHardening] for the platform-safe
     * configuration and the parser-independent DOCTYPE defense.
     */
    private fun parseSecureXml(bytes: ByteArray): Document =
        OfficeXmlHardening.parseSecureXml(bytes)

    fun familyFor(mainPart: String): Family = when {
        mainPart.startsWith("word/") -> Family.WORD
        mainPart.startsWith("xl/") -> Family.EXCEL
        mainPart.startsWith("ppt/") -> Family.POWERPOINT
        else -> Family.UNKNOWN
    }

    fun expectedFamily(formatId: String): Family? = when (formatId) {
        "docx" -> Family.WORD
        "xlsx" -> Family.EXCEL
        "pptx" -> Family.POWERPOINT
        else -> null
    }

    fun variantFor(contentType: String, hasMacros: Boolean): Variant {
        val lower = contentType.lowercase()
        val template = lower.contains("template")
        val show = lower.contains("slideshow")
        val addIn = lower.contains("addin")
        return when {
            addIn -> Variant.ADD_IN
            show -> Variant.SHOW
            hasMacros && template -> Variant.MACRO_TEMPLATE
            hasMacros -> Variant.MACRO_ENABLED
            template -> Variant.TEMPLATE
            lower.contains("macroenabled") -> Variant.MACRO_ENABLED
            else -> Variant.STANDARD
        }
    }

    /** Exposed for focused unit tests and shared with archive-path checks. */
    fun isUnsafeEntryName(raw: String): Boolean {
        if (raw.isEmpty() || raw.length > MAX_NAME_LENGTH || raw.contains('\u0000')) return true
        val name = raw.replace('\\', '/')
        val lower = name.lowercase()
        if (name.startsWith("/") || name.startsWith("//") || name.contains("//")) return true
        if (name.length >= 2 && name[1] == ':' && name[0].isLetter()) return true
        if (lower.contains("%2e%2e") || lower.contains("%2f") || lower.contains("%5c")) return true
        val segments = name.split('/')
        if (segments.size > MAX_PATH_DEPTH) return true
        return segments.any { it == ".." || it == "." }
    }
}
