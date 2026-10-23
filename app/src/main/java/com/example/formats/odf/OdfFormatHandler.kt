package com.example.formats.odf

import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatOpenRequest
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities

/** Text extraction from OpenDocument files without inventing richer office rendering. */
class OdfFormatHandler : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = FORMAT_IDS.associateWith {
        capabilities(FormatCapability.PREVIEW, readOnlyReason = READ_ONLY_REASON)
    }
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState =
        FileContentState.TextSuccess(
            content = OdfTextExtractor.extract(request.file),
            file = request.fileEntity,
            readOnlyReason = READ_ONLY_REASON
        )

    companion object {
        const val READ_ONLY_REASON = "Text extracted from an OpenDocument file (read-only)."
        val FORMAT_IDS = setOf("odt", "ods", "odp")
    }
}
