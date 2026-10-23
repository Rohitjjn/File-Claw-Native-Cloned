package com.example.formats.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Loop 14: pure math behind the PDF view rotation (toolbar button, 90-degree
 * steps) and the right-edge page scrubber. The bundled reader mirrors
 * [PdfRotationPolicy.rotateRect] / rotatedSize in JavaScript; these tests pin
 * the contract both implementations must agree on.
 */
class PdfRotationPolicyTest {

    // --- normalize / stepping ------------------------------------------------

    @Test
    fun `normalize maps negative and oversized degrees into 0 to 359`() {
        assertEquals(0, PdfRotationPolicy.normalize(0))
        assertEquals(90, PdfRotationPolicy.normalize(90))
        assertEquals(0, PdfRotationPolicy.normalize(360))
        assertEquals(0, PdfRotationPolicy.normalize(720))
        assertEquals(270, PdfRotationPolicy.normalize(-90))
        assertEquals(180, PdfRotationPolicy.normalize(-180))
        assertEquals(90, PdfRotationPolicy.normalize(450))
    }

    @Test
    fun `stepAfter cycles through all four clockwise steps`() {
        assertEquals(90, PdfRotationPolicy.stepAfter(0))
        assertEquals(180, PdfRotationPolicy.stepAfter(90))
        assertEquals(270, PdfRotationPolicy.stepAfter(180))
        assertEquals(0, PdfRotationPolicy.stepAfter(270))
        assertEquals(90, PdfRotationPolicy.stepAfter(360))
    }

    // --- geometry ------------------------------------------------------------

    @Test
    fun `quarter turns swap width and height`() {
        assertEquals(842f to 595f, PdfRotationPolicy.rotatedSize(595f, 842f, 90))
        assertEquals(842f to 595f, PdfRotationPolicy.rotatedSize(595f, 842f, 270))
    }

    @Test
    fun `zero and 180 keep dimensions`() {
        assertEquals(595f to 842f, PdfRotationPolicy.rotatedSize(595f, 842f, 0))
        assertEquals(595f to 842f, PdfRotationPolicy.rotatedSize(595f, 842f, 180))
    }

    @Test
    fun `rotateRect at 90 degrees moves the top-left corner clockwise`() {
        // A rect hugging the top-left of the page ends up hugging the top-right
        // after a clockwise quarter turn.
        val rotated = PdfRotationPolicy.rotateRect(
            PdfRotationPolicy.NormRect(0f, 0f, 0.2f, 0.1f), 90
        )
        assertTrue(rotated.left > 0.7f)
        assertEquals(0f, rotated.top, 1e-6f)
        assertEquals(1f, rotated.right, 1e-6f)
        assertTrue(rotated.bottom <= 0.2f + 1e-6f)
    }

    @Test
    fun `rotateRect at 180 degrees mirrors both axes`() {
        val rotated = PdfRotationPolicy.rotateRect(
            PdfRotationPolicy.NormRect(0.1f, 0.2f, 0.4f, 0.3f), 180
        )
        assertEquals(1f - 0.4f, rotated.left, 1e-6f)
        assertEquals(1f - 0.3f, rotated.top, 1e-6f)
        assertEquals(1f - 0.1f, rotated.right, 1e-6f)
        assertEquals(1f - 0.2f, rotated.bottom, 1e-6f)
    }

    @Test
    fun `rotateRect is closed under four applications`() {
        val original = PdfRotationPolicy.NormRect(0.12f, 0.34f, 0.56f, 0.78f)
        var current = original
        for (step in 1..4) {
            current = PdfRotationPolicy.rotateRect(current, 90)
        }
        assertEquals(original.left, current.left, 1e-4f)
        assertEquals(original.top, current.top, 1e-4f)
        assertEquals(original.right, current.right, 1e-4f)
        assertEquals(original.bottom, current.bottom, 1e-4f)
    }

    @Test
    fun `full-page rect always maps to full-page rect`() {
        for (deg in intArrayOf(0, 90, 180, 270, 450, -90)) {
            val rotated = PdfRotationPolicy.rotateRect(PdfRotationPolicy.NormRect.FULL, deg)
            assertEquals(0f, rotated.left, 1e-6f)
            assertEquals(0f, rotated.top, 1e-6f)
            assertEquals(1f, rotated.right, 1e-6f)
            assertEquals(1f, rotated.bottom, 1e-6f)
        }
    }

    @Test
    fun `rotation preserves normalized area`() {
        val rect = PdfRotationPolicy.NormRect(0.1f, 0.2f, 0.5f, 0.7f)
        val originalArea = (rect.right - rect.left) * (rect.bottom - rect.top)
        for (deg in intArrayOf(90, 180, 270)) {
            val rotated = PdfRotationPolicy.rotateRect(rect, deg)
            val rotatedArea = (rotated.right - rotated.left) * (rotated.bottom - rotated.top)
            assertTrue(abs(originalArea - rotatedArea) < 1e-5f)
        }
    }

    // --- scroll anchor ---------------------------------------------------------

    @Test
    fun `anchor fraction is clamped into 0 to 1`() {
        assertEquals(0f, PdfRotationPolicy.anchorFraction(-10f, 500f), 1e-6f)
        assertEquals(0.5f, PdfRotationPolicy.anchorFraction(250f, 500f), 1e-6f)
        assertEquals(1f, PdfRotationPolicy.anchorFraction(900f, 500f), 1e-6f)
        assertEquals(0f, PdfRotationPolicy.anchorFraction(10f, 0f), 1e-6f)
        assertEquals(0f, PdfRotationPolicy.anchorFraction(Float.NaN, 500f), 1e-6f)
    }

    @Test
    fun `anchor round-trip restores the same relative position`() {
        val scrollTop = 300f
        val pageHeight = 900f
        val anchor = PdfRotationPolicy.anchorFraction(scrollTop, pageHeight)
        // After rotation the (taller) rotated page is 1200px: the anchor lands at
        // the same RELATIVE position, never past the page end.
        val restored = PdfRotationPolicy.scrollTopForAnchor(anchor, 1200f)
        assertEquals(400f, restored, 1e-3f)
        assertTrue(restored <= 1200f)
    }

    // --- scrubber mapping ------------------------------------------------------

    @Test
    fun `scrubber fraction maps to clamped 1-based pages`() {
        assertEquals(1, PdfRotationPolicy.pageFromFraction(0f, 500))
        assertEquals(500, PdfRotationPolicy.pageFromFraction(1f, 500))
        assertEquals(250, PdfRotationPolicy.pageFromFraction(0.498f, 500))
        // Sloppy input can never escape the document.
        assertEquals(1, PdfRotationPolicy.pageFromFraction(-3f, 500))
        assertEquals(500, PdfRotationPolicy.pageFromFraction(9f, 500))
        assertEquals(1, PdfRotationPolicy.pageFromFraction(0.5f, 1))
        assertEquals(1, PdfRotationPolicy.pageFromFraction(0.5f, 0))
        assertEquals(1, PdfRotationPolicy.pageFromFraction(Float.NaN, 42))
    }

    @Test
    fun `scrubber page and fraction are inverse mappings`() {
        val pageCount = 371
        for (page in intArrayOf(1, 2, 186, 370, 371)) {
            val fraction = PdfRotationPolicy.fractionForPage(page, pageCount)
            assertEquals(page, PdfRotationPolicy.pageFromFraction(fraction, pageCount))
        }
    }

    @Test
    fun `single-page document keeps the thumb at the top`() {
        assertEquals(0f, PdfRotationPolicy.fractionForPage(1, 1), 1e-6f)
        assertEquals(0f, PdfRotationPolicy.fractionForPage(1, 0), 1e-6f)
    }

    // --- guardrails ------------------------------------------------------------

    @Test
    fun `allowed rotation steps are exactly the right angles`() {
        for (deg in PdfRotationPolicy.ALLOWED_DEGREES) {
            assertEquals(deg, PdfRotationPolicy.normalize(deg))
            assertEquals(deg % 180 == 90, PdfRotationPolicy.isQuarterTurn(deg))
        }
        assertFalse(PdfRotationPolicy.isQuarterTurn(0))
        assertFalse(PdfRotationPolicy.isQuarterTurn(180))
        assertTrue(PdfRotationPolicy.isQuarterTurn(90))
        assertTrue(PdfRotationPolicy.isQuarterTurn(270))
    }
}
