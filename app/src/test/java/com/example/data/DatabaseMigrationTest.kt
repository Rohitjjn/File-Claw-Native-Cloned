package com.example.data

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class DatabaseMigrationTest {

    @Test
    fun migration3to4_addsSourceUriColumn() {
        val executedSql = mutableListOf<String>()
        val dummyDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        AppDatabase.MIGRATION_3_4.migrate(dummyDb)

        assertEquals(1, executedSql.size)
        val sql = executedSql[0]
        assertTrue("SQL must alter recent_files table", sql.contains("recent_files"))
        assertTrue("SQL must add column sourceUri", sql.contains("sourceUri"))
    }

    @Test
    fun recentFileEntity_supportsSourceUri() {
        val entity = RecentFileEntity(
            id = 1,
            path = "/storage/emulated/0/Download/report.txt",
            name = "report.txt",
            size = 1024L,
            extension = "txt",
            lastOpened = 123456789L,
            sourceUri = "content://com.android.providers.downloads.documents/document/123"
        )
        assertEquals("content://com.android.providers.downloads.documents/document/123", entity.sourceUri)
    }
}
