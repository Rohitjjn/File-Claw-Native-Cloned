package com.example.core.text

/** Extracts readable text from RTF. Skips font/colour tables, pictures and `{\*...}` destinations. */
object RtfExtractor {

    private val skipDestinations = setOf(
        "fonttbl", "colortbl", "stylesheet", "info", "pict", "object", "header", "footer",
        "headerl", "headerr", "footerl", "footerr", "themedata", "colorschememapping", "datastore", "latentstyles"
    )

    fun extract(rtf: String, maxChars: Int = 5_000_000): String {
        val out = StringBuilder()
        var i = 0
        val n = rtf.length
        val skipStack = ArrayList<Boolean>()
        var skipping = false
        var uc = 1
        var pendingSkip = 0

        while (i < n && out.length < maxChars) {
            val c = rtf[i]
            when (c) {
                '{' -> {
                    skipStack.add(skipping)
                    i++
                    if (i + 1 < n && rtf[i] == '\\' && rtf[i + 1] == '*') skipping = true
                }
                '}' -> {
                    skipping = if (skipStack.isNotEmpty()) skipStack.removeAt(skipStack.size - 1) else false
                    i++
                }
                '\\' -> {
                    i++
                    if (i >= n) break
                    val d = rtf[i]
                    if (!d.isLetter()) {
                        when (d) {
                            '\\', '{', '}' -> if (!skipping) {
                                if (pendingSkip > 0) pendingSkip-- else out.append(d)
                            }
                            '\'' -> if (i + 2 < n) {
                                val hex = rtf.substring(i + 1, i + 3).toIntOrNull(16)
                                if (pendingSkip > 0) {
                                    pendingSkip--
                                } else if (hex != null && !skipping) {
                                    out.append(String(byteArrayOf(hex.toByte()), Charsets.ISO_8859_1))
                                }
                                i += 2
                            }
                            '~' -> if (!skipping) out.append('\u00A0')
                            '_' -> if (!skipping) out.append('-')
                            else -> Unit
                        }
                        i++
                    } else {
                        var j = i
                        while (j < n && rtf[j].isLetter()) j++
                        val word = rtf.substring(i, j)
                        var k = j
                        if (k < n && (rtf[k] == '-' || rtf[k].isDigit())) {
                            k++
                            while (k < n && rtf[k].isDigit()) k++
                        }
                        val param = rtf.substring(j, k).toIntOrNull()
                        i = k
                        if (i < n && rtf[i] == ' ') i++
                        if (word in skipDestinations) skipping = true
                        if (!skipping) {
                            when (word) {
                                "par", "line", "sect", "page" -> out.append('\n')
                                "tab" -> out.append('\t')
                                "emdash" -> out.append('\u2014')
                                "endash" -> out.append('\u2013')
                                "bullet" -> out.append('\u2022')
                                "lquote" -> out.append('\u2018')
                                "rquote" -> out.append('\u2019')
                                "ldblquote" -> out.append('\u201C')
                                "rdblquote" -> out.append('\u201D')
                                "uc" -> uc = param ?: 1
                                "u" -> if (param != null) {
                                    out.append((if (param < 0) param + 65536 else param).toChar())
                                    pendingSkip = uc
                                }
                            }
                        }
                    }
                }
                '\r', '\n' -> i++
                else -> {
                    if (!skipping) {
                        if (pendingSkip > 0) pendingSkip-- else out.append(c)
                    }
                    i++
                }
            }
        }
        return out.toString().replace(Regex("\n{3,}"), "\n\n").trim()
    }
}
