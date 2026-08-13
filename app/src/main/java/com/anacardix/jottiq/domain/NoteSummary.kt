package com.anacardix.jottiq.domain

/**
 * A [Note]'s metadata without its [NoteDocument] content — everything the note-list screens
 * (Home, Folder, Trash) render. Fetched through a dedicated projection so those screens never pay
 * the cost of reading and JSON-decoding every note's document just to show a title and a date.
 * Sync-ready invariants mirror [Note]'s.
 */
data class NoteSummary(
    val id: String,
    val folderId: String?,
    val title: String,
    val isFavorite: Boolean,
    val isLocked: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
