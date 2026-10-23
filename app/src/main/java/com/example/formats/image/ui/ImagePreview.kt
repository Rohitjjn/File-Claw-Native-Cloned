package com.example.formats.image.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.data.RecentFileEntity
import java.io.File
import androidx.compose.ui.semantics.contentDescription

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ImagePreview(
    fileEntity: RecentFileEntity,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
    onPageChanged: (java.io.File) -> Unit = {}
) {
    val parentDir = remember(fileEntity) { java.io.File(fileEntity.path).parentFile }
    val imageFiles = remember(parentDir) {
        parentDir?.listFiles()?.filter {
            val ext = it.extension.lowercase()
            ext in listOf("png", "jpg", "jpeg", "gif", "bmp", "webp")
        }?.sortedBy { it.name } ?: listOf(java.io.File(fileEntity.path))
    }

    val initialIndex = remember(imageFiles, fileEntity) {
        val idx = imageFiles.indexOfFirst { it.absolutePath == fileEntity.path }
        if (idx == -1) 0 else idx
    }

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialIndex,
        pageCount = { imageFiles.size }
    )


    // Keep the existing recent-file behavior while routing storage updates through the ViewModel.
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != initialIndex) {
            imageFiles.getOrNull(pagerState.currentPage)?.let(onPageChanged)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val file = imageFiles[page]
            val zoomSpec = remember {
                me.saket.telephoto.zoomable.ZoomSpec(maxZoomFactor = 15f)
            }
            val zoomableState = me.saket.telephoto.zoomable.rememberZoomableState(zoomSpec = zoomSpec)
            val zoomableImageState = me.saket.telephoto.zoomable.rememberZoomableImageState(zoomableState = zoomableState)

            me.saket.telephoto.zoomable.coil.ZoomableAsyncImage(
                model = file,
                contentDescription = "Image preview",
                modifier = Modifier.fillMaxSize(),
                state = zoomableImageState,
                onClick = { onTap() }
            )
        }
    }
}
