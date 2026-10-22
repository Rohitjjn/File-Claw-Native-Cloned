package com.example.utils

import android.content.ComponentCallbacks2
import android.content.Context
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owns the app's cache directories. Changes in 1.1.0:
 *  - no directory I/O at construction (it ran on the main thread during Application.onCreate),
 *  - removed the unused put/get preview/doc/thumb/index APIs and the entry-counting LruCache that
 *    was sized in KiB (it counted bitmaps, not bytes, so it never bounded memory),
 *  - cleanup deletes only stale files instead of everything, so an open archive entry survives,
 *  - the Coil disk cache is bounded by its own LRU and is no longer wiped on every cleanup.
 */
class CacheManager private constructor(private val context: Context) {

    companion object {
        @Volatile private var INSTANCE: CacheManager? = null

        fun getInstance(c: Context): CacheManager = INSTANCE ?: synchronized(this) {
            INSTANCE ?: CacheManager(c.applicationContext).also { INSTANCE = it }
        }

        const val IMG_MB = 250
        const val TOTAL_CACHE_MB = 2048
        private const val HOUR_MS = 60 * 60 * 1000L
        private const val DAY_MS = 24 * HOUR_MS
    }

    private val tempDir get() = File(context.cacheDir, "temp")
    private val zipDir get() = File(context.cacheDir, "zip_extracts")
    private val imgDir get() = File(context.cacheDir, "img_cache")
    private val indexFile get() = File(context.cacheDir, "device_files_cache.txt")

    /** Directories created by older versions that nothing reads any more. */
    private val legacyDirs
        get() = listOf(File(context.cacheDir, "previews"), File(context.cacheDir, "docs"), File(context.cacheDir, "file_cache"))

    fun setupCoil() {
        val imageLoader = ImageLoader.Builder(context)
            .memoryCache { MemoryCache.Builder(context).maxSizePercent(0.25).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(imgDir)
                    .maxSizeBytes(IMG_MB * 1024L * 1024L)
                    .build()
            }
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)
    }

    private fun sizeOf(dir: File): Long =
        if (dir.exists()) dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L

    private fun deleteOlderThan(dir: File, ageMs: Long) {
        val cutoff = System.currentTimeMillis() - ageMs
        dir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.deleteRecursively() }
    }

    suspend fun smartCleanup() = withContext(Dispatchers.IO) {
        // Stale scratch files are always safe to remove; recent ones may still be open in a viewer.
        deleteOlderThan(tempDir, HOUR_MS)
        deleteOlderThan(zipDir, DAY_MS)
        legacyDirs.forEach { it.deleteRecursively() }
        context.cacheDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("docx_media_") }?.forEach { it.deleteRecursively() }

        // Over budget: drop the oldest extracted entries first.
        val limit = TOTAL_CACHE_MB * 1024L * 1024L
        if (getTotalSizeBytes() > limit) {
            val files = zipDir.listFiles()?.sortedBy { it.lastModified() }.orEmpty()
            var size = getTotalSizeBytes()
            for (f in files) {
                if (size <= limit * 0.6) break
                size -= f.length()
                f.deleteRecursively()
            }
        }
    }

    fun clearTemp() {
        tempDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    fun onLowMemory() {
        clearTemp()
    }

    fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) clearTemp()
    }

    fun getTotalSizeBytes(): Long = sizeOf(tempDir) + sizeOf(zipDir) + sizeOf(imgDir) +
        (if (indexFile.exists()) indexFile.length() else 0L)

    fun getTotalSize(): String = formatSize(getTotalSizeBytes())

    fun getBreakdown(): Map<String, String> = mapOf(
        "Images" to formatSize(sizeOf(imgDir)),
        "Archives" to formatSize(sizeOf(zipDir)),
        "Temp" to formatSize(sizeOf(tempDir)),
        "Index" to formatSize(if (indexFile.exists()) indexFile.length() else 0L)
    )

    suspend fun nuke() = withContext(Dispatchers.IO) {
        tempDir.deleteRecursively()
        zipDir.deleteRecursively()
        imgDir.deleteRecursively()
        indexFile.delete()
        legacyDirs.forEach { it.deleteRecursively() }
        context.cacheDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("docx_media_") }?.forEach { it.deleteRecursively() }
    }

    private fun formatSize(bytes: Long): String = when {
        bytes >= 1024L * 1024 * 1024 -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024L * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
        bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
