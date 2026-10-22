package com.example.core.security

import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PathGuardTest {
    private lateinit var dir: File

    @Before fun setUp() { dir = Files.createTempDirectory("pg").toFile() }
    @After fun tearDown() { dir.deleteRecursively() }

    @Test fun sanitizeStripsSeparatorsControlsAndDots() {
        assertEquals("c.txt", PathGuard.sanitizeFileName("a/b\\c.txt"))
        assertEquals("passwd", PathGuard.sanitizeFileName("../../passwd"))
        assertEquals("fallback", PathGuard.sanitizeFileName("..", "fallback"))
        assertEquals("fallback", PathGuard.sanitizeFileName("", "fallback"))
        assertEquals("fallback", PathGuard.sanitizeFileName(null, "fallback"))
        assertEquals("fallback", PathGuard.sanitizeFileName("///", "fallback"))
        assertEquals("a_b.txt", PathGuard.sanitizeFileName("a\u0000b.txt"))
        assertEquals("report.pdf", PathGuard.sanitizeFileName("  report.pdf  "))
        assertEquals("a_b_.txt", PathGuard.sanitizeFileName("a:b*.txt"))
    }

    @Test fun sanitizeBoundsLengthKeepingExtension() {
        val s = PathGuard.sanitizeFileName("x".repeat(400) + ".pdf")
        assertEquals(120, s.length)
        assertTrue(s.endsWith(".pdf"))
        val noExt = PathGuard.sanitizeFileName("y".repeat(400))
        assertEquals(120, noExt.length)
    }

    @Test fun uniqueFileAddsCounter() {
        assertEquals("a.txt", PathGuard.uniqueFile(dir, "a.txt").name)
        File(dir, "a.txt").writeText("1")
        assertEquals("a (1).txt", PathGuard.uniqueFile(dir, "a.txt").name)
        File(dir, "a (1).txt").writeText("1")
        assertEquals("a (2).txt", PathGuard.uniqueFile(dir, "a.txt").name)
        File(dir, "noext").writeText("1")
        assertEquals("noext (1)", PathGuard.uniqueFile(dir, "noext").name)
    }

    @Test fun isInsideResolvesDotDotAndSymlinks() {
        val sub = File(dir, "sub").also { it.mkdirs() }
        assertTrue(PathGuard.isInside(dir, File(sub, "f.txt")))
        assertTrue(PathGuard.isInside(dir, dir))
        assertFalse(PathGuard.isInside(sub, File(sub, "../outside.txt")))
        assertFalse(PathGuard.isInside(sub, dir))
        val sibling = File(dir, "sub2").also { it.mkdirs() }
        assertFalse("prefix match must not count", PathGuard.isInside(sub, sibling))
        val link = File(sub, "link")
        try {
            Files.createSymbolicLink(link.toPath(), dir.toPath())
            assertFalse(PathGuard.isInside(sub, File(link, "x")))
        } catch (e: UnsupportedOperationException) {
            // platform without symlinks
        }
    }

    @Test fun cacheKeysAreStableAndDistinct() {
        assertEquals(PathGuard.cacheKey("/a/b"), PathGuard.cacheKey("/a/b"))
        assertFalse(PathGuard.cacheKey("/a/b") == PathGuard.cacheKey("/a/c"))
        assertEquals(24, PathGuard.cacheKey("x").length)
    }
}
