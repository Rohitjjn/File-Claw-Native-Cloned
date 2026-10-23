package com.example.formats.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Loop 10 tests for the device-tiered PDF canvas pixel budget policy. */
class PdfRenderBudgetPolicyTest {

    @Test
    fun `low ram devices get the conservative budget`() {
        assertEquals(6_000_000L, PdfRenderBudgetPolicy.budgetPixels(isLowRamDevice = true, memoryClassMb = 512))
        assertEquals(6_000_000L, PdfRenderBudgetPolicy.budgetPixels(isLowRamDevice = true, memoryClassMb = null))
    }

    @Test
    fun `large heap devices get the highest budget`() {
        assertEquals(16_000_000L, PdfRenderBudgetPolicy.budgetPixels(isLowRamDevice = false, memoryClassMb = 256))
        assertEquals(16_000_000L, PdfRenderBudgetPolicy.budgetPixels(isLowRamDevice = false, memoryClassMb = 512))
    }

    @Test
    fun `mid heap devices get the default budget`() {
        assertEquals(12_000_000L, PdfRenderBudgetPolicy.budgetPixels(isLowRamDevice = false, memoryClassMb = 128))
        assertEquals(12_000_000L, PdfRenderBudgetPolicy.budgetPixels(isLowRamDevice = false, memoryClassMb = 255))
    }

    @Test
    fun `small heap devices stay on the previous fixed budget`() {
        assertEquals(9_000_000L, PdfRenderBudgetPolicy.budgetPixels(isLowRamDevice = false, memoryClassMb = 96))
        assertEquals(9_000_000L, PdfRenderBudgetPolicy.budgetPixels(isLowRamDevice = false, memoryClassMb = 48))
    }

    @Test
    fun `missing heap signal falls back to the default budget`() {
        assertEquals(12_000_000L, PdfRenderBudgetPolicy.budgetPixels(isLowRamDevice = false, memoryClassMb = null))
    }

    @Test
    fun `budget is monotonic across heap tiers`() {
        val low = PdfRenderBudgetPolicy.budgetPixels(false, 48)
        val mid = PdfRenderBudgetPolicy.budgetPixels(false, 128)
        val high = PdfRenderBudgetPolicy.budgetPixels(false, 256)
        val lowRam = PdfRenderBudgetPolicy.budgetPixels(true, 48)
        assertTrue(lowRam < low)
        assertTrue(low <= mid)
        assertTrue(mid <= high)
    }

    @Test
    fun `coerceBudget clamps to the safe range`() {
        assertEquals(PdfRenderBudgetPolicy.MIN_BUDGET_PIXELS, PdfRenderBudgetPolicy.coerceBudget(1L))
        assertEquals(PdfRenderBudgetPolicy.MAX_BUDGET_PIXELS, PdfRenderBudgetPolicy.coerceBudget(999_000_000L))
        assertEquals(12_000_000L, PdfRenderBudgetPolicy.coerceBudget(12_000_000L))
    }

    @Test
    fun `max budget stays inside the shipped JS clamp`() {
        // search.html derives the per-page budget with the same [4e6, 24e6] clamp;
        // the policy must agree.
        assertTrue(PdfRenderBudgetPolicy.MAX_BUDGET_PIXELS <= 24_000_000L)
        assertTrue(PdfRenderBudgetPolicy.MIN_BUDGET_PIXELS >= 4_000_000L)
    }

    @Test
    fun `total budget covers the resident page window in RGBA bytes`() {
        // Loop 12: total = perPage pixels x resident window x 4 bytes, so the reader
        // recovers exactly the Loop 10 per-page tiers from the total it receives.
        assertEquals(12_000_000L * 12, PdfRenderBudgetPolicy.totalCanvasBytes(false, 128))
        assertEquals(16_000_000L * 12, PdfRenderBudgetPolicy.totalCanvasBytes(false, 256))
        assertEquals(9_000_000L * 12, PdfRenderBudgetPolicy.totalCanvasBytes(false, 96))
        assertEquals(6_000_000L * 12, PdfRenderBudgetPolicy.totalCanvasBytes(true, 256))
        assertEquals(12_000_000L * 12, PdfRenderBudgetPolicy.totalCanvasBytes(false, null))
    }

    @Test
    fun `resident window keeps aggregate canvas memory bounded`() {
        // The whole point of the Loop 12 change: 3 resident canvases must never
        // exceed the total budget on any tier.
        for (memoryClass in listOf(null, 48, 96, 128, 256)) {
            val perPage = PdfRenderBudgetPolicy.budgetPixels(false, memoryClass)
            val totalBytes = PdfRenderBudgetPolicy.totalCanvasBytes(false, memoryClass)
            val residentBytes = perPage * PdfRenderBudgetPolicy.RESIDENT_PAGE_WINDOW * PdfRenderBudgetPolicy.BYTES_PER_PIXEL
            assertEquals("memoryClass=$memoryClass", totalBytes, residentBytes)
        }
    }
}
