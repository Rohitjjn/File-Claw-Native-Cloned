package com.example.formats.spreadsheet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpreadsheetGridPayloadTest {

    @Test fun jsonEscapesQuotesBackslashesAndControlCharacters() {
        val json = SpreadsheetGridPayload.buildJson(
            listOf(listOf("quote \" slash \\", "line\nbreak", "Unicode नमस्ते 🧠")),
            editable = true,
            sheetName = "Sheet \"1\"",
            notice = "Read only"
        )
        assertTrue(json.contains("\\\""))
        assertTrue(json.contains("\\\\"))
        assertTrue(json.contains("\\n"))
        assertTrue(json.contains("नमस्ते 🧠"))
        assertTrue(json.contains("\"editable\":true"))
        assertTrue(json.contains("\"notice\":\"Read only\""))
    }

    @Test fun safeSheetNameRemovesExcelForbiddenCharactersAndCapsLength() {
        assertEquals("Sheet1", SpreadsheetGridPayload.safeSheetName(null))
        assertEquals("Clean name", SpreadsheetGridPayload.safeSheetName("Clean[name]"))
        assertEquals(31, SpreadsheetGridPayload.safeSheetName("A".repeat(100)).length)
    }

    @Test fun clampCellBoundsSingleCellAndReportsTruncation() {
        val (safe, truncated) = SpreadsheetGridPayload.clampCell("x".repeat(10_000), 10_000_000)
        assertEquals(SpreadsheetGridPayload.MAX_CELL_CHARS, safe.length)
        assertTrue(truncated)
        val (unchanged, unchangedFlag) = SpreadsheetGridPayload.clampCell("value", 100)
        assertEquals("value", unchanged)
        assertFalse(unchangedFlag)
    }

    @Test fun oversizedPayloadBecomesReadOnlyAndCarriesNotice() {
        val rows = List(SpreadsheetGridPayload.MAX_ROWS + 5) { listOf("A", "B") }
        val payload = SpreadsheetGridPayload.build(rows, editable = true, sheetName = "Data", notice = null)
        assertTrue(payload.truncated)
        assertFalse(payload.editable)
        assertEquals(SpreadsheetGridPayload.MAX_ROWS, payload.rows.size)
        assertTrue(payload.notice?.contains("Editing is disabled") == true)
        assertTrue(payload.json.contains("\"editable\":false"))
    }

    @Test fun smallEditablePayloadPreservesEditability() {
        val payload = SpreadsheetGridPayload.build(
            listOf(listOf("A1", "B1"), listOf("A2", "B2")),
            editable = true,
            sheetName = "Summary",
            notice = null
        )
        assertFalse(payload.truncated)
        assertTrue(payload.editable)
        assertEquals(null, payload.notice)
    }
}
