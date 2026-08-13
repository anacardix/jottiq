package com.anacardix.jottiq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anacardix.jottiq.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE deletedAt IS NULL")
    fun observeActive(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE deletedAt IS NOT NULL")
    fun observeTrashed(): Flow<List<FolderEntity>>

    /** One-shot snapshot of active folders, used to compute a folder's subtree before trashing it. */
    @Query("SELECT * FROM folders WHERE deletedAt IS NULL")
    suspend fun getActiveOnce(): List<FolderEntity>

    /** One-shot snapshot of trashed folders, used to compute a folder's subtree before restoring it. */
    @Query("SELECT * FROM folders WHERE deletedAt IS NOT NULL")
    suspend fun getTrashedOnce(): List<FolderEntity>

    /**
     * Targeted [FolderEntity.isLocked] lookup for a single active folder — avoids loading every
     * active folder just to read one parent's lock state when creating a note/folder inside it.
     * Null if [id] doesn't name a currently-active folder.
     */
    @Query("SELECT isLocked FROM folders WHERE id = :id AND deletedAt IS NULL")
    suspend fun isFolderLocked(id: String): Boolean?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FolderEntity)

    // No updatedAt bump here on purpose: trashing/restoring is a metadata change, not a content
    // edit, so it must not change the folder's "Edited …" label.
    @Query("UPDATE folders SET deletedAt = :deletedAt WHERE id IN (:ids)")
    suspend fun setDeletedAt(ids: List<String>, deletedAt: Long?)

    // No updatedAt bump here on purpose: locking is a metadata toggle, not a content edit, so it
    // must not change the folder's "Edited …" label.
    @Query("UPDATE folders SET isLocked = :isLocked WHERE id IN (:ids)")
    suspend fun setLocked(ids: List<String>, isLocked: Boolean)

    @Query("DELETE FROM folders WHERE deletedAt IS NOT NULL")
    suspend fun deleteAllTrashed()
}
