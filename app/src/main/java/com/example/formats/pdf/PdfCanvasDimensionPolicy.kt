package com.example.formats.pdf

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Loop 12 canvas rasterization limits for the bundled PDF.js reader.
 *
 * Why a second policy next to [PdfRenderBudgetPolicy]: the pixel budget bounds the
 * canvas AREA, but a page can still defeat it in two ways:
 *
 *  1. **Oversized pages.** A poster/blueprint page whose 1x area already exceeds the
 *     budget kept the old `max(1, ...)` ratio floor, so the reader allocated the full
 *     1x canvas anyway. Canvas backing stores above the browser's internal limits do
 *     not fail loudly — they silently ignore every draw command and the page shows up
 *     permanently BLANK (see pdf.js issue #11195 and the canvas-size test matrix:
 *     Chrome/Android area limits range ~11k-16k px per side depending on device).
 *  2. **Extreme aspect ratios.** A very wide page at a ratio capped only by area can
 *     produce a single dimension far beyond what the GPU can texture.
 *
 * The policy therefore clamps the render ratio three ways: by the caller's desired
 * ratio, by the pixel budget (area), and by a conservative per-dimension cap — with
 * NO lower floor of 1.0, so oversized pages are downsampled into a legal canvas
 * instead of rendering blank. [MIN_RENDER_RATIO] keeps degenerate pages visible.
 *
 * The bundled reader (`search.html`) mirrors these constants exactly; keep the two
 * implementations in sync when tuning.
 */
object PdfCanvasDimensionPolicy {

    /**
     * Conservative per-dimension ceiling in backing pixels. Chromium on Android
     * supports far larger dimensions on many devices (up to 16384+), but several
     * GPU/driver combinations fail earlier; 8192 keeps every page renderable while
     * staying inside the pixel budget for all realistic page sizes.
     */
    const val MAX_CANVAS_DIMENSION_PX: Double = 8192.0

    /** Ratio floor so even a pathological page renders *something* recognizable. */
    const val MIN_RENDER_RATIO: Double = 0.05

    /**
     * Highest backing-store ratio the page may use right now.
     *
     * @param viewportWidthCss  page width in CSS pixels (from the PDF.js viewport).
     * @param viewportHeightCss page height in CSS pixels.
     * @param desiredRatio      what the caller wants (devicePixelRatio x render scale).
     * @param pixelBudget       per-page backing-pixel budget (width x height cap).
     * @param maxRatio          absolute ratio cap (the reader's maxRenderRatio).
     */
    fun renderRatio(
        viewportWidthCss: Double,
        viewportHeightCss: Double,
        desiredRatio: Double,
        pixelBudget: Double,
        maxRatio: Double
    ): Double {
        val safeDesired = if (desiredRatio.isFinite() && desiredRatio > 0.0) desiredRatio else 1.0
        val safeBudget = if (pixelBudget.isFinite() && pixelBudget > 0.0) pixelBudget else 4_000_000.0
        val safeMax = if (maxRatio.isFinite() && maxRatio > 0.0) maxRatio else 8.0
        val width = if (viewportWidthCss.isFinite() && viewportWidthCss > 0.0) viewportWidthCss else 1.0
        val height = if (viewportHeightCss.isFinite() && viewportHeightCss > 0.0) viewportHeightCss else 1.0

        val areaCap = sqrt(safeBudget / (width * height))
        val dimensionCap = MAX_CANVAS_DIMENSION_PX / max(width, height)
        return max(MIN_RENDER_RATIO, min(safeDesired, min(safeMax, min(areaCap, dimensionCap))))
    }

    /**
     * Backing-store dimensions for a page, after the ratio clamp. Rows/columns are
     * floored to at least 1 so an empty canvas is never requested.
     */
    fun backingDimensions(viewportWidthCss: Double, viewportHeightCss: Double, ratio: Double): Pair<Int, Int> {
        val width = if (viewportWidthCss.isFinite() && viewportWidthCss > 0.0) viewportWidthCss else 1.0
        val height = if (viewportHeightCss.isFinite() && viewportHeightCss > 0.0) viewportHeightCss else 1.0
        val safeRatio = if (ratio.isFinite() && ratio > 0.0) ratio else 1.0
        return Pair(max(1.0, (width * safeRatio)).toInt(), max(1.0, (height * safeRatio)).toInt())
    }
}
