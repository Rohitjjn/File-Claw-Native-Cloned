package com.example.domain.preview

import com.example.core.format.Detection
import com.example.core.format.FormatRouteHandler
import com.example.data.RecentFileEntity
import java.io.File

/** Operations a format handler may safely provide for a recognized format. */
enum class FormatCapability {
    PREVIEW,
    SEARCH,
    EDIT,
    SAVE,
    PRINT,
    EXPORT
}

data class FormatCapabilities(
    val available: Set<FormatCapability>,
    val readOnlyReason: String? = null,
    val unavailableReasons: Map<FormatCapability, String> = emptyMap()
) {
    fun supports(capability: FormatCapability): Boolean = capability in available

    fun reasonFor(capability: FormatCapability): String? = when {
        supports(capability) -> null
        else -> unavailableReasons[capability] ?: readOnlyReason
    }
}

/** Complete input for opening one already-detected file. */
data class FormatOpenRequest(
    val file: File,
    val fileEntity: RecentFileEntity,
    val detection: Detection,
    val archivePassword: String = "",
    val encoding: String = "UTF-8"
)

data class FormatSearchRequest(
    val file: File,
    val query: String,
    val charsetName: String = "UTF-8",
    /** Lets bounded streaming handlers stop when the caller's search job is cancelled. */
    val shouldContinue: () -> Boolean = { true }
)

data class FormatSearchHit(val offset: Long, val label: String? = null)

data class FormatSearchResult(
    val hits: List<FormatSearchHit>,
    val matchCount: Int,
    val truncated: Boolean
)

/** Typed save requests avoid passing nullable, format-ambiguous payloads to handlers. */
sealed interface FormatSaveRequest {
    val file: File

    data class Text(
        override val file: File,
        val content: String,
        val charsetName: String,
        val hadBom: Boolean
    ) : FormatSaveRequest

    data class Delimited(
        override val file: File,
        val rows: List<List<String>>,
        val charsetName: String,
        val hadBom: Boolean,
        val delimiter: Char,
        val lineEnd: String
    ) : FormatSaveRequest
}

data class FormatExportRequest(
    val source: File,
    val destination: File,
    val entryPath: String? = null,
    val archivePassword: String = ""
)

data class FormatPrintRequest(
    val file: File,
    val title: String,
    val mimeType: String
)

/** A platform-neutral print plan; Android's print adapter performs the final platform action. */
sealed interface FormatPrintPlan {
    val title: String

    data class SourceFile(
        val file: File,
        override val title: String,
        val mimeType: String
    ) : FormatPrintPlan

    data class Html(
        val html: String,
        override val title: String
    ) : FormatPrintPlan

    data class WebDocument(
        val file: File,
        override val title: String,
        val rendererId: String
    ) : FormatPrintPlan
}

sealed interface FormatOperationResult {
    data object Completed : FormatOperationResult
    data class Unsupported(val reason: String) : FormatOperationResult
    data class Failed(val message: String) : FormatOperationResult
}

/**
 * The single format-family contract used by the existing registry routing. Optional operations
 * default to an explicit unsupported result; handlers advertise only capabilities they implement.
 */
interface FormatHandler : FormatRouteHandler {
    fun capabilities(formatId: String): FormatCapabilities

    suspend fun open(request: FormatOpenRequest): FileContentState

    suspend fun search(request: FormatSearchRequest): FormatSearchResult =
        throw UnsupportedOperationException("Search is not supported for ${request.file.name}")

    suspend fun save(request: FormatSaveRequest): FormatOperationResult =
        FormatOperationResult.Unsupported("Saving is not supported for this format.")

    fun preparePrint(request: FormatPrintRequest): FormatPrintPlan? = null

    suspend fun export(request: FormatExportRequest): FormatOperationResult =
        FormatOperationResult.Unsupported("Export is not supported for this format.")

    /** Releases any resources associated with a successfully opened state. */
    fun close(state: FileContentState) = Unit
}
