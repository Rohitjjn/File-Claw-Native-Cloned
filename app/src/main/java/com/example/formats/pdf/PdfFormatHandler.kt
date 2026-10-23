package com.example.formats.pdf

import com.example.core.io.AtomicFileExport
import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatExportRequest
import com.example.domain.preview.FormatOpenRequest
import com.example.domain.preview.FormatOperationResult
import com.example.domain.preview.FormatPrintPlan
import com.example.domain.preview.FormatPrintRequest
import com.example.domain.preview.FormatSearchHit
import com.example.domain.preview.FormatSearchRequest
import com.example.domain.preview.FormatSearchResult
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities

/** PDF preview/search/print planning; rendering itself remains in the existing PDF UI. */
class PdfFormatHandler : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = mapOf(
        "pdf" to capabilities(
            FormatCapability.PREVIEW,
            FormatCapability.SEARCH,
            FormatCapability.PRINT,
            FormatCapability.EXPORT
        )
    )
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState =
        FileContentState.PdfSuccess(request.fileEntity)

    override suspend fun search(request: FormatSearchRequest): FormatSearchResult {
        val results = PdfTextSearcher.searchInPdf(request.file, request.query)
        return FormatSearchResult(
            hits = results.map { FormatSearchHit(offset = it.pageNumber.toLong(), label = it.matchedText) },
            matchCount = results.size,
            truncated = results.size >= MAX_SEARCH_RESULTS
        )
    }

    override fun preparePrint(request: FormatPrintRequest): FormatPrintPlan =
        FormatPrintPlan.SourceFile(request.file, request.title, "application/pdf")

    override suspend fun export(request: FormatExportRequest): FormatOperationResult = try {
        AtomicFileExport.copy(request.source, request.destination)
        FormatOperationResult.Completed
    } catch (error: Exception) {
        FormatOperationResult.Failed(error.message ?: "Could not export PDF")
    }

    companion object {
        val FORMAT_IDS = setOf("pdf")
        private const val MAX_SEARCH_RESULTS = 100
    }
}
