package com.anacardix.jottiq.data.local.entity

/**
 * Room query-result projection of [NoteEntity]'s metadata columns — every column except
 * [NoteEntity.documentJson]. Not a table (`@Entity`) itself; Room maps a column-list `SELECT` onto
 * this shape by matching field names. Used by [com.anacardix.jottiq.data.local.dao.NoteDao]'s
 * summary queries so list screens skip reading/decoding the (potentially large) document column.
 */
data class NoteSummaryEntity(
    val id: String,
    val folderId: String?,
    val title: String,
    val isFavorite: Boolean,
    val isLocked: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
