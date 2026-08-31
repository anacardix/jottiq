package com.anacardix.jottiq.fakes

import com.anacardix.jottiq.domain.DataError
import com.anacardix.jottiq.domain.DataResult
import com.anacardix.jottiq.domain.Folder
import com.anacardix.jottiq.domain.FoldersRepository
import com.anacardix.jottiq.domain.usecase.CollectFolderSubtreeIdsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [FoldersRepository] fake, reused across screen tests per CLAUDE.md's fakes-first policy. */
class FakeFoldersRepository : FoldersRepository {

    private val collectSubtreeIds = CollectFolderSubtreeIdsUseCase()
    private val foldersFlow = MutableStateFlow<List<Folder>>(emptyList())

    /** Set to make the next [createFolder] call fail instead of succeeding. */
    var createFolderFailure: DataError? = null

    /** Set to make the next [moveToTrash] call fail instead of succeeding. */
    var moveToTrashFailure: DataError? = null

    /** Set to make the next [restoreFromTrash] call fail instead of succeeding. */
    var restoreFromTrashFailure: DataError? = null

    /** Set to make the next [emptyTrash] call fail instead of succeeding. */
    var emptyTrashFailure: DataError? = null

    /** Set to make the next [setFolderLocked] call fail instead of succeeding. */
    var setFolderLockedFailure: DataError? = null

    /** Set to make the next [setParent] call fail instead of succeeding. */
    var setParentFailure: DataError? = null

    private var nextGeneratedId = 0
    private var fixedTime = 0L

    override fun observeActiveFolders() = foldersFlow.map { folders -> folders.filter { it.deletedAt == null } }

    override fun observeTrashedFolders() = foldersFlow.map { folders -> folders.filter { it.deletedAt != null } }

    override suspend fun createFolder(parentId: String?, name: String): DataResult<Folder> {
        createFolderFailure?.let { return DataResult.Failure(it) }
        val folder = Folder(
            id = "fake-folder-${nextGeneratedId++}",
            parentId = parentId,
            name = name,
            isLocked = false,
            createdAt = 0L,
            updatedAt = 0L,
            deletedAt = null,
        )
        foldersFlow.update { it + folder }
        return DataResult.Success(folder)
    }

    override suspend fun moveToTrash(folderId: String): DataResult<Unit> {
        moveToTrashFailure?.let { return DataResult.Failure(it) }
        val subtreeIds = collectSubtreeIds(foldersFlow.value.filter { it.deletedAt == null }, folderId)
        foldersFlow.update { current ->
            current.map { if (it.id in subtreeIds) it.copy(deletedAt = fixedTime) else it }
        }
        return DataResult.Success(Unit)
    }

    override suspend fun moveToTrash(folderIds: List<String>): DataResult<Unit> {
        moveToTrashFailure?.let { return DataResult.Failure(it) }
        val activeFolders = foldersFlow.value.filter { it.deletedAt == null }
        val subtreeIds = folderIds.flatMap { collectSubtreeIds(activeFolders, it) }.toSet()
        foldersFlow.update { current ->
            current.map { if (it.id in subtreeIds) it.copy(deletedAt = fixedTime) else it }
        }
        return DataResult.Success(Unit)
    }

    override suspend fun restoreFromTrash(folderId: String): DataResult<Unit> {
        restoreFromTrashFailure?.let { return DataResult.Failure(it) }
        val subtreeIds = collectSubtreeIds(foldersFlow.value.filter { it.deletedAt != null }, folderId)
        foldersFlow.update { current ->
            current.map { if (it.id in subtreeIds) it.copy(deletedAt = null) else it }
        }
        return DataResult.Success(Unit)
    }

    override suspend fun restoreFromTrash(folderIds: List<String>): DataResult<Unit> {
        restoreFromTrashFailure?.let { return DataResult.Failure(it) }
        val trashedFolders = foldersFlow.value.filter { it.deletedAt != null }
        val subtreeIds = folderIds.flatMap { collectSubtreeIds(trashedFolders, it) }.toSet()
        foldersFlow.update { current ->
            current.map { if (it.id in subtreeIds) it.copy(deletedAt = null) else it }
        }
        return DataResult.Success(Unit)
    }

    override suspend fun emptyTrash(): DataResult<Unit> {
        emptyTrashFailure?.let { return DataResult.Failure(it) }
        foldersFlow.update { current -> current.filter { it.deletedAt == null } }
        return DataResult.Success(Unit)
    }

    override suspend fun setFolderLocked(folderId: String, isLocked: Boolean): DataResult<Unit> {
        setFolderLockedFailure?.let { return DataResult.Failure(it) }
        val subtreeIds = collectSubtreeIds(foldersFlow.value.filter { it.deletedAt == null }, folderId)
        foldersFlow.update { current ->
            current.map { if (it.id in subtreeIds) it.copy(isLocked = isLocked) else it }
        }
        return DataResult.Success(Unit)
    }

    override suspend fun setParent(folderIds: List<String>, parentId: String?): DataResult<Unit> {
        setParentFailure?.let { return DataResult.Failure(it) }
        foldersFlow.update { current ->
            current.map { if (it.id in folderIds) it.copy(parentId = parentId) else it }
        }
        return DataResult.Success(Unit)
    }

    /** The raw backing list, regardless of [Folder.deletedAt] — for asserting persisted state in tests. */
    val allFolders: List<Folder> get() = foldersFlow.value

    fun setFolders(folders: List<Folder>) {
        foldersFlow.value = folders
    }

    /** Timestamp stamped onto folders by [moveToTrash] — mirrors the real repository's TimeProvider use. */
    fun setFixedTime(time: Long) {
        fixedTime = time
    }
}
