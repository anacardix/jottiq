package com.anacardix.jottiq.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room row for a folder. Folders nest via [parentId] (`null` = top level). */
@Entity(
    tableName = "folders",
    indices = [Index("parentId"), Index("deletedAt")],
)
data class FolderEntity(
    @PrimaryKey val id: String,
    val parentId: String?,
    val name: String,
    val isLocked: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
