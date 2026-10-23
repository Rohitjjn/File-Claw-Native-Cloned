package com.example.formats.common.search

/**
 * Pure command-issuing policy for the WebView search pipelines (DOCX / TXT / MD).
 *
 * Loop 12 root cause for the reported "7 results but the arrows only reach 2-3":
 * both WebView search effects combined the query step and the navigation step into
 * ONE script. Every arrow press re-ran `searchWord()`, which rebuilds the highlight
 * list and resets the renderer's active index to 0, and the appended
 * `nextSearchMatch()` then only advanced one step from that freshly-reset index.
 * Deterministic outcome: the counter could never move past 2 on "next" no matter
 * how many matches existed (and "previous" always wrapped to the last match and
 * back). The list itself was never truncated — the commands were just wrong.
 *
 * The policy owns the *ordering* rules so the Compose effects stay thin:
 *  1. A query that has not been issued to the renderer yet is always searched first.
 *  2. Navigation triggers become next/prev DELTAS, never another `searchWord()`.
 *  3. A trigger counter that moved backwards (screen reset on file change) is
 *     absorbed silently instead of being re-issued as steps.
 */
object InViewerSearchCommandPolicy {

    /** Ledger of what the renderer has already been told to do. */
    data class Issued(
        val query: String? = null,
        val next: Int = 0,
        val prev: Int = 0
    )

    data class Plan(
        val search: Boolean,
        val nextSteps: Int,
        val prevSteps: Int,
        val issued: Issued
    ) {
        val isEmpty: Boolean get() = !search && nextSteps == 0 && prevSteps == 0
    }

    /**
     * Plans the next batch of renderer commands.
     *
     * @param issued last state actually delivered to the renderer.
     * @param query current find-bar query (raw, not normalized).
     * @param nextTarget monotonic "next" trigger counter owned by the screen.
     * @param prevTarget monotonic "previous" trigger counter owned by the screen.
     */
    fun plan(issued: Issued, query: String, nextTarget: Int, prevTarget: Int): Plan {
        val search = issued.query != query
        // Forward deltas only; a backwards move means the counters were reset
        // (file switched / find mode closed) and must be absorbed, not replayed.
        val nextSteps = (nextTarget - issued.next).coerceAtLeast(0)
        val prevSteps = (prevTarget - issued.prev).coerceAtLeast(0)
        return Plan(
            search = search,
            nextSteps = nextSteps,
            prevSteps = prevSteps,
            issued = Issued(query = query, next = nextTarget, prev = prevTarget)
        )
    }
}

/**
 * Pure match-navigation math shared by the viewer header find bar (Loop 10). Kept
 * side-effect free so the header arrows and the IME Search action provably walk the
 * same indices, and so the wrap-around behaviour is unit-testable without a device.
 */
object InViewerSearchNavigation {

    /**
     * Wraps forward through [count] matches. Returns null when there is nothing to
     * navigate (no matches), otherwise the next index clamped into [0, count).
     */
    fun nextIndex(current: Int, count: Int): Int? {
        if (count <= 0) return null
        val safeCurrent = if (current in 0 until count) current else 0
        return (safeCurrent + 1) % count
    }

    /**
     * Wraps backward through [count] matches. Returns null when there is nothing to
     * navigate (no matches), otherwise the previous index clamped into [0, count).
     */
    fun prevIndex(current: Int, count: Int): Int? {
        if (count <= 0) return null
        val safeCurrent = if (current in 0 until count) current else 0
        return (safeCurrent - 1 + count) % count
    }
}
