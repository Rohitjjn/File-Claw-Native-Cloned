package com.example.formats.csv

import java.io.StringReader
import java.io.StringWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DelimitedTest {

    private fun parse(s: String, d: Char = ',', maxRows: Int = 1000, maxCols: Int = 100) =
        DelimitedParser.parse(StringReader(s), d, maxRows, maxCols)

    @Test fun parsesSimpleRows() {
        val r = parse("a,b,c\n1,2,3\n")
        assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "3")), r.rows)
        assertFalse(r.truncated)
    }

    @Test fun handlesCrlfCrAndMissingFinalNewline() {
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), parse("a,b\r\nc,d").rows)
        assertEquals(listOf(listOf("a"), listOf("b")), parse("a\rb\r").rows)
        assertEquals(listOf(listOf("a"), listOf("b")), parse("a\nb").rows)
    }

    @Test fun handlesQuotedFieldsWithDelimitersNewlinesAndEscapedQuotes() {
        val r = parse("\"a,b\",\"line1\nline2\",\"say \"\"hi\"\"\"\n")
        assertEquals(listOf(listOf("a,b", "line1\nline2", "say \"hi\"")), r.rows)
        assertEquals(listOf(listOf("", "x")), parse("\"\",x").rows)
        assertEquals(listOf(listOf("\"a\"")), parse("\"\"\"a\"\"\"").rows)
    }

    @Test fun keepsEmptyCellsAndRaggedRows() {
        val r = parse("a,,c\n1\n,,\n")
        assertEquals(listOf(listOf("a", "", "c"), listOf("1"), listOf("", "", "")), r.rows)
    }

    @Test fun supportsTabAndOtherDelimiters() {
        assertEquals(listOf(listOf("a", "b,c")), parse("a\tb,c", '\t').rows)
        assertEquals(listOf(listOf("a", "b")), parse("a;b", ';').rows)
        assertEquals('\t', DelimitedParser.delimiterFor("TSV"))
        assertEquals('\t', DelimitedParser.delimiterFor("tab"))
        assertEquals('|', DelimitedParser.delimiterFor("psv"))
        assertEquals(',', DelimitedParser.delimiterFor("csv"))
    }

    @Test fun rowCapSetsTruncatedOnlyWhenMoreDataExists() {
        val exact = parse("1\n2\n3\n", maxRows = 3)
        assertEquals(3, exact.rows.size)
        assertFalse(exact.truncated)
        val over = parse("1\n2\n3\n4\n", maxRows = 3)
        assertEquals(3, over.rows.size)
        assertTrue(over.truncated)
        val overNoNl = parse("1\n2\n3\n4", maxRows = 3)
        assertTrue(overNoNl.truncated)
        val overCr = parse("1\r2\r3\r4\r5", maxRows = 3)
        assertTrue(overCr.truncated)
    }

    @Test fun columnCapSetsTruncated() {
        val r = parse("a,b,c,d\n", maxCols = 2)
        assertEquals(listOf(listOf("a", "b")), r.rows)
        assertTrue(r.truncated)
    }

    @Test fun emptyInputGivesNoRows() {
        assertTrue(parse("").rows.isEmpty())
    }

    @Test fun sniffsDelimiters() {
        assertEquals(';', DelimitedParser.sniffDelimiter("a;b;c\n1;2;3\n4;5;6\n"))
        assertEquals('\t', DelimitedParser.sniffDelimiter("a\tb\n1\t2\n"))
        assertEquals(',', DelimitedParser.sniffDelimiter("a,b,c\n1,2,3\n"))
        assertEquals('|', DelimitedParser.sniffDelimiter("a|b\n1|2\n"))
        assertEquals(',', DelimitedParser.sniffDelimiter("just words here\nand more\n"))
        assertEquals('\t', DelimitedParser.sniffDelimiter("", '\t'))
        assertEquals(',', DelimitedParser.sniffDelimiter("\"a;b\",c\n\"d;e\",f\n"))
    }

    @Test fun writerQuotesWhenNeededAndRoundTrips() {
        val rows = listOf(listOf("a", "b,c", "say \"x\""), listOf("multi\nline", " pad ", ""))
        val sw = StringWriter()
        DelimitedWriter.write(rows, sw)
        assertEquals("a,\"b,c\",\"say \"\"x\"\"\"\n\"multi\nline\",\" pad \",\n", sw.toString())
        assertEquals(rows, parse(sw.toString()).rows)
        val tsv = StringWriter()
        DelimitedWriter.write(listOf(listOf("a", "b\tc")), tsv, '\t', "\r\n")
        assertEquals("a\t\"b\tc\"\r\n", tsv.toString())
        assertEquals(listOf(listOf("a", "b\tc")), parse(tsv.toString(), '\t').rows)
    }
}
