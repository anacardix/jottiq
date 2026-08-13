package com.anacardix.jottiq.domain

/**
 * A note. Sync-ready invariants (CLAUDE.md): [id] is a client-generated UUID string, [createdAt]/
 * [updatedAt] are epoch millis UTC, and deletion is soft via [deletedAt] (trash retention).
 */
data class Note(
    val id: String,
    val folderId: String?,
    val title: String,
    val document: NoteDocument,
    val isFavorite: Boolean,
    val isLocked: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
