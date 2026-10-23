package com.example.formats.docx

/**
 * Loop 14: decision policy for the DOCX text-recovery state. A plain-text view of
 * a DOCX is ONLY ever a labeled recovery/error surface after the formatted
 * renderer (docx-preview in the WebView) genuinely failed — it must never
 * masquerade as a normal preview. Keeping the decision pure makes the routing
 * testable without a device or a WebView.
 */
object DocxTextRecovery {

    /** Max characters of a renderer failure reason shown to the user (no document content, ever). */
    const val MAX_REASON_CHARS = 180

    /**
     * True when the recovered text is worth showing under the recovery banner.
     * A DOCX whose extraction yields zero paragraphs has nothing to recover:
     * the state stays an honest "nothing could be recovered" error surface.
     */
    fun shouldShowRecoveredText(paragraphCount: Int): Boolean = paragraphCount > 0

    /** Banner headline — must read as a failure, not as a feature. */
    fun bannerHeadline(): String = "Formatted preview failed — showing recovered text"

    /** Failure reason clamped for one-line display; null-safe and content-free. */
    fun displayReason(rawReason: String?): String? {
        val trimmed = rawReason?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return trimmed.take(MAX_REASON_CHARS)
    }

    /** Recovery stats line; empty when there is nothing recovered to count. */
    fun statsLabel(wordCount: Int, paragraphCount: Int): String? =
        if (paragraphCount > 0) "$wordCount words • $paragraphCount paragraphs recovered" else null
}
