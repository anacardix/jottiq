package com.anacardix.jottiq.fakes

import com.anacardix.jottiq.domain.DataError
import com.anacardix.jottiq.domain.DataResult
import com.anacardix.jottiq.domain.Note
import com.anacardix.jottiq.domain.NoteDocument
import com.anacardix.jottiq.domain.NoteSummary
import com.anacardix.jottiq.domain.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [NotesRepository] fake, reused across screen tests per CLAUDE.md's fakes-first policy. */
class FakeNotesRepository : NotesRepository {

    private val notesFlow = MutableStateFlow<List<Note>>(emptyList())

    /** Set to make the next [createNote] call fail instead of succeeding. */
    var createNoteFailure: DataError? = null

    /** Set to make the next [updateNote] call fail instead of succeeding. */
    var updateNoteFailure: DataError? = null

    /** How many times [updateNote] has been called — for asserting autosave scheduling. */
    var updateNoteCallCount: Int = 0
        private set

    /** Set to make the next [setFavorite] call fail instead of succeeding. */
    var setFavoriteFailure: DataError? = null

    /** Set to make the next [setLocked] call fail instead of succeeding. */
    var setLockedFailure: DataError? = null

    /** Set to make the next [setFolder] call fail instead of succeeding. */
    var setFolderFailure: DataError? = null

    /** Set to make the next [moveToTrash] call fail instead of succeeding. */
    var moveToTrashFailure: DataError? = null

    /** Set to make the next [restoreFromTrash] call fail instead of succeeding. */
    var restoreFromTrashFailure: DataError? = null

    /** Set to make the next [emptyTrash] call fail instead of succeeding. */
    var emptyTrashFailure: DataError? = null

    /** Set to make the next [discardBlankNote] call fail instead of succeeding. */
    var discardBlankNoteFailure: DataError? = null

    /** How many times [discardBlankNote] has been called — for asserting hard-delete-vs-trash choice. */
    var discardBlankNoteCallCount: Int = 0
        private set

    private var nextGeneratedId = 0
    private var fixedTime = 0L

    override fun observeActiveNoteSummaries() =
        notesFlow.map { notes -> notes.filter { it.deletedAt == null }.map { it.toSummary() } }

    override fun observeTrashedNoteSummaries() =
        notesFlow.map { notes -> notes.filter { it.deletedAt != null }.map { it.toSummary() } }

    override fun observeNoteById(noteId: String) = notesFlow.map { notes -> notes.firstOrNull { it.id == noteId } }

    override suspend fun createNote(folderId: String?): DataResult<Note> {
        createNoteFailure?.let { return DataResult.Failure(it) }
        val note = Note(
            id = "fake-note-${nextGeneratedId++}",
            folderId = folderId,
            title = "",
            document = NoteDocument(),
            isFavorite = false,
            isLocked = false,
            createdAt = 0L,
            updatedAt = 0L,
            deletedAt = null,
        )
        notesFlow.update { it + note }
        return DataResult.Success(note)
    }

    override suspend fun updateNote(note: Note): DataResult<Unit> {
        updateNoteCallCount++
        updateNoteFailure?.let { return DataResult.Failure(it) }
        notesFlow.update { current -> current.map { if (it.id == note.id) note else it } }
        return DataResult.Success(Unit)
    }

    override suspend fun setFavorite(noteId: String, isFavorite: Boolean): DataResult<Unit> {
        setFavoriteFailure?.let { return DataResult.Failure(it) }
        notesFlow.update { current ->
            current.map { if (it.id == noteId) it.copy(isFavorite = isFavorite) else it }
        }
        return DataResult.Success(Unit)
    }

    override suspend fun setLocked(noteId: String, isLocked: Boolean): DataResult<Unit> {
        setLockedFailure?.let { return DataResult.Failure(it) }
        notesFlow.update { current ->
            current.map { if (it.id == noteId) it.copy(isLocked = isLocked) else it }
        }
        return DataResult.Success(Unit)
    }

    override suspend fun setFolder(noteId: String, folderId: String?): DataResult<Unit> {
        setFolderFailure?.let { return DataResult.Failure(it) }
        notesFlow.update { current ->
            current.map { if (it.id == noteId) it.copy(folderId = folderId) else it }
        }
        return DataResult.Success(Unit)
    }

    override suspend fun moveToTrash(noteId: String): DataResult<Unit> {
        moveToTrashFailure?.let { return DataResult.Failure(it) }
        notesFlow.update { current ->
            current.map { if (it.id == noteId) it.copy(deletedAt = fixedTime) else it }
        }
        return DataResult.Success(Unit)
    }

    override suspend fun restoreFromTrash(noteId: String): DataResult<Unit> {
        restoreFromTrashFailure?.let { return DataResult.Failure(it) }
        notesFlow.update { current ->
            current.map { if (it.id == noteId) it.copy(deletedAt = null) else it }
        }
        return DataResult.Success(Unit)
    }

    override suspend fun emptyTrash(): DataResult<Unit> {
        emptyTrashFailure?.let { return DataResult.Failure(it) }
        notesFlow.update { current -> current.filter { it.deletedAt == null } }
        return DataResult.Success(Unit)
    }

    override suspend fun discardBlankNote(noteId: String): DataResult<Unit> {
        discardBlankNoteCallCount++
        discardBlankNoteFailure?.let { return DataResult.Failure(it) }
        notesFlow.update { current -> current.filterNot { it.id == noteId } }
        return DataResult.Success(Unit)
    }

    /** The raw backing list, regardless of [Note.deletedAt] — for asserting persisted state in tests. */
    val allNotes: List<Note> get() = notesFlow.value

    fun setNotes(notes: List<Note>) {
        notesFlow.value = notes
    }

    /** Timestamp stamped onto notes by [moveToTrash] — mirrors the real repository's TimeProvider use. */
    fun setFixedTime(time: Long) {
        fixedTime = time
    }
}

private fun Note.toSummary() = NoteSummary(
    id = id,
    folderId = folderId,
    title = title,
    isFavorite = isFavorite,
    isLocked = isLocked,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)
