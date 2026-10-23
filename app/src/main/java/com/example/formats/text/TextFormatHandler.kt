package com.example.formats.text

import com.example.core.io.TextLoadException
import com.example.core.io.TextLoader
import com.example.core.io.TextSaver
import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatOpenRequest
import com.example.domain.preview.FormatOperationResult
import com.example.domain.preview.FormatPrintPlan
import com.example.domain.preview.FormatPrintRequest
import com.example.domain.preview.FormatSaveRequest
import com.example.domain.preview.FormatSearchHit
import com.example.domain.preview.FormatSearchRequest
import com.example.domain.preview.FormatSearchResult
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities
import java.nio.charset.Charset

/** Plain-text, source-code, markup, and Markdown preview/edit/search behavior. */
class TextFormatHandler : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = FORMAT_IDS.associateWith {
        capabilities(
            FormatCapability.PREVIEW,
            FormatCapability.SEARCH,
            FormatCapability.EDIT,
            FormatCapability.SAVE,
            FormatCapability.PRINT
        )
    }
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState = try {
        val loaded = TextLoader.load(request.file, request.encoding, MAX_PREVIEW_CHARS)
        FileContentState.TextSuccess(
            content = loaded.text,
            file = request.fileEntity,
            truncated = loaded.truncated,
            totalBytes = loaded.totalBytes,
            charsetName = loaded.charset.name(),
            hadBom = loaded.hadBom
        )
    } catch (error: TextLoadException) {
        FileContentState.Error(error.message ?: "Could not read file")
    }

    override suspend fun search(request: FormatSearchRequest): FormatSearchResult {
        val charset = Charset.forName(request.charsetName)
        val result = TextFileSearchEngine.search(
            request.file,
            request.query,
            charset = charset,
            shouldContinue = request.shouldContinue
        )
        return FormatSearchResult(
            hits = result.matches.map { FormatSearchHit(offset = it.byteOffset) },
            matchCount = result.matchCount,
            truncated = result.truncated
        )
    }

    override suspend fun save(request: FormatSaveRequest): FormatOperationResult {
        if (request !is FormatSaveRequest.Text) {
            return FormatOperationResult.Unsupported("Text files require a text save request.")
        }
        return try {
            val charset = Charset.forName(request.charsetName)
            TextSaver.save(request.file, request.content, charset, request.hadBom)
            FormatOperationResult.Completed
        } catch (error: Exception) {
            FormatOperationResult.Failed(error.message ?: "Could not save text file")
        }
    }

    override fun preparePrint(request: FormatPrintRequest): FormatPrintPlan =
        FormatPrintPlan.SourceFile(request.file, request.title, request.mimeType)

    companion object {
        const val MAX_PREVIEW_CHARS = 250_000

        val FORMAT_IDS = setOf(
            "text", "json", "xml", "yaml", "config", "code-jvm", "code-web", "code-native",
            "code-script", "code-data", "markup-text", "markdown", "html"
        )
    }
}
