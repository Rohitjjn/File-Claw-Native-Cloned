package com.example.feature.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingEntity

/**
 * Pure, Compose-free text surgery helpers powering the inline MD/TXT edit mode.
 * Kept side-effect free so they are trivially unit-testable (see InlineTextEditTest).
 */

/** Result of a wrapping operation: the new text plus where the selection should land. */
data class WrapResult(val text: String, val newSelection: TextRange)

/**
 * Markdown-aware wrap/unwrap of the current selection with [wrapper] ("**" for bold, "*" for italic).
 * - Selecting text and tapping toggles the wrapper around exactly the selection.
 * - With a collapsed cursor and no surrounding wrapper, a pair is inserted at the cursor.
 * - With a collapsed cursor placed exactly between an existing pair, the wrapper is removed.
 */
fun applyMarkdownWrap(
    text: String,
    selection: TextRange,
    wrapper: String
): WrapResult {
    val safeText = text
    val safeStart = selection.min.coerceIn(0, safeText.length)
    val safeEnd = selection.max.coerceIn(0, safeText.length)

    val alreadyWrapped = safeStart >= wrapper.length &&
        safeEnd + wrapper.length <= safeText.length &&
        safeText.regionMatches(safeStart - wrapper.length, wrapper, 0, wrapper.length) &&
        safeText.regionMatches(safeEnd, wrapper, 0, wrapper.length)

    return if (alreadyWrapped) {
        val newText = safeText.removeRange(safeEnd, safeEnd + wrapper.length)
            .removeRange(safeStart - wrapper.length, safeStart)
        WrapResult(
            text = newText,
            newSelection = TextRange(safeStart - wrapper.length, safeEnd - wrapper.length)
        )
    } else {
        val sb = StringBuilder(safeText.length + wrapper.length * 2)
        sb.append(safeText, 0, safeStart)
            .append(wrapper)
            .append(safeText, safeStart, safeEnd)
            .append(wrapper)
            .append(safeText, safeEnd, safeText.length)
        WrapResult(
            text = sb.toString(),
            newSelection = TextRange(safeStart + wrapper.length, safeEnd + wrapper.length)
        )
    }
}

/**
 * Inserts a tab indent (spaces matching [tabSize]) at the cursor, or indents every selected
 * line by one step when a multi-line selection exists. Returns null when there is nothing to do.
 */
fun applyTabIndent(text: String, selection: TextRange, tabSize: Int): WrapResult? {
    val indent = " ".repeat(tabSize.coerceIn(1, 8))
    val safeStart = selection.min.coerceIn(0, text.length)
    val safeEnd = selection.max.coerceIn(0, text.length)

    val selectionSpansMultipleLines = text.indexOf('\n', safeStart).let { it in safeStart..safeEnd - 1 }

    return if (!selectionSpansMultipleLines) {
        // Insert the indent at the anchor; never drop the selected text (typing-tab behavior).
        val newText = text.substring(0, safeStart) + indent + text.substring(safeStart)
        WrapResult(newText, TextRange(safeStart + indent.length))
    } else {
        // Indent every line touched by the selection, exactly one step.
        val lineStart = if (safeStart == 0) 0 else text.lastIndexOf('\n', safeStart - 1) + 1
        val sb = StringBuilder(text)
        var offsetDelta = 0
        var cursor = lineStart
        while (cursor <= sb.length) {
            if (cursor > safeEnd + offsetDelta) break
            if (cursor < sb.length && sb[cursor] != '\n') {
                sb.insert(cursor, indent)
                offsetDelta += indent.length
            }
            val next = sb.indexOf('\n', cursor)
            if (next < 0) break
            cursor = next + 1
        }
        WrapResult(
            text = sb.toString(),
            newSelection = TextRange(safeStart + indent.length, safeEnd + offsetDelta)
        )
    }
}

/** Small monospace-typography mapping shared by the inline editor workspace. */
fun inlineEditorFontSize(settings: SettingEntity) = when (settings.fontSize) {
    "Small" -> 13.sp
    "Large" -> 18.sp
    else -> 15.sp
}

/**
 * The inline editing workspace: a borderless monospace BasicTextField that sits on the exact
 * same surface as the preview canvas, so the screen visually stays the same when switching
 * from preview to edit. No line-number gutter is rendered, per spec.
 */
@Composable
fun InlineEditWorkspace(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    settings: SettingEntity,
    modifier: Modifier = Modifier
) {
    val workspaceScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val fontSize = inlineEditorFontSize(settings)
    val lineHeight = 20.sp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize,
                lineHeight = lineHeight,
                color = MaterialTheme.colorScheme.onBackground
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(workspaceScroll)
                .then(
                    if (!settings.wordWrap) Modifier.horizontalScroll(horizontalScroll) else Modifier
                )
                .padding(16.dp)
        )
    }
}

/**
 * Undo-history viewer for the inline editor: lists the snapshots captured in the undo stack,
 * newest last, and lets the user jump back to any point without leaving the screen.
 */
@Composable
fun EditHistoryDialog(
    entries: List<TextFieldValue>,
    onDismissRequest: () -> Unit,
    onRestore: (TextFieldValue) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Edit History",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            if (entries.isEmpty()) {
                Text(
                    text = "No history yet. Changes you make while editing are captured here automatically.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            } else {
                Column {
                    Text(
                        text = "${entries.size} snapshot${if (entries.size == 1) "" else "s"} • oldest first. Tap one to restore it.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(entries) { index, entry ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                onClick = {
                                    onRestore(entry)
                                    onDismissRequest()
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(38.dp)
                                    )
                                    Text(
                                        text = entry.text
                                            .lineSequence()
                                            .firstOrNull { it.isNotBlank() }
                                            ?.take(48)
                                            ?.ifBlank { "(empty)" }
                                            ?: "(empty)",
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
