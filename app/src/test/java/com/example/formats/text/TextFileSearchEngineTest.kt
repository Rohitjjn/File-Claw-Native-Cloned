package com.example.formats.text

import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TextFileSearchEngineTest {
    private lateinit var dir: File

    @Before fun setUp() { dir = Files.createTempDirectory("text-search").toFile() }
    @After fun tearDown() { dir.deleteRecursively() }

    private fun file(name: String, content: ByteArray) = File(dir, name).also { it.writeBytes(content) }

    @Test
    fun findsCaseInsensitiveMatchesAcrossReadBoundaries() {
        val source = "xxAbCyyABCzz"
        val input = file("cross-boundary.txt", source.toByteArray(Charsets.UTF_8))

        val result = TextFileSearchEngine.search(input, "abc", bufferSize = 4)

        assertEquals(2, result.matchCount)
        assertEquals(
            listOf(2L, 7L),
            result.matches.map { it.byteOffset }
        )
        assertFalse(result.truncated)
    }

    @Test
    fun findsMatchesThatCrossNewlineBoundaries() {
        val source = "Alpha\nBETA"
        val input = file("newline.txt", source.toByteArray(Charsets.UTF_8))

        val result = TextFileSearchEngine.search(input, "a\nbe", bufferSize = 3)

        assertEquals(1, result.matchCount)
        assertEquals(4L, result.matches.single().byteOffset)
    }

    @Test
    fun boundsStoredOffsetsButCountsEveryMatch() {
        val source = "hit hit hit hit"
        val input = file("many.txt", source.toByteArray())

        val result = TextFileSearchEngine.search(input, "hit", maxMatches = 2, bufferSize = 5)

        assertEquals(4, result.matchCount)
        assertEquals(listOf(0L, 4L), result.matches.map { it.byteOffset })
        assertTrue(result.truncated)
    }

    @Test
    fun calculatesUtf8OffsetsForUnicodeText() {
        val source = "x café 😊 and CAFÉ"
        val input = file("unicode.txt", source.toByteArray(Charsets.UTF_8))
        val expected = listOf(
            source.indexOf("café").let { source.substring(0, it).toByteArray(Charsets.UTF_8).size.toLong() },
            source.indexOf("CAFÉ").let { source.substring(0, it).toByteArray(Charsets.UTF_8).size.toLong() }
        )

        val result = TextFileSearchEngine.search(input, "café", bufferSize = 4)

        assertEquals(2, result.matchCount)
        assertEquals(expected, result.matches.map { it.byteOffset })
    }

    @Test
    fun calculatesUtf16OffsetsIncludingBom() {
        val charset = Charset.forName("UTF-16LE")
        val input = file("utf16.txt", byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "Alpha\nBETA".toByteArray(charset))

        val result = TextFileSearchEngine.search(input, "beta", charset = charset, bufferSize = 3)

        assertEquals(1, result.matchCount)
        assertEquals(14L, result.matches.single().byteOffset)
    }

    @Test(expected = java.util.concurrent.CancellationException::class)
    fun allowsCallerToCancelALongScan() {
        val input = file("cancel.txt", "word ".repeat(100).toByteArray())
        TextFileSearchEngine.search(input, "word", bufferSize = 4, shouldContinue = { false })
    }

    @Test
    fun emptyQueryReturnsNoMatchesAndOverlappingHitsAreNotDuplicated() {
        val input = file("overlap.txt", "banana".toByteArray())

        assertEquals(0, TextFileSearchEngine.search(input, "  ").matchCount)
        assertEquals(1, TextFileSearchEngine.search(input, "ana", bufferSize = 2).matchCount)
    }
}
