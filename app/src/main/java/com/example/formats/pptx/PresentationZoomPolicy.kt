package com.example.formats.pptx

/** Zoom range shared by the presentation WebView and its Compose controls. */
object PresentationZoomPolicy {
    const val MIN_ZOOM = 0.5f
    const val MAX_ZOOM = 3.0f
    const val DEFAULT_ZOOM = 1.0f
    const val ZOOMED_READING = 2.0f

    fun coerce(zoom: Float): Float =
        if (!zoom.isFinite()) DEFAULT_ZOOM else zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)

    fun toggleDoubleTap(zoom: Float): Float =
        if (coerce(zoom) > 1.4f) DEFAULT_ZOOM else ZOOMED_READING

    fun toPercent(zoom: Float): Int = Math.round(coerce(zoom) * 100f)

    fun fromPercent(percent: Int): Float = coerce(percent / 100f)
}
