package com.example.formats.rtf

import com.example.core.io.TextLoader
import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatOpenRequest
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities

/** Read-only text extraction for Rich Text Format documents. */
class RtfFormatHandler : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = mapOf(
        "rtf" to capabilities(
            FormatCapability.PREVIEW,
            readOnlyReason = "Rich Text is shown as plain text (read-only)."
        )
    )
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState {
        val raw = TextLoader.load(request.file, "ISO-8859-1", MAX_RTF_PREVIEW_CHARS)
        return FileContentState.TextSuccess(
            content = RtfExtractor.extract(raw.text),
            file = request.fileEntity,
            readOnlyReason = "Rich Text is shown as plain text (read-only)."
        )
    }

    companion object {
        const val MAX_RTF_PREVIEW_CHARS = 20_000_000
        val FORMAT_IDS = setOf("rtf")
    }
}
