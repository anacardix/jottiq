package com.anacardix.jottiq.domain

import kotlinx.coroutines.flow.Flow

/** Source of truth for notes. Implemented in `data/` against Room; never expose entities here. */
@Suppress("TooManyFunctions") // one focused method per repository capability; none is a pass-through
interface NotesRepository {
    /** Metadata only (no [Note.document]) — what the Home/Folder list rows render. */
    fun observeActiveNoteSummaries(): Flow<List<NoteSummary>>

    /** Metadata only (no [Note.document]) — what Trash's rows render. */
    fun observeTrashedNoteSummaries(): Flow<List<NoteSummary>>

    /** A single note (active or trashed) by id, with its full [Note.document] — for the editor. */
    fun observeNoteById(noteId: String): Flow<Note?>

    /** Creates a note in [folderId], inheriting that folder's [Note.isLocked] (CLAUDE.md: a
     * folder's lock protects its entire subtree, including items added to it later). */
    suspend fun createNote(folderId: String?): DataResult<Note>

    /** Persists [note]'s content/flags; [Note.updatedAt] is stamped with the current time. */
    suspend fun updateNote(note: Note): DataResult<Unit>

    /**
     * Sets [Note.isFavorite]. Unlike [updateNote], this deliberately leaves [Note.updatedAt] alone —
     * favoriting is a metadata toggle, not a content edit, so it must not move the note in "Date
     * edited" sort or change its "Edited …" label.
     */
    suspend fun setFavorite(noteId: String, isFavorite: Boolean): DataResult<Unit>

    /** Bulk version of [setFavorite] for multi-select favorite/unfavorite. Same [Note.updatedAt]
     * behavior as the single-item overload — left untouched. */
    suspend fun setFavorite(noteIds: List<String>, isFavorite: Boolean): DataResult<Unit>

    /**
     * Sets [Note.isLocked]. Unlike [updateNote], this deliberately leaves [Note.updatedAt] alone —
     * locking is a metadata toggle, not a content edit, so it must not change the note's
     * "Edited …" label. Only writing content or changing the title does that.
     */
    suspend fun setLocked(noteId: String, isLocked: Boolean): DataResult<Unit>

    /**
     * Sets [Note.folderId]. Unlike [updateNote], this deliberately leaves [Note.updatedAt] alone —
     * moving a note to another folder is an organizational change, not a content edit, so it must
     * not move the note in "Date edited" sort or change its "Edited …" label.
     */
    suspend fun setFolder(noteId: String, folderId: String?): DataResult<Unit>

    /** Bulk version of [setFolder] for multi-select "Move to folder", e.g. Home/Folder's bulk move
     * on a selection. Same [Note.updatedAt] behavior as the single-item overload — left untouched. */
    suspend fun setFolder(noteIds: List<String>, folderId: String?): DataResult<Unit>

    /** Soft-deletes: stamps [Note.deletedAt] with the current time (CLAUDE.md's trash invariant). */
    suspend fun moveToTrash(noteId: String): DataResult<Unit>

    /**
     * Bulk version of [moveToTrash] for multi-select, e.g. Home/Folder's "Delete" on a selection.
     * All notes share one [Note.deletedAt] timestamp, same as the single-item overload.
     */
    suspend fun moveToTrash(noteIds: List<String>): DataResult<Unit>

    /** Clears [Note.deletedAt], returning the note to wherever [Note.folderId] still points. */
    suspend fun restoreFromTrash(noteId: String): DataResult<Unit>

    /**
     * Bulk version of [restoreFromTrash] for multi-select, e.g. undoing a bulk delete or Trash's
     * "Restore" on a selection.
     */
    suspend fun restoreFromTrash(noteIds: List<String>): DataResult<Unit>

    /** Hard-deletes every trashed note. The only place hard-delete is allowed (CLAUDE.md). */
    suspend fun emptyTrash(): DataResult<Unit>

    /**
     * Hard-deletes [noteIds] outright instead of trashing them — Trash's multi-select "Delete
     * Forever" on a selection, gated behind a confirmation dialog since it's irreversible. Same
     * "hard-delete only in trash purge" carve-out as [emptyTrash] (CLAUDE.md).
     */
    suspend fun deleteForever(noteIds: List<String>): DataResult<Unit>

    /**
     * Hard-deletes [noteId] outright instead of trashing it. For a just-created note the user left
     * completely blank (no title, no content ever typed), so it never held any user data — the one
     * exception to "hard-delete only in trash purge" (CLAUDE.md), since there is nothing to lose.
     * Never call this for a note that ever held content.
     */
    suspend fun discardBlankNote(noteId: String): DataResult<Unit>
}
