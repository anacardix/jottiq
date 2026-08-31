package com.anacardix.jottiq.data

import com.anacardix.jottiq.data.local.TransactionRunner
import com.anacardix.jottiq.data.local.dao.FolderDao
import com.anacardix.jottiq.data.local.dao.NoteDao
import com.anacardix.jottiq.data.local.entity.NoteEntity
import com.anacardix.jottiq.data.local.entity.NoteSummaryEntity
import com.anacardix.jottiq.data.local.json.NoteDocumentDto
import com.anacardix.jottiq.data.local.json.toDomain
import com.anacardix.jottiq.data.local.json.toDto
import com.anacardix.jottiq.di.DefaultDispatcher
import com.anacardix.jottiq.domain.DataResult
import com.anacardix.jottiq.domain.Note
import com.anacardix.jottiq.domain.NoteDocument
import com.anacardix.jottiq.domain.NoteSummary
import com.anacardix.jottiq.domain.NotesRepository
import com.anacardix.jottiq.domain.TimeProvider
import com.anacardix.jottiq.domain.runCatchingDataResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

@Suppress("TooManyFunctions") // one focused method per repository capability; none is a pass-through
class NotesRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao,
    private val timeProvider: TimeProvider,
    private val json: Json,
    private val transactionRunner: TransactionRunner,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : NotesRepository {

    override fun observeActiveNoteSummaries(): Flow<List<NoteSummary>> =
        noteDao.observeActiveSummaries().map { entities -> entities.map { it.toDomain() } }.flowOn(defaultDispatcher)

    override fun observeTrashedNoteSummaries(): Flow<List<NoteSummary>> =
        noteDao.observeTrashedSummaries().map { entities -> entities.map { it.toDomain() } }.flowOn(defaultDispatcher)

    override fun observeNoteById(noteId: String): Flow<Note?> =
        noteDao.observeById(noteId).map { entity -> entity?.toDomain(json) }.flowOn(defaultDispatcher)

    override suspend fun createNote(folderId: String?): DataResult<Note> = runCatchingDataResult {
        val now = timeProvider.nowEpochMillis()
        // Inherits the parent folder's lock (CLAUDE.md: a folder's lock protects its entire
        // subtree) so a note created inside an already-locked folder is locked from the start.
        val isLocked = folderId?.let { id -> folderDao.isFolderLocked(id) } ?: false
        val note = Note(
            id = UUID.randomUUID().toString(),
            folderId = folderId,
            title = "",
            document = NoteDocument(),
            isFavorite = false,
            isLocked = isLocked,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
        noteDao.upsert(note.toEntity(json))
        note
    }

    override suspend fun updateNote(note: Note): DataResult<Unit> = runCatchingDataResult {
        val updated = note.copy(updatedAt = timeProvider.nowEpochMillis())
        noteDao.upsert(updated.toEntity(json))
    }

    override suspend fun setFavorite(noteId: String, isFavorite: Boolean): DataResult<Unit> = runCatchingDataResult {
        noteDao.setFavorite(noteId, isFavorite)
    }

    override suspend fun setFavorite(noteIds: List<String>, isFavorite: Boolean): DataResult<Unit> =
        runCatchingDataResult {
            noteDao.setFavoriteForIds(noteIds, isFavorite)
        }

    override suspend fun setLocked(noteId: String, isLocked: Boolean): DataResult<Unit> = runCatchingDataResult {
        noteDao.setLocked(noteId, isLocked)
    }

    override suspend fun setFolder(noteId: String, folderId: String?): DataResult<Unit> = runCatchingDataResult {
        noteDao.setFolderId(noteId, folderId)
    }

    override suspend fun setFolder(noteIds: List<String>, folderId: String?): DataResult<Unit> =
        runCatchingDataResult {
            noteDao.setFolderIdForIds(noteIds, folderId)
        }

    override suspend fun moveToTrash(noteId: String): DataResult<Unit> = runCatchingDataResult {
        noteDao.setDeletedAt(noteId, timeProvider.nowEpochMillis())
    }

    override suspend fun moveToTrash(noteIds: List<String>): DataResult<Unit> = runCatchingDataResult {
        noteDao.setDeletedAtForIds(noteIds, timeProvider.nowEpochMillis())
    }

    override suspend fun restoreFromTrash(noteId: String): DataResult<Unit> = runCatchingDataResult {
        // Transactional: without it, a folder could be trashed between the active-folder read and
        // the reparent write, letting a note get "restored" into a folder that's no longer active.
        transactionRunner.run {
            val activeFolderIds = folderDao.getActiveOnce().map { it.id }
            noteDao.restoreReparentingIfOrphan(noteId, activeFolderIds)
        }
    }

    override suspend fun restoreFromTrash(noteIds: List<String>): DataResult<Unit> = runCatchingDataResult {
        // Same reasoning as the single-item overload: one active-folder snapshot shared by every
        // reparent write in this transaction, so a mid-batch folder trash can't orphan a restore.
        transactionRunner.run {
            val activeFolderIds = folderDao.getActiveOnce().map { it.id }
            noteIds.forEach { noteId -> noteDao.restoreReparentingIfOrphan(noteId, activeFolderIds) }
        }
    }

    override suspend fun emptyTrash(): DataResult<Unit> = runCatchingDataResult {
        noteDao.deleteAllTrashed()
    }

    override suspend fun deleteForever(noteIds: List<String>): DataResult<Unit> = runCatchingDataResult {
        noteDao.deleteByIds(noteIds)
    }

    override suspend fun discardBlankNote(noteId: String): DataResult<Unit> = runCatchingDataResult {
        noteDao.deleteById(noteId)
    }
}

private fun NoteEntity.toDomain(json: Json): Note = Note(
    id = id,
    folderId = folderId,
    title = title,
    document = json.decodeFromString<NoteDocumentDto>(documentJson).toDomain(),
    isFavorite = isFavorite,
    isLocked = isLocked,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun NoteSummaryEntity.toDomain(): NoteSummary = NoteSummary(
    id = id,
    folderId = folderId,
    title = title,
    isFavorite = isFavorite,
    isLocked = isLocked,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun Note.toEntity(json: Json): NoteEntity = NoteEntity(
    id = id,
    folderId = folderId,
    title = title,
    documentJson = json.encodeToString(document.toDto()),
    isFavorite = isFavorite,
    isLocked = isLocked,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)
