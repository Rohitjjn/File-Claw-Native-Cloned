package com.example.core.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HttpRangeTest {

    private val fileSize = 10_000L

    @Test
    fun `parse valid closed range`() {
        val range = HttpRange.parse("bytes=0-499", fileSize)
        assertNotNull(range)
        assertEquals(0L, range!!.start)
        assertEquals(499L, range.end)
        assertEquals(500L, range.length)
        assertEquals(10_000L, range.total)
        assertEquals("bytes 0-499/10000", range.contentRangeHeader)
    }

    @Test
    fun `parse open-ended range`() {
        val range = HttpRange.parse("bytes=9500-", fileSize)
        assertNotNull(range)
        assertEquals(9500L, range!!.start)
        assertEquals(9999L, range.end)
        assertEquals(500L, range.length)
        assertEquals("bytes 9500-9999/10000", range.contentRangeHeader)
    }

    @Test
    fun `parse suffix range`() {
        val range = HttpRange.parse("bytes=-500", fileSize)
        assertNotNull(range)
        assertEquals(9500L, range!!.start)
        assertEquals(9999L, range.end)
        assertEquals(500L, range.length)
        assertEquals("bytes 9500-9999/10000", range.contentRangeHeader)
    }

    @Test
    fun `clamp end exceeding total file size`() {
        val range = HttpRange.parse("bytes=9000-20000", fileSize)
        assertNotNull(range)
        assertEquals(9000L, range!!.start)
        assertEquals(9999L, range.end)
        assertEquals(1000L, range.length)
    }

    @Test
    fun `unsatisfiable ranges return null`() {
        // Start beyond file size
        assertNull(HttpRange.parse("bytes=15000-16000", fileSize))
        // Start greater than end
        assertNull(HttpRange.parse("bytes=500-200", fileSize))
        // Suffix zero
        assertNull(HttpRange.parse("bytes=-0", fileSize))
        // Non-bytes unit
        assertNull(HttpRange.parse("items=0-10", fileSize))
        // Malformed headers
        assertNull(HttpRange.parse("invalid-header", fileSize))
        assertNull(HttpRange.parse("", fileSize))
        assertNull(HttpRange.parse(null, fileSize))
        assertNull(HttpRange.parse("bytes=abc-def", fileSize))
    }

    @Test
    fun `multiple ranges parses first range`() {
        val range = HttpRange.parse("bytes=0-100, 200-300", fileSize)
        assertNotNull(range)
        assertEquals(0L, range!!.start)
        assertEquals(100L, range.end)
    }
}
