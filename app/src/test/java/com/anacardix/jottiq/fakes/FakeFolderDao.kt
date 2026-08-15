package com.anacardix.jottiq.fakes

import com.anacardix.jottiq.data.local.dao.FolderDao
import com.anacardix.jottiq.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [FolderDao] fake, reused across data-layer tests per CLAUDE.md's fakes-first policy. */
class FakeFolderDao : FolderDao {

    private val entitiesFlow = MutableStateFlow<List<FolderEntity>>(emptyList())

    override fun observeActive() = entitiesFlow.map { entities -> entities.filter { it.deletedAt == null } }

    override fun observeTrashed() = entitiesFlow.map { entities -> entities.filter { it.deletedAt != null } }

    override suspend fun getActiveOnce(): List<FolderEntity> = observeActive().first()

    override suspend fun getTrashedOnce(): List<FolderEntity> = observeTrashed().first()

    override suspend fun isFolderLocked(id: String): Boolean? =
        entitiesFlow.value.firstOrNull { it.id == id && it.deletedAt == null }?.isLocked

    override suspend fun upsert(entity: FolderEntity) {
        entitiesFlow.update { current -> current.filterNot { it.id == entity.id } + entity }
    }

    override suspend fun setDeletedAt(ids: List<String>, deletedAt: Long?) {
        entitiesFlow.update { current ->
            current.map { if (it.id in ids) it.copy(deletedAt = deletedAt) else it }
        }
    }

    override suspend fun clearDeletedAtMatching(ids: List<String>, deletedAt: Long) {
        entitiesFlow.update { current ->
            current.map {
                if (it.id in ids && it.deletedAt == deletedAt) it.copy(deletedAt = null) else it
            }
        }
    }

    override suspend fun setLocked(ids: List<String>, isLocked: Boolean) {
        entitiesFlow.update { current ->
            current.map { if (it.id in ids) it.copy(isLocked = isLocked) else it }
        }
    }

    override suspend fun deleteAllTrashed() {
        entitiesFlow.update { current -> current.filter { it.deletedAt == null } }
    }
}
