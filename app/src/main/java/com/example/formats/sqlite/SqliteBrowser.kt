package com.example.formats.sqlite

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.formats.sqlite.SqlIdentifier
import java.io.Closeable

data class SqlitePage(val columns: List<String>, val rows: List<List<String>>, val totalRows: Long)

/**
 * Read-only SQLite browser. The database is untrusted input: it is opened read-only, identifiers are
 * always quoted, and only SELECT statements built here are ever run.
 */
class SqliteBrowser(path: String) : Closeable {

    private val db: SQLiteDatabase = SQLiteDatabase.openDatabase(
        path, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
    )

    fun tables(): List<String> {
        val out = ArrayList<String>()
        db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type IN ('table','view') AND name NOT LIKE 'sqlite_%' ORDER BY name",
            null
        ).use { c -> while (c.moveToNext()) out.add(c.getString(0)) }
        return out
    }

    fun page(table: String, offset: Int, limit: Int): SqlitePage {
        val q = SqlIdentifier.quote(table)
        val total = db.rawQuery("SELECT COUNT(*) FROM $q", null).use { if (it.moveToFirst()) it.getLong(0) else 0L }
        db.rawQuery("SELECT * FROM $q LIMIT ? OFFSET ?", arrayOf(limit.toString(), offset.toString())).use { c ->
            val cols = c.columnNames.toList()
            val rows = ArrayList<List<String>>()
            while (c.moveToNext()) {
                val row = ArrayList<String>(cols.size)
                for (i in cols.indices) row.add(cell(c, i))
                rows.add(row)
            }
            return SqlitePage(cols, rows, total)
        }
    }

    private fun cell(c: Cursor, i: Int): String = when (c.getType(i)) {
        Cursor.FIELD_TYPE_NULL -> "NULL"
        Cursor.FIELD_TYPE_BLOB -> "<BLOB ${c.getBlob(i).size} bytes>"
        else -> SqlIdentifier.truncateCell(c.getString(i) ?: "")
    }

    override fun close() {
        if (db.isOpen) db.close()
    }

    companion object {
        const val PAGE_SIZE = 100
    }
}
