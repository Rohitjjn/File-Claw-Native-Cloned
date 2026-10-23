package com.example.core.model3d

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Triangle soup: [positions] holds x,y,z per vertex, three consecutive vertices per triangle. */
class Mesh(val positions: FloatArray, val triangleCount: Int, val totalTriangles: Int) {
    val truncated: Boolean get() = totalTriangles > triangleCount

    /** Axis-aligned bounds: minX, minY, minZ, maxX, maxY, maxZ. */
    fun bounds(): FloatArray {
        val b = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        var i = 0
        while (i + 2 < positions.size) {
            for (k in 0..2) {
                val v = positions[i + k]
                if (v < b[k]) b[k] = v
                if (v > b[k + 3]) b[k + 3] = v
            }
            i += 3
        }
        return b
    }
}

class MeshFormatException(message: String) : Exception(message)

object MeshParser {

    const val DEFAULT_MAX_TRIANGLES = 300_000

    fun parseStl(file: File, maxTriangles: Int = DEFAULT_MAX_TRIANGLES): Mesh {
        val len = file.length()
        if (len >= 84) {
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(80)
                val cnt = ByteArray(4)
                raf.readFully(cnt)
                val n = ByteBuffer.wrap(cnt).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
                if (n > 0 && len == 84L + n * 50L) return readBinaryStl(raf, n, maxTriangles)
            }
        }
        return parseAsciiStl(file, maxTriangles)
    }

    private fun readBinaryStl(raf: RandomAccessFile, n: Long, maxTriangles: Int): Mesh {
        val take = minOf(n, maxTriangles.toLong()).toInt()
        // Stride-sample when over the cap so the whole model stays visible.
        val stride = if (n > take) (n / take).coerceAtLeast(1) else 1L
        val pos = FloatArray(take * 9)
        val rec = ByteArray(50)
        var out = 0
        var t = 0L
        var kept = 0
        while (t < n && kept < take) {
            raf.seek(84 + t * 50)
            raf.readFully(rec)
            val bb = ByteBuffer.wrap(rec).order(ByteOrder.LITTLE_ENDIAN)
            bb.position(12)
            for (k in 0 until 9) pos[out++] = bb.float
            kept++
            t += stride
        }
        return Mesh(pos.copyOf(out), kept, n.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    }

    private fun parseAsciiStl(file: File, maxTriangles: Int): Mesh {
        val list = FloatList()
        var tris = 0
        var total = 0
        var vInTri = 0
        file.bufferedReader().useLines { lines ->
            for (raw in lines) {
                val line = raw.trim()
                if (!line.startsWith("vertex")) continue
                val p = line.split(Regex("\\s+"))
                if (p.size < 4) throw MeshFormatException("bad vertex line")
                val x = p[1].toFloatOrNull()
                val y = p[2].toFloatOrNull()
                val z = p[3].toFloatOrNull()
                if (x == null || y == null || z == null) throw MeshFormatException("bad vertex number")
                vInTri++
                if (tris < maxTriangles) { list.add(x); list.add(y); list.add(z) }
                if (vInTri == 3) {
                    vInTri = 0
                    total++
                    if (tris < maxTriangles) tris++
                }
            }
        }
        if (total == 0) throw MeshFormatException("no triangles found")
        return Mesh(list.toArray(), tris, total)
    }

    /** Wavefront OBJ: `v` vertices and `f` faces (n-gons are fan-triangulated; negative indices supported). */
    fun parseObj(file: File, maxTriangles: Int = DEFAULT_MAX_TRIANGLES): Mesh {
        val verts = FloatList()
        val out = FloatList()
        var tris = 0
        var total = 0
        file.bufferedReader().useLines { lines ->
            for (raw in lines) {
                if (raw.length < 2) continue
                if (raw.startsWith("v ")) {
                    val p = raw.trim().split(Regex("\\s+"))
                    if (p.size < 4) continue
                    verts.add(p[1].toFloatOrNull() ?: 0f)
                    verts.add(p[2].toFloatOrNull() ?: 0f)
                    verts.add(p[3].toFloatOrNull() ?: 0f)
                } else if (raw.startsWith("f ")) {
                    val p = raw.trim().split(Regex("\\s+")).drop(1)
                    if (p.size < 3) continue
                    val vc = verts.size / 3
                    val idx = IntArray(p.size) { k ->
                        val v = p[k].substringBefore('/').toIntOrNull() ?: throw MeshFormatException("bad face index")
                        if (v < 0) vc + v else v - 1
                    }
                    for (k in 1 until idx.size - 1) {
                        total++
                        if (tris >= maxTriangles) continue
                        val tri = intArrayOf(idx[0], idx[k], idx[k + 1])
                        if (tri.any { it < 0 || it >= vc }) throw MeshFormatException("face index out of range")
                        for (vi in tri) {
                            out.add(verts[vi * 3]); out.add(verts[vi * 3 + 1]); out.add(verts[vi * 3 + 2])
                        }
                        tris++
                    }
                }
            }
        }
        if (total == 0) throw MeshFormatException("no faces found")
        return Mesh(out.toArray(), tris, total)
    }

    private class FloatList {
        private var a = FloatArray(1024)
        var size = 0
            private set

        fun add(v: Float) {
            if (size == a.size) a = a.copyOf(a.size * 2)
            a[size++] = v
        }

        operator fun get(i: Int) = a[i]
        fun toArray(): FloatArray = a.copyOf(size)
    }
}
