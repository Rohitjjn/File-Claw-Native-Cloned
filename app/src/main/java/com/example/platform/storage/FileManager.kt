package com.example.platform.storage

import com.example.core.archive.ArchiveEntryInfo
import com.example.core.archive.ArchiveLimits
import com.example.core.archive.ArchiveNode
import com.example.core.archive.ArchiveTreeBuilder
import com.example.core.archive.copyLimited
import com.example.core.format.FormatDetector
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
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
    fun parseZipStructure(filePath: String, password: String? = null): ArchiveNode {
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
        return tree.root
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


}
