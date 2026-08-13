package com.anacardix.jottiq.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room row for a note. [documentJson] is the note's [com.anacardix.jottiq.domain.NoteDocument],
 * serialized via `com.anacardix.jottiq.data.local.json.NoteDocumentDto` (mapping happens at the
 * repository boundary — entities never leave the data layer, per CLAUDE.md).
 */
@Entity(
    tableName = "notes",
    indices = [Index("folderId"), Index("deletedAt")],
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val folderId: String?,
    val title: String,
    val documentJson: String,
    val isFavorite: Boolean,
    val isLocked: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
