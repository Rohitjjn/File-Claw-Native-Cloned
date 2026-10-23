package com.example.core.security

import java.io.File

object PathGuard {

    private const val MAX_NAME = 120

    /**
     * Turns an untrusted display name (from a content:// provider or an archive) into a safe single
     * path segment: no separators, control characters, or leading dots; bounded length; extension kept.
     */
    fun sanitizeFileName(raw: String?, fallback: String = "file"): String {
        // Keep only the last path segment: a display name must never carry directories.
        val base = (raw ?: "").substringAfterLast('/').substringAfterLast('\\')
        var s = base.replace(Regex("[\\u0000-\\u001F\\u007F:*?\"<>|]"), "_").trim()
        s = s.trim('.', ' ')
        if (s.isEmpty() || s.all { it == '_' }) return fallback
        if (s.length > MAX_NAME) {
            val dot = s.lastIndexOf('.')
            val ext = if (dot > 0 && s.length - dot <= 12) s.substring(dot) else ""
            s = s.substring(0, MAX_NAME - ext.length) + ext
        }
        return s
    }

    /** Returns a file in [dir] that does not exist yet: `name.ext`, `name (1).ext`, `name (2).ext` ... */
    fun uniqueFile(dir: File, name: String): File {
        var f = File(dir, name)
        if (!f.exists()) return f
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var i = 1
        while (f.exists() && i < 10_000) {
            f = File(dir, "$base ($i)$ext")
            i++
        }
        return f
    }

    /** True when [candidate] resolves (after symlinks and `..`) to a location inside [root]. */
    fun isInside(root: File, candidate: File): Boolean {
        return try {
            val r = root.canonicalFile.path
            val c = candidate.canonicalFile.path
            c == r || c.startsWith(if (r.endsWith(File.separator)) r else r + File.separator)
        } catch (e: java.io.IOException) {
            false
        }
    }

    /** Stable, collision-resistant cache key for a path (replaces `String.hashCode()` keys). */
    fun cacheKey(path: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256").digest(path.toByteArray(Charsets.UTF_8))
        return md.take(12).joinToString("") { "%02x".format(it) }
    }
}
