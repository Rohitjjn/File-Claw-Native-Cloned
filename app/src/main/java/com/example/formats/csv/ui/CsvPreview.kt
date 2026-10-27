package com.example.formats.csv.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentFileEntity
import com.example.formats.spreadsheet.SpreadsheetGridPayload
import com.example.formats.spreadsheet.ui.SpreadsheetGridWebView
import com.example.ui.component.formatFileSize
import com.example.ui.component.formatElapsedTime
import com.example.platform.clipboard.ClipboardHelper
import com.example.viewmodel.MainViewModel

/**
 * Existing delimited-table preview surface, now backed by the shared bounded
 * grid. The x-spreadsheet `file:///android_asset/` page and its whole-sheet
 * Base64 `evaluateJavascript()` injection were removed: with file access
 * disabled that page could not reliably load its scripts, and a dense sheet
 * could exceed WebView's script-string limit before any cell appeared.
 *
 * Editing remains values-only for supported tables/workbooks and still goes
 * through `MainViewModel.saveSpreadsheetFile` and the atomic format handlers.
 */
@Composable
fun CsvPreview(
    csvRows: List<List<String>>,
    fileEntity: RecentFileEntity,
    viewModel: MainViewModel,
    editable: Boolean,
    notice: String?,
    onDiscardAndExit: () -> Unit,
    modifier: Modifier = Modifier,
    sheetName: String? = null,
    searchQuery: String = "",
    searchTriggerNext: Int = 0,
    searchTriggerPrev: Int = 0,
    onSearchResultCount: (Int, Int, Boolean) -> Unit = { _, _, _ -> },
    zoomFactor: Float = 1f,
    onZoomChanged: (Float) -> Unit = {},
    onWebViewCreated: (android.webkit.WebView?) -> Unit = {},
    onReady: () -> Unit = {},
    onSingleTap: () -> Unit = {}
) {
    val isNightMode = androidx.compose.foundation.isSystemInDarkTheme()
    val context = LocalContext.current
    var draftRows by remember(csvRows) { mutableStateOf(csvRows) }
    var hasPendingEdits by remember(csvRows) { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(csvRows) {
        draftRows = csvRows
        hasPendingEdits = false
    }

    fun updateCell(row: Int, column: Int, text: String) {
        if (row < 0 || row >= SpreadsheetGridPayload.MAX_ROWS ||
            column < 0 || column >= SpreadsheetGridPayload.MAX_COLUMNS
        ) return
        val next: MutableList<MutableList<String>> = draftRows.map { it.toMutableList() }.toMutableList()
        val targetWidth = maxOf((next.maxOfOrNull { it.size } ?: 0), column + 1)
        while (next.size <= row) next.add(MutableList(targetWidth) { "" })
        while (next[row].size <= column) next[row].add("")
        next[row][column] = text.take(SpreadsheetGridPayload.MAX_CELL_CHARS)
        draftRows = next
        hasPendingEdits = true
    }

    androidx.activity.compose.BackHandler(enabled = hasPendingEdits) { showDiscardDialog = true }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Unsaved changes") },
            text = { Text("Save your edits to ${fileEntity.name} before leaving?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveSpreadsheetFile(fileEntity, draftRows)
                    hasPendingEdits = false
                    showDiscardDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        draftRows = csvRows
                        hasPendingEdits = false
                        showDiscardDialog = false
                        onDiscardAndExit()
                    }) { Text("Discard") }
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    text = fileEntity.extension.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = "${formatFileSize(fileEntity.size)} • Modified ${formatElapsedTime(fileEntity.lastOpened)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.weight(1f))
            when {
                !editable -> Text(
                    text = "View only",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
                hasPendingEdits -> TextButton(onClick = {
                    viewModel.saveSpreadsheetFile(fileEntity, draftRows)
                    hasPendingEdits = false
                }) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
                else -> Text(
                    text = "Edit Mode",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        SpreadsheetGridWebView(
            fileEntity = fileEntity,
            rows = draftRows,
            editable = editable,
            sheetName = sheetName,
            notice = notice,
            isNightMode = isNightMode,
            searchQuery = searchQuery,
            searchTriggerNext = searchTriggerNext,
            searchTriggerPrev = searchTriggerPrev,
            onSearchResultCount = onSearchResultCount,
            zoomFactor = zoomFactor,
            onZoomChanged = onZoomChanged,
            onCellChanged = { row, column, text -> updateCell(row, column, text) },
            onCopyText = { text -> ClipboardHelper.copyText(context, fileEntity.name, text) },
            onSingleTap = onSingleTap,
            onWebViewCreated = onWebViewCreated,
            onReady = onReady,
            modifier = Modifier.fillMaxSize()
        )
    }
}
