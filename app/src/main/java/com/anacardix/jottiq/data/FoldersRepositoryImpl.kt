package com.anacardix.jottiq.data

import com.anacardix.jottiq.data.local.TransactionRunner
import com.anacardix.jottiq.data.local.dao.FolderDao
import com.anacardix.jottiq.data.local.dao.NoteDao
import com.anacardix.jottiq.data.local.entity.FolderEntity
import com.anacardix.jottiq.domain.DataResult
import com.anacardix.jottiq.domain.Folder
import com.anacardix.jottiq.domain.FoldersRepository
import com.anacardix.jottiq.domain.TimeProvider
import com.anacardix.jottiq.domain.runCatchingDataResult
import com.anacardix.jottiq.domain.usecase.CollectFolderSubtreeIdsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class FoldersRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao,
    private val noteDao: NoteDao,
    private val timeProvider: TimeProvider,
    private val collectSubtreeIds: CollectFolderSubtreeIdsUseCase,
    private val transactionRunner: TransactionRunner,
) : FoldersRepository {

    override fun observeActiveFolders(): Flow<List<Folder>> =
        folderDao.observeActive().map { entities -> entities.map { it.toDomain() } }

    override fun observeTrashedFolders(): Flow<List<Folder>> =
        folderDao.observeTrashed().map { entities -> entities.map { it.toDomain() } }

    override suspend fun createFolder(parentId: String?, name: String): DataResult<Folder> =
        runCatchingDataResult {
            val now = timeProvider.nowEpochMillis()
            // Inherits the parent folder's lock (CLAUDE.md: a folder's lock protects its entire
            // subtree) so a subfolder created inside an already-locked folder is locked from the start.
            val isLocked = parentId?.let { id -> folderDao.isFolderLocked(id) } ?: false
            val folder = Folder(
                id = UUID.randomUUID().toString(),
                parentId = parentId,
                name = name,
                isLocked = isLocked,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            )
            folderDao.upsert(folder.toEntity())
            folder
        }

    override suspend fun moveToTrash(folderId: String): DataResult<Unit> = runCatchingDataResult {
        // Transactional: a crash between the folder and note writes must not leave a trashed
        // folder whose notes are still active.
        transactionRunner.run {
            val activeFolders = folderDao.getActiveOnce().map { it.toDomain() }
            val subtreeIds = collectSubtreeIds(activeFolders, folderId).toList()
            val now = timeProvider.nowEpochMillis()
            folderDao.setDeletedAt(subtreeIds, now)
            noteDao.setDeletedAtForFolders(subtreeIds, now)
        }
    }

    override suspend fun moveToTrash(folderIds: List<String>): DataResult<Unit> = runCatchingDataResult {
        // One active-folder snapshot and one shared `now` for every selected root, so the whole
        // selection is cascade-trashed atomically and restores together (same as a single folder).
        transactionRunner.run {
            val activeFolders = folderDao.getActiveOnce().map { it.toDomain() }
            val subtreeIds = folderIds.flatMap { collectSubtreeIds(activeFolders, it) }.distinct()
            val now = timeProvider.nowEpochMillis()
            folderDao.setDeletedAt(subtreeIds, now)
            noteDao.setDeletedAtForFolders(subtreeIds, now)
        }
    }

    override suspend fun restoreFromTrash(folderId: String): DataResult<Unit> = runCatchingDataResult {
        transactionRunner.run {
            val trashedFolders = folderDao.getTrashedOnce().map { it.toDomain() }
            restoreSubtree(trashedFolders, folderId)
        }
    }

    override suspend fun restoreFromTrash(folderIds: List<String>): DataResult<Unit> = runCatchingDataResult {
        // One trashed-folder snapshot shared by every root's restore, each still applying its own
        // "only rows trashed together" guard (see restoreSubtree) independently.
        transactionRunner.run {
            val trashedFolders = folderDao.getTrashedOnce().map { it.toDomain() }
            folderIds.forEach { folderId -> restoreSubtree(trashedFolders, folderId) }
        }
    }

    /**
     * Restores [rootFolderId] and the cascaded subtree [moveToTrash] trashed alongside it, out of a
     * [trashedFolders] snapshot taken once by the caller. Every folder/note trashed together shares
     * the exact deletedAt timestamp written by [moveToTrash]'s single `now`; restoring only rows
     * matching it (instead of clearing every trashed row in the subtree) avoids resurrecting
     * notes/subfolders the user had already trashed individually before this folder was deleted.
     */
    private suspend fun restoreSubtree(trashedFolders: List<Folder>, rootFolderId: String) {
        val deletionTimestamp = trashedFolders.firstOrNull { it.id == rootFolderId }?.deletedAt
            ?: return
        val subtreeIds = collectSubtreeIds(trashedFolders, rootFolderId).toList()
        folderDao.clearDeletedAtMatching(subtreeIds, deletionTimestamp)
        noteDao.clearDeletedAtForFoldersMatching(subtreeIds, deletionTimestamp)
    }

    override suspend fun emptyTrash(): DataResult<Unit> = runCatchingDataResult {
        folderDao.deleteAllTrashed()
    }

    override suspend fun setFolderLocked(folderId: String, isLocked: Boolean): DataResult<Unit> =
        runCatchingDataResult {
            transactionRunner.run {
                val activeFolders = folderDao.getActiveOnce().map { it.toDomain() }
                val subtreeIds = collectSubtreeIds(activeFolders, folderId).toList()
                folderDao.setLocked(subtreeIds, isLocked)
                noteDao.setLockedForFolders(subtreeIds, isLocked)
            }
        }
}

private fun FolderEntity.toDomain(): Folder = Folder(
    id = id,
    parentId = parentId,
    name = name,
    isLocked = isLocked,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun Folder.toEntity(): FolderEntity = FolderEntity(
    id = id,
    parentId = parentId,
    name = name,
    isLocked = isLocked,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)
