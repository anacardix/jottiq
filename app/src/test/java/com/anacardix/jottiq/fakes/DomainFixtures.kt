package com.anacardix.jottiq.fakes

import com.anacardix.jottiq.domain.Folder
import com.anacardix.jottiq.domain.Note
import com.anacardix.jottiq.domain.NoteDocument

/** Exhaustive test-data builder mirroring every [Note] field, reused across ViewModel tests. */
@Suppress("LongParameterList")
fun note(
    id: String,
    folderId: String? = null,
    title: String = "Note $id",
    isFavorite: Boolean = false,
    isLocked: Boolean = false,
    createdAt: Long = 0L,
    updatedAt: Long = 0L,
    deletedAt: Long? = null,
) = Note(
    id = id,
    folderId = folderId,
    title = title,
    document = NoteDocument(),
    isFavorite = isFavorite,
    isLocked = isLocked,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

/** Exhaustive test-data builder mirroring every [Folder] field, reused across ViewModel tests. */
@Suppress("LongParameterList")
fun folder(
    id: String,
    parentId: String? = null,
    name: String = "Folder $id",
    isLocked: Boolean = false,
    createdAt: Long = 0L,
    updatedAt: Long = 0L,
    deletedAt: Long? = null,
) = Folder(
    id = id,
    parentId = parentId,
    name = name,
    isLocked = isLocked,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)
