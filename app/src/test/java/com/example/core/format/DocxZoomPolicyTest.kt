package com.example.core.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocxZoomPolicyTest {

    @Test
    fun `default zoom is 100 percent fit-to-width`() {
        assertEquals(1.00f, DocxZoomPolicy.DEFAULT_ZOOM)
        assertEquals(100, DocxZoomPolicy.defaultPercent())
    }

    @Test
    fun `coerce clamps to 50-500 percent`() {
        assertEquals(0.50f, DocxZoomPolicy.coerce(0.1f))
        assertEquals(5.0f, DocxZoomPolicy.coerce(9f))
        assertEquals(1.00f, DocxZoomPolicy.coerce(1.00f))
    }

    @Test
    fun `pinch multiplies and clamps`() {
        assertEquals(2.00f, DocxZoomPolicy.applyPinch(1.00f, 2.0f))
        assertEquals(0.50f, DocxZoomPolicy.applyPinch(1.00f, 0.2f))
        assertEquals(5.0f, DocxZoomPolicy.applyPinch(4.9f, 1.5f))
        // Degenerate deltas leave the value untouched (but coerced).
        assertEquals(1.00f, DocxZoomPolicy.applyPinch(1.00f, Float.NaN))
        assertEquals(0.50f, DocxZoomPolicy.applyPinch(0.10f, 0f))
    }

    @Test
    fun `pinch rounds away float drift`() {
        val zoomed = DocxZoomPolicy.applyPinch(1.00f, 1.3333f)
        assertEquals(1.333f, zoomed)
    }

    @Test
    fun `double tap toggles between fit and zoomed reading`() {
        assertEquals(DocxZoomPolicy.ZOOMED_READING, DocxZoomPolicy.toggleDoubleTap(1.00f))
        assertEquals(DocxZoomPolicy.DEFAULT_ZOOM, DocxZoomPolicy.toggleDoubleTap(2.00f))
        assertEquals(DocxZoomPolicy.ZOOMED_READING, DocxZoomPolicy.toggleDoubleTap(0.50f))
        assertEquals(DocxZoomPolicy.DEFAULT_ZOOM, DocxZoomPolicy.toggleDoubleTap(3.00f))
    }

    @Test
    fun `percent conversions round trip`() {
        assertEquals(1.00f, DocxZoomPolicy.fromPercent(100))
        assertEquals(125, DocxZoomPolicy.toPercent(1.25f))
        assertEquals(500, DocxZoomPolicy.toPercent(9f))
        assertTrue(DocxZoomPolicy.toPercent(DocxZoomPolicy.fromPercent(137)) == 137)
    }
}
