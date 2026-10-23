package com.example.formats.archive

import com.example.core.format.Support
import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatExportRequest
import com.example.domain.preview.FormatOpenRequest
import com.example.domain.preview.FormatOperationResult
import com.example.formats.binary.BinaryFormatHandler
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities
import com.example.platform.storage.FileManager

/** Archive listing and bounded extraction, delegating archive safety policy to FileManager/core. */
class ArchiveFormatHandler(
    private val fileManager: FileManager,
    private val binaryHandler: BinaryFormatHandler
) : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = FORMAT_IDS.associateWith { formatId ->
        if (formatId == "rar") {
            capabilities(FormatCapability.PREVIEW, readOnlyReason = "RAR archives can be identified but not opened.")
        } else {
            capabilities(FormatCapability.PREVIEW, FormatCapability.EXPORT)
        }
    }
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState {
        if (request.detection.spec.support == Support.IDENTIFY) {
            return binaryHandler.identifyOnly(
                request,
                "${request.detection.spec.displayName} files can't be opened yet."
            )
        }
        if (request.archivePassword.isEmpty() && fileManager.isArchiveEncrypted(request.file.absolutePath)) {
            return FileContentState.ArchivePasswordRequired(request.fileEntity)
        }
        return try {
            val root = fileManager.parseZipStructure(
                request.file.absolutePath,
                request.archivePassword.ifEmpty { null }
            )
            FileContentState.ZipSuccess(root, request.fileEntity, request.archivePassword)
        } catch (error: UnsupportedOperationException) {
            FileContentState.Error(error.message ?: "This archive type is not supported yet")
        }
    }

    override suspend fun export(request: FormatExportRequest): FormatOperationResult {
        val entryPath = request.entryPath
            ?: return FormatOperationResult.Unsupported("Choose an archive entry to extract.")
        return if (fileManager.extractZipEntry(
                request.source.absolutePath,
                entryPath,
                request.destination,
                request.archivePassword.ifEmpty { null }
            )
        ) {
            FormatOperationResult.Completed
        } else {
            FormatOperationResult.Failed("Could not extract the selected archive entry.")
        }
    }

    companion object {
        val FORMAT_IDS = setOf(
            "zip", "zip-app", "7z", "tar", "tgz", "tbz2", "txz", "ar", "cpio", "gz", "bz2", "xz", "rar"
        )
    }
}
