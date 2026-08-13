package com.anacardix.jottiq.domain.usecase

import com.anacardix.jottiq.domain.Folder
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private fun folder(id: String, parentId: String?, name: String, isLocked: Boolean = false) = Folder(
    id = id,
    parentId = parentId,
    name = name,
    isLocked = isLocked,
    createdAt = 0L,
    updatedAt = 0L,
    deletedAt = null,
)

class BuildFolderTreeUseCaseTest {

    private val buildFolderTree = BuildFolderTreeUseCase()

    @Test
    fun `empty folder list produces an empty tree`() {
        assertThat(buildFolderTree(emptyList())).isEmpty()
    }

    @Test
    fun `top-level folders are sorted alphabetically at depth 1`() {
        val folders = listOf(
            folder(id = "w", parentId = null, name = "Work"),
            folder(id = "j", parentId = null, name = "Journal"),
        )

        val tree = buildFolderTree(folders)

        assertThat(tree).containsExactly(
            FolderTreeRow("j", "Journal", depth = 1, isLocked = false),
            FolderTreeRow("w", "Work", depth = 1, isLocked = false),
        ).inOrder()
    }

    @Test
    fun `nested folders appear depth-first directly after their parent`() {
        val folders = listOf(
            folder(id = "personal", parentId = null, name = "Personal"),
            folder(id = "sketches", parentId = null, name = "Sketches"),
            folder(id = "travel", parentId = "personal", name = "Travel"),
            folder(id = "japan", parentId = "travel", name = "Japan 2026"),
        )

        val tree = buildFolderTree(folders)

        assertThat(tree.map { it.id }).containsExactly("personal", "travel", "japan", "sketches").inOrder()
        assertThat(tree.first { it.id == "travel" }.depth).isEqualTo(2)
        assertThat(tree.first { it.id == "japan" }.depth).isEqualTo(3)
    }

    @Test
    fun `isLocked is carried through from the source folder`() {
        val folders = listOf(folder(id = "journal", parentId = null, name = "Journal", isLocked = true))

        val tree = buildFolderTree(folders)

        assertThat(tree.single().isLocked).isTrue()
    }
}
