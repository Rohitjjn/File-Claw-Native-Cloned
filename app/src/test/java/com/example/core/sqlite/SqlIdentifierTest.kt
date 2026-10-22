package com.example.core.sqlite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SqlIdentifierTest {
    @Test fun quotesPlainNames() {
        assertEquals("\"users\"", SqlIdentifier.quote("users"))
        assertEquals("\"my table\"", SqlIdentifier.quote("my table"))
    }

    @Test fun neutralisesInjectionAttempts() {
        assertEquals("\"a\"\"; DROP TABLE x; --\"", SqlIdentifier.quote("a\"; DROP TABLE x; --"))
        assertEquals("\"ab\"", SqlIdentifier.quote("a\u0000b"))
        assertEquals("\"\"", SqlIdentifier.quote(""))
    }

    @Test fun truncatesLongCellsSafely() {
        assertEquals("short", SqlIdentifier.truncateCell("short"))
        val t = SqlIdentifier.truncateCell("x".repeat(600))
        assertEquals(501, t.length)
        assertTrue(t.endsWith("\u2026"))
        val emoji = SqlIdentifier.truncateCell("😀".repeat(400), 5)
        assertFalse(Character.isHighSurrogate(emoji[emoji.length - 2]))
    }
}
