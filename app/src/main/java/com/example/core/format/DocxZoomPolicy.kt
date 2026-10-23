package com.example.core.format

/**
 * Zoom policy for the DOCX preview engine.
 *
 * Uses fit-to-width as the 1.0 (100%) baseline:
 * - Documents open at [DEFAULT_ZOOM] (1.0 = page fits device screen width).
 * - Pinch gestures scale smoothly relative to fingers and anchor at focal point.
 * - Double-tap toggles between fit-width (1.0 = 100%) and 2.0 (200% reading zoom).
 * All values are expressed as relative factors (0.50 - 5.00).
 */
object DocxZoomPolicy {

    const val DEFAULT_ZOOM = 1.00f
    const val MIN_ZOOM = 0.50f
    const val MAX_ZOOM = 5.00f
    const val ZOOMED_READING = 2.00f

    fun coerce(zoom: Float): Float =
        zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)

    /** Applies a pinch gesture delta to the current zoom, clamped and de-noised. */
    fun applyPinch(current: Float, delta: Float): Float {
        if (!delta.isFinite() || delta <= 0f) return coerce(current)
        val next = current * delta
        val rounded = Math.round(next * 1000f) / 1000f
        return coerce(rounded)
    }

    /** Double-tap toggle: fit-width (100%) <-> 200% reading zoom. */
    fun toggleDoubleTap(current: Float): Float =
        if (current > 1.4f) DEFAULT_ZOOM else ZOOMED_READING

    fun fromPercent(percent: Int): Float = coerce(percent / 100f)

    fun toPercent(zoom: Float): Int = Math.round(coerce(zoom) * 100f)

    /** Default display zoom percent: 100% (Fit Width). */
    fun defaultPercent(): Int = 100
}
