package com.example.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Loop 10 tests for the header find bar navigation math. Guards the wrap-around and
 * empty-state behaviour that the header prev/next arrows and the IME Search action share.
 */
class InViewerSearchNavigationTest {

    @Test
    fun `no matches never navigate`() {
        assertNull(InViewerSearchNavigation.nextIndex(0, 0))
        assertNull(InViewerSearchNavigation.prevIndex(0, 0))
        assertNull(InViewerSearchNavigation.nextIndex(3, 0))
        assertNull(InViewerSearchNavigation.prevIndex(3, 0))
        assertNull(InViewerSearchNavigation.nextIndex(0, -1))
    }

    @Test
    fun `single match always stays on index zero`() {
        assertEquals(0, InViewerSearchNavigation.nextIndex(0, 1))
        assertEquals(0, InViewerSearchNavigation.prevIndex(0, 1))
        assertEquals(0, InViewerSearchNavigation.nextIndex(9, 1))
    }

    @Test
    fun `next wraps forward through the whole list`() {
        assertEquals(1, InViewerSearchNavigation.nextIndex(0, 4))
        assertEquals(2, InViewerSearchNavigation.nextIndex(1, 4))
        assertEquals(3, InViewerSearchNavigation.nextIndex(2, 4))
        assertEquals(0, InViewerSearchNavigation.nextIndex(3, 4))
    }

    @Test
    fun `prev wraps backward through the whole list`() {
        assertEquals(3, InViewerSearchNavigation.prevIndex(0, 4))
        assertEquals(2, InViewerSearchNavigation.prevIndex(3, 4))
        assertEquals(1, InViewerSearchNavigation.prevIndex(2, 4))
        assertEquals(0, InViewerSearchNavigation.prevIndex(1, 4))
    }

    @Test
    fun `out of range current index is clamped instead of crashing`() {
        // A stale index from a previous query must not break navigation on a new query.
        assertEquals(1, InViewerSearchNavigation.nextIndex(999, 4))
        assertEquals(3, InViewerSearchNavigation.prevIndex(999, 4))
        assertEquals(1, InViewerSearchNavigation.nextIndex(-5, 4))
    }

    @Test
    fun `truncated counts stay internally consistent`() {
        // Navigation walks the stored (navigable) matches; a truncated total never
        // produces an index outside the stored range.
        val navigable = 10
        var index = 0
        repeat(25) { index = InViewerSearchNavigation.nextIndex(index, navigable)!! }
        assertEquals(5, index)
    }
}
