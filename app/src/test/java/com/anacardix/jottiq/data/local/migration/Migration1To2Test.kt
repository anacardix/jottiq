package com.anacardix.jottiq.data.local.migration

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.anacardix.jottiq.data.local.JottiqDatabase
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val TEST_DB = "migration-test"

@RunWith(RobolectricTestRunner::class)
class Migration1To2Test {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        JottiqDatabase::class.java,
    )

    @Test
    fun `migrate1To2 drops placeholder and creates empty notes and folders tables`() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO placeholder (id, createdAt, updatedAt, deletedAt) VALUES " +
                    "('11111111-1111-1111-1111-111111111111', 1000, 1000, NULL)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        migrated.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='placeholder'",
        ).use { cursor ->
            assertThat(cursor.count).isEqualTo(0)
        }
        migrated.query("SELECT * FROM notes").use { cursor -> assertThat(cursor.count).isEqualTo(0) }
        migrated.query("SELECT * FROM folders").use { cursor -> assertThat(cursor.count).isEqualTo(0) }
    }
}
