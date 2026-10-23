package com.example.services

import com.example.core.archive.ArchiveEntryInfo
import com.example.core.archive.ArchiveLimits
import com.example.core.archive.ArchiveNode
import com.example.core.archive.ArchiveTreeBuilder
import com.example.core.archive.copyLimited
import com.example.core.csv.DelimitedParser
import com.example.core.csv.DelimitedWriter
import com.example.core.format.FormatDetector
import com.example.core.io.AtomicFileWriter
import com.example.core.io.TextEncoding
import com.example.core.io.TextLoadResult
import com.example.core.io.TextLoader
import com.example.core.io.TextSaver
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream

/**
 * Thin Android-facing facade over the pure-Kotlin `com.example.core` layer.
 * Every write is atomic, every read is bounded, every archive is limit-checked.
 */
class FileManager {

    data class ZipNode(
        val name: String,
        val isDirectory: Boolean,
        val path: String,
        val size: Long = 0,
        val children: MutableList<ZipNode> = mutableListOf(),
        /** Entry name was rejected as unsafe (zip-slip / too deep): shown with a warning, never extractable. */
        val unsafe: Boolean = false
    )

    /** Parsed delimited text plus what is needed to write it back byte-compatibly. */
    data class TableData(
        val rows: List<List<String>>,
        val truncated: Boolean,
        val delimiter: Char,
        val charset: Charset,
        val hadBom: Boolean,
        val lineEnd: String
    )

    data class ExcelData(val rows: List<List<String>>, val truncated: Boolean, val sheetCount: Int, val sheetName: String)

    // ------------------------------------------------------------------------------------------
    // Text
    // ------------------------------------------------------------------------------------------

    /** Bounded text load. Throws instead of returning error strings as content. */
    fun loadText(filePath: String, encoding: String): TextLoadResult =
        TextLoader.load(File(filePath), encoding)

    fun saveText(filePath: String, content: String, charset: Charset, writeBom: Boolean) {
        TextSaver.save(File(filePath), content, charset, writeBom)
    }

    // ------------------------------------------------------------------------------------------
    // Delimited text (CSV / TSV / PSV)
    // ------------------------------------------------------------------------------------------

    fun loadDelimited(filePath: String, extension: String, encoding: String): TableData {
        val file = File(filePath)
        if (!file.isFile) throw IOException("File not found")
        val head = ByteArray(8192)
        val n = file.inputStream().use { it.read(head) }.coerceAtLeast(0)
        val detected = TextEncoding.detect(head, n)
        val charset = TextEncoding.resolve(encoding) ?: detected.charset
        val hadBom = detected.bomLength > 0 && charset == detected.charset
        val sample = String(head, 0, n, charset)
        val ext = extension.lowercase()
        val base = DelimitedParser.delimiterFor(ext)
        val delimiter = if (ext == "csv") DelimitedParser.sniffDelimiter(sample, base) else base
        val lineEnd = if (sample.contains("\r\n")) "\r\n" else "\n"
        val result = InputStreamReader(file.inputStream().buffered(64 * 1024), charset).use { reader ->
            DelimitedParser.parse(reader, delimiter, VIEW_MAX_ROWS, VIEW_MAX_COLS)
        }
        var rows = result.rows
        if (rows.isNotEmpty() && rows[0].isNotEmpty() && rows[0][0].startsWith("\uFEFF")) {
            val first = rows[0].toMutableList().also { it[0] = it[0].removePrefix("\uFEFF") }
            rows = listOf<List<String>>(first) + rows.drop(1)
        }
        return TableData(rows, result.truncated, delimiter, charset, hadBom, lineEnd)
    }

    fun saveDelimited(filePath: String, rows: List<List<String>>, data: TableData) {
        AtomicFileWriter.write(File(filePath)) { out ->
            if (data.hadBom) out.write(TextEncoding.bomBytes(data.charset))
            // Not closed on purpose: AtomicFileWriter owns the stream and must fsync before closing it.
            val writer = OutputStreamWriter(out, data.charset)
            DelimitedWriter.write(rows, writer, data.delimiter, data.lineEnd)
        }
    }

    // ------------------------------------------------------------------------------------------
    // Excel (read-only: writing would flatten multi-sheet workbooks and drop formulas/styles)
    // ------------------------------------------------------------------------------------------

    fun loadExcel(filePath: String): ExcelData {
        val file = File(filePath)
        if (!file.isFile) throw IOException("File not found")
        org.dhatim.fastexcel.reader.ReadableWorkbook(file).use { wb ->
            val sheetCount = wb.sheets.count().toInt()
            if (sheetCount == 0) return ExcelData(emptyList(), false, 0, "")
            val sheet = wb.firstSheet
            val rows = ArrayList<List<String>>()
            var truncated = false
            sheet.openStream().use { stream ->
                val iter = stream.iterator()
                while (iter.hasNext()) {
                    if (rows.size >= VIEW_MAX_ROWS) {
                        truncated = true
                        break
                    }
                    val values = ArrayList<String>()
                    for (cell in iter.next()) {
                        if (values.size >= VIEW_MAX_COLS) {
                            truncated = true
                            break
                        }
                        values.add(cell?.text ?: "")
                    }
                    rows.add(values)
                }
            }
            return ExcelData(rows, truncated, sheetCount, sheet.name ?: "")
        }
    }

    // ------------------------------------------------------------------------------------------
    // Archives
    // ------------------------------------------------------------------------------------------

    private val limits = ArchiveLimits()

    private val zip4jIds = setOf("zip", "zip-app", "docx", "xlsx", "pptx", "odt", "ods", "odp", "epub")
    private val tarLikeIds = setOf("tar", "tgz", "tbz2", "txz", "ar", "cpio")
    private val singleStreamIds = setOf("gz", "bz2", "xz")

    private fun archiveId(file: File): String = FormatDetector.detect(file).spec.id

    fun isArchiveEncrypted(filePath: String): Boolean {
        val file = File(filePath)
        if (!file.exists()) return false
        return try {
            when (archiveId(file)) {
                "7z" -> try {
                    SevenZFile(file).use { }
                    false
                } catch (e: Exception) {
                    looksLikePasswordError(e)
                }
                in zip4jIds -> net.lingala.zip4j.ZipFile(file).use { it.isEncrypted }
                else -> false
            }
        } catch (e: Exception) {
            looksLikePasswordError(e)
        }
    }

    private fun looksLikePasswordError(e: Exception): Boolean =
        e.message?.lowercase()?.contains("password") == true || e.javaClass.simpleName.contains("Password")

    /**
     * Lists an archive as a tree. Throws [UnsupportedOperationException] for formats that can be
     * identified but not read (RAR, ISO...) and [IOException] for corrupt archives, so the UI can
     * show a real error instead of an empty folder.
     */
    fun parseZipStructure(filePath: String, password: String? = null): ZipNode {
        val file = File(filePath)
        if (!file.isFile) throw IOException("Archive not found")
        val id = archiveId(file)
        val rootName = file.name
        val tree = when {
            id in zip4jIds -> net.lingala.zip4j.ZipFile(file).use { zf ->
                if (!password.isNullOrEmpty()) zf.setPassword(password.toCharArray())
                val entries = zf.fileHeaders.asSequence().map {
                    ArchiveEntryInfo(it.fileName, it.isDirectory, it.uncompressedSize, it.compressedSize)
                }
                ArchiveTreeBuilder.build(rootName, entries, limits)
            }
            id == "7z" -> {
                val sz = if (!password.isNullOrEmpty()) SevenZFile(file, password.toCharArray()) else SevenZFile(file)
                sz.use { s ->
                    val entries = generateSequence { s.nextEntry }.map {
                        ArchiveEntryInfo(it.name ?: "", it.isDirectory, it.size)
                    }
                    ArchiveTreeBuilder.build(rootName, entries, limits)
                }
            }
            id in tarLikeIds -> openArchiveStream(file, id).use { s ->
                val entries = generateSequence { s.nextEntry }.map { e: ArchiveEntry ->
                    ArchiveEntryInfo(e.name ?: "", e.isDirectory, e.size)
                }
                ArchiveTreeBuilder.build(rootName, entries, limits)
            }
            id in singleStreamIds -> {
                val inner = singleStreamEntryName(file)
                ArchiveTreeBuilder.build(rootName, sequenceOf(ArchiveEntryInfo(inner, false, 0)), limits)
            }
            id == "rar" -> throw UnsupportedOperationException("RAR archives are not supported yet")
            else -> throw UnsupportedOperationException("This archive type is not supported yet")
        }
        return convert(tree.root)
    }

    /** Iterative conversion: archive trees can be deep, and recursion would risk a StackOverflowError. */
    private fun convert(root: ArchiveNode): ZipNode {
        val out = ZipNode(root.name, true, root.path, root.size)
        val stack = ArrayDeque<Pair<ArchiveNode, ZipNode>>()
        stack.addLast(root to out)
        while (stack.isNotEmpty()) {
            val (src, dst) = stack.removeLast()
            for (c in src.children) {
                val z = ZipNode(c.name, c.isDirectory, c.path, c.size, unsafe = c.unsafe)
                dst.children.add(z)
                if (c.children.isNotEmpty()) stack.addLast(c to z)
            }
        }
        return out
    }

    private fun openArchiveStream(file: File, id: String): ArchiveInputStream<*> {
        val raw = BufferedInputStream(FileInputStream(file), 64 * 1024)
        return try {
            when (id) {
                "tar" -> TarArchiveInputStream(raw)
                "tgz" -> TarArchiveInputStream(GzipCompressorInputStream(raw))
                "tbz2" -> TarArchiveInputStream(BZip2CompressorInputStream(raw))
                "txz" -> TarArchiveInputStream(XZCompressorInputStream(raw))
                "ar" -> ArArchiveInputStream(raw)
                "cpio" -> CpioArchiveInputStream(raw)
                else -> throw UnsupportedOperationException("Unsupported archive type")
            }
        } catch (e: Exception) {
            raw.close()
            throw e
        }
    }

    private fun singleStreamEntryName(file: File): String {
        val n = file.name
        val lower = n.lowercase()
        return listOf(".gz", ".bz2", ".xz", ".lzma").firstOrNull { lower.endsWith(it) && n.length > it.length }
            ?.let { n.substring(0, n.length - it.length) } ?: (n + ".out")
    }

    private fun openSingleStream(file: File, id: String): InputStream {
        val raw = BufferedInputStream(FileInputStream(file), 64 * 1024)
        return when (id) {
            "gz" -> GzipCompressorInputStream(raw)
            "bz2" -> BZip2CompressorInputStream(raw)
            "xz" -> XZCompressorInputStream(raw)
            else -> raw
        }
    }

    /**
     * Extracts one entry to [destFile] (a cache path chosen by the app, never derived from the entry name).
     * Enforces per-entry size and compression-ratio limits; a partial file is deleted on any failure.
     */
    fun extractZipEntry(zipFilePath: String, entryPath: String, destFile: File, password: String? = null): Boolean {
        if (entryPath.startsWith("!unsafe/")) return false
        val file = File(zipFilePath)
        if (!file.isFile) return false
        var ok = false
        try {
            destFile.parentFile?.mkdirs()
            val id = archiveId(file)
            when {
                id in zip4jIds -> net.lingala.zip4j.ZipFile(file).use { zf ->
                    if (!password.isNullOrEmpty()) zf.setPassword(password.toCharArray())
                    val header = zf.getFileHeader(entryPath) ?: return false
                    limits.violation(header.compressedSize, header.uncompressedSize)?.let { throw IOException(it) }
                    zf.getInputStream(header).use { input ->
                        destFile.outputStream().buffered().use { copyLimited(input, it, limits.maxEntryBytes) }
                    }
                    ok = true
                }
                id == "7z" -> {
                    val sz = if (!password.isNullOrEmpty()) SevenZFile(file, password.toCharArray()) else SevenZFile(file)
                    sz.use { s ->
                        var entry = s.nextEntry
                        while (entry != null) {
                            if (entry.name == entryPath && !entry.isDirectory) {
                                limits.violation(-1, entry.size)?.let { throw IOException(it) }
                                s.getInputStream(entry).use { input ->
                                    destFile.outputStream().buffered().use { copyLimited(input, it, limits.maxEntryBytes) }
                                }
                                ok = true
                                break
                            }
                            entry = s.nextEntry
                        }
                    }
                }
                id in tarLikeIds -> openArchiveStream(file, id).use { s ->
                    var entry = s.nextEntry
                    while (entry != null) {
                        if (entry.name?.trimStart('/') == entryPath && !entry.isDirectory) {
                            limits.violation(-1, entry.size)?.let { throw IOException(it) }
                            destFile.outputStream().buffered().use { copyLimited(s, it, limits.maxEntryBytes) }
                            ok = true
                            break
                        }
                        entry = s.nextEntry
                    }
                }
                id in singleStreamIds -> openSingleStream(file, id).use { input ->
                    destFile.outputStream().buffered().use { copyLimited(input, it, limits.maxEntryBytes) }
                    ok = true
                }
            }
        } catch (e: Exception) {
            ok = false
        } finally {
            if (!ok) destFile.delete()
        }
        return ok
    }

    /**
     * Replaces one entry inside a ZIP. The new archive is built beside the original and swapped in
     * with an atomic rename, so a crash can never leave the user without their archive.
     */
    fun updateZipEntry(zipFilePath: String, entryPath: String, entrySrcFile: File): Boolean {
        val zip = File(zipFilePath)
        val dir = zip.absoluteFile.parentFile ?: return false
        var temp: File? = null
        return try {
            val tmp = File.createTempFile(".fc-zip-", ".tmp", dir)
            temp = tmp
            ZipFile(zip).use { zf ->
                ZipOutputStream(tmp.outputStream().buffered()).use { zos ->
                    val seen = HashSet<String>()
                    val entries = zf.entries()
                    while (entries.hasMoreElements()) {
                        val old = entries.nextElement()
                        if (!seen.add(old.name)) continue
                        val copy = ZipEntry(old.name).apply { time = old.time }
                        zos.putNextEntry(copy)
                        if (old.name == entryPath) {
                            entrySrcFile.inputStream().use { it.copyTo(zos) }
                        } else if (!old.isDirectory) {
                            zf.getInputStream(old).use { it.copyTo(zos) }
                        }
                        zos.closeEntry()
                    }
                }
            }
            try {
                Files.move(tmp.toPath(), zip.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (e: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(tmp.toPath(), zip.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            true
        } catch (e: Exception) {
            false
        } finally {
            temp?.takeIf { it.exists() }?.delete()
        }
    }

    companion object {
        /** Row/column caps for the in-app grid; beyond these the sheet is shown read-only. */
        const val VIEW_MAX_ROWS = 50_000
        const val VIEW_MAX_COLS = 256
    }
}
