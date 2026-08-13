package com.anacardix.jottiq.domain.usecase

import com.anacardix.jottiq.domain.NoteSummary
import com.anacardix.jottiq.domain.SortOrder
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private fun note(id: String, title: String, createdAt: Long, updatedAt: Long) = NoteSummary(
    id = id,
    folderId = null,
    title = title,
    isFavorite = false,
    isLocked = false,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = null,
)

class SortNotesUseCaseTest {

    private val sortNotes = SortNotesUseCase()

    private val notes = listOf(
        note(id = "a", title = "Banana", createdAt = 1L, updatedAt = 20L),
        note(id = "b", title = "apple", createdAt = 3L, updatedAt = 10L),
    )

    @Test
    fun `DateEdited orders by updatedAt descending`() {
        val sorted = sortNotes(notes, SortOrder.DateEdited)

        assertThat(sorted.map { it.id }).containsExactly("a", "b").inOrder()
    }

    @Test
    fun `DateCreated orders by createdAt descending`() {
        val sorted = sortNotes(notes, SortOrder.DateCreated)

        assertThat(sorted.map { it.id }).containsExactly("b", "a").inOrder()
    }

    @Test
    fun `TitleAsc orders case-insensitively`() {
        val sorted = sortNotes(notes, SortOrder.TitleAsc)

        assertThat(sorted.map { it.id }).containsExactly("b", "a").inOrder()
    }
}
