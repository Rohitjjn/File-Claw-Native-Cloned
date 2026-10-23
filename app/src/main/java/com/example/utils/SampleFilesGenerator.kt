package com.example.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.RecentFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SampleFilesGenerator {

    suspend fun generateAllSamples(context: Context): List<RecentFileEntity> = withContext(Dispatchers.IO) {
        val sampleDir = File(context.filesDir, "samples").apply { mkdirs() }
        val createdEntities = mutableListOf<RecentFileEntity>()

        // 1. Multi-page High-Formatting PDF
        val pdfFile = File(sampleDir, "benchmark_report.pdf")
        if (!pdfFile.exists()) {
            generatePdfSample(pdfFile)
        }
        createdEntities.add(
            RecentFileEntity(
                name = pdfFile.name,
                path = pdfFile.absolutePath,
                size = pdfFile.length(),
                lastOpened = System.currentTimeMillis(),
                extension = "pdf",
                isSample = true
            )
        )

        // 2. High-Formatting DOCX document
        val docxFile = File(sampleDir, "executive_summary.docx")
        if (!docxFile.exists()) {
            generateDocxSample(docxFile)
        }
        createdEntities.add(
            RecentFileEntity(
                name = docxFile.name,
                path = docxFile.absolutePath,
                size = docxFile.length(),
                lastOpened = System.currentTimeMillis(),
                extension = "docx",
                isSample = true
            )
        )

        // 3. Large Server Trace LOG (2000+ lines)
        val logFile = File(sampleDir, "production_trace.log")
        if (!logFile.exists()) {
            generateLogSample(logFile)
        }
        createdEntities.add(
            RecentFileEntity(
                name = logFile.name,
                path = logFile.absolutePath,
                size = logFile.length(),
                lastOpened = System.currentTimeMillis(),
                extension = "log",
                isSample = true
            )
        )

        // 4. Markdown Document with Tables, Code & Checklists
        val mdFile = File(sampleDir, "system_architecture.md")
        if (!mdFile.exists()) {
            generateMarkdownSample(mdFile)
        }
        createdEntities.add(
            RecentFileEntity(
                name = mdFile.name,
                path = mdFile.absolutePath,
                size = mdFile.length(),
                lastOpened = System.currentTimeMillis(),
                extension = "md",
                isSample = true
            )
        )

        // 5. Formatted Clean Notes TXT
        val txtFile = File(sampleDir, "engineering_handbook.txt")
        if (!txtFile.exists()) {
            generateTextSample(txtFile)
        }
        createdEntities.add(
            RecentFileEntity(
                name = txtFile.name,
                path = txtFile.absolutePath,
                size = txtFile.length(),
                lastOpened = System.currentTimeMillis(),
                extension = "txt",
                isSample = true
            )
        )

        createdEntities
    }

    private fun generatePdfSample(file: File) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val titlePaint = Paint().apply {
            color = Color.rgb(204, 120, 92) // Terracotta #CC785C
            textSize = 22f
            isFakeBoldText = true
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(20, 20, 19)
            textSize = 15f
            isFakeBoldText = true
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(50, 50, 48)
            textSize = 11f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.rgb(130, 130, 128)
            textSize = 9f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val boxPaint = Paint().apply {
            color = Color.rgb(245, 243, 237)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(218, 214, 204)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val totalPages = 5
        val sections = listOf(
            "Overview & Architecture",
            "Performance Benchmark Metrics",
            "Memory & Lifecycle Analysis",
            "Offline Document Pipeline",
            "Verification & Compliance Report"
        )

        for (pageIndex in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Decorative top header bar
            canvas.drawRect(40f, 40f, pageWidth - 40f, 44f, titlePaint)

            // Running Header
            canvas.drawText("FILE CLAW BENCHMARK & VERIFICATION SUITE", 40f, 62f, mutedPaint)
            canvas.drawText("CONFIDENTIAL // OFF-LINE AUDIT", pageWidth - 200f, 62f, mutedPaint)
            canvas.drawLine(40f, 70f, pageWidth - 40f, 70f, borderPaint)

            // Page Title
            val sectionTitle = sections[pageIndex - 1]
            canvas.drawText("Chapter $pageIndex: $sectionTitle", 40f, 105f, titlePaint)

            // Content Box
            canvas.drawRect(40f, 125f, pageWidth - 40f, 215f, boxPaint)
            canvas.drawRect(40f, 125f, pageWidth - 40f, 215f, borderPaint)

            canvas.drawText("Key Metric Summary", 55f, 150f, headerPaint)
            canvas.drawText("• Range-Request Streaming Latency: < 45 ms for 150 MB dataset", 55f, 172f, bodyPaint)
            canvas.drawText("• Focal-Point Pinch Zoom Anchoring Accuracy: 99.8% precision", 55f, 192f, bodyPaint)

            // Detailed body paragraphs
            var yPos = 245f
            canvas.drawText("Executive Notes on System Integrity", 40f, yPos, headerPaint)
            yPos += 20f

            val loremParagraphs = listOf(
                "This document demonstrates multi-page vector PDF rendering with authentic font metrics, structured line layouts,",
                "page headers, footers, tabular data cells, and bounding geometry. The File Claw PDF engine leverages PDF.js",
                "with an offline byte-range responder (HTTP 206 Partial Content) to stream only needed pages into memory.",
                "",
                "When rotating the device, the layout preserves the topmost visible page fraction using accelerometer-guided",
                "dual-sided landscape orientation, guaranteeing that no reading progress or transient scroll anchor is discarded.",
                "",
                "Testing with heavy documents proves resident canvas memory bounds remain strictly respected across rapid swipes."
            )

            for (line in loremParagraphs) {
                if (line.isEmpty()) {
                    yPos += 10f
                } else {
                    canvas.drawText(line, 40f, yPos, bodyPaint)
                    yPos += 16f
                }
            }

            // Tabular Grid
            yPos += 20f
            canvas.drawText("Component Performance Matrix", 40f, yPos, headerPaint)
            yPos += 15f

            val tableTop = yPos
            val rowHeight = 24f
            val col1 = 40f
            val col2 = 200f
            val col3 = 360f
            val colRight = pageWidth - 40f

            for (r in 0..4) {
                val rowY = tableTop + (r * rowHeight)
                if (r % 2 == 1) {
                    canvas.drawRect(col1, rowY, colRight, rowY + rowHeight, boxPaint)
                }
                canvas.drawLine(col1, rowY, colRight, rowY, borderPaint)

                when (r) {
                    0 -> {
                        canvas.drawText("Format", col1 + 10f, rowY + 16f, headerPaint)
                        canvas.drawText("Engine", col2 + 10f, rowY + 16f, headerPaint)
                        canvas.drawText("Memory Target", col3 + 10f, rowY + 16f, headerPaint)
                    }
                    1 -> {
                        canvas.drawText("PDF (150 MB)", col1 + 10f, rowY + 16f, bodyPaint)
                        canvas.drawText("PDF.js + HTTP 206", col2 + 10f, rowY + 16f, bodyPaint)
                        canvas.drawText("< 32 MB resident", col3 + 10f, rowY + 16f, bodyPaint)
                    }
                    2 -> {
                        canvas.drawText("DOCX (50 MB)", col1 + 10f, rowY + 16f, bodyPaint)
                        canvas.drawText("docx-preview + Vector Print", col2 + 10f, rowY + 16f, bodyPaint)
                        canvas.drawText("< 40 MB resident", col3 + 10f, rowY + 16f, bodyPaint)
                    }
                    3 -> {
                        canvas.drawText("LOG (500 MB)", col1 + 10f, rowY + 16f, bodyPaint)
                        canvas.drawText("TextWindowReader", col2 + 10f, rowY + 16f, bodyPaint)
                        canvas.drawText("< 16 MB resident", col3 + 10f, rowY + 16f, bodyPaint)
                    }
                }
            }
            canvas.drawLine(col1, tableTop + (4 * rowHeight), colRight, tableTop + (4 * rowHeight), borderPaint)

            // Running Footer
            canvas.drawLine(40f, pageHeight - 50f, pageWidth - 40f, pageHeight - 50f, borderPaint)
            canvas.drawText("File Claw Offline Document Core v1.1.0", 40f, pageHeight - 34f, mutedPaint)
            canvas.drawText("Page $pageIndex of $totalPages", pageWidth - 100f, pageHeight - 34f, mutedPaint)

            document.finishPage(page)
        }

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun generateDocxSample(file: File) {
        val documentXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p>
                  <w:pPr><w:jc w:val="center"/></w:pPr>
                  <w:r><w:rPr><w:b/><w:sz w:val="48"/><w:color w:val="CC785C"/></w:rPr><w:t>EXECUTIVE SPECIFICATION REPORT</w:t></w:r>
                </w:p>
                <w:p>
                  <w:pPr><w:jc w:val="center"/></w:pPr>
                  <w:r><w:rPr><w:i/><w:sz w:val="24"/><w:color w:val="706E6B"/></w:rPr><w:t>Modern OOXML Document Engine Validation — File Claw</w:t></w:r>
                </w:p>
                <w:p/>
                <w:p>
                  <w:r><w:rPr><w:b/><w:sz w:val="32"/><w:color w:val="141413"/></w:rPr><w:t>1. Architectural Intent</w:t></w:r>
                </w:p>
                <w:p>
                  <w:r><w:t>This DOCX document confirms accurate rich text parsing including font hierarchies, bold/italic variations, color accents, tables, and page layout. The preview engine uses a fit-to-width viewport baseline with focal-point anchored pinch zoom and seamless double-tap scaling.</w:t></w:r>
                </w:p>
                <w:p/>
                <w:p>
                  <w:r><w:rPr><w:b/><w:sz w:val="32"/><w:color w:val="141413"/></w:rPr><w:t>2. Core Feature Matrix</w:t></w:r>
                </w:p>
                <w:tbl>
                  <w:tblPr>
                    <w:tblW w:w="9000" w:type="dxa"/>
                    <w:tblBorders>
                      <w:top w:val="single" w:sz="6" w:space="0" w:color="DAD6CC"/>
                      <w:left w:val="none"/>
                      <w:bottom w:val="single" w:sz="6" w:space="0" w:color="DAD6CC"/>
                      <w:right w:val="none"/>
                      <w:insideH w:val="single" w:sz="4" w:space="0" w:color="ECE9DF"/>
                      <w:insideV w:val="none"/>
                    </w:tblBorders>
                  </w:tblPr>
                  <w:tr>
                    <w:tc><w:p><w:r><w:rPr><w:b/><w:color w:val="CC785C"/></w:rPr><w:t>Component</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:rPr><w:b/><w:color w:val="CC785C"/></w:rPr><w:t>Behavior</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:rPr><w:b/><w:color w:val="CC785C"/></w:rPr><w:t>Status</w:t></w:r></w:p></w:tc>
                  </w:tr>
                  <w:tr>
                    <w:tc><w:p><w:r><w:t>Focal Pinch Zoom</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:t>Two-finger anchored touch scaling with zero drift</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:rPr><w:b/><w:color w:val="2E7D32"/></w:rPr><w:t>ACTIVE</w:t></w:r></w:p></w:tc>
                  </w:tr>
                  <w:tr>
                    <w:tc><w:p><w:r><w:t>Print Engine</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:t>Authentic vector page layout via docx_editor?print=1</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:rPr><w:b/><w:color w:val="2E7D32"/></w:rPr><w:t>ACTIVE</w:t></w:r></w:p></w:tc>
                  </w:tr>
                  <w:tr>
                    <w:tc><w:p><w:r><w:t>Single/Double Tap</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:t>250ms debouncer prevents focus/zoom collision</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:rPr><w:b/><w:color w:val="2E7D32"/></w:rPr><w:t>ACTIVE</w:t></w:r></w:p></w:tc>
                  </w:tr>
                </w:tbl>
                <w:p/>
                <w:p>
                  <w:r><w:rPr><w:b/><w:sz w:val="32"/><w:color w:val="141413"/></w:rPr><w:t>3. Concluding Summary</w:t></w:r>
                </w:p>
                <w:p>
                  <w:r><w:t>All formatting rules comply with ISO/IEC 29500-1 Strict and Transitional OOXML schemas. The preview renders fully offline with zero internet access required.</w:t></w:r>
                </w:p>
              </w:body>
            </w:document>
        """.trimIndent()

        val contentTypes = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
        """.trimIndent()

        val packageRels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            </Relationships>
        """.trimIndent()

        FileOutputStream(file).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // [Content_Types].xml
                zos.putNextEntry(ZipEntry("[Content_Types].xml"))
                zos.write(contentTypes.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // _rels/.rels
                zos.putNextEntry(ZipEntry("_rels/.rels"))
                zos.write(packageRels.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // word/document.xml
                zos.putNextEntry(ZipEntry("word/document.xml"))
                zos.write(documentXml.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
    }

    private fun generateLogSample(file: File) {
        val stringBuilder = StringBuilder(120_000)
        val services = listOf("AuthService", "DocumentIndexer", "PdfStreamer", "CacheCleaner", "ThumbnailEngine", "PathGuard")
        val levels = listOf("INFO", "INFO", "INFO", "WARN", "DEBUG", "ERROR")

        stringBuilder.append("=== FILE CLAW CLUSTER TRANSACTION TRACE ===\n")
        stringBuilder.append("Start timestamp: 2026-10-22T00:00:00.000Z\n")
        stringBuilder.append("Node ID: claw-production-worker-09\n")
        stringBuilder.append("--------------------------------------------------------------------------------\n")

        var timestamp = 1729555200000L
        for (i in 1..2200) {
            timestamp += (10..450).random()
            val service = services.random()
            val level = levels.random()
            val traceId = "tx-%06x".format(i * 37)
            val line = when (level) {
                "INFO" -> "[$timestamp] [INFO ] [$service] [$traceId] Processed request chunk #${i} in ${(2..18).random()}ms (heap_usage: ${(24..48).random()}MB)"
                "DEBUG" -> "[$timestamp] [DEBUG] [$service] [$traceId] Byte-range read requested: bytes=${i * 1024}-${(i + 1) * 1024 - 1} from source"
                "WARN" -> "[$timestamp] [WARN ] [$service] [$traceId] Transient latency spike detected on thread pool worker ${(1..8).random()}: ${(80..150).random()}ms"
                "ERROR" -> "[$timestamp] [ERROR] [$service] [$traceId] Recovered from simulated disconnect: channel reset successfully, 0 frames lost"
                else -> "[$timestamp] [INFO ] [$service] [$traceId] Heartbeat check OK"
            }
            stringBuilder.append(line).append('\n')

            if (i % 250 == 0) {
                stringBuilder.append("--- CHECKPOINT ").append(i).append(" REACHED: ALL SYSTEM PIPELINES HEALTHY ---\n")
            }
        }

        stringBuilder.append("=== TRACE COMPLETE: 2200 TRANSACTIONS RECORDED ===\n")
        file.writeText(stringBuilder.toString(), Charsets.UTF_8)
    }

    private fun generateMarkdownSample(file: File) {
        val content = """
            # File Claw — High-Performance Offline Engine

            Welcome to the **File Claw** architectural overview and markdown showcase. This document demonstrates typography, formatting, tables, lists, and syntax-highlighted code blocks.

            ---

            ## 🛠 Key Capabilities

            - **100% Offline**: Zero external network calls or cloud dependencies.
            - **Byte-Range PDF Streaming**: Seamless rendering for 100+ MB PDFs via HTTP 206 Partial Content.
            - **Dual-Sided Sensor Landscape**: Fluid orientation switching powered by the device accelerometer.
            - **Focal-Point DOCX Pinch Zoom**: Multi-touch zoom anchored directly beneath user finger coordinates.

            ---

            ## 📊 Performance Benchmarks

            | Format | Engine | Memory Budget | Zoom Range |
            | :--- | :--- | :--- | :--- |
            | **PDF** | PDF.js + Canvas LRU | ≤ 32 MB | 0.5x – 8.0x |
            | **DOCX** | docx-preview + Vector Print | ≤ 40 MB | 0.5x – 4.0x |
            | **Markdown** | Prism Syntax Highlighter | ≤ 12 MB | 0.25x – 4.0x |
            | **Log / Text** | Continuous Streamer | ≤ 16 MB | 0.25x – 4.0x |

            ---

            ## 💻 Code Example: HTTP Range Responder

            ```kotlin
            class PdfRangeResponder(private val file: File) {
                fun respond(rangeHeader: String?): WebResourceResponse {
                    val range = HttpRange.parse(rangeHeader, file.length())
                    val stream = BoundedFileInputStream(file, range.start, range.length)
                    val headers = mapOf(
                        "Content-Range" to "bytes " + "${'$'}{range.start}-${'$'}{range.end}/${'$'}{file.length()}",
                        "Content-Length" to "${'$'}{range.length}"
                    )
                    return WebResourceResponse("application/pdf", null, 206, "Partial Content", headers, stream)
                }
            }
            ```

            ---

            ## ✅ Implementation Checklist

            - [x] Floating pill page scrubber with touch bubble
            - [x] 250ms debouncer resolving single-tap vs double-tap
            - [x] Rounded (12px) page canvas & shimmer skeleton
            - [x] Claude-style warm glass cards in Settings
            - [x] Native vector DOCX printing adapter
            - [x] Dual-sided sensor landscape rotation
        """.trimIndent()
        file.writeText(content, Charsets.UTF_8)
    }

    private fun generateTextSample(file: File) {
        val content = """
            ================================================================================
            FILE CLAW ENGINEERING HANDBOOK & FIELD REFERENCE
            Revision: 2026.10 // Build: Release
            ================================================================================

            1. PURPOSE
            --------------------------------------------------------------------------------
            File Claw is engineered to be an indestructible offline document viewer and editor
            for mobile computing environments. It adheres strictly to zero-data-loss and
            bounded-memory principles.

            2. CORE POLICIES
            --------------------------------------------------------------------------------
            * Security & Isolation:
              PathGuard strictly prevents path traversal vulnerabilities.
              No arbitrary external code execution or remote scripting is allowed.

            * Aesthetic Philosophy:
              Warm paper (#FAF9F5), Charcoal (#141413), Terracotta accents (#CC785C).
              Restrained glassmorphic surfaces with clear visual hierarchies.

            * Durability:
              All writes employ atomic temp-file replace patterns with sync to disk.
              Unsaved edits trigger immediate confirmation before file transition.

            3. MEMORY ALLOCATION TIERS
            --------------------------------------------------------------------------------
            Tier 1 (Low RAM <= 2GB):
              Canvas Budget: 4,000,000 pixels
              Cache Slots: 2 active pages

            Tier 2 (Standard RAM 4GB - 6GB):
              Canvas Budget: 12,000,000 pixels
              Cache Slots: 4 active pages

            Tier 3 (High RAM >= 8GB):
              Canvas Budget: 24,000,000 pixels
              Cache Slots: 8 active pages

            ================================================================================
            END OF HANDBOOK
            ================================================================================
        """.trimIndent()
        file.writeText(content, Charsets.UTF_8)
    }
}
