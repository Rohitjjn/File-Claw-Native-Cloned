package com.example.formats.image

import com.example.core.io.AtomicFileExport
import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatExportRequest
import com.example.domain.preview.FormatOpenRequest
import com.example.domain.preview.FormatOperationResult
import com.example.domain.preview.FormatPrintPlan
import com.example.domain.preview.FormatPrintRequest
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities

/** Raster-image preview and safe copy/export behavior; decoding remains in the existing UI. */
class ImageFormatHandler : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = FORMAT_IDS.associateWith {
        capabilities(FormatCapability.PREVIEW, FormatCapability.PRINT, FormatCapability.EXPORT)
    }
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState =
        FileContentState.ImageSuccess(request.fileEntity)

    override fun preparePrint(request: FormatPrintRequest): FormatPrintPlan =
        FormatPrintPlan.SourceFile(request.file, request.title, request.mimeType)

    override suspend fun export(request: FormatExportRequest): FormatOperationResult = try {
        AtomicFileExport.copy(request.source, request.destination)
        FormatOperationResult.Completed
    } catch (error: Exception) {
        FormatOperationResult.Failed(error.message ?: "Could not export image")
    }

    companion object {
        val FORMAT_IDS = setOf("png", "jpeg", "gif", "webp", "bmp", "ico", "heif", "tiff", "raw")
    }
}
