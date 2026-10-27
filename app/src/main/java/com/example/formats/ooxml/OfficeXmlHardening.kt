package com.example.formats.ooxml

import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Secure DOM parsing for the small OOXML metadata parts ([Content_Types].xml, *.rels)
 * that works on both the desktop JVM (Gradle unit tests, CI) and Android's bundled
 * Harmony XML parser.
 *
 * Root cause this file fixes: Android's `javax.xml.parsers.DocumentBuilderFactory` is not
 * Xerces. Its implementation supports only the `namespaces` and `validation` features:
 * `setXIncludeAware()` always throws `UnsupportedOperationException` on Android, and
 * `FEATURE_SECURE_PROCESSING` plus every Xerces-specific feature URL throw
 * `ParserConfigurationException` (verified against AOSP `libcore`,
 * `org/apache/harmony/xml/parsers/DocumentBuilderFactoryImpl.java` and the
 * `javax.xml.parsers.DocumentBuilderFactory` base class). Configuring the parser
 * unconditionally therefore rejected every valid Office package on real devices while
 * passing every desktop-JVM test. Features are now applied best-effort where the
 * platform supports them.
 *
 * Trust boundary preserved without parser features: the full bytes of every part parsed
 * here are scanned for `<!DOCTYPE`/`<!ENTITY` declarations (parser independent, no
 * 64 KB prolog gap) before parsing; entity references are disabled through the base
 * API; Android's KXML/Expat DOM does not resolve external entities, and on platforms
 * where the hardening features exist they are still applied on top.
 */
internal object OfficeXmlHardening {

    /** Hardening features attempted on every factory, most important first. */
    internal val OPTIONAL_FEATURES = listOf(
        XMLConstants.FEATURE_SECURE_PROCESSING to true,
        "http://apache.org/xml/features/disallow-doctype-decl" to true,
        "http://xml.org/sax/features/external-general-entities" to false,
        "http://xml.org/sax/features/external-parameter-entities" to false,
        "http://apache.org/xml/features/nonvalidating/load-external-dtd" to false
    )

    /**
     * Which hardening features a platform accepted. `unsupported` is populated when the
     * parser rejects a feature URL; callers must then rely on the mandatory byte-scan
     * defense in [parseSecureXml], which is platform independent.
     */
    data class HardeningReport(
        val applied: List<String>,
        val unsupported: List<String>
    )

    /**
     * Builds a hardened factory. Never throws for unsupported features: configuration
     * problems of the platform parser must not turn valid Office packages into preview
     * errors. Base settings (namespace-unaware DOM for these metadata parts, entity
     * reference expansion disabled) are always applied.
     */
    fun newSecureFactory(): Pair<DocumentBuilderFactory, HardeningReport> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isExpandEntityReferences = false
            // setXIncludeAware always throws UnsupportedOperationException on Android
            // (the base implementation is not overridden there); XInclude is off by
            // default on every platform, so a refusal must never abort parsing.
            runCatching { isXIncludeAware = false }
        }
        val applied = mutableListOf<String>()
        val unsupported = mutableListOf<String>()
        for ((feature, value) in OPTIONAL_FEATURES) {
            try {
                factory.setFeature(feature, value)
                applied += feature
            } catch (_: Exception) {
                // Android's Harmony parser only supports namespaces/validation and
                // throws ParserConfigurationException for every other feature URL.
                // The mandatory DOCTYPE/ENTITY byte scan below keeps the same
                // guarantee without the feature.
                unsupported += feature
            }
        }
        // setXIncludeAware throws UnsupportedOperationException on Android; the
        // XInclude state is off by default everywhere, so a refusal is harmless.
        return factory to HardeningReport(applied.toList(), unsupported.toList())
    }

    /**
     * Parser-independent rejection of DOCTYPE/ENTITY declarations over the complete
     * part bytes. This closes the >64 KB prolog gap that the prefix scan in
     * [OoxmlPackageInspector] cannot cover on platforms without the
     * disallow-doctype-decl feature, and it makes the rejection decision identical on
     * every platform.
     */
    fun containsDoctypeOrEntity(bytes: ByteArray): Boolean {
        val utf8Text = String(bytes, Charsets.UTF_8).uppercase()
        if (utf8Text.contains("<!DOCTYPE") || utf8Text.contains("<!ENTITY")) return true
        // UTF-16 XML has NULs between ASCII characters; strip them for the same
        // signature check used by the prefix scan without decoding the encoding.
        val zeroStripped = String(bytes, Charsets.ISO_8859_1)
            .replace("\u0000", "")
            .uppercase()
        return zeroStripped.contains("<!DOCTYPE") || zeroStripped.contains("<!ENTITY")
    }

    /**
     * Parses one bounded OOXML metadata part with the hardened configuration.
     * `bytes` must already be size-bounded by the caller (readPart caps).
     */
    fun parseSecureXml(bytes: ByteArray): Document {
        // Mandatory, parser-independent defense: reject DTD/entity declarations in the
        // exact bytes that are about to be parsed, on every platform.
        if (containsDoctypeOrEntity(bytes)) {
            throw OoxmlPackageInspector.InvalidPackageException(
                "This Office package contains unsafe XML declarations."
            )
        }
        val (factory, _) = newSecureFactory()
        return try {
            factory.newDocumentBuilder().parse(InputSource(ByteArrayInputStream(bytes)))
        } catch (_: Exception) {
            throw OoxmlPackageInspector.InvalidPackageException(
                "This Office package contains invalid XML metadata."
            )
        }
    }
}
