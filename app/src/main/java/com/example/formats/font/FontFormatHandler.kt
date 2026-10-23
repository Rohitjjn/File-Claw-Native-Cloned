package com.example.formats.font

import com.example.core.format.Support
import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatOpenRequest
import com.example.formats.binary.BinaryFormatHandler
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities

/** TrueType/OpenType font preview and bounded identify-only fallback for WOFF-family fonts. */
class FontFormatHandler(
    private val binaryHandler: BinaryFormatHandler
) : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = mapOf(
        "ttf" to capabilities(FormatCapability.PREVIEW),
        "woff" to capabilities(FormatCapability.PREVIEW)
    )
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState =
        if (request.detection.spec.support == Support.FULL) {
            FileContentState.FontSuccess(request.fileEntity)
        } else {
            binaryHandler.identifyOnly(
                request,
                "${request.detection.spec.displayName} can't be previewed yet."
            )
        }

    companion object {
        val FORMAT_IDS = setOf("ttf", "woff")
    }
}
