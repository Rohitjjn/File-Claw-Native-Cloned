package com.example.formats.svg

import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatOpenRequest
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities

/** SVG content remains in the SVG family; rendering uses the shared hardened WebView. */
class SvgFormatHandler : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = mapOf("svg" to capabilities(FormatCapability.PREVIEW))
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState =
        FileContentState.SvgSuccess(request.fileEntity)

    companion object {
        val FORMAT_IDS = setOf("svg")
    }
}
