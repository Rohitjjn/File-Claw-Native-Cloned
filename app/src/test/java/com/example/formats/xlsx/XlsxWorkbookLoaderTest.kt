package com.example.formats.xlsx

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.example.formats.spreadsheet.SpreadsheetGridPayload
import java.io.File
import java.nio.file.Files

class XlsxWorkbookLoaderTest {
    private lateinit var dir: File

    @Before fun setUp() { dir = Files.createTempDirectory("xlsx-loader").toFile() }
    @After fun tearDown() { dir.deleteRecursively() }

    @Test fun writeProducesReadableSingleSheetWorkbook() {
        val file = File(dir, "values.xlsx")
        XlsxWorkbookLoader.write(
            file,
            listOf(listOf("Name", "Value"), listOf("Item", "42")),
            "Unsafe/name:[]"
        )
        org.dhatim.fastexcel.reader.ReadableWorkbook(file).use { workbook ->
            val sheet = workbook.firstSheet
            assertEquals("Unsafe name", sheet.name)
            val rows = sheet.openStream().use { stream ->
                stream.iterator().asSequence().map { row -> row.map { cell -> cell?.text.orEmpty() } }.toList()
            }
            assertEquals(listOf("Name", "Value"), rows[0])
            assertEquals(listOf("Item", "42"), rows[1])
        }
    }

    @Test fun oversizedEditDoesNotTouchOriginalFile() {
        val file = File(dir, "original.xlsx")
        val original = byteArrayOf(1, 2, 3, 4)
        file.writeBytes(original)
        val tooManyRows = List(SpreadsheetGridPayload.MAX_ROWS + 2) { listOf("value") }
        assertThrows(IllegalArgumentException::class.java) {
            XlsxWorkbookLoader.write(file, tooManyRows, "Data")
        }
        assertTrue(file.readBytes().contentEquals(original))
    }
}
