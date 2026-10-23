package com.example.formats.docx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Loop 10 regression tests for the DOCX pinch gesture gate.
 *
 * Device symptom being guarded against: the first multi-touch event (the transition
 * where the second finger lands) used to produce a wildly wrong `calculateZoom()` delta
 * that slammed the zoom into the 25%/500% clamp on first contact.
 */
class PinchGestureGateTest {

    @Test
    fun `transition event with second finger landing is skipped`() {
        val gate = PinchGestureGate()
        // Finger 1 down, moves: no zoom.
        assertNull(gate.onPointerEvent(1, 1f))
        // Transition event: second finger just landed (bogus delta arrives from calculateZoom).
        assertNull(gate.onPointerEvent(2, 4.7f))
        assertFalse(gate.onPointerEvent(2, 4.7f) == null)
    }

    @Test
    fun `stable two-pointer events pass their deltas through`() {
        val gate = PinchGestureGate()
        gate.onPointerEvent(1, 1f)
        assertNull(gate.onPointerEvent(2, 3.1f)) // transition skipped
        assertEquals(1.05f, gate.onPointerEvent(2, 1.05f)!!)
        assertEquals(0.96f, gate.onPointerEvent(2, 0.96f)!!)
        assertTrue(gate.isGestureActive)
    }

    @Test
    fun `finger count change mid gesture re-arms the gate`() {
        val gate = PinchGestureGate()
        gate.onPointerEvent(1, 1f)
        gate.onPointerEvent(2, 2.0f) // transition, skipped
        assertEquals(1.1f, gate.onPointerEvent(2, 1.1f)!!)
        // Third finger lands: another transition with mixed history - must be skipped.
        assertNull(gate.onPointerEvent(3, 5.5f))
        assertEquals(1.2f, gate.onPointerEvent(3, 1.2f)!!)
        // One finger lifts: transition again, skipped.
        assertNull(gate.onPointerEvent(2, 0.1f))
        assertEquals(0.9f, gate.onPointerEvent(2, 0.9f)!!)
    }

    @Test
    fun `single pointer event always disarms and returns null`() {
        val gate = PinchGestureGate()
        gate.onPointerEvent(1, 1f)
        gate.onPointerEvent(2, 2.0f)
        assertTrue(gate.isGestureActive)
        assertNull(gate.onPointerEvent(1, 1.4f))
        assertFalse(gate.isGestureActive)
        // Next gesture must start clean: the first two-pointer event is a transition.
        assertNull(gate.onPointerEvent(2, 9.9f))
    }

    @Test
    fun `all pointers lifted ends the gesture`() {
        val gate = PinchGestureGate()
        gate.onPointerEvent(2, 2.0f)
        gate.onPointerEvent(2, 1.1f)
        assertNull(gate.onPointerEvent(0, 1f))
        assertFalse(gate.isGestureActive)
    }

    @Test
    fun `degenerate deltas are never forwarded`() {
        val gate = PinchGestureGate()
        gate.onPointerEvent(2, 2.0f) // arms (transition)
        assertNull(gate.onPointerEvent(2, 1f)) // identity delta
        assertNull(gate.onPointerEvent(2, Float.NaN))
        assertNull(gate.onPointerEvent(2, 0f))
        assertNull(gate.onPointerEvent(2, -1.5f))
        assertNull(gate.onPointerEvent(2, Float.POSITIVE_INFINITY))
    }

    @Test
    fun `reset forces the gate back to idle`() {
        val gate = PinchGestureGate()
        gate.onPointerEvent(2, 2.0f)
        gate.onPointerEvent(2, 1.1f)
        assertTrue(gate.isGestureActive)
        gate.reset()
        assertFalse(gate.isGestureActive)
        // After reset the gate has no pointer history, so the next event re-arms.
        assertNull(gate.onPointerEvent(2, 3.3f))
    }
}
