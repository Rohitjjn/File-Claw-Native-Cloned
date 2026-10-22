package com.example.core.archive

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ArchiveSafetyTest {

    @Test fun normalizeAcceptsRelativeAndCleansSegments() {
        assertEquals("a/b.txt", ArchivePaths.normalize("a/b.txt"))
        assertEquals("a/b.txt", ArchivePaths.normalize("a\\b.txt"))
        assertEquals("b.txt", ArchivePaths.normalize("./a/../b.txt"))
        assertEquals("a/b", ArchivePaths.normalize("a//b/"))
    }

    @Test fun normalizeRejectsZipSlipAndAbsolutePaths() {
        assertNull(ArchivePaths.normalize("../evil"))
        assertNull(ArchivePaths.normalize("a/../../evil"))
        assertNull(ArchivePaths.normalize("/etc/passwd"))
        assertNull(ArchivePaths.normalize("C:\\Windows\\x"))
        assertNull(ArchivePaths.normalize("a\u0000b"))
        assertNull(ArchivePaths.normalize(""))
        assertNull(ArchivePaths.normalize("./"))
        assertFalse(ArchivePaths.isSafe("..\\..\\x"))
        assertTrue(ArchivePaths.isSafe("ok/file"))
    }

    @Test fun limitsFlagOversizedAndBombRatios() {
        val l = ArchiveLimits(maxEntryBytes = 1000, maxRatio = 100.0, ratioFloorBytes = 10)
        assertNotNull(l.violation(1, 2000))
        assertNull(l.violation(50, 900))
        assertNotNull(l.violation(1, 500))      // ratio 500 > 100 and above floor
        assertNull(l.violation(10, 500))
        assertNull(l.violation(-1, 500))        // unknown compressed size is not judged
        assertNull(ArchiveLimits().violation(1000, 5000))
    }

    @Test fun limitedInputStreamThrowsRatherThanTruncating() {
        val data = ByteArray(100)
        val ok = LimitedInputStream(ByteArrayInputStream(data), 100)
        assertEquals(100, ok.readBytes().size)
        assertEquals(100L, ok.count)
        val bad = LimitedInputStream(ByteArrayInputStream(data), 99)
        try {
            bad.readBytes()
            fail("expected limit exception")
        } catch (e: ArchiveLimitExceededException) {
            assertTrue(e.message!!.contains("99"))
        }
        val single = LimitedInputStream(ByteArrayInputStream(byteArrayOf(1, 2)), 1)
        assertEquals(1, single.read())
        try { single.read(); fail("expected") } catch (e: ArchiveLimitExceededException) { /* ok */ }
    }

    @Test fun copyLimitedCopiesOrThrows() {
        val out = ByteArrayOutputStream()
        assertEquals(10L, copyLimited(ByteArrayInputStream(ByteArray(10) { 7 }), out, 10))
        assertEquals(10, out.size())
        try {
            copyLimited(ByteArrayInputStream(ByteArray(10)), ByteArrayOutputStream(), 9)
            fail("expected")
        } catch (e: ArchiveLimitExceededException) { /* ok */ }
    }

    @Test fun treeBuilderCreatesIntermediateDirsAndSorts() {
        val entries = sequenceOf(
            ArchiveEntryInfo("z.txt", false, 5),
            ArchiveEntryInfo("docs/readme.md", false, 10),
            ArchiveEntryInfo("docs/img/b.png", false, 20),
            ArchiveEntryInfo("docs/img/", true, 0),
            ArchiveEntryInfo("A.txt", false, 1),
            ArchiveEntryInfo("empty/", true, 0)
        )
        val tree = ArchiveTreeBuilder.build("test.zip", entries)
        assertEquals(6, tree.entryCount)
        assertEquals(36L, tree.totalSize)
        assertFalse(tree.truncated)
        val names = tree.root.children.map { it.name }
        assertEquals(listOf("docs", "empty", "A.txt", "z.txt"), names)
        val docs = tree.root.children[0]
        assertTrue(docs.isDirectory)
        assertEquals(listOf("img", "readme.md"), docs.children.map { it.name })
        assertEquals("docs/img/b.png", docs.children[0].children[0].path)
        assertEquals(20L, docs.children[0].children[0].size)
    }

    @Test fun treeBuilderMarksUnsafeEntriesAndNeverGivesThemRealPaths() {
        val tree = ArchiveTreeBuilder.build("x.zip", sequenceOf(
            ArchiveEntryInfo("../../etc/passwd", false, 3),
            ArchiveEntryInfo("good.txt", false, 1),
            ArchiveEntryInfo("\u0000", false, 1)
        ))
        val unsafe = tree.root.children.filter { it.unsafe }
        assertEquals(2, unsafe.size)
        assertTrue(unsafe.all { it.path.startsWith("!unsafe/") })
        assertTrue(tree.root.children.any { it.name == "good.txt" && !it.unsafe })
    }

    @Test fun treeBuilderEnforcesEntryCap() {
        val many = (1..50).asSequence().map { ArchiveEntryInfo("f$it.txt", false, 1) }
        val tree = ArchiveTreeBuilder.build("big", many, ArchiveLimits(maxEntries = 10))
        assertTrue(tree.truncated)
        assertEquals(10, tree.root.children.size)
    }

    @Test fun treeBuilderIsLinearOnLargeArchives() {
        val n = 100_000
        val seq = (0 until n).asSequence().map { ArchiveEntryInfo("d${it % 100}/sub${it % 7}/file$it.txt", false, 1) }
        val start = System.nanoTime()
        val tree = ArchiveTreeBuilder.build("huge.zip", seq, ArchiveLimits(maxEntries = 1_000_000))
        val ms = (System.nanoTime() - start) / 1_000_000
        assertEquals(n, tree.entryCount)
        assertTrue("took ${ms}ms", ms < 5_000)
    }

    @Test fun pathologicallyDeepEntriesAreFlaggedNotNested() {
        val deep = (1..200).joinToString("/") { "d" } + "/f.txt"
        val tree = ArchiveTreeBuilder.build("x", sequenceOf(ArchiveEntryInfo(deep, false, 1), ArchiveEntryInfo("ok/f.txt", false, 1)))
        assertEquals(2, tree.root.children.size)
        assertTrue(tree.root.children.any { it.unsafe && it.name == "f.txt" })
        var depth = 0
        var n = tree.root.children.first { !it.unsafe }
        while (n.children.isNotEmpty()) { n = n.children[0]; depth++ }
        assertTrue(depth < 5)
    }

    @Test fun sameFileNameInDifferentDirsAreDistinctNodes() {
        val tree = ArchiveTreeBuilder.build("x", sequenceOf(
            ArchiveEntryInfo("a/x.txt", false, 1), ArchiveEntryInfo("b/x.txt", false, 2)))
        assertEquals(2, tree.root.children.size)
        assertEquals("a/x.txt", tree.root.children[0].children[0].path)
        assertEquals("b/x.txt", tree.root.children[1].children[0].path)
    }
}
