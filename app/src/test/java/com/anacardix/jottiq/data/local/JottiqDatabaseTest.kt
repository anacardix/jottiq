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

    @Test
    fun `setDeletedAtForIds trashes only active notes among the given ids`() = runTest {
        val alreadyTrashed = note(id = "n1", deletedAt = 500L)
        val active1 = note(id = "n2")
        val active2 = note(id = "n3")
        val untouched = note(id = "n4")
        listOf(alreadyTrashed, active1, active2, untouched).forEach { database.noteDao().upsert(it) }

        database.noteDao().setDeletedAtForIds(listOf("n1", "n2", "n3"), deletedAt = 9_000L)

        val byId = database.noteDao().observeActive().first().associateBy { it.id } +
            database.noteDao().observeTrashed().first().associateBy { it.id }
        // n1 was already trashed at 500L — the IS NULL guard must leave its original timestamp alone.
        assertThat(byId.getValue("n1").deletedAt).isEqualTo(500L)
        assertThat(byId.getValue("n2").deletedAt).isEqualTo(9_000L)
        assertThat(byId.getValue("n3").deletedAt).isEqualTo(9_000L)
        assertThat(byId.getValue("n4").deletedAt).isNull()
    }

    @Test
    fun `setFavoriteForIds updates favorite only on the given ids`() = runTest {
        val selected1 = note(id = "n1", isFavorite = false)
        val selected2 = note(id = "n2", isFavorite = true)
        val untouched = note(id = "n3", isFavorite = false)
        listOf(selected1, selected2, untouched).forEach { database.noteDao().upsert(it) }

        database.noteDao().setFavoriteForIds(listOf("n1", "n2"), isFavorite = true)

        val byId = database.noteDao().observeActive().first().associateBy { it.id }
        assertThat(byId.getValue("n1").isFavorite).isTrue()
        assertThat(byId.getValue("n2").isFavorite).isTrue()
        assertThat(byId.getValue("n3").isFavorite).isFalse()
    }

    @Test
    fun `setFolderIdForIds updates folderId only on the given ids`() = runTest {
        val selected1 = note(id = "n1")
        val selected2 = note(id = "n2")
        val untouched = note(id = "n3")
        listOf(selected1, selected2, untouched).forEach { database.noteDao().upsert(it) }

        database.noteDao().setFolderIdForIds(listOf("n1", "n2"), folderId = "work")

        val byId = database.noteDao().observeActive().first().associateBy { it.id }
        assertThat(byId.getValue("n1").folderId).isEqualTo("work")
        assertThat(byId.getValue("n2").folderId).isEqualTo("work")
        assertThat(byId.getValue("n3").folderId).isNull()
    }

    @Test
    fun `deleteByIds hard-deletes exactly the given ids`() = runTest {
        val toDelete1 = note(id = "n1", deletedAt = 500L)
        val toDelete2 = note(id = "n2", deletedAt = 500L)
        val untouched = note(id = "n3", deletedAt = 500L)
        listOf(toDelete1, toDelete2, untouched).forEach { database.noteDao().upsert(it) }

        database.noteDao().deleteByIds(listOf("n1", "n2"))

        val remainingIds = database.noteDao().observeTrashed().first().map { it.id }
        assertThat(remainingIds).containsExactly("n3")
    }

    @Test
    fun `setParentId reparents only the given ids`() = runTest {
        val selected1 = folder(id = "f1")
        val selected2 = folder(id = "f2")
        val untouched = folder(id = "f3")
        listOf(selected1, selected2, untouched).forEach { database.folderDao().upsert(it) }

        database.folderDao().setParentId(listOf("f1", "f2"), parentId = "work")

        val byId = database.folderDao().observeActive().first().associateBy { it.id }
        assertThat(byId.getValue("f1").parentId).isEqualTo("work")
        assertThat(byId.getValue("f2").parentId).isEqualTo("work")
        assertThat(byId.getValue("f3").parentId).isNull()
    }

    private fun folder(id: String) = FolderEntity(
        id = id,
        parentId = null,
        name = "Folder $id",
        isLocked = false,
        createdAt = 1_000L,
        updatedAt = 1_000L,
        deletedAt = null,
    )

    private fun note(
        id: String,
        isFavorite: Boolean = false,
        deletedAt: Long? = null,
    ) = NoteEntity(
        id = id,
        folderId = null,
        title = "Note $id",
        documentJson = "{}",
        isFavorite = isFavorite,
        isLocked = false,
        createdAt = 1_000L,
        updatedAt = 1_000L,
        deletedAt = deletedAt,
    )
}
