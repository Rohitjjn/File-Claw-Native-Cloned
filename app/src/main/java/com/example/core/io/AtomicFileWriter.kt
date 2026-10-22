package com.example.core.io

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Crash-safe file replacement.
 *
 * The old code opened the target with a truncating stream (`File.writeText`, `FileOutputStream(file)`),
 * so any exception halfway through left the user's file empty or corrupt. This writer streams into a
 * temp file in the same directory, fsyncs it, then atomically renames it over the target. If anything
 * fails the original file is left byte-for-byte untouched.
 */
object AtomicFileWriter {

    /** Runs [block] against a stream for a temp file, then atomically replaces [target]. */
    fun write(target: File, block: (OutputStream) -> Unit) {
        val dir = target.absoluteFile.parentFile ?: throw IOException("no parent directory for ${target.name}")
        if (!dir.isDirectory) throw IOException("parent directory missing")
        val temp = File.createTempFile(".fc-", ".tmp", dir)
        try {
            FileOutputStream(temp).use { fos ->
                val out = BufferedOutputStream(fos, 64 * 1024)
                block(out)
                out.flush()
                fos.fd.sync()
            }
            replace(temp, target)
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    fun writeBytes(target: File, bytes: ByteArray) = write(target) { it.write(bytes) }

    private fun replace(temp: File, target: File) {
        try {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
