package com.example.formats.registry

import com.example.domain.preview.FormatHandler
import com.example.formats.archive.ArchiveFormatHandler
import com.example.formats.binary.BinaryFormatHandler
import com.example.formats.csv.DelimitedFormatHandler
import com.example.formats.ooxml.OoxmlDocumentHandler
import com.example.formats.epub.EpubFormatHandler
import com.example.formats.font.FontFormatHandler
import com.example.formats.image.ImageFormatHandler
import com.example.formats.legacyoffice.LegacyOfficeFormatHandler
import com.example.formats.media.MediaFormatHandler
import com.example.formats.model3d.Model3dFormatHandler
import com.example.formats.odf.OdfFormatHandler
import com.example.formats.pdf.PdfFormatHandler
import com.example.formats.rtf.RtfFormatHandler
import com.example.formats.sqlite.SqliteFormatHandler
import com.example.formats.svg.SvgFormatHandler
import com.example.formats.text.TextFormatHandler
import com.example.formats.xlsx.XlsxFormatHandler
import com.example.platform.storage.FileManager

/** Creates the one runtime handler router from format-family implementations. */
object DefaultFormatHandlers {
    fun create(fileManager: FileManager): List<FormatHandler> {
        val binary = BinaryFormatHandler()
        val archive = ArchiveFormatHandler(fileManager, binary)
        return listOf(
                TextFormatHandler(),
                DelimitedFormatHandler(),
                XlsxFormatHandler(),
                OoxmlDocumentHandler(archive),
                PdfFormatHandler(),
                archive,
                ImageFormatHandler(),
                SvgFormatHandler(),
                MediaFormatHandler(),
                SqliteFormatHandler(),
                FontFormatHandler(binary),
                Model3dFormatHandler(binary),
                RtfFormatHandler(),
                OdfFormatHandler(),
                EpubFormatHandler(),
                LegacyOfficeFormatHandler(binary),
                binary
        )
    }
}
