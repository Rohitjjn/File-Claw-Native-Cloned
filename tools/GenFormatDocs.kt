import com.example.core.format.FormatRegistry
import com.example.core.format.Support

/** Prints the source-backed format inventory for docs/FILE_FORMATS.md. */
fun main() {
    val formats = FormatRegistry.all
    fun extensionCount(support: Support) = formats
        .filter { it.support == support }
        .sumOf { it.extensions.size }
    fun supportLabel(support: Support) = when (support) {
        Support.FULL -> "Full"
        Support.PARTIAL -> "Partial"
        Support.IDENTIFY -> "Identify only"
    }
    fun kindLabel(kind: String) = kind
        .lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }

    println("<!-- Inventory parsed from app/src/main/java/com/example/core/format/FormatRegistry.kt. Companion generator: tools/GenFormatDocs.kt; it is not wired to a Gradle task. -->")
    println()
    println("**${FormatRegistry.extensionCount} registered extensions across ${formats.size} format groups:** " +
        "${extensionCount(Support.FULL)} Full, ${extensionCount(Support.PARTIAL)} Partial, " +
        "${extensionCount(Support.IDENTIFY)} Identify only.")
    println()
    println("| ID | Format name | File kind | Metadata support | MIME type | Extensions |")
    println("|---|---|---|---|---|---|")
    for (format in formats) {
        val extensions = format.extensions.joinToString(", ") { "`$it`" }
        println("| `${format.id}` | ${format.displayName} | ${kindLabel(format.kind.name)} | " +
            "${supportLabel(format.support)} | `${format.mime}` | $extensions |")
    }
}
