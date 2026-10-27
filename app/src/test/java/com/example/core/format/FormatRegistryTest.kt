package com.example.core.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatRegistryTest {

    @Test
    fun registryHasBroadCoverage() {
        assertTrue("extensions=${FormatRegistry.extensionCount}", FormatRegistry.extensionCount >= 350)
        assertTrue(FormatRegistry.fullSupportExtensionCount >= 250)
        assertTrue(FormatRegistry.all.all { it.extensions.isNotEmpty() })
    }

    @Test
    fun lookupIsCaseInsensitiveAndDotTolerant() {
        assertEquals("pdf", FormatRegistry.byExtension("PDF")?.id)
        assertEquals("pdf", FormatRegistry.byExtension(".pdf")?.id)
        assertNull(FormatRegistry.byExtension("definitely-not-real"))
    }

    @Test
    fun extensionOfHandlesCompoundAndEdgeCases() {
        assertEquals("tar.gz", FormatRegistry.extensionOf("backup.TAR.GZ"))
        assertEquals("tar.bz2", FormatRegistry.extensionOf("a.tar.bz2"))
        assertEquals("tar.xz", FormatRegistry.extensionOf("a.tar.xz"))
        assertEquals("gz", FormatRegistry.extensionOf("file.gz"))
        assertEquals("", FormatRegistry.extensionOf("Makefile"))
        assertEquals("", FormatRegistry.extensionOf("trailingdot."))
        assertEquals("gitignore", FormatRegistry.extensionOf(".gitignore"))
        assertEquals("txt", FormatRegistry.extensionOf("dir/sub/notes.txt"))
    }

    @Test
    fun keyFormatsMapToExpectedKinds() {
        assertEquals(FileKind.SQLITE, FormatRegistry.byExtension("sqlite3")?.kind)
        assertEquals(FileKind.EPUB, FormatRegistry.byExtension("epub")?.kind)
        assertEquals(FileKind.MODEL_3D, FormatRegistry.byExtension("stl")?.kind)
        assertEquals(FileKind.FONT, FormatRegistry.byExtension("otf")?.kind)
        assertEquals(FileKind.DELIMITED, FormatRegistry.byExtension("tsv")?.kind)
        assertEquals(FileKind.LEGACY_OFFICE, FormatRegistry.byExtension("xls")?.kind)
        assertEquals(Support.IDENTIFY, FormatRegistry.byExtension("rar")?.support)
        assertEquals(Support.PARTIAL, FormatRegistry.byId("ole2")?.support)
        assertEquals(FileKind.TEXT, FormatRegistry.byExtension("ts")?.kind)
    }

    @Test
    fun idsResolveAndFallbacksExist() {
        assertNotNull(FormatRegistry.byId("docx"))
        assertNull(FormatRegistry.byId("nope"))
        assertEquals(FileKind.BINARY, FormatRegistry.unknownBinary.kind)
        assertEquals(FileKind.TEXT, FormatRegistry.plainText.kind)
        assertFalse(FormatRegistry.all.isEmpty())
    }
}
