package com.example.formats.ooxml

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class OoxmlPackageInspectorTest {
    private lateinit var dir: File

    @Before fun setUp() { dir = Files.createTempDirectory("ooxml").toFile() }
    @After fun tearDown() { dir.deleteRecursively() }

    private fun zip(name: String, entries: List<Pair<String, String>>): File {
        val file = File(dir, name)
        ZipOutputStream(FileOutputStream(file)).use { out ->
            for ((entryName, content) in entries) {
                out.putNextEntry(ZipEntry(entryName))
                out.write(content.toByteArray(Charsets.UTF_8))
                out.closeEntry()
            }
        }
        return file
    }

    private fun wordPackage(name: String = "word.docx", mainXml: String = "<w:document/>"): File = zip(
        name,
        listOf(
            "[Content_Types].xml" to """
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                </Types>
            """.trimIndent(),
            "_rels/.rels" to """
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                </Relationships>
            """.trimIndent(),
            "word/document.xml" to mainXml
        )
    )

    private fun excelPackage(name: String = "book.xlsx"): File = zip(
        name,
        listOf(
            "[Content_Types].xml" to """
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                </Types>
            """.trimIndent(),
            "xl/workbook.xml" to "<workbook/>"
        )
    )

    private fun presentationPackage(name: String = "deck.pptx"): File = zip(
        name,
        listOf(
            "[Content_Types].xml" to """
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
                </Types>
            """.trimIndent(),
            "ppt/presentation.xml" to "<p:presentation/>"
        )
    )

    @Test fun validWordExcelAndPowerPointFamiliesAreIdentified() {
        assertEquals(OoxmlPackageInspector.Family.WORD, OoxmlPackageInspector.inspect(wordPackage()).family)
        assertEquals(OoxmlPackageInspector.Family.EXCEL, OoxmlPackageInspector.inspect(excelPackage()).family)
        assertEquals(OoxmlPackageInspector.Family.POWERPOINT, OoxmlPackageInspector.inspect(presentationPackage()).family)
    }

    @Test fun standardPackageHasNoVariantNotice() {
        val result = OoxmlPackageInspector.inspect(wordPackage())
        assertEquals(OoxmlPackageInspector.Variant.STANDARD, result.variant)
        assertFalse(result.hasMacros)
        assertEquals(null, result.notice)
    }

    @Test fun macroTemplateAndShowVariantsAreRecognised() {
        assertEquals(
            OoxmlPackageInspector.Variant.MACRO_ENABLED,
            OoxmlPackageInspector.variantFor("application/vnd.ms-word.document.macroEnabled.main+xml", true)
        )
        assertEquals(
            OoxmlPackageInspector.Variant.TEMPLATE,
            OoxmlPackageInspector.variantFor("application/vnd.openxmlformats-officedocument.wordprocessingml.template.main+xml", false)
        )
        assertEquals(
            OoxmlPackageInspector.Variant.MACRO_TEMPLATE,
            OoxmlPackageInspector.variantFor("application/vnd.ms-powerpoint.template.macroEnabledTemplate.main+xml", true)
        )
        assertEquals(
            OoxmlPackageInspector.Variant.SHOW,
            OoxmlPackageInspector.variantFor("application/vnd.openxmlformats-officedocument.presentationml.slideshow.main+xml", false)
        )
    }

    @Test fun unsafeEntryNamesAreRejectedStatically() {
        assertTrue(OoxmlPackageInspector.isUnsafeEntryName("../evil.xml"))
        assertTrue(OoxmlPackageInspector.isUnsafeEntryName("word\\..\\evil.xml"))
        assertTrue(OoxmlPackageInspector.isUnsafeEntryName("/absolute.xml"))
        assertTrue(OoxmlPackageInspector.isUnsafeEntryName("C:/evil.xml"))
        assertTrue(OoxmlPackageInspector.isUnsafeEntryName("word/%2e%2e/evil.xml"))
        assertFalse(OoxmlPackageInspector.isUnsafeEntryName("word/document.xml"))
    }

    @Test fun pathTraversalPackageIsRejected() {
        val file = zip(
            "traversal.docx",
            listOf(
                "[Content_Types].xml" to "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>",
                "word/document.xml" to "<w:document/>",
                "../../evil.txt" to "escape"
            )
        )
        assertThrows(OoxmlPackageInspector.InvalidPackageException::class.java) {
            OoxmlPackageInspector.inspect(file)
        }
    }

    @Test fun doctypeAndEntityDeclarationsAreRejected() {
        val file = wordPackage(
            "xxe.docx",
            "<!DOCTYPE w:document [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><w:document>&xxe;</w:document>"
        )
        assertThrows(OoxmlPackageInspector.InvalidPackageException::class.java) {
            OoxmlPackageInspector.inspect(file)
        }
    }

    @Test fun externalHttpRelationshipIsReportedButAllowed() {
        val file = zip(
            "external.docx",
            listOf(
                "[Content_Types].xml" to "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>",
                "word/document.xml" to "<w:document/>",
                "word/_rels/document.xml.rels" to "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId2\" Target=\"https://example.invalid/\" TargetMode=\"External\"/></Relationships>"
            )
        )
        val result = OoxmlPackageInspector.inspect(file)
        assertEquals(1, result.externalRelationshipCount)
        assertTrue(result.notice?.contains("External links") == true)
    }

    @Test fun unsafeExternalRelationshipSchemeIsRejected() {
        val file = zip(
            "file-rel.docx",
            listOf(
                "[Content_Types].xml" to "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>",
                "word/document.xml" to "<w:document/>",
                "word/_rels/document.xml.rels" to "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId2\" Target=\"file:///etc/passwd\" TargetMode=\"External\"/></Relationships>"
            )
        )
        assertThrows(OoxmlPackageInspector.InvalidPackageException::class.java) {
            OoxmlPackageInspector.inspect(file)
        }
    }

    @Test fun compressionBombIsRejected() {
        val file = File(dir, "bomb.docx")
        ZipOutputStream(FileOutputStream(file)).use { out ->
            out.putNextEntry(ZipEntry("[Content_Types].xml"))
            out.write("<Types><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>".toByteArray())
            out.closeEntry()
            out.putNextEntry(ZipEntry("word/document.xml"))
            out.write("<w:document/>".toByteArray())
            out.closeEntry()
            out.putNextEntry(ZipEntry("word/media/huge.bin"))
            val zeros = ByteArray(64 * 1024)
            repeat(256) { out.write(zeros) }
            out.closeEntry()
        }
        assertThrows(OoxmlPackageInspector.InvalidPackageException::class.java) {
            OoxmlPackageInspector.inspect(file)
        }
    }

    @Test fun encryptedOle2WrapperIsRejected() {
        val file = File(dir, "encrypted.docx")
        file.writeBytes(
            byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A.toByte(), 0xE1.toByte()) +
                ByteArray(512)
        )
        assertThrows(OoxmlPackageInspector.InvalidPackageException::class.java) {
            OoxmlPackageInspector.inspect(file)
        }
    }

    @Test fun emptyAndMissingContentTypesAreRejected() {
        val empty = File(dir, "empty.xlsx").apply { writeBytes(ByteArray(0)) }
        assertThrows(OoxmlPackageInspector.InvalidPackageException::class.java) {
            OoxmlPackageInspector.inspect(empty)
        }
        val missing = zip("missing.docx", listOf("word/document.xml" to "<w:document/>"))
        assertThrows(OoxmlPackageInspector.InvalidPackageException::class.java) {
            OoxmlPackageInspector.inspect(missing)
        }
    }

    @Test fun contentCanContradictTheExtensionWithoutChangingTheRegistry() {
        val wordBytesWithExcelName = wordPackage("actually-word.xlsx")
        val inspected = OoxmlPackageInspector.inspect(wordBytesWithExcelName)
        assertEquals(OoxmlPackageInspector.Family.WORD, inspected.family)
        assertEquals(OoxmlPackageInspector.Family.EXCEL, OoxmlPackageInspector.expectedFamily("xlsx"))
    }
}
