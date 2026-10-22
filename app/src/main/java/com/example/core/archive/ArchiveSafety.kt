package com.example.core.archive

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Thrown when an archive exceeds a safety limit (zip bomb, too many entries, oversized entry). */
class ArchiveLimitExceededException(message: String) : IOException(message)

data class ArchiveLimits(
    val maxEntries: Int = 200_000,
    val maxEntryBytes: Long = 2L * 1024 * 1024 * 1024,
    val maxTotalBytes: Long = 16L * 1024 * 1024 * 1024,
    /** Compression ratio above which an entry larger than [ratioFloorBytes] is treated as a bomb. */
    val maxRatio: Double = 2000.0,
    /** Entries nested deeper than this are shown flat and flagged, protecting recursive UI code. */
    val maxDepth: Int = 64,
    val ratioFloorBytes: Long = 4L * 1024 * 1024
) {
    /** Checks one entry's declared sizes. Returns a reason string when it must be refused. */
    fun violation(compressed: Long, uncompressed: Long): String? {
        if (uncompressed > maxEntryBytes) return "entry too large"
        if (compressed > 0 && uncompressed > ratioFloorBytes && uncompressed.toDouble() / compressed > maxRatio) {
            return "suspicious compression ratio"
        }
        return null
    }
}

/** Counts bytes and throws as soon as [limit] is exceeded; never silently truncates. */
class LimitedInputStream(input: InputStream, private val limit: Long) : FilterInputStream(input) {
    var count: Long = 0
        private set

    override fun read(): Int {
        val b = super.read()
        if (b >= 0) add(1)
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = super.read(b, off, len)
        if (n > 0) add(n.toLong())
        return n
    }

    private fun add(n: Long) {
        count += n
        if (count > limit) throw ArchiveLimitExceededException("data exceeds $limit bytes")
    }
}

object ArchivePaths {

    /**
     * Normalises an entry name to a relative '/'-separated path.
     * Returns null for names that could escape a destination directory (zip-slip):
     * absolute paths, drive letters, NUL bytes, or `..` segments that climb above the root.
     */
    fun normalize(raw: String): String? {
        if (raw.indexOf('\u0000') >= 0) return null
        val name = raw.replace('\\', '/')
        if (name.startsWith("/") || (name.length >= 2 && name[1] == ':' && name[0].isLetter())) return null
        val out = ArrayList<String>()
        for (seg in name.split('/')) {
            when (seg) {
                "", "." -> Unit
                ".." -> if (out.isEmpty()) return null else out.removeAt(out.size - 1)
                else -> out.add(seg)
            }
        }
        if (out.isEmpty()) return null
        return out.joinToString("/")
    }

    fun isSafe(raw: String): Boolean = normalize(raw) != null
}

/** Copies [input] to [out], refusing to write more than [limit] bytes. Returns bytes copied. */
fun copyLimited(input: InputStream, out: OutputStream, limit: Long): Long {
    val buf = ByteArray(32 * 1024)
    var total = 0L
    while (true) {
        val n = input.read(buf)
        if (n < 0) break
        total += n
        if (total > limit) throw ArchiveLimitExceededException("data exceeds $limit bytes")
        out.write(buf, 0, n)
    }
    return total
}

data class ArchiveEntryInfo(val path: String, val isDirectory: Boolean, val size: Long, val compressedSize: Long = -1)

class ArchiveNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    var size: Long = 0,
    /** True when the entry name was rejected by [ArchivePaths.normalize]; it is shown but never extracted. */
    val unsafe: Boolean = false
) {
    val children: MutableList<ArchiveNode> = ArrayList()
}

data class ArchiveTree(val root: ArchiveNode, val entryCount: Int, val totalSize: Long, val truncated: Boolean)

object ArchiveTreeBuilder {

    /** Builds a sorted tree in O(n log n) using a path index (the old builder scanned siblings: O(n^2)). */
    fun build(rootName: String, entries: Sequence<ArchiveEntryInfo>, limits: ArchiveLimits = ArchiveLimits()): ArchiveTree {
        val root = ArchiveNode(rootName, "", true)
        val index = HashMap<String, ArchiveNode>()
        var count = 0
        var total = 0L
        var truncated = false

        for (e in entries) {
            if (count >= limits.maxEntries) { truncated = true; break }
            count++
            val clean = ArchivePaths.normalize(e.path)
            if (clean == null || clean.count { it == '/' } >= limits.maxDepth) {
                val label = e.path.replace('\u0000', '?').replace('\\', '/').trim('/').ifEmpty { "(unnamed)" }
                val n = ArchiveNode(label.substringAfterLast('/'), "!unsafe/$count", false, e.size, unsafe = true)
                root.children.add(n)
                continue
            }
            val parts = clean.split('/')
            var parent = root
            var acc = ""
            for (i in parts.indices) {
                acc = if (acc.isEmpty()) parts[i] else "$acc/${parts[i]}"
                val last = i == parts.lastIndex
                val dir = !last || e.isDirectory
                var node = index[acc]
                if (node == null) {
                    node = ArchiveNode(parts[i], acc, dir, if (last && !dir) e.size else 0)
                    index[acc] = node
                    parent.children.add(node)
                    if (last && !dir) total += e.size.coerceAtLeast(0)
                }
                parent = node
            }
        }
        sort(root)
        return ArchiveTree(root, count, total, truncated)
    }

    private fun sort(root: ArchiveNode) {
        // Iterative to stay safe on pathological depth.
        val stack = ArrayDeque<ArchiveNode>()
        stack.addLast(root)
        val cmp = compareByDescending<ArchiveNode> { it.isDirectory }.thenBy { it.name.lowercase() }
        while (stack.isNotEmpty()) {
            val n = stack.removeLast()
            if (n.children.size > 1) n.children.sortWith(cmp)
            for (c in n.children) if (c.isDirectory && c.children.isNotEmpty()) stack.addLast(c)
        }
    }
}
