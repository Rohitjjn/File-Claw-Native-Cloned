package com.example.formats.epub

import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatOpenRequest
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities

/** EPUB package extraction and safe HTML rendering input. */
class EpubFormatHandler : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = mapOf("epub" to capabilities(FormatCapability.PREVIEW))
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState =
        FileContentState.HtmlSuccess(
            html = EpubReader.toHtml(request.file),
            file = request.fileEntity,
            title = request.fileEntity.name
        )

    companion object {
        val FORMAT_IDS = setOf("epub")
    }
}
