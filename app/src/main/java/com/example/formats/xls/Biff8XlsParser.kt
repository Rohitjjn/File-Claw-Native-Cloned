package com.example.formats.xls

import com.example.formats.legacyoffice.Ole2CompoundFile
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure Kotlin BIFF8 Excel (.xls) binary workbook parser.
 * Reads Microsoft Excel 97–2003 spreadsheets (.xls) without external libraries.
 */
object Biff8XlsParser {

    data class ParsedXls(
        val sheetNames: List<String>,
        val primarySheetRows: List<List<String>>
    )

    fun parse(file: File): ParsedXls {
        if (!Ole2CompoundFile.isOle2File(file)) {
            return ParsedXls(emptyList(), emptyList())
        }

        Ole2CompoundFile.open(file).use { cfb ->
            val workbookBytes = cfb.openStream("Workbook")
                ?: cfb.openStream("Book")
                ?: return ParsedXls(emptyList(), emptyList())

            return parseWorkbookStream(workbookBytes)
        }
    }

    fun parseWorkbookStream(bytes: ByteArray): ParsedXls {
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val sstList = mutableListOf<String>()
        val sheetNames = mutableListOf<String>()

        // Map of (row, col) to cell string value
        val cells = mutableMapOf<Pair<Int, Int>, String>()
        var maxRow = -1
        var maxCol = -1

        while (bb.remaining() >= 4) {
            val recordType = bb.short.toInt() and 0xFFFF
            val recordLen = bb.short.toInt() and 0xFFFF

            if (bb.remaining() < recordLen) break

            val recordStart = bb.position()

            when (recordType) {
                // BOUNDSHEET (0x0085): Sheet Name
                0x0085 -> {
                    if (recordLen >= 7) {
                        bb.position(recordStart + 6)
                        val nameLen = bb.get().toInt() and 0xFF
                        val isUtf16 = if (recordLen > 7) (bb.get().toInt() and 1) != 0 else false
                        val name = if (isUtf16 && bb.remaining() >= nameLen * 2) {
                            val chars = CharArray(nameLen)
                            for (i in 0 until nameLen) chars[i] = bb.char
                            String(chars)
                        } else if (bb.remaining() >= nameLen) {
                            val strBytes = ByteArray(nameLen)
                            bb.get(strBytes)
                            String(strBytes, Charsets.ISO_8859_1)
                        } else "Sheet${sheetNames.size + 1}"
                        sheetNames.add(name)
                    }
                }

                // SST (0x00FC): Shared String Table
                0x00FC -> {
                    parseSst(bb, recordStart, recordLen, sstList)
                }

                // LABELSST (0x00FD): Cell with string reference to SST
                0x00FD -> {
                    if (recordLen >= 10) {
                        val row = bb.short.toInt() and 0xFFFF
                        val col = bb.short.toInt() and 0xFFFF
                        bb.short // xf
                        val sstIndex = bb.int
                        if (sstIndex in sstList.indices) {
                            cells[row to col] = sstList[sstIndex]
                            if (row > maxRow) maxRow = row
                            if (col > maxCol) maxCol = col
                        }
                    }
                }

                // NUMBER (0x0203): IEEE 754 Floating point cell
                0x0203 -> {
                    if (recordLen >= 14) {
                        val row = bb.short.toInt() and 0xFFFF
                        val col = bb.short.toInt() and 0xFFFF
                        bb.short // xf
                        val value = bb.double
                        val text = if (value == value.toLong().toDouble()) value.toLong().toString()
                                   else "%.4f".format(value).trimEnd('0').trimEnd('.')
                        cells[row to col] = text
                        if (row > maxRow) maxRow = row
                        if (col > maxCol) maxCol = col
                    }
                }

                // RK (0x027E and 0x007E): Compact integer / float
                0x027E, 0x007E -> {
                    if (recordLen >= 10) {
                        val row = bb.short.toInt() and 0xFFFF
                        val col = bb.short.toInt() and 0xFFFF
                        bb.short // xf
                        val rk = bb.int
                        cells[row to col] = decodeRk(rk)
                        if (row > maxRow) maxRow = row
                        if (col > maxCol) maxCol = col
                    }
                }

                // MULRK (0x00BD): Multiple RK cells in one row
                0x00BD -> {
                    if (recordLen >= 6) {
                        val row = bb.short.toInt() and 0xFFFF
                        val firstCol = bb.short.toInt() and 0xFFFF
                        val lastCol = bb.getShort(recordStart + recordLen - 2).toInt() and 0xFFFF
                        val numRks = lastCol - firstCol + 1
                        for (i in 0 until numRks) {
                            if (bb.position() + 6 <= recordStart + recordLen - 2) {
                                bb.short // xf
                                val rk = bb.int
                                val col = firstCol + i
                                cells[row to col] = decodeRk(rk)
                                if (row > maxRow) maxRow = row
                                if (col > maxCol) maxCol = col
                            }
                        }
                    }
                }

                // LABEL (0x0204): Direct string cell (BIFF2-BIFF7)
                0x0204 -> {
                    if (recordLen >= 8) {
                        val row = bb.short.toInt() and 0xFFFF
                        val col = bb.short.toInt() and 0xFFFF
                        bb.short // xf
                        val strLen = bb.short.toInt() and 0xFFFF
                        if (bb.remaining() >= strLen) {
                            val strBytes = ByteArray(strLen)
                            bb.get(strBytes)
                            cells[row to col] = String(strBytes, Charsets.ISO_8859_1)
                            if (row > maxRow) maxRow = row
                            if (col > maxCol) maxCol = col
                        }
                    }
                }
            }

            bb.position(recordStart + recordLen)
        }

        val rows = mutableListOf<List<String>>()
        if (maxRow >= 0 && maxCol >= 0) {
            val safeMaxRow = minOf(maxRow, 5000)
            val safeMaxCol = minOf(maxCol, 100)
            for (r in 0..safeMaxRow) {
                val rowList = mutableListOf<String>()
                for (c in 0..safeMaxCol) {
                    rowList.add(cells[r to c] ?: "")
                }
                // Only include if row has at least one non-blank entry
                if (rowList.any { it.isNotEmpty() }) {
                    rows.add(rowList)
                }
            }
        }

        return ParsedXls(sheetNames, rows)
    }

    private fun parseSst(bb: ByteBuffer, recordStart: Int, recordLen: Int, outList: MutableList<String>) {
        if (recordLen < 8) return
        bb.position(recordStart + 4)
        val uniqueCount = bb.int
        val limit = minOf(uniqueCount, 25_000)

        for (i in 0 until limit) {
            if (bb.position() >= recordStart + recordLen - 3) break
            val charCount = bb.short.toInt() and 0xFFFF
            val flags = bb.get().toInt() and 0xFF
            val isUtf16 = (flags and 1) != 0
            val hasFarEast = (flags and 4) != 0
            val hasRichText = (flags and 8) != 0

            var formattingRuns = 0
            if (hasRichText && bb.position() + 2 <= recordStart + recordLen) {
                formattingRuns = bb.short.toInt() and 0xFFFF
            }
            var extRstLen = 0
            if (hasFarEast && bb.position() + 4 <= recordStart + recordLen) {
                extRstLen = bb.int
            }

            val str = if (isUtf16) {
                val neededBytes = charCount * 2
                if (bb.position() + neededBytes <= recordStart + recordLen) {
                    val chars = CharArray(charCount)
                    for (c in 0 until charCount) chars[c] = bb.char
                    String(chars)
                } else null
            } else {
                if (bb.position() + charCount <= recordStart + recordLen) {
                    val strBytes = ByteArray(charCount)
                    bb.get(strBytes)
                    String(strBytes, Charsets.ISO_8859_1)
                } else null
            }

            if (str != null) {
                outList.add(str)
            } else {
                break
            }

            // Skip formatting runs and extended phonetic info
            val skipBytes = formattingRuns * 4 + extRstLen
            if (bb.position() + skipBytes <= recordStart + recordLen) {
                bb.position(bb.position() + skipBytes)
            } else break
        }
    }

    fun decodeRk(rk: Int): String {
        val isDiv100 = (rk and 1) != 0
        val isInt = (rk and 2) != 0
        return if (isInt) {
            val v = rk shr 2
            if (isDiv100) (v / 100.0).toString() else v.toString()
        } else {
            val bits = (rk.toLong() and 0xFFFFFFFC.toInt().toLong()) shl 32
            val d = java.lang.Double.longBitsToDouble(bits)
            val v = if (isDiv100) d / 100.0 else d
            if (v == v.toLong().toDouble()) v.toLong().toString()
            else "%.4f".format(v).trimEnd('0').trimEnd('.')
        }
    }
}
