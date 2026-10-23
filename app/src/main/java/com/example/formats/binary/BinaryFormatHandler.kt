package com.example.formats.binary

import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatOpenRequest
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities

/** Identify-only binary formats retain the bounded hex/ASCII viewer. */
class BinaryFormatHandler : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = FORMAT_IDS.associateWith {
        capabilities(FormatCapability.PREVIEW)
    }
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState = identifyOnly(
        request,
        if (request.detection.mismatch) "The extension says one thing, but the content is binary data." else null
    )

    fun identifyOnly(request: FormatOpenRequest, note: String?): FileContentState {
        val (hexRows, asciiRows) = HexDumpReader.read(request.file)
        return FileContentState.BinarySuccess(
            hexRows = hexRows,
            asciiRows = asciiRows,
            file = request.fileEntity,
            label = request.detection.spec.displayName,
            note = note
        )
    }

    companion object {
        val FORMAT_IDS = setOf("psd", "archive-other", "exe", "data")
    }
}
