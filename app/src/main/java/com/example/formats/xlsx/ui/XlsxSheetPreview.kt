package com.example.formats.xlsx.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.RecentFileEntity
import com.example.formats.csv.ui.CsvPreview
import com.example.viewmodel.MainViewModel

/**
 * XLSX adapter for the shared bounded grid.
 *
 * The XLSX handler supplies the visible sheet name and decides whether a
 * values-only save is safe. This adapter keeps that decision out of the shared
 * CSV surface while reusing the same viewer, search, zoom and save architecture.
 */
@Composable
fun XlsxSheetPreview(
    rows: List<List<String>>,
    fileEntity: RecentFileEntity,
    sheetName: String?,
    viewModel: MainViewModel,
    editable: Boolean,
    notice: String?,
    onDiscardAndExit: () -> Unit,
    modifier: Modifier = Modifier,
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
    CsvPreview(
        csvRows = rows,
        fileEntity = fileEntity,
        viewModel = viewModel,
        editable = editable,
        notice = notice,
        onDiscardAndExit = onDiscardAndExit,
        modifier = modifier,
        sheetName = sheetName,
        searchQuery = searchQuery,
        searchTriggerNext = searchTriggerNext,
        searchTriggerPrev = searchTriggerPrev,
        onSearchResultCount = onSearchResultCount,
        zoomFactor = zoomFactor,
        onZoomChanged = onZoomChanged,
        onWebViewCreated = onWebViewCreated,
        onReady = onReady,
        onSingleTap = onSingleTap
    )
}
