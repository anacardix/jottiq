package com.anacardix.jottiq.data

import com.anacardix.jottiq.data.local.entity.FolderEntity
import com.anacardix.jottiq.data.local.entity.NoteEntity
import com.anacardix.jottiq.domain.DataResult
import com.anacardix.jottiq.domain.TimeProvider
import com.anacardix.jottiq.domain.usecase.CollectFolderSubtreeIdsUseCase
import com.anacardix.jottiq.fakes.FakeFolderDao
import com.anacardix.jottiq.fakes.FakeNoteDao
import com.anacardix.jottiq.fakes.FakeTransactionRunner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val FIXED_TIME = 1_700_000_000_000L

private fun folderEntity(id: String, parentId: String? = null, deletedAt: Long? = null) = FolderEntity(
    id = id,
    parentId = parentId,
    name = "Folder $id",
    isLocked = false,
    createdAt = 0L,
    updatedAt = 0L,
    deletedAt = deletedAt,
)

private fun noteEntity(id: String, folderId: String?, deletedAt: Long? = null) = NoteEntity(
    id = id,
    folderId = folderId,
    title = "Note $id",
    documentJson = "{}",
    isFavorite = false,
    isLocked = false,
    createdAt = 0L,
    updatedAt = 0L,
    deletedAt = deletedAt,
)

class FoldersRepositoryImplTest {

    private val folderDao = FakeFolderDao()
    private val noteDao = FakeNoteDao()
    private val repository = FoldersRepositoryImpl(
        folderDao = folderDao,
        noteDao = noteDao,
        timeProvider = TimeProvider { FIXED_TIME },
        collectSubtreeIds = CollectFolderSubtreeIdsUseCase(),
        transactionRunner = FakeTransactionRunner(),
    )

    @Test
    fun `createFolder persists a new top-level folder and returns it`() = runTest {
        val result = repository.createFolder(parentId = null, name = "Recipes")

        check(result is DataResult.Success)
        assertThat(result.value.parentId).isNull()
        assertThat(result.value.name).isEqualTo("Recipes")
        assertThat(result.value.isLocked).isFalse()
        assertThat(result.value.createdAt).isEqualTo(FIXED_TIME)
        assertThat(result.value.updatedAt).isEqualTo(FIXED_TIME)
    }

    @Test
    fun `createFolder persists the given parent id`() = runTest {
        val result = repository.createFolder(parentId = "personal", name = "Travel")

        check(result is DataResult.Success)
        assertThat(result.value.parentId).isEqualTo("personal")
    }

    @Test
    fun `createFolder inherits isLocked from an already-locked parent`() = runTest {
        folderDao.upsert(folderEntity("journal").copy(isLocked = true))

        val result = repository.createFolder(parentId = "journal", name = "2026")

        check(result is DataResult.Success)
        assertThat(result.value.isLocked).isTrue()
    }

    @Test
    fun `createFolder under an unlocked parent is not locked`() = runTest {
        folderDao.upsert(folderEntity("journal").copy(isLocked = false))

        val result = repository.createFolder(parentId = "journal", name = "2026")

        check(result is DataResult.Success)
        assertThat(result.value.isLocked).isFalse()
    }

    @Test
    fun `created folders get distinct ids`() = runTest {
        val first = repository.createFolder(parentId = null, name = "A")
        val second = repository.createFolder(parentId = null, name = "B")

        check(first is DataResult.Success)
        check(second is DataResult.Success)
        assertThat(first.value.id).isNotEqualTo(second.value.id)
    }

    @Test
    fun `observeActiveFolders reflects folders persisted through createFolder`() = runTest {
        val created = repository.createFolder(parentId = null, name = "Recipes")
        check(created is DataResult.Success)

        val observed = repository.observeActiveFolders().first()

        assertThat(observed).containsExactly(created.value)
    }

    @Test
    fun `moveToTrash soft-deletes the folder and its descendant folders`() = runTest {
        folderDao.upsert(folderEntity("personal"))
        folderDao.upsert(folderEntity("travel", parentId = "personal"))
        folderDao.upsert(folderEntity("japan", parentId = "travel"))
        folderDao.upsert(folderEntity("work"))

        repository.moveToTrash("personal")

        val trashed = repository.observeTrashedFolders().first().map { it.id }.toSet()
        assertThat(trashed).containsExactly("personal", "travel", "japan")
        val active = repository.observeActiveFolders().first().map { it.id }
        assertThat(active).containsExactly("work")
    }

    @Test
    fun `moveToTrash cascades to notes in the folder and its descendants`() = runTest {
        folderDao.upsert(folderEntity("personal"))
        folderDao.upsert(folderEntity("travel", parentId = "personal"))
        noteDao.upsert(noteEntity("n1", folderId = "personal"))
        noteDao.upsert(noteEntity("n2", folderId = "travel"))
        noteDao.upsert(noteEntity("n3", folderId = null))

        repository.moveToTrash("personal")

        val trashedNoteIds = noteDao.observeTrashed().first().map { it.id }.toSet()
        assertThat(trashedNoteIds).containsExactly("n1", "n2")
        val activeNoteIds = noteDao.observeActive().first().map { it.id }
        assertThat(activeNoteIds).containsExactly("n3")
    }

    @Test
    fun `moveToTrash does not affect sibling folders or their notes`() = runTest {
        folderDao.upsert(folderEntity("personal"))
        folderDao.upsert(folderEntity("work"))
        noteDao.upsert(noteEntity("n1", folderId = "work"))

        repository.moveToTrash("personal")

        assertThat(noteDao.observeActive().first().map { it.id }).containsExactly("n1")
    }

    @Test
    fun `restoreFromTrash clears deletedAt on the folder, its subtree, and their notes`() = runTest {
        folderDao.upsert(folderEntity("personal", deletedAt = FIXED_TIME))
        folderDao.upsert(folderEntity("travel", parentId = "personal", deletedAt = FIXED_TIME))
        noteDao.upsert(noteEntity("n1", folderId = "personal", deletedAt = FIXED_TIME))
        noteDao.upsert(noteEntity("n2", folderId = "travel", deletedAt = FIXED_TIME))

        repository.restoreFromTrash("personal")

        val activeFolderIds = repository.observeActiveFolders().first().map { it.id }.toSet()
        assertThat(activeFolderIds).containsExactly("personal", "travel")
        val activeNoteIds = noteDao.observeActive().first().map { it.id }.toSet()
        assertThat(activeNoteIds).containsExactly("n1", "n2")
    }

    @Test
    fun `moveToTrash does not touch updatedAt on the folder or cascaded notes`() = runTest {
        val sentinelUpdatedAt = 42L
        folderDao.upsert(folderEntity("personal").copy(updatedAt = sentinelUpdatedAt))
        noteDao.upsert(noteEntity("n1", folderId = "personal").copy(updatedAt = sentinelUpdatedAt))

        repository.moveToTrash("personal")

        assertThat(folderDao.observeTrashed().first().single().updatedAt).isEqualTo(sentinelUpdatedAt)
        assertThat(noteDao.observeTrashed().first().single().updatedAt).isEqualTo(sentinelUpdatedAt)
    }

    @Test
    fun `restoreFromTrash does not touch updatedAt on the folder or cascaded notes`() = runTest {
        val sentinelUpdatedAt = 42L
        folderDao.upsert(folderEntity("personal", deletedAt = FIXED_TIME).copy(updatedAt = sentinelUpdatedAt))
        noteDao.upsert(
            noteEntity("n1", folderId = "personal", deletedAt = FIXED_TIME).copy(updatedAt = sentinelUpdatedAt),
        )

        repository.restoreFromTrash("personal")

        assertThat(folderDao.observeActive().first().single().updatedAt).isEqualTo(sentinelUpdatedAt)
        assertThat(noteDao.observeActive().first().single().updatedAt).isEqualTo(sentinelUpdatedAt)
    }

    @Test
    fun `emptyTrash hard-deletes every trashed folder and leaves active folders alone`() = runTest {
        folderDao.upsert(folderEntity("trashed", deletedAt = FIXED_TIME))
        folderDao.upsert(folderEntity("active"))

        repository.emptyTrash()

        assertThat(folderDao.observeTrashed().first()).isEmpty()
        assertThat(folderDao.observeActive().first().map { it.id }).containsExactly("active")
    }

    @Test
    fun `setFolderLocked locks the folder, its descendant folders, and their notes`() = runTest {
        folderDao.upsert(folderEntity("personal"))
        folderDao.upsert(folderEntity("travel", parentId = "personal"))
        folderDao.upsert(folderEntity("work"))
        noteDao.upsert(noteEntity("n1", folderId = "personal"))
        noteDao.upsert(noteEntity("n2", folderId = "travel"))
        noteDao.upsert(noteEntity("n3", folderId = "work"))

        repository.setFolderLocked("personal", isLocked = true)

        val lockedFolderIds = folderDao.observeActive().first().filter { it.isLocked }.map { it.id }.toSet()
        assertThat(lockedFolderIds).containsExactly("personal", "travel")
        val lockedNoteIds = noteDao.observeActive().first().filter { it.isLocked }.map { it.id }.toSet()
        assertThat(lockedNoteIds).containsExactly("n1", "n2")
    }

    @Test
    fun `setFolderLocked with isLocked false unlocks the folder, its subtree, and their notes`() = runTest {
        folderDao.upsert(folderEntity("personal").copy(isLocked = true))
        folderDao.upsert(folderEntity("travel", parentId = "personal").copy(isLocked = true))
        noteDao.upsert(noteEntity("n1", folderId = "personal").copy(isLocked = true))
        noteDao.upsert(noteEntity("n2", folderId = "travel").copy(isLocked = true))

        repository.setFolderLocked("personal", isLocked = false)

        assertThat(folderDao.observeActive().first().none { it.isLocked }).isTrue()
        assertThat(noteDao.observeActive().first().none { it.isLocked }).isTrue()
    }

    @Test
    fun `setFolderLocked does not affect sibling folders or their notes`() = runTest {
        folderDao.upsert(folderEntity("personal"))
        folderDao.upsert(folderEntity("work"))
        noteDao.upsert(noteEntity("n1", folderId = "work"))

        repository.setFolderLocked("personal", isLocked = true)

        assertThat(folderDao.observeActive().first().first { it.id == "work" }.isLocked).isFalse()
        assertThat(noteDao.observeActive().first().single().isLocked).isFalse()
    }

    @Test
    fun `setFolderLocked does not touch updatedAt on the folder or cascaded notes`() = runTest {
        val sentinelUpdatedAt = 42L
        folderDao.upsert(folderEntity("personal").copy(updatedAt = sentinelUpdatedAt))
        noteDao.upsert(noteEntity("n1", folderId = "personal").copy(updatedAt = sentinelUpdatedAt))

        repository.setFolderLocked("personal", isLocked = true)

        assertThat(folderDao.observeActive().first().single().updatedAt).isEqualTo(sentinelUpdatedAt)
        assertThat(noteDao.observeActive().first().single().updatedAt).isEqualTo(sentinelUpdatedAt)
    }
}
