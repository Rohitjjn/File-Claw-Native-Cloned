package com.example.core.format

import com.example.data.RecentFileEntity
import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatHandler
import com.example.domain.preview.FormatOpenRequest
import com.example.formats.registry.DefaultFormatHandlers
import com.example.formats.registry.open
import com.example.formats.registry.capabilities
import com.example.platform.storage.FileManager
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatRegistryRoutingTest {
    @Test
    fun defaultHandlersCoverEveryExistingFormatMetadataId() {
        installDefaultHandlers()

        assertEquals(FormatRegistry.all.map { it.id }.toSet(), FormatRegistry.registeredHandlerIds)
    }

    @Test
    fun routesOpeningByTheExistingDetectionId() = runBlocking {
        installDefaultHandlers()
        val detection = Detection(FormatRegistry.byId("pdf")!!, Confidence.HIGH, "test")

        val result = FormatRegistry.open(
            FormatOpenRequest(
                file = File("sample.pdf"),
                fileEntity = RecentFileEntity(
                    path = "sample.pdf",
                    name = "sample.pdf",
                    size = 0,
                    extension = "pdf",
                    lastOpened = 0
                ),
                detection = detection
            )
        )

        assertTrue(result is FileContentState.PdfSuccess)
    }

    @Test
    fun rejectsTwoHandlersForOneFormatId() {
        val failure = runCatching {
            FormatRegistry.registerHandlers(listOf(FakeFormatHandler("pdf"), FakeFormatHandler("PDF")))
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    private fun installDefaultHandlers() {
        FormatRegistry.registerHandlers(DefaultFormatHandlers.create(FileManager()))
    }

    private class FakeFormatHandler(override val formatIds: Set<String>) : FormatHandler {
        constructor(formatId: String) : this(setOf(formatId))

        override fun capabilities(formatId: String) = capabilities(FormatCapability.PREVIEW)

        override suspend fun open(request: FormatOpenRequest): FileContentState =
            FileContentState.Error("${request.detection.spec.id} handler")
    }
}
