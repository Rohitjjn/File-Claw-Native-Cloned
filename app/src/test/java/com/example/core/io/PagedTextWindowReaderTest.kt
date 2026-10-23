package com.example.core.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PagedTextWindowReaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun readWindow_alignsToCleanNewlineBoundaries() {
        val file = tempFolder.newFile("sample.log")
        val lines = (1..500).map { "Line $it: The quick brown fox jumps over the lazy dog." }
        file.writeText(lines.joinToString("\n"))

        PagedTextWindowReader(file).use { reader ->
            // Request reading near byte 1000 with a small 2KB window
            val window = reader.readWindow(requestedOffset = 1000, windowSizeBytes = 2048)

            assertFalse("Middle window should not be start of file", window.isStartOfFile)
            assertTrue("Window should start with full line", window.text.startsWith("Line "))
            assertTrue("Window should end with full line", window.text.endsWith(".\n") || window.text.endsWith("."))
            assertTrue("Window text should not be empty", window.text.isNotEmpty())
            assertTrue("Should contain multiple lines", window.lineCount > 10)
        }
    }

    @Test
    fun readWindow_handlesStartAndEndOfFileCorrectly() {
        val file = tempFolder.newFile("short.txt")
        file.writeText("Line 1\nLine 2\nLine 3\n")

        PagedTextWindowReader(file).use { reader ->
            val window = reader.readWindow(requestedOffset = 0, windowSizeBytes = 1024)

            assertTrue(window.isStartOfFile)
            assertTrue(window.isEndOfFile)
            assertEquals("Line 1\nLine 2\nLine 3\n", window.text)
            assertEquals(3, window.lineCount)
        }
    }

    @Test
    fun estimateTotalLines_accuratelyEstimatesLineCount() {
        val file = tempFolder.newFile("counted.log")
        val content = (1..1000).joinToString("\n") { "Log entry at timestamp $it" }
        file.writeText(content)

        PagedTextWindowReader(file).use { reader ->
            val estimated = reader.estimateTotalLines()
            assertTrue("Estimated lines should be close to 1000, got $estimated", estimated in 900..1100)
        }
    }
}
