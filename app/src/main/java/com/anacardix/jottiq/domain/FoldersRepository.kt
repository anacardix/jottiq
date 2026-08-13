package com.anacardix.jottiq.domain

import kotlinx.coroutines.flow.Flow

/** Source of truth for folders. Implemented in `data/` against Room; never expose entities here. */
interface FoldersRepository {
    fun observeActiveFolders(): Flow<List<Folder>>
    fun observeTrashedFolders(): Flow<List<Folder>>

    /** Creates a folder under [parentId], inheriting [parentId]'s [Folder.isLocked] (CLAUDE.md: a
     * folder's lock protects its entire subtree, including items added to it later). */
    suspend fun createFolder(parentId: String?, name: String): DataResult<Folder>

    /**
     * Soft-deletes [folderId] **and its whole subtree** (CLAUDE.md: a folder's fate applies to its
     * descendants) — every descendant folder, plus every note living in the folder or one of those
     * descendants, all stamped with the same [Folder.deletedAt].
     */
    suspend fun moveToTrash(folderId: String): DataResult<Unit>

    /**
     * Clears [Folder.deletedAt] on [folderId] and the same cascaded subtree [moveToTrash] trashed.
     * Note: a note that was independently trashed *before* its folder was cascade-trashed is, as a
     * v1 simplification, restored too if it's still sitting in that subtree — there's no separate
     * "why was this deleted" marker to tell the two cases apart.
     */
    suspend fun restoreFromTrash(folderId: String): DataResult<Unit>

    /** Hard-deletes every trashed folder. The only place hard-delete is allowed (CLAUDE.md). */
    suspend fun emptyTrash(): DataResult<Unit>

    /**
     * Sets [Folder.isLocked] to [isLocked] on [folderId] **and its whole subtree** (CLAUDE.md: a
     * folder's lock protects its entire subtree) — every descendant folder, plus every note living
     * in the folder or one of those descendants.
     */
    suspend fun setFolderLocked(folderId: String, isLocked: Boolean): DataResult<Unit>
}
