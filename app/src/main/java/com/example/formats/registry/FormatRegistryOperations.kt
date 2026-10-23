package com.example.formats.registry

import com.example.core.format.Detection
import com.example.core.format.FormatRegistry
import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapabilities
import com.example.domain.preview.FormatExportRequest
import com.example.domain.preview.FormatHandler
import com.example.domain.preview.FormatOpenRequest
import com.example.domain.preview.FormatOperationResult
import com.example.domain.preview.FormatPrintPlan
import com.example.domain.preview.FormatPrintRequest
import com.example.domain.preview.FormatSaveRequest
import com.example.domain.preview.FormatSearchRequest
import com.example.domain.preview.FormatSearchResult

/** Type-safe application operations routed through the existing core FormatRegistry ID table. */
private fun FormatRegistry.formatHandler(detection: Detection): FormatHandler? =
    handlerFor(detection) as? FormatHandler

fun FormatRegistry.formatCapabilities(formatId: String): FormatCapabilities? =
    (handlerFor(formatId) as? FormatHandler)?.capabilities(formatId)

suspend fun FormatRegistry.open(request: FormatOpenRequest): FileContentState {
    val handler = formatHandler(request.detection)
        ?: return FileContentState.Error("No preview handler is registered for ${request.detection.spec.displayName} files.")
    return handler.open(request)
}

suspend fun FormatRegistry.save(
    detection: Detection,
    request: FormatSaveRequest
): FormatOperationResult =
    formatHandler(detection)?.save(request)
        ?: FormatOperationResult.Unsupported("No save handler is registered for ${detection.spec.displayName}.")

suspend fun FormatRegistry.search(
    detection: Detection,
    request: FormatSearchRequest
): FormatSearchResult =
    formatHandler(detection)?.search(request)
        ?: throw UnsupportedOperationException("No search handler is registered for ${detection.spec.displayName}.")

fun FormatRegistry.preparePrint(
    detection: Detection,
    request: FormatPrintRequest
): FormatPrintPlan? = formatHandler(detection)?.preparePrint(request)

suspend fun FormatRegistry.export(
    detection: Detection,
    request: FormatExportRequest
): FormatOperationResult =
    formatHandler(detection)?.export(request)
        ?: FormatOperationResult.Unsupported("No export handler is registered for ${detection.spec.displayName}.")

fun FormatRegistry.close(detection: Detection, state: FileContentState) {
    formatHandler(detection)?.close(state)
}
