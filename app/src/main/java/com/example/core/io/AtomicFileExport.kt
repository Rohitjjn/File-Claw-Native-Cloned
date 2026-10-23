package com.example.core.io

import java.io.File

/** Bounded-stream, atomic copy used by format exports. */
object AtomicFileExport {
    fun copy(source: File, destination: File) {
        require(source.isFile) { "Source file not found." }
        AtomicFileWriter.write(destination) { output ->
            source.inputStream().buffered().use { input -> input.copyTo(output) }
        }
    }
}
