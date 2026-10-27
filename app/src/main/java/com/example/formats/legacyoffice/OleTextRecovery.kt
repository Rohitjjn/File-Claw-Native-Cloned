package com.example.formats.legacyoffice

/**
 * Shared, bounded plain-text recovery for legacy OLE2 Office streams.
 *
 * It intentionally extracts text only: no formatting, images, tables, macros or
 * executable content. Both Word and PowerPoint legacy handlers use these pure
 * functions so Unicode (including Devanagari/Arabic) and long files are handled
 * consistently without another parser dependency.
 */
object OleTextRecovery {
    const val DEFAULT_MAX_CHARS = 1_000_000

    private val PRINTABLE_UNICODE_TYPES = setOf(
        Character.UPPERCASE_LETTER.toInt(),
        Character.LOWERCASE_LETTER.toInt(),
        Character.TITLECASE_LETTER.toInt(),
        Character.MODIFIER_LETTER.toInt(),
        Character.OTHER_LETTER.toInt(),
        Character.DECIMAL_DIGIT_NUMBER.toInt(),
        Character.OTHER_NUMBER.toInt(),
        Character.SPACE_SEPARATOR.toInt(),
        Character.DASH_PUNCTUATION.toInt(),
        Character.START_PUNCTUATION.toInt(),
        Character.END_PUNCTUATION.toInt(),
        Character.CONNECTOR_PUNCTUATION.toInt(),
        Character.OTHER_PUNCTUATION.toInt(),
        Character.MATH_SYMBOL.toInt(),
        Character.CURRENCY_SYMBOL.toInt(),
        Character.MODIFIER_SYMBOL.toInt(),
        Character.OTHER_SYMBOL.toInt(),
        Character.SURROGATE.toInt(),
        // Combining marks keep Indic/Arabic words intact; format characters
        // preserve zero-width joiners and emoji variation selectors.
        Character.NON_SPACING_MARK.toInt(),
        Character.COMBINING_SPACING_MARK.toInt(),
        Character.ENCLOSING_MARK.toInt(),
        Character.FORMAT.toInt(),
        Character.LETTER_NUMBER.toInt()
    )

    private val USEFUL_MARK_TYPES = setOf(
        Character.NON_SPACING_MARK.toInt(),
        Character.COMBINING_SPACING_MARK.toInt(),
        Character.ENCLOSING_MARK.toInt(),
        Character.FORMAT.toInt()
    )

    data class RecoveredText(
        val paragraphs: List<String>,
        val text: String
    )

    fun fromUtf16Le(
        bytes: ByteArray,
        startOffset: Int = 0,
        endOffset: Int = bytes.size,
        maxChars: Int = DEFAULT_MAX_CHARS
    ): RecoveredText {
        val start = startOffset.coerceIn(0, bytes.size)
        val end = endOffset.coerceIn(start, bytes.size)
        val paragraphs = mutableListOf<String>()
        val current = StringBuilder()
        var emitted = 0
        var i = start
        while (i + 1 < end && emitted < maxChars) {
            val code = (bytes[i].toInt() and 0xFF) or ((bytes[i + 1].toInt() and 0xFF) shl 8)
            val char = code.toChar()
            when {
                char == '\r' || char == '\n' || char == '\u0007' || char == '\u000B' -> {
                    commitParagraph(paragraphs, current)
                }
                char == '\t' -> current.append("    ")
                isPrintableUnicode(char) -> {
                    current.append(char)
                    // Preserve a surrogate pair when both halves are present.
                    if (Character.isHighSurrogate(char) && i + 3 < end) {
                        val low = ((bytes[i + 2].toInt() and 0xFF) or
                            ((bytes[i + 3].toInt() and 0xFF) shl 8)).toChar()
                        if (Character.isLowSurrogate(low)) {
                            current.append(low)
                            i += 2
                        }
                    }
                }
                else -> commitParagraph(paragraphs, current)
            }
            emitted++
            i += 2
        }
        commitParagraph(paragraphs, current)
        return cleaned(paragraphs)
    }

    fun fromSingleByte(
        bytes: ByteArray,
        startOffset: Int = 0,
        endOffset: Int = bytes.size,
        maxChars: Int = DEFAULT_MAX_CHARS
    ): RecoveredText {
        val start = startOffset.coerceIn(0, bytes.size)
        val end = endOffset.coerceIn(start, bytes.size)
        val paragraphs = mutableListOf<String>()
        val current = StringBuilder()
        var i = start
        while (i < end && (i - start) < maxChars) {
            val code = bytes[i].toInt() and 0xFF
            val char = code.toChar()
            when {
                char == '\r' || char == '\n' || char == '\u0007' || char == '\u000B' ->
                    commitParagraph(paragraphs, current)
                char == '\t' -> current.append("    ")
                code in 0x20..0x7E || code >= 0xA0 -> current.append(char)
                else -> commitParagraph(paragraphs, current)
            }
            i++
        }
        commitParagraph(paragraphs, current)
        return cleaned(paragraphs)
    }

    private fun isUsefulTextChar(char: Char): Boolean =
        char.isLetterOrDigit() ||
            char.isWhitespace() ||
            char in ".,;:!?-()[]{}'\"“”/\\" ||
            Character.getType(char) in USEFUL_MARK_TYPES

    /**
     * Letters and digits are stronger evidence of correctly decoded text than
     * whitespace/punctuation. Weighting them prevents a mis-decoded 8-bit view
     * of UTF-16LE bytes (mostly spaces and NUL separators) from outscoring the
     * real Unicode recovery.
     */
    fun qualityScore(recovered: RecoveredText): Int = recovered.paragraphs.fold(0) { total, paragraph ->
        total + paragraph.fold(0) { score, char ->
            score + when {
                char.isLetterOrDigit() -> 2
                isUsefulTextChar(char) -> 1
                else -> 0
            }
        }
    }

    private fun isPrintableUnicode(char: Char): Boolean {
        if (Character.isISOControl(char)) return false
        if (char == '\uFFFE' || char == '\uFFFF' || char == '\u0000') return false
        val type = Character.getType(char)
        return Character.isLetterOrDigit(char) ||
            char.isWhitespace() ||
            type in PRINTABLE_UNICODE_TYPES
    }

    private fun commitParagraph(paragraphs: MutableList<String>, current: StringBuilder) {
        val value = current.toString().trim()
        if (value.length >= 3 && looksLikeText(value)) paragraphs.add(value)
        current.clear()
    }

    private fun looksLikeText(value: String): Boolean {
        val useful = value.count(::isUsefulTextChar)
        if (useful * 100 < value.length * 85) return false
        // Long runs with no word separator are usually mis-decoded binary data.
        return value.length <= 40 || value.any { it.isWhitespace() || it == ',' || it == '.' }
    }

    private fun cleaned(paragraphs: List<String>): RecoveredText {
        val clean = paragraphs.distinct().take(50_000)
        return RecoveredText(clean, clean.joinToString("\n\n"))
    }
}
