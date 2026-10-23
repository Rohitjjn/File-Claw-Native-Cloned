package com.example.formats.pdf

/**
 * Pure math for the Loop 14 PDF view rotation and the right-edge page scrubber.
 *
 * The reader renders every page through pdf.js with an extra user rotation in
 * 90-degree steps. Rotation is VIEWER STATE: it never reloads the document,
 * never recreates the WebView and never touches the authenticated PDF.js
 * session, so a password-protected file is asked for its password exactly once
 * per open session regardless of how often the user rotates.
 *
 * The bundled reader (assets/pdfjs/search.html) mirrors [rotatedSize] and
 * [rotateRect] in JavaScript; keep the two implementations mathematically
 * identical. This object exists so the mapping rules stay unit-tested in the
 * JVM suite (the WebView bundle cannot be unit-tested there).
 */
object PdfRotationPolicy {

    /** Valid rotation steps, in degrees clockwise. */
    val ALLOWED_DEGREES = intArrayOf(0, 90, 180, 270)

    /** Normalizes any integer degree into [0, 360). */
    fun normalize(degrees: Int): Int = ((degrees % 360) + 360) % 360

    /** The next clockwise rotation step after [degrees]. */
    fun stepAfter(degrees: Int): Int = (normalize(degrees) + 90) % 360

    /** True when the rotation swaps the page width and height (90 / 270). */
    fun isQuarterTurn(degrees: Int): Boolean = normalize(degrees) % 180 == 90

    /**
     * Display size of a page whose unrotated size is [width] x [height] at
     * [degrees] rotation. A quarter turn swaps the axes.
     */
    fun rotatedSize(width: Float, height: Float, degrees: Int): Pair<Float, Float> =
        if (isQuarterTurn(degrees)) height to width else width to height

    /** A rectangle in normalized (0..1) page space. */
    data class NormRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        companion object {
            val FULL = NormRect(0f, 0f, 1f, 1f)
        }
    }

    /**
     * Maps a highlight rectangle from unrotated page space into display space
     * after a clockwise rotation of [degrees]. Used by the reader so search
     * highlights stay glued to their text at every rotation step:
     *  -   0: (l, t, r, b)
     *  -  90: (1-b, l, 1-t, r)   [clockwise quarter turn]
     *  - 180: (1-r, 1-b, 1-l, 1-t)
     *  - 270: (t, 1-r, b, 1-l)
     */
    fun rotateRect(rect: NormRect, degrees: Int): NormRect = when (normalize(degrees)) {
        90 -> NormRect(
            left = 1f - rect.bottom,
            top = rect.left,
            right = 1f - rect.top,
            bottom = rect.right
        )
        180 -> NormRect(
            left = 1f - rect.right,
            top = 1f - rect.bottom,
            right = 1f - rect.left,
            bottom = 1f - rect.top
        )
        270 -> NormRect(
            left = rect.top,
            top = 1f - rect.right,
            right = rect.bottom,
            bottom = 1f - rect.left
        )
        else -> rect
    }

    /**
     * Page the scrubber thumb maps to for a drag [fraction] (0..1) across
     * [pageCount] pages: 1-based, clamped, never 0 and never pageCount+1 even
     * for sloppy input, so a drag can always be sent straight to gotoPdfPage.
     */
    fun pageFromFraction(fraction: Float, pageCount: Int): Int {
        if (pageCount <= 0) return 1
        val safe = if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f)
        val page = 1 + Math.round(safe * (pageCount - 1).toFloat()).toInt()
        return page.coerceIn(1, pageCount)
    }

    /** Inverse of [pageFromFraction]: thumb center fraction for a 1-based page. */
    fun fractionForPage(page: Int, pageCount: Int): Float {
        if (pageCount <= 1) return 0f
        val safe = page.coerceIn(1, pageCount)
        return (safe - 1).toFloat() / (pageCount - 1).toFloat()
    }

    /**
     * Scroll-anchor model used while rotating: the reader remembers the visible
     * page plus the fraction scrolled into that page, then re-applies both once
     * the rotated geometry is laid out. Fractions are clamped so a re-anchor can
     * never overshoot the (possibly shorter) rotated page.
     */
    fun anchorFraction(scrollTopWithinPage: Float, pageHeight: Float): Float {
        if (pageHeight <= 0f) return 0f
        val raw = scrollTopWithinPage / pageHeight
        return if (raw.isNaN()) 0f else raw.coerceIn(0f, 1f)
    }

    fun scrollTopForAnchor(anchor: Float, pageHeight: Float): Float {
        if (pageHeight <= 0f || anchor.isNaN()) return 0f
        return anchor.coerceIn(0f, 1f) * pageHeight
    }
}
