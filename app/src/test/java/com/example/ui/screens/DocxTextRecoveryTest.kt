package com.example.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Loop 14: the DOCX plain-text view is an honest RECOVERY state. These tests pin
 * the decision rules: recovered text shows only when extraction actually found
 * content, the headline reads as a failure (never as a feature), and the reason
 * line is clamped and content-free.
 */
class DocxTextRecoveryTest {

    @Test
    fun `recovered text shows only when paragraphs were extracted`() {
        assertTrue(DocxTextRecovery.shouldShowRecoveredText(1))
        assertTrue(DocxTextRecovery.shouldShowRecoveredText(1200))
        assertFalse(DocxTextRecovery.shouldShowRecoveredText(0))
        assertFalse(DocxTextRecovery.shouldShowRecoveredText(-3))
    }

    @Test
    fun `headline reads as a failure not a feature`() {
        val headline = DocxTextRecovery.bannerHeadline()
        assertTrue(headline.contains("failed", ignoreCase = true))
        assertTrue(headline.contains("recovered", ignoreCase = true))
        // The old "Clean Document View" label masqueraded as a normal preview.
        assertFalse(headline.contains("Clean Document View", ignoreCase = true))
    }

    @Test
    fun `reason is null for blank input and clamped otherwise`() {
        assertNull(DocxTextRecovery.displayReason(null))
        assertNull(DocxTextRecovery.displayReason(""))
        assertNull(DocxTextRecovery.displayReason("   "))
        val long = "x".repeat(500)
        val clamped = DocxTextRecovery.displayReason(long)
        assertEquals(DocxTextRecovery.MAX_REASON_CHARS, clamped!!.length)
        val short = "HTTP 404"
        assertEquals(short, DocxTextRecovery.displayReason(short))
        // Whitespace-only padding is trimmed before display.
        assertEquals("net::ERR_FILE_NOT_FOUND", DocxTextRecovery.displayReason("  net::ERR_FILE_NOT_FOUND  "))
    }

    @Test
    fun `stats label is null when nothing was recovered`() {
        assertNull(DocxTextRecovery.statsLabel(0, 0))
        val stats = DocxTextRecovery.statsLabel(4821, 57)
        assertTrue(stats!!.contains("4821"))
        assertTrue(stats.contains("57"))
        assertTrue(stats.contains("recovered"))
    }
}
