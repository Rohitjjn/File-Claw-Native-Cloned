package com.example.formats.legacyoffice

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure Kotlin reader for Microsoft OLE2 Compound File Binary (CFB) format.
 * Specification: [MS-CFB].
 * Supports reading streams (e.g. "Workbook", "Book", "WordDocument", "PowerPoint Document")
 * completely offline with bounded memory.
 */
class Ole2CompoundFile private constructor(private val file: File) : AutoCloseable {

    private val raf = RandomAccessFile(file, "r")
    private val sectorSize: Int
    private val fatSectorNumbers: List<Int>
    private val directoryEntries = mutableListOf<DirEntry>()

    data class DirEntry(
        val name: String,
        val type: Int, // 1: storage, 2: stream, 5: root
        val startSector: Int,
        val streamSize: Long
    )

    init {
        val header = ByteArray(512)
        raf.seek(0)
        raf.readFully(header)

        // Magic check: D0 CF 11 E0 A1 B1 1A E1
        val magic = byteArrayOf(
            0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte(),
            0xA1.toByte(), 0xB1.toByte(), 0x1A.toByte(), 0xE1.toByte()
        )
        for (i in magic.indices) {
            if (header[i] != magic[i]) {
                throw IllegalArgumentException("Not an OLE2 Compound File (invalid magic header)")
            }
        }

        val headerBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val sectorShift = headerBuffer.getShort(30).toInt()
        sectorSize = 1 shl sectorShift

        val fatSectorsCount = headerBuffer.getInt(44)
        val firstDirSector = headerBuffer.getInt(48)

        // Read initial 109 FAT sector IDs from header (starting at offset 76)
        val fatSectors = mutableListOf<Int>()
        for (i in 0 until minOf(109, fatSectorsCount)) {
            val sec = headerBuffer.getInt(76 + i * 4)
            if (sec >= 0) fatSectors.add(sec)
        }
        fatSectorNumbers = fatSectors

        // Read FAT table into memory (only the sectors we need)
        val fatTable = readFatTable()

        // Read Directory entries starting at firstDirSector
        readDirectory(firstDirSector, fatTable)
    }

    private fun sectorOffset(sectorNumber: Int): Long {
        return (sectorNumber + 1).toLong() * sectorSize
    }

    private fun readFatTable(): IntArray {
        val totalEntries = fatSectorNumbers.size * (sectorSize / 4)
        val fat = IntArray(totalEntries)
        var offset = 0
        val buffer = ByteArray(sectorSize)
        for (sec in fatSectorNumbers) {
            raf.seek(sectorOffset(sec))
            raf.readFully(buffer)
            val bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
            while (bb.hasRemaining()) {
                if (offset < fat.size) {
                    fat[offset++] = bb.int
                } else break
            }
        }
        return fat
    }

    private fun readDirectory(firstDirSector: Int, fat: IntArray) {
        val dirSectors = mutableListOf<Int>()
        var curr = firstDirSector
        val maxIterations = 5000
        var count = 0
        while (curr >= 0 && curr < fat.size && curr != -2 && count++ < maxIterations) { // -2 is ENDOFCHAIN
            dirSectors.add(curr)
            curr = fat[curr]
        }

        val dirBuffer = ByteArray(sectorSize)
        for (sec in dirSectors) {
            raf.seek(sectorOffset(sec))
            raf.readFully(dirBuffer)
            val bb = ByteBuffer.wrap(dirBuffer).order(ByteOrder.LITTLE_ENDIAN)
            // Each DirEntry is 128 bytes
            for (entryIdx in 0 until (sectorSize / 128)) {
                bb.position(entryIdx * 128)
                val rawName = ByteArray(64)
                bb.get(rawName)
                val nameLen = bb.short.toInt() // length in bytes including trailing null
                val type = bb.get().toInt()
                bb.position(entryIdx * 128 + 116)
                val startSector = bb.int
                val size = bb.long

                if (type == 1 || type == 2 || type == 5) {
                    val nameChars = if (nameLen > 2) (nameLen - 2) / 2 else 0
                    val nameBuilder = StringBuilder()
                    for (c in 0 until minOf(31, nameChars)) {
                        val ch = (rawName[c * 2].toInt() and 0xFF) or ((rawName[c * 2 + 1].toInt() and 0xFF) shl 8)
                        if (ch != 0) nameBuilder.append(ch.toChar())
                    }
                    val name = nameBuilder.toString()
                    if (name.isNotEmpty()) {
                        directoryEntries.add(DirEntry(name, type, startSector, size))
                    }
                }
            }
        }
    }

    fun hasStream(streamName: String): Boolean {
        return directoryEntries.any { it.name.equals(streamName, ignoreCase = true) }
    }

    fun getStreamNames(): List<String> = directoryEntries.map { it.name }

    /**
     * Reads the entire contents of a stream into a byte array, capped at [maxBytes].
     */
    fun openStream(streamName: String, maxBytes: Int = 30_000_000): ByteArray? {
        val entry = directoryEntries.firstOrNull { it.name.equals(streamName, ignoreCase = true) } ?: return null
        if (entry.streamSize <= 0) return ByteArray(0)

        val fat = readFatTable()
        val readSize = minOf(entry.streamSize, maxBytes.toLong()).toInt()
        val out = ByteArray(readSize)
        var bytesRead = 0

        var currSector = entry.startSector
        val sectorBuf = ByteArray(sectorSize)
        var count = 0
        while (currSector >= 0 && currSector < fat.size && currSector != -2 && bytesRead < readSize && count++ < 50_000) {
            raf.seek(sectorOffset(currSector))
            val toRead = minOf(sectorSize, readSize - bytesRead)
            raf.readFully(sectorBuf, 0, toRead)
            System.arraycopy(sectorBuf, 0, out, bytesRead, toRead)
            bytesRead += toRead
            currSector = fat[currSector]
        }

        return out
    }

    override fun close() {
        try {
            raf.close()
        } catch (_: Exception) {}
    }

    companion object {
        fun isOle2File(file: File): Boolean {
            if (!file.isFile || file.length() < 512) return false
            return try {
                RandomAccessFile(file, "r").use { f ->
                    val magic = ByteArray(8)
                    f.readFully(magic)
                    magic[0] == 0xD0.toByte() && magic[1] == 0xCF.toByte() &&
                    magic[2] == 0x11.toByte() && magic[3] == 0xE0.toByte() &&
                    magic[4] == 0xA1.toByte() && magic[5] == 0xB1.toByte() &&
                    magic[6] == 0x1A.toByte() && magic[7] == 0xE1.toByte()
                }
            } catch (_: Exception) {
                false
            }
        }

        fun open(file: File): Ole2CompoundFile {
            return Ole2CompoundFile(file)
        }
    }
}
