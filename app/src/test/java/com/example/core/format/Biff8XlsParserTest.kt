package com.example.core.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Biff8XlsParserTest {

    @Test
    fun decodeRk_handlesIntegerValues() {
        // RK = (value shl 2) or 2 (integer flag)
        val rk1 = (42 shl 2) or 2
        assertEquals("42", Biff8XlsParser.decodeRk(rk1))

        // Negative integer
        val rkNeg = (-15 shl 2) or 2
        assertEquals("-15", Biff8XlsParser.decodeRk(rkNeg))

        // Integer divided by 100: bit 0 = 1, bit 1 = 1
        val rkCent = (1250 shl 2) or 3
        assertEquals("12.5", Biff8XlsParser.decodeRk(rkCent))
    }

    @Test
    fun parseWorkbookStream_extractsSheetsAndCells() {
        val stream = java.io.ByteArrayOutputStream()

        fun writeRecord(type: Int, payload: ByteArray) {
            val h = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            h.putShort(type.toShort())
            h.putShort(payload.size.toShort())
            stream.write(h.array())
            stream.write(payload)
        }

        // 1. BOF (0x0809)
        writeRecord(0x0809, ByteArray(8))

        // 2. BOUNDSHEET (0x0085)
        val sheetName = "Quarterly"
        val bsBuf = ByteBuffer.allocate(8 + sheetName.length).order(ByteOrder.LITTLE_ENDIAN)
        bsBuf.putInt(0) // offset
        bsBuf.put(0) // hsState
        bsBuf.put(0) // dt
        bsBuf.put(sheetName.length.toByte())
        bsBuf.put(0) // ASCII flag
        bsBuf.put(sheetName.toByteArray(Charsets.ISO_8859_1))
        writeRecord(0x0085, bsBuf.array())

        // 3. SST (0x00FC): 2 strings ("Apple", "Banana")
        val sstStream = java.io.ByteArrayOutputStream()
        val sstHeader = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        sstHeader.putInt(2) // total
        sstHeader.putInt(2) // unique
        sstStream.write(sstHeader.array())

        fun writeSstString(str: String) {
            val buf = ByteBuffer.allocate(3 + str.length).order(ByteOrder.LITTLE_ENDIAN)
            buf.putShort(str.length.toShort())
            buf.put(0) // ASCII
            buf.put(str.toByteArray(Charsets.ISO_8859_1))
            sstStream.write(buf.array())
        }
        writeSstString("Apple")
        writeSstString("Banana")
        writeRecord(0x00FC, sstStream.toByteArray())

        // 4. LABELSST at Row 0, Col 0 -> "Apple" (SST 0)
        val label0 = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN)
        label0.putShort(0) // row 0
        label0.putShort(0) // col 0
        label0.putShort(0) // xf
        label0.putInt(0) // sst 0
        writeRecord(0x00FD, label0.array())

        // 5. LABELSST at Row 0, Col 1 -> "Banana" (SST 1)
        val label1 = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN)
        label1.putShort(0) // row 0
        label1.putShort(1) // col 1
        label1.putShort(0) // xf
        label1.putInt(1) // sst 1
        writeRecord(0x00FD, label1.array())

        // 6. NUMBER at Row 1, Col 1 -> 99.0
        val num = ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN)
        num.putShort(1) // row 1
        num.putShort(1) // col 1
        num.putShort(0) // xf
        num.putDouble(99.0)
        writeRecord(0x0203, num.array())

        // 7. EOF (0x000A)
        writeRecord(0x000A, ByteArray(0))

        val parsed = Biff8XlsParser.parseWorkbookStream(stream.toByteArray())

        assertEquals(1, parsed.sheetNames.size)
        assertEquals("Quarterly", parsed.sheetNames[0])
        assertTrue("Rows should not be empty", parsed.primarySheetRows.isNotEmpty())
        assertEquals("Apple", parsed.primarySheetRows[0][0])
        assertEquals("Banana", parsed.primarySheetRows[0][1])
        assertEquals("99", parsed.primarySheetRows[1][1])
    }
}
