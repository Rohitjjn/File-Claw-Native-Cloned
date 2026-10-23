package com.example.formats.media

import com.example.core.format.FileKind
import com.example.core.io.AtomicFileExport
import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatExportRequest
import com.example.domain.preview.FormatOpenRequest
import com.example.domain.preview.FormatOperationResult
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities

/** Audio/video dispatch and safe export-copy support. */
class MediaFormatHandler : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = FORMAT_IDS.associateWith {
        capabilities(FormatCapability.PREVIEW, FormatCapability.EXPORT)
    }
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState =
        FileContentState.MediaSuccess(
            request.fileEntity,
            isAudio = request.detection.kind == FileKind.AUDIO
        )

    override suspend fun export(request: FormatExportRequest): FormatOperationResult = try {
        AtomicFileExport.copy(request.source, request.destination)
        FormatOperationResult.Completed
    } catch (error: Exception) {
        FormatOperationResult.Failed(error.message ?: "Could not export media file")
    }

    companion object {
        val FORMAT_IDS = setOf(
            "mp3", "wav", "ogg", "m4a", "flac", "midi", "audio-other",
            "mp4", "mkv", "webm", "3gp", "video-other"
        )
    }
}
