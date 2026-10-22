package com.example.core.io

import com.example.core.security.PathGuard
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Pure Kotlin batch file operations engine.
 * Thread-safe, non-destructive safety checks, recursive directory handling,
 * and robust error collection.
 */
object BatchFileOperations {

    enum class BatchAction {
        COPY,
        MOVE,
        DELETE,
        TRASH
    }

    data class BatchOperationResult(
        val action: BatchAction,
        val totalCount: Int,
        val successfulCount: Int,
        val failedCount: Int,
        val errors: List<String>,
        val affectedFiles: List<File>
    ) {
        val isAllSuccessful: Boolean get() = failedCount == 0 && totalCount > 0
        val isPartialFailure: Boolean get() = failedCount > 0 && successfulCount > 0
    }

    /**
     * Deletes multiple files or directories recursively.
     */
    fun deleteFiles(files: List<File>): BatchOperationResult {
        if (files.isEmpty()) {
            return BatchOperationResult(BatchAction.DELETE, 0, 0, 0, emptyList(), emptyList())
        }

        val errors = mutableListOf<String>()
        val deleted = mutableListOf<File>()

        for (file in files) {
            try {
                if (!file.exists()) {
                    errors.add("${file.name}: File does not exist")
                    continue
                }

                val success = deleteRecursively(file)
                if (success) {
                    deleted.add(file)
                } else {
                    errors.add("${file.name}: Could not be deleted")
                }
            } catch (e: Exception) {
                errors.add("${file.name}: ${e.message ?: "Failed to delete"}")
            }
        }

        return BatchOperationResult(
            action = BatchAction.DELETE,
            totalCount = files.size,
            successfulCount = deleted.size,
            failedCount = files.size - deleted.size,
            errors = errors,
            affectedFiles = deleted
        )
    }

    /**
     * Copies multiple files or directories into [destinationDir].
     * Avoids recursive directory copying into itself, uses unique file names to avoid
     * accidental data loss unless [overwrite] is true.
     */
    fun copyFiles(
        files: List<File>,
        destinationDir: File,
        overwrite: Boolean = false
    ): BatchOperationResult {
        if (files.isEmpty()) {
            return BatchOperationResult(BatchAction.COPY, 0, 0, 0, emptyList(), emptyList())
        }

        val errors = mutableListOf<String>()
        val copied = mutableListOf<File>()

        if (!destinationDir.exists() || !destinationDir.isDirectory) {
            return BatchOperationResult(
                action = BatchAction.COPY,
                totalCount = files.size,
                successfulCount = 0,
                failedCount = files.size,
                errors = listOf("Destination directory does not exist or is not a folder: ${destinationDir.path}"),
                affectedFiles = emptyList()
            )
        }

        for (file in files) {
            try {
                if (!file.exists()) {
                    errors.add("${file.name}: Source file does not exist")
                    continue
                }

                if (file.isDirectory && PathGuard.isInside(file, destinationDir)) {
                    errors.add("${file.name}: Cannot copy a directory into itself or its subdirectory")
                    continue
                }

                val targetFile = if (overwrite) {
                    File(destinationDir, file.name)
                } else {
                    PathGuard.uniqueFile(destinationDir, file.name)
                }

                if (file.isDirectory) {
                    copyDirectoryRecursively(file, targetFile)
                } else {
                    copySingleFile(file, targetFile)
                }
                copied.add(targetFile)
            } catch (e: Exception) {
                errors.add("${file.name}: ${e.message ?: "Failed to copy"}")
            }
        }

        return BatchOperationResult(
            action = BatchAction.COPY,
            totalCount = files.size,
            successfulCount = copied.size,
            failedCount = files.size - copied.size,
            errors = errors,
            affectedFiles = copied
        )
    }

    /**
     * Moves multiple files or directories into [destinationDir].
     */
    fun moveFiles(
        files: List<File>,
        destinationDir: File,
        overwrite: Boolean = false
    ): BatchOperationResult {
        if (files.isEmpty()) {
            return BatchOperationResult(BatchAction.MOVE, 0, 0, 0, emptyList(), emptyList())
        }

        val errors = mutableListOf<String>()
        val moved = mutableListOf<File>()

        if (!destinationDir.exists() || !destinationDir.isDirectory) {
            return BatchOperationResult(
                action = BatchAction.MOVE,
                totalCount = files.size,
                successfulCount = 0,
                failedCount = files.size,
                errors = listOf("Destination directory does not exist: ${destinationDir.path}"),
                affectedFiles = emptyList()
            )
        }

        for (file in files) {
            try {
                if (!file.exists()) {
                    errors.add("${file.name}: Source file does not exist")
                    continue
                }

                // If source and destination parent are identical and no overwrite renaming
                if (file.parentFile?.canonicalPath == destinationDir.canonicalPath) {
                    errors.add("${file.name}: Already in the destination directory")
                    continue
                }

                if (file.isDirectory && PathGuard.isInside(file, destinationDir)) {
                    errors.add("${file.name}: Cannot move a directory into its own subdirectory")
                    continue
                }

                val targetFile = if (overwrite) {
                    File(destinationDir, file.name)
                } else {
                    PathGuard.uniqueFile(destinationDir, file.name)
                }

                // Try atomic move first
                var moveSuccess = false
                try {
                    Files.move(
                        file.toPath(),
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                    moveSuccess = true
                } catch (e: Exception) {
                    // Fall back to copy + recursive delete (e.g. across mount points)
                    if (file.isDirectory) {
                        copyDirectoryRecursively(file, targetFile)
                        deleteRecursively(file)
                        moveSuccess = true
                    } else {
                        copySingleFile(file, targetFile)
                        file.delete()
                        moveSuccess = true
                    }
                }

                if (moveSuccess) {
                    moved.add(targetFile)
                } else {
                    errors.add("${file.name}: Move operation failed")
                }
            } catch (e: Exception) {
                errors.add("${file.name}: ${e.message ?: "Failed to move"}")
            }
        }

        return BatchOperationResult(
            action = BatchAction.MOVE,
            totalCount = files.size,
            successfulCount = moved.size,
            failedCount = files.size - moved.size,
            errors = errors,
            affectedFiles = moved
        )
    }

    /**
     * Moves files into [trashDir] for soft deletion, allowing easy recovery.
     */
    fun moveToTrash(files: List<File>, trashDir: File): BatchOperationResult {
        if (files.isEmpty()) {
            return BatchOperationResult(BatchAction.TRASH, 0, 0, 0, emptyList(), emptyList())
        }

        if (!trashDir.exists()) {
            trashDir.mkdirs()
        }

        val errors = mutableListOf<String>()
        val trashed = mutableListOf<File>()

        for (file in files) {
            try {
                if (!file.exists()) {
                    errors.add("${file.name}: Source file does not exist")
                    continue
                }

                if (file.isDirectory && PathGuard.isInside(file, trashDir)) {
                    errors.add("${file.name}: Cannot trash the trash directory itself")
                    continue
                }

                val targetFile = PathGuard.uniqueFile(trashDir, file.name)
                val success = file.renameTo(targetFile) || run {
                    if (file.isDirectory) copyDirectoryRecursively(file, targetFile)
                    else copySingleFile(file, targetFile)
                    deleteRecursively(file)
                }

                if (success) {
                    trashed.add(targetFile)
                } else {
                    errors.add("${file.name}: Could not move to trash")
                }
            } catch (e: Exception) {
                errors.add("${file.name}: ${e.message ?: "Failed to trash"}")
            }
        }

        return BatchOperationResult(
            action = BatchAction.TRASH,
            totalCount = files.size,
            successfulCount = trashed.size,
            failedCount = files.size - trashed.size,
            errors = errors,
            affectedFiles = trashed
        )
    }

    private fun copySingleFile(source: File, target: File) {
        target.parentFile?.mkdirs()
        source.inputStream().buffered(64 * 1024).use { input ->
            target.outputStream().buffered(64 * 1024).use { output ->
                input.copyTo(output)
            }
        }
        target.setLastModified(source.lastModified())
    }

    private fun copyDirectoryRecursively(sourceDir: File, targetDir: File) {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        val items = sourceDir.listFiles() ?: return
        for (item in items) {
            val destChild = File(targetDir, item.name)
            if (item.isDirectory) {
                copyDirectoryRecursively(item, destChild)
            } else {
                copySingleFile(item, destChild)
            }
        }
    }

    private fun deleteRecursively(file: File): Boolean {
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children != null) {
                for (child in children) {
                    deleteRecursively(child)
                }
            }
        }
        return file.delete()
    }
}
