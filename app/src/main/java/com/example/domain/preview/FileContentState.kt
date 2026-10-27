package com.example.domain.preview

import com.example.core.archive.ArchiveNode
import com.example.data.RecentFileEntity
import com.example.formats.csv.DelimitedTableData
import com.example.formats.model3d.Mesh

/**
 * Application-facing state produced by a format handler and rendered by the preview feature.
 * This state intentionally contains the existing Room file record so the migration does not
 * change recent-file, source-URI, or archive-entry behavior.
 */
sealed class FileContentState {
    object Idle : FileContentState()
    object Loading : FileContentState()

    /**
     * [truncated] and [readOnlyReason] are flags, never text inside [content]: the editor refuses to
     * save a truncated or converted view, so it can never overwrite the original with a fragment.
     */
    data class TextSuccess(
        val content: String,
        val file: RecentFileEntity,
        val truncated: Boolean = false,
        val totalBytes: Long = 0,
        val charsetName: String = "UTF-8",
        val hadBom: Boolean = false,
        val readOnlyReason: String? = null
    ) : FileContentState() {
        val editable: Boolean get() = !truncated && readOnlyReason == null
    }

    data class CsvSuccess(
        val rows: List<List<String>>,
        val file: RecentFileEntity,
        val truncated: Boolean = false,
        val readOnlyReason: String? = null,
        val table: DelimitedTableData? = null,
        /**
         * Light-edit mode for workbook formats (XLSX) that don't have a [table]: a
         * values-only save handled by [FormatSaveRequest.Spreadsheet] rather than the
         * delimited-table path. [sheetName] is the single sheet a save writes back to;
         * only set alongside [spreadsheetEditable].
         */
        val spreadsheetEditable: Boolean = false,
        val sheetName: String? = null
    ) : FileContentState() {
        val editable: Boolean
            get() = !truncated && readOnlyReason == null && (table != null || spreadsheetEditable)
    }

    data class ZipSuccess(
        val root: ArchiveNode,
        val file: RecentFileEntity,
        val password: String = ""
    ) : FileContentState()

    data class ArchivePasswordRequired(val file: RecentFileEntity) : FileContentState()
    data class ImageSuccess(val file: RecentFileEntity) : FileContentState()
    data class SvgSuccess(val file: RecentFileEntity) : FileContentState()
    data class PdfSuccess(val file: RecentFileEntity) : FileContentState()
    data class DocxSuccess(val base64Data: String, val file: RecentFileEntity) : FileContentState()
    data class PptxSuccess(val base64Data: String, val file: RecentFileEntity) : FileContentState()
    data class MediaSuccess(val file: RecentFileEntity, val isAudio: Boolean) : FileContentState()
    data class SqliteSuccess(val file: RecentFileEntity) : FileContentState()
    data class FontSuccess(val file: RecentFileEntity) : FileContentState()
    data class Model3dSuccess(val mesh: Mesh, val file: RecentFileEntity, val label: String) : FileContentState()
    data class HtmlSuccess(val html: String, val file: RecentFileEntity, val title: String) : FileContentState()
    data class BinarySuccess(
        val hexRows: List<String>,
        val asciiRows: List<String>,
        val file: RecentFileEntity,
        val label: String? = null,
        val note: String? = null
    ) : FileContentState()

    data class Error(val message: String) : FileContentState()

    /** The file this state is showing, whichever viewer it belongs to. */
    val fileOrNull: RecentFileEntity?
        get() = when (this) {
            is TextSuccess -> file
            is CsvSuccess -> file
            is ZipSuccess -> file
            is ArchivePasswordRequired -> file
            is ImageSuccess -> file
            is SvgSuccess -> file
            is PdfSuccess -> file
            is DocxSuccess -> file
            is PptxSuccess -> file
            is MediaSuccess -> file
            is SqliteSuccess -> file
            is FontSuccess -> file
            is Model3dSuccess -> file
            is HtmlSuccess -> file
            is BinarySuccess -> file
            else -> null
        }
}
