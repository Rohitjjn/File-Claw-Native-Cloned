import com.example.formats.ooxml.OoxmlPackageInspector
import java.io.File
import java.time.Instant

private enum class Expectation { ACCEPT, REJECT }

private data class Case(
    val name: String,
    val expectation: Expectation,
    val family: String? = null,
    val variant: String? = null,
    val macros: Boolean? = null,
    val noticeContains: List<String> = emptyList(),
    val externalAtLeast: Int? = null,
)

private data class Result(val name: String, val passed: Boolean, val detail: String?)

private val CASES = listOf(
    Case("basic.docx", Expectation.ACCEPT, "WORD", "STANDARD", false),
    Case("rich.docx", Expectation.ACCEPT, "WORD", "STANDARD", false),
    Case("basic.docm", Expectation.ACCEPT, "WORD", "MACRO_ENABLED", true, listOf("Macros")),
    Case("basic.dotx", Expectation.ACCEPT, "WORD", "TEMPLATE", false, listOf("Template")),
    Case("poi/poi-60158.docm", Expectation.ACCEPT, "WORD", "MACRO_ENABLED", true, listOf("Macros")),
    Case("poi/poi-60316.dotx", Expectation.ACCEPT, "WORD", "TEMPLATE", false, listOf("Template")),
    Case("poi/poi-sample.docx", Expectation.ACCEPT, "WORD", "STANDARD", false),
    Case("basic.xlsx", Expectation.ACCEPT, "EXCEL", "STANDARD", false),
    Case("rich.xlsx", Expectation.ACCEPT, "EXCEL", "STANDARD", false),
    Case("large.xlsx", Expectation.ACCEPT, "EXCEL", "STANDARD", false),
    Case("basic.xlsm", Expectation.ACCEPT, "EXCEL", "MACRO_ENABLED", true, listOf("Macros")),
    Case("basic.xltx", Expectation.ACCEPT, "EXCEL", "TEMPLATE", false, listOf("Template")),
    Case("poi/poi-sample.xlsx", Expectation.ACCEPT, "EXCEL", "STANDARD", false),
    Case("poi/poi-suffix.xlsm", Expectation.ACCEPT, "EXCEL", "MACRO_ENABLED", true, listOf("Macros")),
    Case("basic.pptx", Expectation.ACCEPT, "POWERPOINT", "STANDARD", false),
    Case("rich.pptx", Expectation.ACCEPT, "POWERPOINT", "STANDARD", false),
    Case("basic.pptm", Expectation.ACCEPT, "POWERPOINT", "MACRO_ENABLED", true, listOf("Macros")),
    Case("basic.potx", Expectation.ACCEPT, "POWERPOINT", "TEMPLATE", false, listOf("Template")),
    Case("poi/poi-51187.pptx", Expectation.ACCEPT, "POWERPOINT", "STANDARD", false),
    Case("external-rel.docx", Expectation.ACCEPT, "WORD", "STANDARD", false, listOf("External links"), 1),
    Case("misleading.pdf", Expectation.ACCEPT, "WORD", "STANDARD", false),
    Case("adversarial.docx", Expectation.REJECT),
    Case("zip-bomb.docx", Expectation.REJECT),
    Case("truncated.docx", Expectation.REJECT),
    Case("corrupt.pptx", Expectation.REJECT),
    Case("empty.xlsx", Expectation.REJECT),
    Case("poi/poi-invalid.xlsx", Expectation.REJECT),
)

private fun jsonString(value: String): String = buildString {
    append('"')
    for (char in value) {
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (char.code < 0x20) {
                append("\\u")
                append(char.code.toString(16).padStart(4, '0'))
            } else {
                append(char)
            }
        }
    }
    append('"')
}

private fun writeReport(path: String, results: List<Result>) {
    val passed = results.count { it.passed }
    val failed = results.size - passed
    val body = buildString {
        append("{\n")
        append("  \"tool\": \"OoxmlPackageInspector JVM harness\",\n")
        append("  \"generatedAt\": ").append(jsonString(Instant.now().toString())).append(",\n")
        append("  \"java\": ").append(jsonString(System.getProperty("java.version") ?: "unknown")).append(",\n")
        append("  \"kotlin\": ").append(jsonString(KotlinVersion.CURRENT.toString())).append(",\n")
        append("  \"cases\": ").append(results.size).append(",\n")
        append("  \"passed\": ").append(passed).append(",\n")
        append("  \"failed\": ").append(failed).append(",\n")
        append("  \"results\": [\n")
        results.forEachIndexed { index, result ->
            append("    {\"name\": ").append(jsonString(result.name))
                .append(", \"status\": ").append(jsonString(if (result.passed) "PASS" else "FAIL"))
                .append(", \"detail\": ")
            if (result.detail == null) append("null") else append(jsonString(result.detail))
            append('}')
            if (index != results.lastIndex) append(',')
            append('\n')
        }
        append("  ]\n")
        append("}\n")
    }
    File(path).writeText(body)
}

fun main(args: Array<String>) {
    require(args.isNotEmpty() && args.size <= 3) { "Usage: OoxmlInspectorHarness <fixtures-dir> [output-json] [--simulate-android-xml]" }
    val simulateAndroidXml = "--simulate-android-xml" in args
    val positional = args.filter { it != "--simulate-android-xml" }
    val root = File(positional[0])
    if (simulateAndroidXml) {
        AndroidLikeXmlFactory.install()
        println("parser mode: ANDROID simulation (setFeature/setXIncludeAware behave as on device)")
    } else {
        println("parser mode: desktop JVM default")
    }
    val results = mutableListOf<Result>()
    try {
        for (case in CASES) {
        val file = File(root, case.name)
        val failures = mutableListOf<String>()
        try {
            val info = OoxmlPackageInspector.inspect(file)
            if (case.expectation != Expectation.ACCEPT) {
                failures += "expected reject, got accept"
            } else {
                if (info.family.name != case.family) failures += "family ${info.family.name} != ${case.family}"
                if (info.variant.name != case.variant) failures += "variant ${info.variant.name} != ${case.variant}"
                if (case.macros != null && info.hasMacros != case.macros) {
                    failures += "macros ${info.hasMacros} != ${case.macros}"
                }
                for (needle in case.noticeContains) {
                    if (info.notice?.contains(needle, ignoreCase = true) != true) {
                        failures += "notice missing '$needle' (${info.notice})"
                    }
                }
                if (case.externalAtLeast != null && info.externalRelationshipCount < case.externalAtLeast) {
                    failures += "external ${info.externalRelationshipCount} < ${case.externalAtLeast}"
                }
            }
        } catch (error: Throwable) {
            if (case.expectation != Expectation.REJECT) {
                failures += "expected accept, got ${error::class.java.simpleName}: ${error.message}"
            }
        }
        val detail = failures.joinToString("; ").takeIf { it.isNotBlank() }
        results += Result(case.name, detail == null, detail)
        println("${if (detail == null) "PASS" else "FAIL"} ${case.name}${detail?.let { ": $it" } ?: ""}")
        }
    } finally {
        if (simulateAndroidXml) AndroidLikeXmlFactory.uninstall()
    }
    val passed = results.count { it.passed }
    val failed = results.size - passed
    println("OOXML_INSPECTOR_RESULT cases=${results.size} passed=$passed failed=$failed${if (simulateAndroidXml) " mode=android-sim" else ""}")
    if (positional.size >= 2) writeReport(positional[1], results)
    if (failed > 0) System.exit(1)
}
