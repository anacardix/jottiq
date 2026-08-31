package com.anacardix.jottiq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anacardix.jottiq.data.local.entity.NoteEntity
import com.anacardix.jottiq.data.local.entity.NoteSummaryEntity
import kotlinx.coroutines.flow.Flow

private const val SUMMARY_COLUMNS = "id, folderId, title, isFavorite, isLocked, createdAt, updatedAt, deletedAt"

@Dao
@Suppress("TooManyFunctions") // one focused query per DAO capability; no query does double duty
interface NoteDao {
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL")
    fun observeActive(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL")
    fun observeTrashed(): Flow<List<NoteEntity>>

    /** Metadata-only projection of [observeActive] (no [NoteEntity.documentJson]) for list screens. */
    @Query("SELECT $SUMMARY_COLUMNS FROM notes WHERE deletedAt IS NULL")
    fun observeActiveSummaries(): Flow<List<NoteSummaryEntity>>

    /** Metadata-only projection of [observeTrashed] (no [NoteEntity.documentJson]) for list screens. */
    @Query("SELECT $SUMMARY_COLUMNS FROM notes WHERE deletedAt IS NOT NULL")
    fun observeTrashedSummaries(): Flow<List<NoteSummaryEntity>>

    /** A single note (active or trashed) by id, for screens (the editor) that need exactly one. */
    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: String): Flow<NoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NoteEntity)

    // No updatedAt bump here on purpose: trashing is a metadata change, not a content edit, so it
    // must not change the note's "Edited …" label.
    @Query("UPDATE notes SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun setDeletedAt(id: String, deletedAt: Long?)

    /**
     * Bulk version of [setDeletedAt] for multi-select trashing. Guarded by `deletedAt IS NULL`,
     * mirroring [setDeletedAtForFolders], so a note already trashed independently isn't restamped.
     *
     * No updatedAt bump here on purpose: trashing is a metadata change, not a content edit, so it
     * must not change the note's "Edited …" label.
     */
    @Query("UPDATE notes SET deletedAt = :deletedAt WHERE id IN (:ids) AND deletedAt IS NULL")
    suspend fun setDeletedAtForIds(ids: List<String>, deletedAt: Long)

    // No updatedAt bump here on purpose: favoriting is a metadata toggle, not a content edit, so it
    // must not move the note in "Date edited" sort or change its "Edited …" label.
    @Query("UPDATE notes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    /**
     * Bulk version of [setFavorite] for multi-select favorite/unfavorite.
     *
     * No updatedAt bump here on purpose: favoriting is a metadata toggle, not a content edit, so it
     * must not move the note in "Date edited" sort or change its "Edited …" label.
     */
    @Query("UPDATE notes SET isFavorite = :isFavorite WHERE id IN (:ids)")
    suspend fun setFavoriteForIds(ids: List<String>, isFavorite: Boolean)

    // No updatedAt bump here on purpose: locking is a metadata toggle, not a content edit, so it
    // must not move the note in "Date edited" sort or change its "Edited …" label.
    @Query("UPDATE notes SET isLocked = :isLocked WHERE id = :id")
    suspend fun setLocked(id: String, isLocked: Boolean)

    // No updatedAt bump here on purpose: moving to another folder is an organizational change, not
    // a content edit, so it must not move the note in "Date edited" sort or change its "Edited …"
    // label.
    @Query("UPDATE notes SET folderId = :folderId WHERE id = :id")
    suspend fun setFolderId(id: String, folderId: String?)

    /**
     * Bulk version of [setFolderId] for multi-select "Move to folder".
     *
     * No updatedAt bump here on purpose: moving to another folder is an organizational change, not
     * a content edit, so it must not move the note in "Date edited" sort or change its "Edited …"
     * label.
     */
    @Query("UPDATE notes SET folderId = :folderId WHERE id IN (:ids)")
    suspend fun setFolderIdForIds(ids: List<String>, folderId: String?)

    /**
     * Restores a single trashed note, falling back to top-level (general notes) if [folderId] no
     * longer names an active folder — e.g. the folder is still trashed or was purged.
     *
     * No updatedAt bump here on purpose: restoring is a metadata change, not a content edit, so it
     * must not change the note's "Edited …" label.
     */
    @Query(
        "UPDATE notes SET deletedAt = NULL, " +
            "folderId = CASE WHEN folderId IN (:activeFolderIds) THEN folderId ELSE NULL END " +
            "WHERE id = :id",
    )
    suspend fun restoreReparentingIfOrphan(id: String, activeFolderIds: List<String>)

    /** Cascades a folder trash: soft-deletes every currently-active note living in [folderIds]. */
    @Query(
        "UPDATE notes SET deletedAt = :deletedAt " +
            "WHERE folderId IN (:folderIds) AND deletedAt IS NULL",
    )
    suspend fun setDeletedAtForFolders(folderIds: List<String>, deletedAt: Long)

    /**
     * Cascades a folder restore: clears [com.anacardix.jottiq.data.local.entity.NoteEntity.deletedAt]
     * only on notes trashed *by that folder deletion* (i.e. sharing its [deletedAt] timestamp,
     * written in the same [com.anacardix.jottiq.data.FoldersRepositoryImpl.moveToTrash] transaction),
     * leaving notes the user trashed individually before the folder deletion still in the trash.
     */
    @Query(
        "UPDATE notes SET deletedAt = NULL " +
            "WHERE folderId IN (:folderIds) AND deletedAt = :deletedAt",
    )
    suspend fun clearDeletedAtForFoldersMatching(folderIds: List<String>, deletedAt: Long)

    /**
     * Cascades a folder lock/unlock: sets every note living in [folderIds] to [isLocked].
     *
     * No updatedAt bump here on purpose: locking is a metadata toggle, not a content edit, so it
     * must not change the note's "Edited …" label.
     */
    @Query("UPDATE notes SET isLocked = :isLocked WHERE folderId IN (:folderIds)")
    suspend fun setLockedForFolders(folderIds: List<String>, isLocked: Boolean)

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL")
    suspend fun deleteAllTrashed()

    /**
     * Hard-deletes a single row outright. The only hard-delete other than trash purge (CLAUDE.md) —
     * reserved for a note [com.anacardix.jottiq.data.NotesRepositoryImpl.createNote] scaffolded that
     * the user never typed anything into, so there is no user data to preserve in trash.
     */
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Hard-deletes multiple trashed notes outright, for Trash's multi-select "Delete Forever"
     * (same hard-delete semantics as [deleteAllTrashed], scoped to a selection instead of all rows).
     */
    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
