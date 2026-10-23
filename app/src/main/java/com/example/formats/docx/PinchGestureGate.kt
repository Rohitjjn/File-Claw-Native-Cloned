package com.example.formats.docx

/**
 * Pure, testable gate for two-finger pinch-to-zoom gestures on the DOCX preview.
 *
 * Why this exists (Loop 10): the previous interceptor fed every pointer event straight
 * into `PointerEvent.calculateZoom()`. The *transition* event — the one where a finger
 * has just landed or lifted while the remaining pointers still carry a different pointer
 * configuration's history — produces a wildly wrong span ratio (one pointer's previous
 * position is stale, the newly-landed one's previous position is its own down position).
 * The resulting spurious delta multiplied the zoom by an arbitrary factor and slammed it
 * into the 25%/500% clamp on first contact, so DOCX pinch zoom felt broken: the
 * percentage chip changed while the document jumped to an extreme size.
 *
 * The gate applies three deliberate rules:
 *  1. Any event whose pressed-pointer count differs from the previous event is a
 *     *transition* event. Its delta is discarded — pointer history is mixed.
 *  2. Deltas are only returned for stable events with exactly the same (>= 2) pressed
 *     count as the previous event, i.e. a confirmed multi-touch gesture.
 *  3. Any event with fewer than 2 pressed pointers disarms the gate (finger lifted,
 *     gesture cancelled), so the next gesture starts clean.
 *
 * The caller owns the zoom state and multiplies returned deltas through
 * [DocxZoomPolicy.applyPinch]; this class owns only the decision "is this delta real?".
 */
class PinchGestureGate {

    /** Pointer count observed on the previous event (0 before any event). */
    private var lastObservedPointerCount: Int = 0

    /** True while a confirmed multi-touch gesture is in progress. */
    var isGestureActive: Boolean = false
        private set

    /**
     * Feed one pointer event.
     *
     * @param pressedPointerCount number of currently pressed pointers in the event.
     * @param rawZoomDelta value computed by `PointerEvent.calculateZoom()` for this event
     *   (1f when fewer than two pointers are pressed).
     * @return the zoom delta to apply, or null when the event must be ignored.
     */
    fun onPointerEvent(pressedPointerCount: Int, rawZoomDelta: Float): Float? {
        val isTransition = pressedPointerCount != lastObservedPointerCount
        lastObservedPointerCount = pressedPointerCount

        if (pressedPointerCount < 2) {
            // Single-finger scroll/long-press or the tail of a pinch: reset, never zoom.
            isGestureActive = false
            return null
        }
        if (isTransition) {
            // A finger just landed or lifted (including 2->3 finger changes): the span
            // history is inconsistent, so skip this delta and re-confirm the gesture.
            isGestureActive = true
            return null
        }
        return if (rawZoomDelta.isFinite() && rawZoomDelta > 0f && rawZoomDelta != 1f) {
            rawZoomDelta
        } else {
            null
        }
    }

    /** Forces the gate back to idle (gesture cancelled, pointer stream ended, ...). */
    fun reset() {
        lastObservedPointerCount = 0
        isGestureActive = false
    }
}
