package com.anacardix.jottiq.domain.usecase

import com.anacardix.jottiq.domain.Folder
import com.anacardix.jottiq.domain.SortOrder
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private fun folder(id: String, name: String, createdAt: Long, updatedAt: Long) = Folder(
    id = id,
    parentId = null,
    name = name,
    isLocked = false,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = null,
)

class SortFoldersUseCaseTest {

    private val sortFolders = SortFoldersUseCase()

    private val folders = listOf(
        folder(id = "a", name = "Work", createdAt = 1L, updatedAt = 20L),
        folder(id = "b", name = "journal", createdAt = 3L, updatedAt = 10L),
    )

    @Test
    fun `DateEdited orders by updatedAt descending`() {
        val sorted = sortFolders(folders, SortOrder.DateEdited)

        assertThat(sorted.map { it.id }).containsExactly("a", "b").inOrder()
    }

    @Test
    fun `DateCreated orders by createdAt descending`() {
        val sorted = sortFolders(folders, SortOrder.DateCreated)

        assertThat(sorted.map { it.id }).containsExactly("b", "a").inOrder()
    }

    @Test
    fun `TitleAsc orders case-insensitively`() {
        val sorted = sortFolders(folders, SortOrder.TitleAsc)

        assertThat(sorted.map { it.id }).containsExactly("b", "a").inOrder()
    }
}
