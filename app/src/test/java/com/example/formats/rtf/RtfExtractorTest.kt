package com.example.formats.rtf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RtfExtractorTest {
    @Test fun rtfExtractsTextAndSkipsTables() {
        val rtf = "{\\rtf1\\ansi{\\fonttbl{\\f0 Arial;}}{\\colortbl;\\red0\\green0\\blue0;}" +
            "\\pard Hello \\b World\\b0\\par Second line\\tab tabbed\\par}"
        val t = RtfExtractor.extract(rtf)
        assertEquals("Hello World\nSecond line\ttabbed", t)
        assertFalse(t.contains("Arial"))
    }

    @Test fun rtfHandlesEscapesUnicodeAndDestinations() {
        assertEquals("caf\u00E9", RtfExtractor.extract("{\\rtf1 caf\\'e9}"))
        assertEquals("a\u20ACb", RtfExtractor.extract("{\\rtf1 a\\u8364?b}"))
        assertEquals("x\uD83Dy".length, RtfExtractor.extract("{\\rtf1 x\\u-10179?y}").length)
        assertEquals("keep", RtfExtractor.extract("{\\rtf1 {\\*\\generator Word;}keep}"))
        assertEquals("a{b}c\\d", RtfExtractor.extract("{\\rtf1 a\\{b\\}c\\\\d}"))
        assertEquals("\u2014\u2013\u2022\u2018\u2019\u201C\u201D", RtfExtractor.extract("{\\rtf1 \\emdash \\endash \\bullet \\lquote\\rquote \\ldblquote\\rdblquote}"))
        assertEquals("a\u00A0b", RtfExtractor.extract("{\\rtf1 a\\~b}"))
        assertEquals("a-b", RtfExtractor.extract("{\\rtf1 a\\_b}"))
        assertEquals("", RtfExtractor.extract("{\\rtf1 {\\pict\\wmetafile8 deadbeef}}"))
        assertEquals("line1\nline2", RtfExtractor.extract("{\\rtf1 line1\\line line2}"))
    }

    @Test fun rtfRespectsMaxCharsAndSurvivesGarbage() {
        assertTrue(RtfExtractor.extract("{\\rtf1 " + "word ".repeat(1000) + "}", maxChars = 50).length < 120)
        RtfExtractor.extract("{\\rtf1 \\")
        RtfExtractor.extract("}}}{{{\\'")
        RtfExtractor.extract("")
    }
}
