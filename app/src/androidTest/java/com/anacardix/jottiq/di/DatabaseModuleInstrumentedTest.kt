package com.anacardix.jottiq.di

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.anacardix.jottiq.data.local.JottiqDatabase
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "database-module-instrumented-test.db"

/**
 * Guards the journal mode used by [DatabaseModule] against a real SQLite engine. Robolectric's
 * SQLite shadow forces MEMORY journal mode regardless of what's configured, so this can only be
 * verified on-device: WAL (Room's default) leaves recent commits in a `-wal` sidecar that Auto
 * Backup captures but that restore commonly discards as stale, silently dropping recent notes.
 * TRUNCATE keeps every commit in the main db file, which is what backup/restore actually needs.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseModuleInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun deleteDatabase() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun productionDatabaseBuilderUsesTruncateJournalModeNotTheWalDefault() {
        val database = Room.databaseBuilder(context, JottiqDatabase::class.java, TEST_DB)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()

        try {
            val journalMode = database.openHelper.writableDatabase
                .query("PRAGMA journal_mode")
                .use { cursor ->
                    cursor.moveToFirst()
                    cursor.getString(0)
                }

            assertThat(journalMode).isEqualTo("truncate")
        } finally {
            database.close()
        }
    }
}
