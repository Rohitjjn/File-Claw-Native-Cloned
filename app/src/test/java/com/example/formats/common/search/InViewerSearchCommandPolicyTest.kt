package com.example.formats.common.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Loop 12 regression tests for the WebView find-bar command policy.
 *
 * Device symptom being guarded against: "the document shows 7 results but the arrows
 * only reach 2-3". The old combined effect re-issued `searchWord()` on EVERY arrow
 * press; `searchWord()` resets the renderer's active index to 0, and the appended
 * next/prev call then advanced one step from that fresh index — so the counter could
 * never walk past 2 no matter how many matches existed. These tests pin the fixed
 * command ordering: a query is searched exactly once, and navigation triggers become
 * pure deltas that walk the whole navigable list in both directions.
 */
class InViewerSearchCommandPolicyTest {

    @Test
    fun `first plan issues exactly one search and no steps`() {
        val plan = InViewerSearchCommandPolicy.plan(
            InViewerSearchCommandPolicy.Issued(),
            query = "invoice",
            nextTarget = 0,
            prevTarget = 0
        )
        assertTrue(plan.search)
        assertEquals(0, plan.nextSteps)
        assertEquals(0, plan.prevSteps)
        assertFalse(plan.isEmpty)
        assertEquals("invoice", plan.issued.query)
    }

    @Test
    fun `seven next presses never re-search and walk every match`() {
        // Simulates the reported bug scenario: 7 matches, user presses next 7 times.
        var issued = InViewerSearchCommandPolicy.Issued(query = "invoice")
        var searchCommands = 0
        var nextStepsTotal = 0
        for (press in 1..7) {
            val plan = InViewerSearchCommandPolicy.plan(issued, "invoice", press, 0)
            if (plan.search) searchCommands++
            nextStepsTotal += plan.nextSteps
            assertEquals("press $press must not re-search", 0, if (plan.search) 1 else 0)
            assertEquals("press $press must advance exactly one step", 1, plan.nextSteps)
            issued = plan.issued
        }
        assertEquals(0, searchCommands)
        assertEquals(7, nextStepsTotal)
    }

    @Test
    fun `previous presses walk backwards without re-searching`() {
        var issued = InViewerSearchCommandPolicy.Issued(query = "q", next = 3)
        for (press in 1..3) {
            val plan = InViewerSearchCommandPolicy.plan(issued, "q", 3, press)
            assertFalse(plan.search)
            assertEquals(0, plan.nextSteps)
            assertEquals(1, plan.prevSteps)
            issued = plan.issued
        }
    }

    @Test
    fun `changed query is searched again before any queued steps`() {
        val plan = InViewerSearchCommandPolicy.plan(
            InViewerSearchCommandPolicy.Issued(query = "old", next = 2),
            query = "new",
            nextTarget = 3,
            prevTarget = 0
        )
        assertTrue(plan.search)
        assertEquals(1, plan.nextSteps)
        assertEquals(0, plan.prevSteps)
    }

    @Test
    fun `typing the same query twice only issues one search`() {
        val first = InViewerSearchCommandPolicy.plan(
            InViewerSearchCommandPolicy.Issued(),
            query = "invoice",
            nextTarget = 0,
            prevTarget = 0
        )
        val second = InViewerSearchCommandPolicy.plan(first.issued, "invoice", 0, 0)
        assertFalse(second.search)
        assertTrue(second.isEmpty)
    }

    @Test
    fun `trigger counter reset is absorbed without replaying steps`() {
        // File switched: the screen resets its counters to 0 while the renderer had
        // already been told next=5. The reset must be absorbed, never replayed.
        val plan = InViewerSearchCommandPolicy.plan(
            InViewerSearchCommandPolicy.Issued(query = "invoice", next = 5, prev = 2),
            query = "invoice",
            nextTarget = 0,
            prevTarget = 0
        )
        assertFalse(plan.search)
        assertEquals(0, plan.nextSteps)
        assertEquals(0, plan.prevSteps)
        assertEquals(0, plan.issued.next)
        assertEquals(0, plan.issued.prev)
    }

    @Test
    fun `plan is empty only when nothing changed`() {
        val issued = InViewerSearchCommandPolicy.Issued(query = "q", next = 4, prev = 2)
        assertTrue(InViewerSearchCommandPolicy.plan(issued, "q", 4, 2).isEmpty)
        assertFalse(InViewerSearchCommandPolicy.plan(issued, "q", 5, 2).isEmpty)
        assertFalse(InViewerSearchCommandPolicy.plan(issued, "q", 4, 3).isEmpty)
        assertFalse(InViewerSearchCommandPolicy.plan(issued, "other", 4, 2).isEmpty)
    }
}
