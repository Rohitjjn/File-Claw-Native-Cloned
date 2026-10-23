package com.example.formats.model3d

import com.example.core.format.Support
import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatCapability
import com.example.domain.preview.FormatOpenRequest
import com.example.formats.binary.BinaryFormatHandler
import com.example.formats.registry.StaticFormatHandler
import com.example.formats.registry.capabilities

/** STL/OBJ parsing; other recognized 3D/CAD formats retain their identify-only hex view. */
class Model3dFormatHandler(
    private val binaryHandler: BinaryFormatHandler
) : StaticFormatHandler(
    formatIds = FORMAT_IDS,
    declaredCapabilities = FORMAT_IDS.associateWith { formatId ->
        if (formatId == "stl" || formatId == "obj") {
            capabilities(FormatCapability.PREVIEW)
        } else {
            capabilities(FormatCapability.PREVIEW, readOnlyReason = "This 3D format can only be identified.")
        }
    }
) {
    override suspend fun open(request: FormatOpenRequest): FileContentState {
        if (request.detection.spec.support != Support.FULL) {
            return binaryHandler.identifyOnly(
                request,
                "${request.detection.spec.displayName} models can't be viewed yet."
            )
        }
        val mesh = when (request.detection.spec.id) {
            "stl" -> MeshParser.parseStl(request.file)
            "obj" -> MeshParser.parseObj(request.file)
            else -> return binaryHandler.identifyOnly(
                request,
                "${request.detection.spec.displayName} models can't be viewed yet."
            )
        }
        return FileContentState.Model3dSuccess(mesh, request.fileEntity, request.detection.spec.id.uppercase())
    }

    companion object {
        val FORMAT_IDS = setOf("stl", "obj", "model-other")
    }
}
