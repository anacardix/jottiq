package com.anacardix.jottiq.domain.usecase

import com.anacardix.jottiq.fakes.folder
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CollectFolderSubtreeIdsUseCaseTest {

    private val collectSubtreeIds = CollectFolderSubtreeIdsUseCase()

    @Test
    fun `a leaf folder with no children returns only itself`() {
        val folders = listOf(folder(id = "sketches", parentId = null))

        val subtree = collectSubtreeIds(folders, rootFolderId = "sketches")

        assertThat(subtree).containsExactly("sketches")
    }

    @Test
    fun `direct children are included`() {
        val folders = listOf(
            folder(id = "personal", parentId = null),
            folder(id = "travel", parentId = "personal"),
            folder(id = "work", parentId = null),
        )

        val subtree = collectSubtreeIds(folders, rootFolderId = "personal")

        assertThat(subtree).containsExactly("personal", "travel")
    }

    @Test
    fun `descendants at every depth are included, siblings are not`() {
        val folders = listOf(
            folder(id = "personal", parentId = null),
            folder(id = "travel", parentId = "personal"),
            folder(id = "japan", parentId = "travel"),
            folder(id = "kyoto", parentId = "japan"),
            folder(id = "sketches", parentId = null),
        )

        val subtree = collectSubtreeIds(folders, rootFolderId = "personal")

        assertThat(subtree).containsExactly("personal", "travel", "japan", "kyoto")
    }

    @Test
    fun `an id not present in the folder list still returns itself`() {
        val subtree = collectSubtreeIds(emptyList(), rootFolderId = "missing")

        assertThat(subtree).containsExactly("missing")
    }
}
