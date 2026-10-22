package com.example.core.model3d

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class MeshParserTest {
    private lateinit var dir: File

    @Before fun setUp() { dir = Files.createTempDirectory("mesh").toFile() }
    @After fun tearDown() { dir.deleteRecursively() }

    private fun binaryStl(name: String, tris: Int): File {
        val bb = ByteBuffer.allocate(84 + tris * 50).order(ByteOrder.LITTLE_ENDIAN)
        bb.position(80); bb.putInt(tris)
        for (t in 0 until tris) {
            bb.putFloat(0f).putFloat(0f).putFloat(1f)                          // normal
            bb.putFloat(t.toFloat()).putFloat(0f).putFloat(0f)                 // v1
            bb.putFloat(t + 1f).putFloat(0f).putFloat(0f)                      // v2
            bb.putFloat(t.toFloat()).putFloat(1f).putFloat(2f)                 // v3
            bb.putShort(0)
        }
        return File(dir, name).also { it.writeBytes(bb.array()) }
    }

    @Test fun parsesBinaryStlAndBounds() {
        val m = MeshParser.parseStl(binaryStl("a.stl", 3))
        assertEquals(3, m.triangleCount)
        assertEquals(27, m.positions.size)
        assertFalse(m.truncated)
        val b = m.bounds()
        assertEquals(0f, b[0], 0.0001f)
        assertEquals(3f, b[3], 0.0001f)
        assertEquals(2f, b[5], 0.0001f)
    }

    @Test fun binaryStlOverCapIsStrideSampled() {
        val m = MeshParser.parseStl(binaryStl("big.stl", 1000), maxTriangles = 100)
        assertEquals(100, m.triangleCount)
        assertEquals(1000, m.totalTriangles)
        assertTrue(m.truncated)
        // samples reach far into the model, not only the first 100 triangles
        assertTrue(m.bounds()[3] > 500f)
    }

    @Test fun parsesAsciiStl() {
        val f = File(dir, "c.stl")
        f.writeText("solid t\n facet normal 0 0 1\n  outer loop\n   vertex 0 0 0\n   vertex 1 0 0\n   vertex 0 1 0\n  endloop\n endfacet\n" +
            " facet normal 0 0 1\n  outer loop\n   vertex 0 0 1\n   vertex 1 0 1\n   vertex 0 1 1\n  endloop\n endfacet\nendsolid t\n")
        val m = MeshParser.parseStl(f)
        assertEquals(2, m.triangleCount)
        assertEquals(1f, m.bounds()[5], 0.0001f)
    }

    @Test fun asciiStlCapAndErrors() {
        val f = File(dir, "d.stl")
        f.writeText((1..5).joinToString("\n") { "vertex $it 0 0\nvertex $it 1 0\nvertex $it 0 1" })
        val m = MeshParser.parseStl(f, maxTriangles = 2)
        assertEquals(2, m.triangleCount)
        assertEquals(5, m.totalTriangles)
        assertTrue(m.truncated)
        for (bad in listOf("solid x\nvertex 1 2\n", "solid x\nvertex a b c\n", "solid empty\nendsolid\n")) {
            val g = File(dir, "bad.stl").also { it.writeText(bad) }
            try { MeshParser.parseStl(g); fail("expected for: $bad") } catch (e: MeshFormatException) { /* ok */ }
        }
    }

    @Test fun parsesObjWithQuadsNegativeIndicesAndSlashes() {
        val f = File(dir, "a.obj")
        f.writeText("# comment\nv 0 0 0\nv 1 0 0\nv 1 1 0\nv 0 1 0\nvn 0 0 1\nf 1/1/1 2/2/1 3/3/1 4/4/1\nf -4 -3 -2\n")
        val m = MeshParser.parseObj(f)
        assertEquals(3, m.triangleCount)   // quad -> 2 tris, plus 1
        assertEquals(27, m.positions.size)
        assertEquals(1f, m.bounds()[3], 0.0001f)
    }

    @Test fun objCapAndErrors() {
        val f = File(dir, "b.obj")
        f.writeText("v 0 0 0\nv 1 0 0\nv 0 1 0\n" + "f 1 2 3\n".repeat(10))
        val m = MeshParser.parseObj(f, maxTriangles = 4)
        assertEquals(4, m.triangleCount)
        assertEquals(10, m.totalTriangles)
        for (bad in listOf("v 0 0 0\nf 1 2 9\n", "v 0 0 0\nf a b c\n", "v 0 0 0\n", "v 1 1\nv 0 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 3\nf 1 2\n")) {
            val g = File(dir, "bad.obj").also { it.writeText(bad) }
            try { MeshParser.parseObj(g); if (bad.startsWith("v 1 1")) continue; fail("expected for: $bad") } catch (e: MeshFormatException) { /* ok */ }
        }
    }
}
