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

@RunWith(RobolectricTestRunner::class)
class JottiqDatabaseTest {

    private lateinit var database: JottiqDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            JottiqDatabase::class.java,
        ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `in-memory database opens successfully`() {
        // Room opens the underlying connection lazily on first access.
        database.openHelper.writableDatabase

        assertThat(database.isOpen).isTrue()
    }

    @Test
    fun `inserted note round-trips through the dao`() = runTest {
        val entity = NoteEntity(
            id = "11111111-1111-1111-1111-111111111111",
            folderId = null,
            title = "Groceries",
            documentJson = "{}",
            isFavorite = true,
            isLocked = false,
            createdAt = 1_000L,
            updatedAt = 1_000L,
            deletedAt = null,
        )

        database.noteDao().upsert(entity)
        val loaded = database.noteDao().observeActive()

        assertThat(loaded.first()).containsExactly(entity)
    }

    @Test
    fun `inserted folder round-trips through the dao`() = runTest {
        val entity = FolderEntity(
            id = "22222222-2222-2222-2222-222222222222",
            parentId = null,
            name = "Personal",
            isLocked = false,
            createdAt = 1_000L,
            updatedAt = 1_000L,
            deletedAt = null,
        )

        database.folderDao().upsert(entity)
        val loaded = database.folderDao().observeActive()

        assertThat(loaded.first()).containsExactly(entity)
    }

    @Test
    fun `observeActiveSummaries and observeTrashedSummaries project note metadata without the document`() = runTest {
        val active = NoteEntity(
            id = "11111111-1111-1111-1111-111111111111",
            folderId = "work",
            title = "Active note",
            documentJson = """{"blocks":[]}""",
            isFavorite = true,
            isLocked = false,
            createdAt = 1_000L,
            updatedAt = 2_000L,
            deletedAt = null,
        )
        val trashed = active.copy(id = "33333333-3333-3333-3333-333333333333", deletedAt = 3_000L)
        database.noteDao().upsert(active)
        database.noteDao().upsert(trashed)

        val activeSummary = database.noteDao().observeActiveSummaries().first().single()
        assertThat(activeSummary.id).isEqualTo(active.id)
        assertThat(activeSummary.folderId).isEqualTo("work")
        assertThat(activeSummary.title).isEqualTo("Active note")
        assertThat(activeSummary.isFavorite).isTrue()
        assertThat(activeSummary.createdAt).isEqualTo(1_000L)
        assertThat(activeSummary.updatedAt).isEqualTo(2_000L)
        assertThat(activeSummary.deletedAt).isNull()

        val trashedSummary = database.noteDao().observeTrashedSummaries().first().single()
        assertThat(trashedSummary.id).isEqualTo(trashed.id)
        assertThat(trashedSummary.deletedAt).isEqualTo(3_000L)
    }

    @Test
    fun `observeById returns the matching note regardless of deletedAt, or null when absent`() = runTest {
        val entity = NoteEntity(
            id = "44444444-4444-4444-4444-444444444444",
            folderId = null,
            title = "Groceries",
            documentJson = "{}",
            isFavorite = false,
            isLocked = false,
            createdAt = 1_000L,
            updatedAt = 1_000L,
            deletedAt = 5_000L,
        )
        database.noteDao().upsert(entity)

        assertThat(database.noteDao().observeById(entity.id).first()).isEqualTo(entity)
        assertThat(database.noteDao().observeById("missing").first()).isNull()
    }

    @Test
    fun `isFolderLocked reads a single active folder's lock state, or null when absent or trashed`() = runTest {
        val locked = FolderEntity(
            id = "55555555-5555-5555-5555-555555555555",
            parentId = null,
            name = "Journal",
            isLocked = true,
            createdAt = 1_000L,
            updatedAt = 1_000L,
            deletedAt = null,
        )
        val trashed = locked.copy(id = "66666666-6666-6666-6666-666666666666", deletedAt = 5_000L)
        database.folderDao().upsert(locked)
        database.folderDao().upsert(trashed)

        assertThat(database.folderDao().isFolderLocked(locked.id)).isTrue()
        assertThat(database.folderDao().isFolderLocked(trashed.id)).isNull()
        assertThat(database.folderDao().isFolderLocked("missing")).isNull()
    }
}
