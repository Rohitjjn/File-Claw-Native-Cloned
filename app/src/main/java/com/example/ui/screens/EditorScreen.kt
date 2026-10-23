package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.component.ClaudeAppBar
import com.example.viewmodel.MainViewModel
import androidx.compose.foundation.lazy.rememberLazyListState

class TextUndoStack(private val capacity: Int = 50) {
    private val buffer = ArrayDeque<TextFieldValue>()
    
    fun push(state: TextFieldValue) {
        if (buffer.size >= capacity) buffer.removeFirst()
        buffer.addLast(state)
    }
    
    fun pop(): TextFieldValue? =
        if (buffer.isEmpty()) null else buffer.removeLast()
    
    fun clear() = buffer.clear()
    
    val isEmpty get() = buffer.isEmpty()
    val isNotEmpty get() = buffer.isNotEmpty()
    val size get() = buffer.size
    
    fun lastOrNull(): TextFieldValue? = buffer.lastOrNull()

    /** Read-only newest-last copy of the stack for the history (undo-log) viewer. */
    fun snapshot(): List<TextFieldValue> = buffer.toList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fileState by viewModel.currentFileState.collectAsState()
    val settings by viewModel.settingsState.collectAsState()

    val textSuccess = fileState as? MainViewModel.FileContentState.TextSuccess
    val fileContent = textSuccess?.content ?: ""

    // Use TextFieldValue to accurately support selection tracker, cursor tracking & Undo/Redo stack engines
    var textValue by remember(fileContent) { 
        mutableStateOf(TextFieldValue(text = fileContent)) 
    }
    
    var originalContent by remember(fileContent) { mutableStateOf(fileContent) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }

    val activeFileEntity = textSuccess?.file
    val hasUnsavedChanges = textValue.text != originalContent

    // Keep active undo/redo history caches
    val undoStack = remember { TextUndoStack(50) }
    val redoStack = remember { TextUndoStack(50) }

    val fontSize = when (settings.fontSize) {
        "Small" -> 13.sp
        "Large" -> 18.sp
        else -> 15.sp
    }
    
    val lineHeight = 20.sp

    val linesCount by remember { derivedStateOf { textValue.text.count { it == '\n' } + 1 } }
    val charsCount by remember { derivedStateOf { textValue.text.length } }

    val handleExit = {
        if (hasUnsavedChanges) {
            if (settings.autoSaveDrafts) {
                activeFileEntity?.let {
                    viewModel.saveTextFile(it, textValue.text)
                }
                viewModel.showNotification("Changes saved to file.")
                onBackClick()
            } else {
                showUnsavedDialog = true
            }
        } else {
            onBackClick()
        }
    }

    BackHandler {
        handleExit()
    }

    Scaffold(
        topBar = {
            ClaudeAppBar(
                title = activeFileEntity?.let { "Edit: ${it.name}" } ?: "Editor",
                onNavIconClick = handleExit,
                navIcon = Icons.Default.Close,
                actions = {
                    // Save As / Export Copy Button
                    IconButton(
                        onClick = { showSaveAsDialog = true },
                        modifier = Modifier.testTag("save_as_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DriveFileMove,
                            contentDescription = "Save As / Export Copy",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Save Button
                    IconButton(
                        onClick = {
                            activeFileEntity?.let {
                                viewModel.saveTextFile(it, textValue.text)
                                originalContent = textValue.text // Mark changes as saved
                            }
                        },
                        modifier = Modifier.testTag("save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save file",
                            tint = if (hasUnsavedChanges) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Stats Footer Row
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Lines: $linesCount • Chars: $charsCount",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Encoding: ${settings.defaultEncoding}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.testTag("editor_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scrollable formatting helper toolbar above the main typing workspace
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    state = rememberLazyListState(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo action
                    item {
                        InputChip(
                            selected = false,
                            onClick = {
                                if (undoStack.isNotEmpty) {
                                    val prevState = undoStack.pop()
                                    if (prevState != null) {
                                        redoStack.push(textValue)
                                        textValue = prevState
                                    }
                                }
                            },
                            label = { Text("Undo", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            enabled = undoStack.isNotEmpty,
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Undo, 
                                    contentDescription = "Undo", 
                                    modifier = Modifier.size(13.dp)
                                ) 
                            }
                        )
                    }

                    // Redo action
                    item {
                        InputChip(
                            selected = false,
                            onClick = {
                                if (redoStack.isNotEmpty) {
                                    val nextState = redoStack.pop()
                                    if (nextState != null) {
                                        undoStack.push(textValue)
                                        textValue = nextState
                                    }
                                }
                            },
                            label = { Text("Redo", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            enabled = redoStack.isNotEmpty,
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Redo, 
                                    contentDescription = "Redo", 
                                    modifier = Modifier.size(13.dp)
                                ) 
                            }
                        )
                    }

                    // Tab Indent Insertion (adds 4 spaces at current cursor alignment)
                    item {
                        InputChip(
                            selected = false,
                            onClick = {
                                val currentText = textValue.text
                                val selection = textValue.selection
                                val insertString = "    "
                                val newText = currentText.substring(0, selection.start) + insertString + currentText.substring(selection.end)
                                val newSelection = TextRange(selection.start + insertString.length)
                                
                                undoStack.push(textValue)
                                redoStack.clear()
                                textValue = TextFieldValue(text = newText, selection = newSelection)
                            },
                            label = { Text("TAB", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    // Smart Auto-Indent block formatter
                    item {
                        InputChip(
                            selected = false,
                            onClick = {
                                undoStack.push(textValue)
                                redoStack.clear()
                                textValue = textValue.copy(text = smartFormatIndent(textValue.text))
                            },
                            label = { Text("Smart Auto-Indent", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.FormatAlignLeft, 
                                    contentDescription = "Smart Auto-Indent", 
                                    modifier = Modifier.size(13.dp)
                                ) 
                            }
                        )
                    }

                    // tabs to spaces helper conversion tool
                    item {
                        InputChip(
                            selected = false,
                            onClick = {
                                undoStack.push(textValue)
                                redoStack.clear()
                                textValue = textValue.copy(text = textValue.text.replace("\t", "    "))
                            },
                            label = { Text("Tabs → Spaces", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    // clean trailing redundant spaces
                    item {
                        InputChip(
                            selected = false,
                            onClick = {
                                undoStack.push(textValue)
                                redoStack.clear()
                                val lines = textValue.text.split("\n")
                                textValue = textValue.copy(text = lines.joinToString("\n") { it.trimEnd() })
                            },
                            label = { Text("Trim Spaces", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.CleaningServices, 
                                    contentDescription = "Trim trailing spaces", 
                                    modifier = Modifier.size(13.dp)
                                ) 
                            }
                        )
                    }

                    // MD headers helper template generator
                    item {
                        InputChip(
                            selected = false,
                            onClick = {
                                val currentText = textValue.text
                                val selection = textValue.selection
                                val lineStart = currentText.lastIndexOf('\n', selection.start - 1) + 1
                                
                                undoStack.push(textValue)
                                redoStack.clear()
                                val newText = currentText.substring(0, lineStart) + "# " + currentText.substring(lineStart)
                                textValue = textValue.copy(text = newText, selection = TextRange(selection.start + 2))
                            },
                            label = { Text("MD Heading (#)", fontSize = 11.sp) }
                        )
                    }

                    // MD bullets helper
                    item {
                        InputChip(
                            selected = false,
                            onClick = {
                                val currentText = textValue.text
                                val selection = textValue.selection
                                val lineStart = currentText.lastIndexOf('\n', selection.start - 1) + 1
                                
                                undoStack.push(textValue)
                                redoStack.clear()
                                val newText = currentText.substring(0, lineStart) + "* " + currentText.substring(lineStart)
                                textValue = textValue.copy(text = newText, selection = TextRange(selection.start + 2))
                            },
                            label = { Text("MD Bullet (*)", fontSize = 11.sp) }
                        )
                    }
                }
            }

            val sharedVerticalScroll = rememberScrollState()
            val horizontalScroll = rememberScrollState()

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // 1. Line Numbers Sidebar
                if (settings.showLineNumbers) {
                    // Formatted sequence text
                    val lineNumbersText = remember(linesCount) {
                        (1..linesCount).joinToString("\n")
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp)
                            .verticalScroll(
                                state = sharedVerticalScroll,
                            ),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = lineNumbersText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                        )
                    }
                    
                    // Divider lane
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outline)
                    )
                }

                // 2. Main Typing Console TextField
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = { newValue ->
                            // If textual characters changed, push state frame into the Undo history cache
                            val textChanged = newValue.text !== textValue.text && newValue.text != textValue.text
                            if (textChanged) {
                                undoStack.push(textValue)
                                redoStack.clear()
                            }
                            textValue = newValue
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(
                                state = sharedVerticalScroll,
                            )
                            .then(
                                if (!settings.wordWrap) {
                                    Modifier.horizontalScroll(horizontalScroll)
                                } else {
                                    Modifier
                                }
                            )
                            .padding(16.dp)
                            .testTag("editor_text_input")
                    )
                }
            }
        }
    }

    // Unsaved Warning Prompt
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Unsaved Changes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text("You modified this file. Exit without saving your changes?") },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsavedDialog = false
                        onBackClick()
                    }
                ) {
                    Text("Exit", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showSaveAsDialog && activeFileEntity != null) {
        val defaultName = remember(activeFileEntity.name) {
            val dotIdx = activeFileEntity.name.lastIndexOf('.')
            if (dotIdx > 0) {
                activeFileEntity.name.substring(0, dotIdx) + "_copy." + activeFileEntity.extension
            } else {
                activeFileEntity.name + "_copy"
            }
        }
        var copyFileNameInput by remember { mutableStateOf(defaultName) }

        AlertDialog(
            onDismissRequest = { showSaveAsDialog = false },
            title = {
                Text(
                    text = "Export Modified Copy",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Atomically save changes into a new copy without overwriting the original file:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = copyFileNameInput,
                        onValueChange = { copyFileNameInput = it },
                        singleLine = true,
                        label = { Text("File Name") },
                        modifier = Modifier.fillMaxWidth().testTag("save_as_filename_input")
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveAsDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (copyFileNameInput.isNotBlank()) {
                            viewModel.exportCopy(activeFileEntity, copyFileNameInput.trim(), textValue.text)
                            showSaveAsDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_as_confirm_button")
                ) {
                    Text("Export Copy")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// Smart indentation logic helper for code brackets
private fun smartFormatIndent(text: String): String {
    val lines = text.split("\n")
    val formatted = StringBuilder()
    var indentLevel = 0
    for (line in lines) {
        val trimmed = line.trim()
        
        // Decrease indent early if closing bracket triggers
        if (trimmed.startsWith("}") || trimmed.startsWith("]") || trimmed.startsWith(")")) {
            indentLevel = maxOf(0, indentLevel - 1)
        }
        
        val indent = "    ".repeat(indentLevel)
        formatted.append(indent).append(trimmed).append("\n")
        
        // Increase indent for subsequent lines if opening bracket is found
        if (trimmed.endsWith("{") || trimmed.endsWith("[") || trimmed.endsWith("(")) {
            indentLevel++
        }
    }
    return formatted.toString().trimEnd()
}
