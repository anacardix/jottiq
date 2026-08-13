package com.anacardix.jottiq.data

import com.anacardix.jottiq.data.local.entity.FolderEntity
import com.anacardix.jottiq.domain.DataResult
import com.anacardix.jottiq.domain.NoteDocument
import com.anacardix.jottiq.domain.TimeProvider
import com.anacardix.jottiq.fakes.FakeFolderDao
import com.anacardix.jottiq.fakes.FakeNoteDao
import com.anacardix.jottiq.fakes.FakeTransactionRunner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test

private const val FIXED_TIME = 1_700_000_000_000L

@OptIn(ExperimentalCoroutinesApi::class)
class NotesRepositoryImplTest {

    private val noteDao = FakeNoteDao()
    private val folderDao = FakeFolderDao()
    private val repository = NotesRepositoryImpl(
        noteDao = noteDao,
        folderDao = folderDao,
        timeProvider = TimeProvider { FIXED_TIME },
        json = Json,
        transactionRunner = FakeTransactionRunner(),
        defaultDispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun `createNote persists a new top-level note and returns it`() = runTest {
        val result = repository.createNote(folderId = null)

        check(result is DataResult.Success)
        assertThat(result.value.folderId).isNull()
        assertThat(result.value.title).isEmpty()
        assertThat(result.value.document).isEqualTo(NoteDocument())
        assertThat(result.value.isFavorite).isFalse()
        assertThat(result.value.isLocked).isFalse()
        assertThat(result.value.createdAt).isEqualTo(FIXED_TIME)
        assertThat(result.value.updatedAt).isEqualTo(FIXED_TIME)
    }

    @Test
    fun `createNote persists the given folder id`() = runTest {
        val result = repository.createNote(folderId = "journal")

        check(result is DataResult.Success)
        assertThat(result.value.folderId).isEqualTo("journal")
    }

    @Test
    fun `createNote inherits isLocked from an already-locked folder`() = runTest {
        folderDao.upsert(activeFolder(id = "journal", isLocked = true))

        val result = repository.createNote(folderId = "journal")

        check(result is DataResult.Success)
        assertThat(result.value.isLocked).isTrue()
    }

    @Test
    fun `createNote in an unlocked folder is not locked`() = runTest {
        folderDao.upsert(activeFolder(id = "journal", isLocked = false))

        val result = repository.createNote(folderId = "journal")

        check(result is DataResult.Success)
        assertThat(result.value.isLocked).isFalse()
    }

    @Test
    fun `created notes get distinct ids`() = runTest {
        val first = repository.createNote(folderId = null)
        val second = repository.createNote(folderId = null)

        check(first is DataResult.Success)
        check(second is DataResult.Success)
        assertThat(first.value.id).isNotEqualTo(second.value.id)
    }

    @Test
    fun `observeActiveNoteSummaries reflects notes persisted through createNote`() = runTest {
        val created = repository.createNote(folderId = null)
        check(created is DataResult.Success)

        val observed = repository.observeActiveNoteSummaries().first()

        assertThat(observed.map { it.id }).containsExactly(created.value.id)
    }

    @Test
    fun `observeNoteById round-trips the note document through JSON`() = runTest {
        val created = repository.createNote(folderId = null)
        check(created is DataResult.Success)

        val entities = noteDao.observeActive().first()
        assertThat(entities.single().documentJson).isNotEmpty()

        val observed = repository.observeNoteById(created.value.id).filterNotNull().first()
        assertThat(observed.document).isEqualTo(NoteDocument())
    }

    @Test
    fun `observeNoteById returns null for an unknown id`() = runTest {
        val observed = repository.observeNoteById("missing").first()

        assertThat(observed).isNull()
    }

    @Test
    fun `updateNote persists changes and stamps updatedAt with the current time`() = runTest {
        val created = repository.createNote(folderId = null)
        check(created is DataResult.Success)

        val result = repository.updateNote(
            created.value.copy(title = "Groceries", isFavorite = true, updatedAt = 0L),
        )

        check(result is DataResult.Success)
        val observed = repository.observeNoteById(created.value.id).filterNotNull().first()
        assertThat(observed.title).isEqualTo("Groceries")
        assertThat(observed.isFavorite).isTrue()
        assertThat(observed.updatedAt).isEqualTo(FIXED_TIME)
    }

    @Test
    fun `setFavorite flips isFavorite`() = runTest {
        val created = repository.createNote(folderId = null)
        check(created is DataResult.Success)

        val result = repository.setFavorite(created.value.id, isFavorite = true)

        check(result is DataResult.Success)
        assertThat(repository.observeActiveNoteSummaries().first().single().isFavorite).isTrue()
    }

    @Test
    fun `setFavorite does not touch updatedAt (favoriting is metadata, not a content edit)`() = runTest {
        val created = repository.createNote(folderId = null)
        check(created is DataResult.Success)
        // FIXED_TIME is the only value the fixed TimeProvider ever produces, so seed a distinguishable
        // updatedAt directly through the DAO to prove setFavorite leaves it alone rather than
        // restamping it to (coincidentally) the same fixed "now".
        val sentinelUpdatedAt = 42L
        val entity = noteDao.observeActive().first().single()
        noteDao.upsert(entity.copy(updatedAt = sentinelUpdatedAt))

        val result = repository.setFavorite(created.value.id, isFavorite = true)

        check(result is DataResult.Success)
        assertThat(repository.observeActiveNoteSummaries().first().single().updatedAt).isEqualTo(sentinelUpdatedAt)
    }

    @Test
    fun `setFavorite can clear isFavorite back to false`() = runTest {
        val created = repository.createNote(folderId = null)
        check(created is DataResult.Success)
        repository.setFavorite(created.value.id, isFavorite = true)

        val result = repository.setFavorite(created.value.id, isFavorite = false)

        check(result is DataResult.Success)
        assertThat(repository.observeActiveNoteSummaries().first().single().isFavorite).isFalse()
    }

    @Test
    fun `setLocked flips isLocked`() = runTest {
        val created = repository.createNote(folderId = null)
        check(created is DataResult.Success)

        val result = repository.setLocked(created.value.id, isLocked = true)

        check(result is DataResult.Success)
        assertThat(repository.observeActiveNoteSummaries().first().single().isLocked).isTrue()
    }

    @Test
    fun `setLocked does not touch updatedAt (locking is metadata, not a content edit)`() = runTest {
        val created = repository.createNote(folderId = null)
        check(created is DataResult.Success)
        val sentinelUpdatedAt = 42L
        val entity = noteDao.observeActive().first().single()
        noteDao.upsert(entity.copy(updatedAt = sentinelUpdatedAt))

        val result = repository.setLocked(created.value.id, isLocked = true)

        check(result is DataResult.Success)
        assertThat(repository.observeActiveNoteSummaries().first().single().updatedAt).isEqualTo(sentinelUpdatedAt)
    }

    @Test
    fun `setLocked can clear isLocked back to false`() = runTest {
        val created = repository.createNote(folderId = null)
        check(created is DataResult.Success)
        repository.setLocked(created.value.id, isLocked = true)

        val result = repository.setLocked(created.value.id, isLocked = false)

        check(result is DataResult.Success)
        assertThat(repository.observeActiveNoteSummaries().first().single().isLocked).isFalse()
    }

    @Test
    fun `setFolder moves the note to the given folder`() = runTest {
        folderDao.upsert(activeFolder(id = "work"))
        val created = repository.createNote(folderId = null)
        check(created is DataResult.Success)

        val result = repository.setFolder(created.value.id, folderId = "work")

        check(result is DataResult.Success)
        assertThat(repository.observeActiveNoteSummaries().first().single().folderId).isEqualTo("work")
    }

    @Test
    fun `setFolder can clear folderId back to top-level`() = runTest {
        folderDao.upsert(activeFolder(id = "work"))
        val created = repository.createNote(folderId = "work")
        check(created is DataResult.Success)

        val result = repository.setFolder(created.value.id, folderId = null)

        check(result is DataResult.Success)
        assertThat(repository.observeActiveNoteSummaries().first().single().folderId).isNull()
    }

    @Test
    fun `setFolder does not touch updatedAt (moving is organizational, not a content edit)`() = runTest {
        val created = repository.createNote(folderId = null)
        check(created is DataResult.Success)
        val sentinelUpdatedAt = 42L
        val entity = noteDao.observeActive().first().single()
        noteDao.upsert(entity.copy(updatedAt = sentinelUpdatedAt))

        val result = repository.setFolder(created.value.id, folderId = "work")

        check(result is DataResult.Success)
        assertThat(repository.observeActiveNoteSummaries().first().single().updatedAt).isEqualTo(sentinelUpdatedAt)
    }

    @Test
    fun `moveToTrash stamps deletedAt and moves the note out of observeActiveNoteSummaries`() = runTest {
        val created = repository.createNote(folderId = null)
        check(created is DataResult.Success)

        val result = repository.moveToTrash(created.value.id)

        check(result is DataResult.Success)
        assertThat(repository.observeActiveNoteSummaries().first()).isEmpty()
        val trashed = repository.observeTrashedNoteSummaries().first().single()
        assertThat(trashed.id).isEqualTo(created.value.id)
        assertThat(trashed.deletedAt).isEqualTo(FIXED_TIME)
    }

    @Test
    fun `moveToTrash does not touch updatedAt (trashing is metadata, not a content edit)`() = runTest {
        val created = repository.createNote(folderId = null)
        check(created is DataResult.Success)
        val sentinelUpdatedAt = 42L
        val entity = noteDao.observeActive().first().single()
        noteDao.upsert(entity.copy(updatedAt = sentinelUpdatedAt))

        val result = repository.moveToTrash(created.value.id)

        check(result is DataResult.Success)
        assertThat(repository.observeTrashedNoteSummaries().first().single().updatedAt).isEqualTo(sentinelUpdatedAt)
    }

    @Test
    fun `restoreFromTrash clears deletedAt and moves the note back to observeActiveNoteSummaries`() = runTest {
        val created = repository.createNote(folderId = null)
        check(created is DataResult.Success)
        repository.moveToTrash(created.value.id)

        val result = repository.restoreFromTrash(created.value.id)

        check(result is DataResult.Success)
        assertThat(repository.observeTrashedNoteSummaries().first()).isEmpty()
        assertThat(repository.observeActiveNoteSummaries().first().single().deletedAt).isNull()
    }

    @Test
    fun `restoreFromTrash does not touch updatedAt (restoring is metadata, not a content edit)`() = runTest {
        val created = repository.createNote(folderId = null)
        check(created is DataResult.Success)
        repository.moveToTrash(created.value.id)
        val sentinelUpdatedAt = 42L
        val entity = noteDao.observeTrashed().first().single()
        noteDao.upsert(entity.copy(updatedAt = sentinelUpdatedAt))

        val result = repository.restoreFromTrash(created.value.id)

        check(result is DataResult.Success)
        assertThat(repository.observeActiveNoteSummaries().first().single().updatedAt).isEqualTo(sentinelUpdatedAt)
    }

    @Test
    fun `restoreFromTrash keeps folderId when the folder is still active`() = runTest {
        folderDao.upsert(activeFolder(id = "work"))
        val created = repository.createNote(folderId = "work")
        check(created is DataResult.Success)
        repository.moveToTrash(created.value.id)

        val result = repository.restoreFromTrash(created.value.id)

        check(result is DataResult.Success)
        assertThat(repository.observeActiveNoteSummaries().first().single().folderId).isEqualTo("work")
    }

    @Test
    fun `restoreFromTrash falls back to general notes when the folder is trashed`() = runTest {
        folderDao.upsert(activeFolder(id = "work"))
        val created = repository.createNote(folderId = "work")
        check(created is DataResult.Success)
        repository.moveToTrash(created.value.id)
        folderDao.setDeletedAt(listOf("work"), FIXED_TIME)

        val result = repository.restoreFromTrash(created.value.id)

        check(result is DataResult.Success)
        assertThat(repository.observeActiveNoteSummaries().first().single().folderId).isNull()
    }

    @Test
    fun `restoreFromTrash falls back to general notes when the folder no longer exists`() = runTest {
        val created = repository.createNote(folderId = "deleted-folder")
        check(created is DataResult.Success)
        repository.moveToTrash(created.value.id)

        val result = repository.restoreFromTrash(created.value.id)

        check(result is DataResult.Success)
        assertThat(repository.observeActiveNoteSummaries().first().single().folderId).isNull()
    }

    @Test
    fun `emptyTrash hard-deletes every trashed note but leaves active notes alone`() = runTest {
        val active = repository.createNote(folderId = null)
        val trashed = repository.createNote(folderId = null)
        check(active is DataResult.Success)
        check(trashed is DataResult.Success)
        repository.moveToTrash(trashed.value.id)

        val result = repository.emptyTrash()

        check(result is DataResult.Success)
        assertThat(repository.observeTrashedNoteSummaries().first()).isEmpty()
        assertThat(repository.observeActiveNoteSummaries().first().single().id).isEqualTo(active.value.id)
    }
}

private fun activeFolder(id: String, isLocked: Boolean = false) = FolderEntity(
    id = id,
    parentId = null,
    name = id,
    isLocked = isLocked,
    createdAt = FIXED_TIME,
    updatedAt = FIXED_TIME,
    deletedAt = null,
)
