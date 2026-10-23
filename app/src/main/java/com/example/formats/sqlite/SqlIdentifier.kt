package com.example.formats.sqlite

/** Safe quoting for table/column names taken from an untrusted database's own schema. */
object SqlIdentifier {

    /** Double-quotes [name], doubling embedded quotes, so it can never break out of the identifier. */
    fun quote(name: String): String = "\"" + name.replace("\u0000", "").replace("\"", "\"\"") + "\""

    /** Shortens a cell value for display without cutting a surrogate pair. */
    fun truncateCell(value: String, max: Int = 500): String {
        if (value.length <= max) return value
        var end = max
        if (Character.isHighSurrogate(value[end - 1])) end--
        return value.substring(0, end) + "\u2026"
    }
}
