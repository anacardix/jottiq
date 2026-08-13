package com.anacardix.jottiq.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.anacardix.jottiq.data.local.entity.FolderEntity
import com.anacardix.jottiq.data.local.entity.NoteEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies [RoomTransactionRunner] gives repositories real cross-DAO atomicity (CLAUDE.md's
 * "every change ships with tests" for the `@Transaction`/`withTransaction` work in
 * [com.anacardix.jottiq.data.FoldersRepositoryImpl] and [com.anacardix.jottiq.data.NotesRepositoryImpl]):
 * a block that writes through both DAOs either commits every write, or — if it throws partway
 * through — leaves no partial state behind.
 */
@RunWith(RobolectricTestRunner::class)
class RoomTransactionRunnerTest {

    private lateinit var database: JottiqDatabase
    private lateinit var transactionRunner: TransactionRunner

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            JottiqDatabase::class.java,
        ).build()
        transactionRunner = RoomTransactionRunner(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `run commits every write made inside the block`() = runTest {
        transactionRunner.run {
            database.folderDao().upsert(folderEntity("f1"))
            database.noteDao().upsert(noteEntity("n1", folderId = "f1"))
        }

        assertThat(database.folderDao().getActiveOnce().map { it.id }).containsExactly("f1")
        assertThat(database.noteDao().observeActive().first().map { it.id }).containsExactly("n1")
    }

    @Test
    fun `run rolls back every write when the block throws partway through`() = runTest {
        database.folderDao().upsert(folderEntity("pre-existing"))

        val thrown = runCatching {
            transactionRunner.run {
                database.folderDao().upsert(folderEntity("f1"))
                database.noteDao().upsert(noteEntity("n1", folderId = "f1"))
                error("simulated failure between the folder and note writes")
            }
        }.exceptionOrNull()

        assertThat(thrown).isNotNull()
        // Only what existed before the failed transaction remains — f1/n1 never committed, so a
        // note table left cascading a folder trash/lock never sees a folder change without it.
        assertThat(database.folderDao().getActiveOnce().map { it.id }).containsExactly("pre-existing")
        assertThat(database.noteDao().observeActive().first()).isEmpty()
    }
}

private fun folderEntity(id: String) = FolderEntity(
    id = id,
    parentId = null,
    name = id,
    isLocked = false,
    createdAt = 0L,
    updatedAt = 0L,
    deletedAt = null,
)

private fun noteEntity(id: String, folderId: String?) = NoteEntity(
    id = id,
    folderId = folderId,
    title = id,
    documentJson = "{}",
    isFavorite = false,
    isLocked = false,
    createdAt = 0L,
    updatedAt = 0L,
    deletedAt = null,
)
