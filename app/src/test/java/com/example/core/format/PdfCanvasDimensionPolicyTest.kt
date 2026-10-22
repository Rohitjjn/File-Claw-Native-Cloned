package com.example.core.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Loop 12 tests for the canvas rasterization limits.
 *
 * Device symptom being guarded against: very large pages (posters, blueprints,
 * CAD exports) rendered permanently blank, because the old ratio had a floor of 1.0
 * and no per-dimension cap — the requested canvas exceeded Chromium's internal
 * canvas limits and silently ignored every draw command.
 */
class PdfCanvasDimensionPolicyTest {

    private fun ratio(
        width: Double,
        height: Double,
        desired: Double = 3.0,
        budget: Double = 12_000_000.0,
        maxRatio: Double = 8.0
    ): Double = PdfCanvasDimensionPolicy.renderRatio(width, height, desired, budget, maxRatio)

    @Test
    fun `normal phone page keeps the desired ratio`() {
        // A4-ish page at ~360 CSS px wide, 12M budget: area cap ~7.9, so desired 3 wins.
        assertEquals(3.0, ratio(360.0, 509.0), 0.0001)
    }

    @Test
    fun `area budget caps the ratio for dense pages`() {
        // 1000 x 1400 CSS px = 1.4M px; area cap = sqrt(12M / 1.4M) ≈ 2.93 < desired 3.
        val value = ratio(1000.0, 1400.0)
        assertTrue("ratio $value must be below desired 3.0", value < 3.0)
        assertEquals(kotlin.math.sqrt(12_000_000.0 / 1_400_000.0), value, 0.0001)
    }

    @Test
    fun `very wide page is dimension capped instead of blank`() {
        // 10000 x 500 CSS px: area cap alone would allow ratio 4.9, but the 8192px
        // dimension cap limits it to 0.8192 — a downsampled, renderable canvas.
        assertEquals(8192.0 / 10000.0, ratio(10000.0, 500.0), 0.0001)
    }

    @Test
    fun `oversized page downsamples below ratio one`() {
        // 20000 x 20000 CSS px poster: area cap 0.077, dimension cap 0.4096 → 0.077.
        val value = ratio(20000.0, 20000.0)
        assertTrue("ratio $value must be below 1.0", value < 1.0)
        assertEquals(kotlin.math.sqrt(12_000_000.0 / 400_000_000.0), value, 0.0001)
    }

    @Test
    fun `ratio never drops below the visibility floor`() {
        // Absurd page: 500000 x 500000. Both caps push far below the floor.
        assertEquals(PdfCanvasDimensionPolicy.MIN_RENDER_RATIO, ratio(500000.0, 500000.0), 0.0001)
    }

    @Test
    fun `absolute ratio cap still binds`() {
        assertEquals(8.0, ratio(100.0, 120.0, desired = 20.0), 0.0001)
    }

    @Test
    fun `invalid inputs degrade to safe defaults`() {
        assertEquals(1.0, ratio(0.0, 0.0, desired = 0.0), 0.0001)
        assertEquals(1.0, ratio(-5.0, Double.NaN, desired = Double.NaN), 0.0001)
    }

    @Test
    fun `backing dimensions are floored to at least one pixel`() {
        val (width, height) = PdfCanvasDimensionPolicy.backingDimensions(360.0, 509.0, 3.0)
        assertEquals(1080, width)
        assertEquals(1527, height)
        val (tinyWidth, tinyHeight) = PdfCanvasDimensionPolicy.backingDimensions(0.01, 0.01, 0.05)
        assertEquals(1, tinyWidth)
        assertEquals(1, tinyHeight)
    }

    @Test
    fun `dimension cap keeps every canvas inside the 8192 ceiling`() {
        // Property sweep: no combination may produce a dimension above the cap.
        val widths = doubleArrayOf(360.0, 1000.0, 5000.0, 8191.0, 8192.0, 20000.0, 60000.0)
        val heights = doubleArrayOf(509.0, 1400.0, 2000.0, 12000.0, 40000.0)
        for (width in widths) {
            for (height in heights) {
                val value = ratio(width, height, desired = 8.0)
                val (backedWidth, backedHeight) =
                    PdfCanvasDimensionPolicy.backingDimensions(width, height, value)
                assertTrue(
                    "width $backedWidth exceeded cap for ${width}x$height",
                    backedWidth <= PdfCanvasDimensionPolicy.MAX_CANVAS_DIMENSION_PX
                )
                assertTrue(
                    "height $backedHeight exceeded cap for ${width}x$height",
                    backedHeight <= PdfCanvasDimensionPolicy.MAX_CANVAS_DIMENSION_PX
                )
            }
        }
    }
}
