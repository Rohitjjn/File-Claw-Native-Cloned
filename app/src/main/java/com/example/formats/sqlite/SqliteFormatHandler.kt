package com.example.formats.sqlite

import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatOpenRequest
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities

/** SQLite connection lifecycle and table browsing are rendered by the existing SQLite viewer. */
class SqliteFormatHandler : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = mapOf("sqlite" to capabilities(FormatCapability.PREVIEW))
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState =
        FileContentState.SqliteSuccess(request.fileEntity)

    companion object {
        val FORMAT_IDS = setOf("sqlite")
    }
}
