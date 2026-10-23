package com.example.feature.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import com.example.data.SettingEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure text-surgery helpers powering the inline MD/TXT edit mode.
 * These run on the JVM (no Android framework required).
 */
class InlineTextEditTest {

    // ---- applyMarkdownWrap (bold "**" / italic "*") ----------------------------------

    @Test
    fun `bold wraps the selected range`() {
        val result = applyMarkdownWrap("hello world", TextRange(0, 5), "**")
        assertEquals("**hello** world", result.text)
        assertEquals(TextRange(2, 7), result.newSelection)
    }

    @Test
    fun `bold unwraps when selection sits inside an existing pair`() {
        val result = applyMarkdownWrap("**hello** world", TextRange(2, 7), "**")
        assertEquals("hello world", result.text)
        assertEquals(TextRange(0, 5), result.newSelection)
    }

    @Test
    fun `italic inserts a pair at a collapsed cursor`() {
        val result = applyMarkdownWrap("ab", TextRange(1), "*")
        assertEquals("a**b", result.text)
        assertEquals(TextRange(2), result.newSelection)
    }

    @Test
    fun `bold works mid-document`() {
        val result = applyMarkdownWrap("say hello to world", TextRange(4, 9), "**")
        assertEquals("say **hello** to world", result.text)
    }

    @Test
    fun `empty text produces an empty emphasis pair`() {
        val result = applyMarkdownWrap("", TextRange(0), "**")
        assertEquals("****", result.text)
        assertEquals(TextRange(2), result.newSelection)
    }

    @Test
    fun `selection is clamped to text bounds`() {
        val result = applyMarkdownWrap("hi", TextRange(0, 99), "**")
        assertEquals("**hi**", result.text)
    }

    // ---- applyTabIndent ----------------------------------------------------------------

    @Test
    fun `single-line indent inserts at the cursor and keeps text`() {
        val result = applyTabIndent("abc\ndef", TextRange(1), 4)!!
        assertEquals("a    bc\ndef", result.text)
        assertEquals(TextRange(5), result.newSelection)
    }

    @Test
    fun `multi-line selection indents every touched line`() {
        val result = applyTabIndent("abc\ndef\nghi", TextRange(1, 8), 4)!!
        assertEquals("    abc\n    def\n    ghi", result.text)
        assertEquals(TextRange(5, 20), result.newSelection)
    }

    @Test
    fun `empty lines are skipped when block-indenting`() {
        val result = applyTabIndent("abc\n\nghi", TextRange(0, 8), 2)!!
        assertEquals("  abc\n\n  ghi", result.text)
    }

    @Test
    fun `selection text is never dropped by the indent`() {
        val result = applyTabIndent("ab", TextRange(0, 2), 4)!!
        assertEquals("    ab", result.text)
        assertEquals(TextRange(4), result.newSelection)
    }

    @Test
    fun `tab size is clamped between 1 and 8`() {
        val result = applyTabIndent("ab", TextRange(0), 99)!!
        assertEquals("        ab", result.text)
    }

    @Test
    fun `selection ending before the newline only indents the first line`() {
        val result = applyTabIndent("one\ntwo", TextRange(0, 3), 4)!!
        assertEquals("    one\ntwo", result.text)
    }

    @Test
    fun `selection covering the newline indents both lines`() {
        val result = applyTabIndent("one\ntwo", TextRange(0, 4), 4)!!
        assertEquals("    one\n    two", result.text)
    }

    // ---- TextUndoStack -----------------------------------------------------------------

    @Test
    fun `undo stack snapshot preserves order and capacity cap`() {
        val stack = TextUndoStack(3)
        stack.push(TextFieldValue("a"))
        stack.push(TextFieldValue("b"))
        stack.push(TextFieldValue("c"))
        stack.push(TextFieldValue("d")) // evicts "a"

        assertEquals(listOf("b", "c", "d"), stack.snapshot().map { it.text })
        assertEquals(3, stack.size)
        assertEquals("d", stack.pop()?.text)
    }

    @Test
    fun `undo stack snapshot of empty stack is empty`() {
        val stack = TextUndoStack(50)
        assertTrue(stack.snapshot().isEmpty())
        assertNull(stack.pop())
    }

    // ---- shared typography mapping ------------------------------------------------------

    @Test
    fun `editor font size maps settings`() {
        assertEquals(13.sp, inlineEditorFontSize(SettingEntity(fontSize = "Small")))
        assertEquals(15.sp, inlineEditorFontSize(SettingEntity(fontSize = "Medium")))
        assertEquals(18.sp, inlineEditorFontSize(SettingEntity(fontSize = "Large")))
    }
}
