package com.anacardix.jottiq.fakes

import com.anacardix.jottiq.data.local.dao.NoteDao
import com.anacardix.jottiq.data.local.entity.NoteEntity
import com.anacardix.jottiq.data.local.entity.NoteSummaryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [NoteDao] fake, reused across data-layer tests per CLAUDE.md's fakes-first policy. */
class FakeNoteDao : NoteDao {

    private val entitiesFlow = MutableStateFlow<List<NoteEntity>>(emptyList())

    override fun observeActive() = entitiesFlow.map { entities -> entities.filter { it.deletedAt == null } }

    override fun observeTrashed() = entitiesFlow.map { entities -> entities.filter { it.deletedAt != null } }

    override fun observeActiveSummaries() =
        entitiesFlow.map { entities -> entities.filter { it.deletedAt == null }.map { it.toSummary() } }

    override fun observeTrashedSummaries() =
        entitiesFlow.map { entities -> entities.filter { it.deletedAt != null }.map { it.toSummary() } }

    override fun observeById(id: String) = entitiesFlow.map { entities -> entities.firstOrNull { it.id == id } }

    override suspend fun upsert(entity: NoteEntity) {
        entitiesFlow.update { current -> current.filterNot { it.id == entity.id } + entity }
    }

    override suspend fun setDeletedAt(id: String, deletedAt: Long?) {
        entitiesFlow.update { current ->
            current.map { if (it.id == id) it.copy(deletedAt = deletedAt) else it }
        }
    }

    override suspend fun setFavorite(id: String, isFavorite: Boolean) {
        entitiesFlow.update { current ->
            current.map { if (it.id == id) it.copy(isFavorite = isFavorite) else it }
        }
    }

    override suspend fun setLocked(id: String, isLocked: Boolean) {
        entitiesFlow.update { current ->
            current.map { if (it.id == id) it.copy(isLocked = isLocked) else it }
        }
    }

    override suspend fun setFolderId(id: String, folderId: String?) {
        entitiesFlow.update { current ->
            current.map { if (it.id == id) it.copy(folderId = folderId) else it }
        }
    }

    override suspend fun restoreReparentingIfOrphan(id: String, activeFolderIds: List<String>) {
        entitiesFlow.update { current ->
            current.map {
                if (it.id == id) {
                    val folderId = it.folderId.takeIf { current -> current in activeFolderIds }
                    it.copy(deletedAt = null, folderId = folderId)
                } else {
                    it
                }
            }
        }
    }

    override suspend fun setDeletedAtForFolders(folderIds: List<String>, deletedAt: Long) {
        entitiesFlow.update { current ->
            current.map {
                if (it.folderId in folderIds && it.deletedAt == null) {
                    it.copy(deletedAt = deletedAt)
                } else {
                    it
                }
            }
        }
    }

    override suspend fun clearDeletedAtForFoldersMatching(folderIds: List<String>, deletedAt: Long) {
        entitiesFlow.update { current ->
            current.map {
                if (it.folderId in folderIds && it.deletedAt == deletedAt) {
                    it.copy(deletedAt = null)
                } else {
                    it
                }
            }
        }
    }

    override suspend fun setLockedForFolders(folderIds: List<String>, isLocked: Boolean) {
        entitiesFlow.update { current ->
            current.map { if (it.folderId in folderIds) it.copy(isLocked = isLocked) else it }
        }
    }

    override suspend fun deleteAllTrashed() {
        entitiesFlow.update { current -> current.filter { it.deletedAt == null } }
    }

    override suspend fun deleteById(id: String) {
        entitiesFlow.update { current -> current.filterNot { it.id == id } }
    }
}

private fun NoteEntity.toSummary() = NoteSummaryEntity(
    id = id,
    folderId = folderId,
    title = title,
    isFavorite = isFavorite,
    isLocked = isLocked,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)
