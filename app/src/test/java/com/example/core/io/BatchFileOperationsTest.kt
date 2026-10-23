package com.example.core.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BatchFileOperationsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var sourceDir: File
    private lateinit var destDir: File

    @Before
    fun setUp() {
        sourceDir = tempFolder.newFolder("source")
        destDir = tempFolder.newFolder("destination")
    }

    @Test
    fun deleteFiles_deletesMultipleFilesAndDirectories() {
        val file1 = File(sourceDir, "doc1.txt").apply { writeText("Hello 1") }
        val file2 = File(sourceDir, "doc2.txt").apply { writeText("Hello 2") }
        val subDir = File(sourceDir, "sub").apply { mkdirs() }
        val subFile = File(subDir, "nested.txt").apply { writeText("Nested") }

        assertTrue(file1.exists())
        assertTrue(file2.exists())
        assertTrue(subFile.exists())

        val result = BatchFileOperations.deleteFiles(listOf(file1, file2, subDir))

        assertEquals(3, result.totalCount)
        assertEquals(3, result.successfulCount)
        assertEquals(0, result.failedCount)
        assertTrue(result.isAllSuccessful)

        assertFalse(file1.exists())
        assertFalse(file2.exists())
        assertFalse(subDir.exists())
    }

    @Test
    fun copyFiles_copiesMultipleFilesWithContentIntegrity() {
        val file1 = File(sourceDir, "alpha.txt").apply { writeText("Content Alpha") }
        val file2 = File(sourceDir, "beta.txt").apply { writeText("Content Beta") }

        val result = BatchFileOperations.copyFiles(listOf(file1, file2), destDir)

        assertEquals(2, result.totalCount)
        assertEquals(2, result.successfulCount)
        assertEquals(0, result.failedCount)

        val destFile1 = File(destDir, "alpha.txt")
        val destFile2 = File(destDir, "beta.txt")

        assertTrue(destFile1.exists())
        assertTrue(destFile2.exists())
        assertEquals("Content Alpha", destFile1.readText())
        assertEquals("Content Beta", destFile2.readText())
        // Originals must remain intact
        assertTrue(file1.exists())
        assertTrue(file2.exists())
    }

    @Test
    fun copyFiles_avoidsOverwriting_whenOverwriteIsFalse() {
        val file1 = File(sourceDir, "notes.txt").apply { writeText("New version") }
        File(destDir, "notes.txt").apply { writeText("Original version") }

        val result = BatchFileOperations.copyFiles(listOf(file1), destDir, overwrite = false)

        assertEquals(1, result.successfulCount)
        val copyUnique = File(destDir, "notes (1).txt")
        assertTrue(copyUnique.exists())
        assertEquals("New version", copyUnique.readText())
        // Original preserved
        assertEquals("Original version", File(destDir, "notes.txt").readText())
    }

    @Test
    fun copyFiles_preventsCopyingDirectoryIntoItself() {
        val dir = File(sourceDir, "parentDir").apply { mkdirs() }
        val childDest = File(dir, "childDir").apply { mkdirs() }

        val result = BatchFileOperations.copyFiles(listOf(dir), childDest)

        assertEquals(1, result.failedCount)
        assertEquals(0, result.successfulCount)
        assertTrue(result.errors.any { it.contains("Cannot copy a directory into itself") })
    }

    @Test
    fun moveFiles_movesMultipleFiles() {
        val file1 = File(sourceDir, "item1.log").apply { writeText("Log 1") }
        val file2 = File(sourceDir, "item2.log").apply { writeText("Log 2") }

        val result = BatchFileOperations.moveFiles(listOf(file1, file2), destDir)

        assertEquals(2, result.totalCount)
        assertEquals(2, result.successfulCount)
        assertEquals(0, result.failedCount)

        assertFalse(file1.exists())
        assertFalse(file2.exists())

        val moved1 = File(destDir, "item1.log")
        val moved2 = File(destDir, "item2.log")
        assertTrue(moved1.exists())
        assertTrue(moved2.exists())
        assertEquals("Log 1", moved1.readText())
        assertEquals("Log 2", moved2.readText())
    }

    @Test
    fun moveFiles_handlesDirectoriesRecursively() {
        val subDir = File(sourceDir, "folder").apply { mkdirs() }
        File(subDir, "inner.txt").apply { writeText("Inner Content") }

        val result = BatchFileOperations.moveFiles(listOf(subDir), destDir)

        assertEquals(1, result.successfulCount)
        assertFalse(subDir.exists())

        val targetDir = File(destDir, "folder")
        assertTrue(targetDir.exists())
        val targetNested = File(targetDir, "inner.txt")
        assertTrue(targetNested.exists())
        assertEquals("Inner Content", targetNested.readText())
    }

    @Test
    fun moveToTrash_safelyMovesFilesToTrashDirectory() {
        val trashDir = tempFolder.newFolder("trash")
        val file1 = File(sourceDir, "important.txt").apply { writeText("Important Data") }

        val result = BatchFileOperations.moveToTrash(listOf(file1), trashDir)

        assertEquals(1, result.successfulCount)
        assertEquals(0, result.failedCount)
        assertFalse(file1.exists())

        val trashedFiles = trashDir.listFiles()?.filter { it.name.endsWith("important.txt") }
        assertTrue("File should exist in trash folder", !trashedFiles.isNullOrEmpty())
        assertEquals("Important Data", trashedFiles!![0].readText())
    }
}

