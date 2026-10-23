package com.example.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlSanitizerTest {

    @Test fun stripsScriptsStylesFramesAndForms() {
        val s = HtmlSanitizer.sanitize("<p>a</p><script>alert(1)</script><style>x{}</style><iframe src=x></iframe><form action=/x><input name=a><button>b</button></form><b>ok</b>")
        assertFalse(s.contains("script"))
        assertFalse(s.contains("<style"))
        assertFalse(s.contains("iframe"))
        assertFalse(s.contains("<form"))
        assertFalse(s.contains("<input"))
        assertTrue(s.contains("<p>a</p>"))
        assertTrue(s.contains("<b>ok</b>"))
    }

    @Test fun stripsUnclosedAndStrayTags() {
        val s = HtmlSanitizer.sanitize("a<script src=x>b<base href=//evil>c<meta http-equiv=refresh content=0></script>d")
        assertFalse(s.contains("<script"))
        assertFalse(s.contains("<base"))
        assertFalse(s.contains("<meta"))
        assertTrue(s.contains("a") && s.contains("d"))
    }

    @Test fun stripsEventHandlersInAllQuotingStyles() {
        for (h in listOf("onclick=\"x()\"", "onerror='x()'", "onload=x()")) {
            val s = HtmlSanitizer.sanitize("<a $h href=\"#\">t</a>")
            assertFalse(h, s.contains("on" + h.substring(2, 3)) && s.contains("x()"))
            assertFalse(s.contains("x()"))
        }
    }

    @Test fun removesJavascriptUrlsInStrictAndImageModes() {
        for (keep in listOf(false, true)) {
            val s = HtmlSanitizer.sanitize("<a href=\"javascript:alert(1)\">x</a><a href='JavaScript:bad()'>y</a>", keep)
            assertFalse(s.contains("alert"))
            assertFalse(s.contains("bad()"))
        }
    }

    @Test fun strictModeDropsImagesAndDataUrls() {
        val s = HtmlSanitizer.sanitize("<img src=\"a.png\"><a href=\"data:text/html,x\">d</a>")
        assertFalse(s.contains("<img"))
        assertFalse(s.contains("data:"))
    }

    @Test fun keepImagesModeKeepsSafeImagesButNotDangerousData() {
        val ok = HtmlSanitizer.sanitize("<img src=\"data:image/png;base64,AAAA\"><img src=\"pic.jpg\">", keepImages = true)
        assertTrue(ok.contains("data:image/png;base64,AAAA"))
        assertTrue(ok.contains("pic.jpg"))
        val bad = HtmlSanitizer.sanitize("<a href=\"data:text/html;base64,AAAA\">x</a><img src=\"data:image/svg+xml,<svg onload=1>\">", keepImages = true)
        assertFalse(bad.contains("text/html"))
        assertFalse(bad.contains("svg+xml"))
    }

    @Test fun plainTextAndEmptyPassThrough() {
        assertEquals("", HtmlSanitizer.sanitize(""))
        assertEquals("<p>hello</p>", HtmlSanitizer.sanitize("<p>hello</p>"))
    }
}
