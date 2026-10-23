package com.example.core.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileSearchRankerTest {

    @Test
    fun `exact match outranks prefix which outranks substring`() {
        val names = listOf("quarterly-report.pdf", "report.pdf", "my report.pdf", "reporting.docx")
        val ranked = FileSearchRanker.rank(names, "report", { it }, limit = 10)
        assertEquals("report.pdf", ranked.first())
        assertTrue(ranked.indexOf("reporting.docx") < ranked.indexOf("quarterly-report.pdf"))
        // All non-matching names are dropped.
        assertTrue(ranked.none { !it.contains("report", ignoreCase = true) })
    }

    @Test
    fun `word boundary match scores higher than plain substring`() {
        assertEquals(200, FileSearchRanker.relevanceScore("report", "my report.pdf"))
        // Hyphen is a word separator: "quarterly-report" contains the word "report".
        assertEquals(200, FileSearchRanker.relevanceScore("report", "quarterly-report.pdf"))
        // "reporting.docx" starts with the query: prefix score wins over boundary logic.
        assertEquals(300, FileSearchRanker.relevanceScore("report", "reporting.docx"))
        assertEquals(300, FileSearchRanker.relevanceScore("report", "report 2024.pdf"))
        // Case-insensitive exact match only when the WHOLE name equals the query.
        assertEquals(400, FileSearchRanker.relevanceScore("report", "Report"))
        assertEquals(400, FileSearchRanker.relevanceScore("Report.PDF", "report.pdf"))
        assertEquals(300, FileSearchRanker.relevanceScore("report", "Report.PDF"))
    }

    @Test
    fun `ranking is case insensitive and tie-broken by shorter name`() {
        val names = listOf("aa notes.txt", "a notes.txt", "b notes.md")
        val ranked = FileSearchRanker.rank(names, "notes", { it }, limit = 10)
        // All three score 200 (word-boundary). Tie-break: shorter name first,
        // then alphabetical — "b notes.md" (10) < "a notes.txt" (11) < "aa notes.txt" (12).
        assertEquals("b notes.md", ranked[0])
        assertEquals("a notes.txt", ranked[1])
        assertEquals("aa notes.txt", ranked[2])
    }

    @Test
    fun `limit caps results and truncation flag works`() {
        val names = (1..20).map { "file $it.txt" }
        val ranked = FileSearchRanker.rank(names, "file", { it }, limit = 5)
        assertEquals(5, ranked.size)
        assertTrue(FileSearchRanker.isTruncated(matchCount = 6, limit = 5))
        assertTrue(!FileSearchRanker.isTruncated(matchCount = 5, limit = 5))
    }

    @Test
    fun `empty query ranks nothing`() {
        assertTrue(FileSearchRanker.rank(listOf("a.txt"), "  ", { it }, 10).isEmpty())
    }

    @Test
    fun `highlight range locates case-insensitive match`() {
        val range = FileSearchRanker.firstMatchRange("NOTE", "My notes file.txt")
        assertEquals(3, range?.start)
        assertEquals(7, range?.end)
        assertNull(FileSearchRanker.firstMatchRange("zzz", "My notes file.txt"))
        assertNull(FileSearchRanker.firstMatchRange("  ", "My notes file.txt"))
    }
}
