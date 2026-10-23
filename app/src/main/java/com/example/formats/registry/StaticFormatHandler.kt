package com.example.formats.registry

import com.example.domain.preview.FormatCapabilities
import com.example.domain.preview.FormatHandler

/** Small base class for handlers whose advertised operations do not vary by file instance. */
abstract class StaticFormatHandler(
    final override val formatIds: Set<String>,
    private val declaredCapabilities: Map<String, FormatCapabilities>
) : FormatHandler {
    init {
        require(formatIds.isNotEmpty()) { "A format handler must own at least one format ID." }
        require(declaredCapabilities.keys.map { it.lowercase() }.toSet() == formatIds.map { it.lowercase() }.toSet()) {
            "Each format ID must have exactly one capability declaration."
        }
    }

    final override fun capabilities(formatId: String): FormatCapabilities =
        declaredCapabilities[formatId.lowercase()]
            ?: throw IllegalArgumentException("${this::class.simpleName} does not handle '$formatId'.")
}

internal fun capabilities(
    vararg supported: com.example.domain.preview.FormatCapability,
    readOnlyReason: String? = null
): FormatCapabilities = FormatCapabilities(supported.toSet(), readOnlyReason)
