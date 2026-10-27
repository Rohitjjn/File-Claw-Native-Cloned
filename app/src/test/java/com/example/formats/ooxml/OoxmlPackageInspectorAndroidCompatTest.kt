package com.example.formats.ooxml

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Regression tests for the Android preview failure of valid Office packages.
 *
 * The production bug: `OoxmlPackageInspector` configured `DocumentBuilderFactory` with
 * desktop-JVM-only calls (`setXIncludeAware`, `FEATURE_SECURE_PROCESSING`, Xerces feature
 * URLs). Android's bundled parser throws `UnsupportedOperationException` and
 * `ParserConfigurationException` for those, the broad catch in `inspect()` turned that
 * into "This Office package is damaged or unsupported.", and every valid DOCX/XLSX/PPTX
 * failed on real devices while all desktop-JVM tests stayed green.
 *
 * These tests install [AndroidLikeDocumentBuilderFactory] as the JAXP factory so the
 * inspector is exercised exactly against Android's factory semantics, while the existing
 * [OoxmlPackageInspectorTest] keeps covering the desktop-JVM configuration.
 */
class OoxmlPackageInspectorAndroidCompatTest {
    private lateinit var dir: File

    @Before fun setUp() {
        dir = Files.createTempDirectory("ooxml-android").toFile()
        AndroidLikeDocumentBuilderFactory.install()
    }

    @After fun tearDown() {
        AndroidLikeDocumentBuilderFactory.uninstall()
        dir.deleteRecursively()
    }

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

    private fun contentTypes(mainPart: String, contentType: String): String =
        """
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Override PartName="/$mainPart" ContentType="$contentType"/>
            </Types>
        """.trimIndent()

    private fun wordPackage(name: String = "word.docx", documentXml: String = "<w:document/>"): File = zip(
        name,
        listOf(
            "[Content_Types].xml" to contentTypes(
                "word/document.xml",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"
            ),
            "_rels/.rels" to """
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                </Relationships>
            """.trimIndent(),
            "word/document.xml" to documentXml
        )
    )

    private fun excelPackage(name: String = "book.xlsx"): File = zip(
        name,
        listOf(
            "[Content_Types].xml" to contentTypes(
                "xl/workbook.xml",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"
            ),
            "xl/workbook.xml" to "<workbook/>"
        )
    )

    private fun presentationPackage(name: String = "deck.pptx"): File = zip(
        name,
        listOf(
            "[Content_Types].xml" to contentTypes(
                "ppt/presentation.xml",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"
            ),
            "ppt/presentation.xml" to "<p:presentation/>"
        )
    )

    @Test fun validWordExcelPowerPointAreAcceptedUnderAndroidParser() {
        // The core regression: this exact path failed on every Android device.
        assertEquals(
            OoxmlPackageInspector.Family.WORD,
            OoxmlPackageInspector.inspect(wordPackage()).family
        )
        assertEquals(
            OoxmlPackageInspector.Family.EXCEL,
            OoxmlPackageInspector.inspect(excelPackage()).family
        )
        assertEquals(
            OoxmlPackageInspector.Family.POWERPOINT,
            OoxmlPackageInspector.inspect(presentationPackage()).family
        )
    }

    @Test fun macrosAndTemplatesAreStillClassifiedUnderAndroidParser() {
        val macro = zip(
            "macro.docm",
            listOf(
                "[Content_Types].xml" to contentTypes(
                    "word/document.xml",
                    "application/vnd.ms-word.document.macroEnabled.main+xml"
                ),
                "word/vbaProject.bin" to "VBA",
                "word/document.xml" to "<w:document/>"
            )
        )
        val info = OoxmlPackageInspector.inspect(macro)
        assertEquals(OoxmlPackageInspector.Variant.MACRO_ENABLED, info.variant)
        assertTrue(info.hasMacros)
        assertTrue(info.notice?.contains("Macros") == true)
    }

    @Test fun doctypeHiddenBehindLargeCommentIsStillRejectedUnderAndroidParser() {
        // Android has no disallow-doctype-decl feature. The mandatory full-bytes scan in
        // OfficeXmlHardening must catch a DOCTYPE that sits beyond the 64 KB prefix
        // window used by the general XML part scan.
        val padding = "<!--" + "x".repeat(70 * 1024) + "-->"
        val hostile = padding + "<!DOCTYPE Types [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>" +
            contentTypes(
                "word/document.xml",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"
            )
        val file = zip(
            "late-doctype.docx",
            listOf("[Content_Types].xml" to hostile, "word/document.xml" to "<w:document/>")
        )
        val error = assertThrows(OoxmlPackageInspector.InvalidPackageException::class.java) {
            OoxmlPackageInspector.inspect(file)
        }
        assertEquals("This Office package contains unsafe XML declarations.", error.message)
    }

    @Test fun invalidXmlMetadataIsStillRejectedUnderAndroidParser() {
        val file = zip(
            "broken.docx",
            listOf(
                "[Content_Types].xml" to "<Types><Override PartName=/word/document.xml></Types>",
                "word/document.xml" to "<w:document/>"
            )
        )
        val error = assertThrows(OoxmlPackageInspector.InvalidPackageException::class.java) {
            OoxmlPackageInspector.inspect(file)
        }
        assertEquals("This Office package contains invalid XML metadata.", error.message)
    }

    @Test fun pathTraversalIsStillRejectedUnderAndroidParser() {
        val file = zip(
            "traversal.docx",
            listOf(
                "[Content_Types].xml" to contentTypes(
                    "word/document.xml",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"
                ),
                "word/document.xml" to "<w:document/>",
                "../../evil.txt" to "escape"
            )
        )
        assertThrows(OoxmlPackageInspector.InvalidPackageException::class.java) {
            OoxmlPackageInspector.inspect(file)
        }
    }

    @Test fun encryptedOle2WrapperIsStillRejectedUnderAndroidParser() {
        val file = File(dir, "encrypted.docx")
        file.writeBytes(
            byteArrayOf(
                0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte(),
                0xA1.toByte(), 0xB1.toByte(), 0x1A.toByte(), 0xE1.toByte()
            ) + ByteArray(512)
        )
        val error = assertThrows(OoxmlPackageInspector.InvalidPackageException::class.java) {
            OoxmlPackageInspector.inspect(file)
        }
        assertTrue(error.message?.contains("encrypted") == true)
    }

    @Test fun failureSinkReceivesStageAndTypeWithoutSensitiveData() {
        val captured = mutableListOf<OoxmlPackageInspector.OfficeInspectionFailure>()
        val previousSink = OoxmlPackageInspector.failureSink
        OoxmlPackageInspector.failureSink = { captured += it }
        try {
            // Unexpected-failure path: hostile archive -> generic message + classification.
            val truncated = File(dir, "truncated.docx")
            val full = wordPackage()
            // Keep only the first bytes: the ZIP central directory is cut off.
            full.inputStream().use { input ->
                FileOutputStream(truncated).use { output ->
                    val head = ByteArray(200)
                    val read = input.read(head)
                    output.write(head, 0, read)
                }
            }
            val unexpected = assertThrows(OoxmlPackageInspector.InvalidPackageException::class.java) {
                OoxmlPackageInspector.inspect(truncated)
            }
            assertEquals("This Office package is damaged or unsupported.", unexpected.message)

            // Expected-rejection path: fixed reason is reported for log diagnosis.
            val empty = File(dir, "empty.docx").apply { writeBytes(ByteArray(0)) }
            assertThrows(OoxmlPackageInspector.InvalidPackageException::class.java) {
                OoxmlPackageInspector.inspect(empty)
            }
        } finally {
            OoxmlPackageInspector.failureSink = previousSink
        }
        assertTrue(captured.isNotEmpty())
        val joined = captured.joinToString(" ") { "${it.stage} ${it.exceptionType} ${it.reason}" }
        // No temp path, temp directory or file name may leak into the diagnostic payload.
        assertTrue(!joined.contains(dir.absolutePath))
        assertTrue(!joined.contains(dir.name))
        assertTrue(!joined.contains("truncated"))
        // Classification identifies the failing stage and exception type.
        assertTrue(captured.any { it.stage == OoxmlPackageInspector.InspectionStage.PACKAGE_ACCESS })
        assertTrue(captured.all { it.exceptionType.isNotBlank() })
    }

    @Test fun hardeningReportReflectsPlatformCapabilities() {
        // Desktop JVM (Gradle unit tests, CI): every hardening feature applies.
        AndroidLikeDocumentBuilderFactory.uninstall()
        val (_, desktopReport) = OfficeXmlHardening.newSecureFactory()
        assertEquals(OfficeXmlHardening.OPTIONAL_FEATURES.size, desktopReport.applied.size)
        assertTrue(desktopReport.unsupported.isEmpty())
        assertNotNull(desktopReport.applied.firstOrNull { it.contains("secure-processing") })

        // Android-like parser: no Xerces feature URL is supported, so the report must
        // honestly show zero applied features while the byte-scan defense stays active.
        AndroidLikeDocumentBuilderFactory.install()
        val (_, androidReport) = OfficeXmlHardening.newSecureFactory()
        assertTrue(androidReport.applied.isEmpty())
        assertEquals(OfficeXmlHardening.OPTIONAL_FEATURES.size, androidReport.unsupported.size)
    }
}
