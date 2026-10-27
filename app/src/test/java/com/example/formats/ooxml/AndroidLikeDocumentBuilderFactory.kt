package com.example.formats.ooxml

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/**
 * Test double that reproduces the factory-configuration behavior of Android's bundled
 * `org.apache.harmony.xml.parsers.DocumentBuilderFactoryImpl` (see AOSP libcore):
 *
 * - `setFeature`/`getFeature` support ONLY the namespaces and validation features and
 *   throw `ParserConfigurationException` for every other feature URL, including
 *   `javax.xml.XMLConstants/feature/secure-processing` and the Xerces URLs.
 * - `setXIncludeAware` is not overridden and the Android base class always throws
 *   `UnsupportedOperationException`.
 *
 * Parsing is delegated to the desktop builder (created with the override property
 * cleared, to avoid factory lookup recursion) so these tests isolate the factory
 * configuration semantics, which is exactly where the desktop JVM and Android diverge.
 * A production call that works here also works against Android's real parser: Android's
 * own KXML/Expat `DocumentBuilder` implements the same base `DocumentBuilder` contract
 * used by the delegated parse.
 */
class AndroidLikeDocumentBuilderFactory : DocumentBuilderFactory() {

    companion object {
        const val PROPERTY = "javax.xml.parsers.DocumentBuilderFactory"
        private const val NAMESPACES = "http://xml.org/sax/features/namespaces"
        private const val VALIDATION = "http://xml.org/sax/features/validation"

        fun install() {
            System.setProperty(PROPERTY, AndroidLikeDocumentBuilderFactory::class.java.name)
        }

        fun uninstall() {
            System.clearProperty(PROPERTY)
        }

        private fun createDesktopDelegate(): DocumentBuilderFactory {
            val saved = System.getProperty(PROPERTY)
            System.clearProperty(PROPERTY)
            try {
                return DocumentBuilderFactory.newInstance()
            } finally {
                if (saved != null) System.setProperty(PROPERTY, saved)
            }
        }
    }

    private val delegate: DocumentBuilderFactory = createDesktopDelegate()

    override fun getFeature(name: String): Boolean {
        if (name == null) throw NullPointerException("name == null")
        if (NAMESPACES == name) return delegate.isNamespaceAware
        if (VALIDATION == name) return delegate.isValidating
        throw ParserConfigurationException(name)
    }

    override fun setFeature(name: String, value: Boolean) {
        if (name == null) throw NullPointerException("name == null")
        if (NAMESPACES == name) {
            delegate.setNamespaceAware(value)
            return
        }
        if (VALIDATION == name) {
            delegate.setValidating(value)
            return
        }
        throw ParserConfigurationException(name)
    }

    override fun setXIncludeAware(state: Boolean) {
        throw UnsupportedOperationException(
            "This parser does not support specification \"" +
                this.javaClass.getPackage().specificationTitle +
                "\" version \"" + this.javaClass.getPackage().specificationVersion + "\""
        )
    }

    override fun isXIncludeAware(): Boolean = false

    override fun newDocumentBuilder(): DocumentBuilder = delegate.newDocumentBuilder()

    override fun getAttribute(name: String): Any = throw IllegalArgumentException(name)

    override fun setAttribute(name: String, value: Any?) = throw IllegalArgumentException(name)
}
