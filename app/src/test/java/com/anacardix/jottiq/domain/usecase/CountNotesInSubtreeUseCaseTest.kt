package com.anacardix.jottiq.domain.usecase

import com.anacardix.jottiq.domain.Folder
import com.anacardix.jottiq.domain.NoteSummary
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CountNotesInSubtreeUseCaseTest {

    private val useCase = CountNotesInSubtreeUseCase()

    private fun folder(id: String, parentId: String?) = Folder(
        id = id,
        parentId = parentId,
        name = id,
        isLocked = false,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
    )

    private fun note(id: String, folderId: String?) = NoteSummary(
        id = id,
        folderId = folderId,
        title = id,
        isFavorite = false,
        isLocked = false,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
    )

    @Test
    fun `counts only notes directly in the folder when it has no children`() {
        val folders = listOf(folder("personal", parentId = null))
        val notes = listOf(note("apartment-ideas", folderId = "personal"))

        val counts = useCase(folders, notes)

        assertThat(counts["personal"]).isEqualTo(1)
    }

    @Test
    fun `rolls up notes from nested descendant folders`() {
        // personal (1 own note) -> travel (0 own notes) -> japan-2026 (2 own notes)
        val folders = listOf(
            folder("personal", parentId = null),
            folder("travel", parentId = "personal"),
            folder("japan-2026", parentId = "travel"),
        )
        val notes = listOf(
            note("apartment-ideas", folderId = "personal"),
            note("flights", folderId = "japan-2026"),
            note("packing-list", folderId = "japan-2026"),
        )

        val counts = useCase(folders, notes)

        assertThat(counts["personal"]).isEqualTo(3)
        assertThat(counts["travel"]).isEqualTo(2)
        assertThat(counts["japan-2026"]).isEqualTo(2)
    }

    @Test
    fun `empty folder with no notes or children counts zero`() {
        val folders = listOf(folder("sketches", parentId = null))

        val counts = useCase(folders, emptyList())

        assertThat(counts["sketches"]).isEqualTo(0)
    }

    @Test
    fun `sibling folders do not leak into an unrelated folder's count`() {
        val folders = listOf(
            folder("work", parentId = null),
            folder("journal", parentId = null),
        )
        val notes = listOf(note("standup-notes", folderId = "journal"))

        val counts = useCase(folders, notes)

        assertThat(counts["work"]).isEqualTo(0)
        assertThat(counts["journal"]).isEqualTo(1)
    }

    @Test
    fun `computes counts for every folder in a single call`() {
        val folders = listOf(
            folder("personal", parentId = null),
            folder("travel", parentId = "personal"),
            folder("work", parentId = null),
        )
        val notes = listOf(
            note("n1", folderId = "personal"),
            note("n2", folderId = "travel"),
            note("n3", folderId = "work"),
        )

        val counts = useCase(folders, notes)

        assertThat(counts).containsExactly("personal", 2, "travel", 1, "work", 1)
    }
}
