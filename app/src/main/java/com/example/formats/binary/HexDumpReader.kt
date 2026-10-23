package com.example.formats.binary

import java.io.File
import java.io.FileInputStream

/** Reads the same bounded, 4 KiB hex/ASCII preview used by the previous preview dispatcher. */
object HexDumpReader {
    const val MAX_BYTES = 4096

    fun read(file: File): Pair<List<String>, List<String>> {
        val hexLines = mutableListOf<String>()
        val asciiLines = mutableListOf<String>()
        try {
            FileInputStream(file).use { input ->
                val buffer = ByteArray(16)
                var offset = 0
                while (offset < MAX_BYTES) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead <= 0) break

                    val hexBuilder = StringBuilder()
                    val asciiBuilder = StringBuilder()
                    hexBuilder.append(String.format("%08X: ", offset))
                    for (index in 0 until 16) {
                        if (index < bytesRead) {
                            val byte = buffer[index].toInt() and 0xFF
                            hexBuilder.append(String.format("%02X ", byte))
                            val character = byte.toChar()
                            asciiBuilder.append(if (character in ' '..'~') character else '.')
                        } else {
                            hexBuilder.append("   ")
                        }
                        if (index == 7) hexBuilder.append(' ')
                    }
                    hexLines += hexBuilder.toString()
                    asciiLines += asciiBuilder.toString()
                    offset += bytesRead
                }
            }
        } catch (_: Exception) {
            hexLines += "Error generating hex dump"
            asciiLines += ""
        }
        return hexLines to asciiLines
    }
}
