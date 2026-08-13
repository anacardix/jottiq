package com.anacardix.jottiq.domain.usecase

import com.anacardix.jottiq.domain.Folder
import javax.inject.Inject

/** One row of a flattened, depth-first folder hierarchy (`design/10. Move to folder.png`). */
data class FolderTreeRow(val id: String, val name: String, val depth: Int, val isLocked: Boolean)

/**
 * Flattens [folders] into a depth-first list for the Move-to-folder sheet: children sorted
 * alphabetically (case-insensitive) within each level, matching the same order Home/Folder-view
 * use for their own folder rows.
 */
class BuildFolderTreeUseCase @Inject constructor() {
    operator fun invoke(folders: List<Folder>): List<FolderTreeRow> = walk(folders, parentId = null, depth = 1)

    private fun walk(folders: List<Folder>, parentId: String?, depth: Int): List<FolderTreeRow> =
        folders.filter { it.parentId == parentId }
            .sortedBy { it.name.lowercase() }
            .flatMap { folder ->
                listOf(FolderTreeRow(folder.id, folder.name, depth, folder.isLocked)) +
                    walk(folders, folder.id, depth + 1)
            }
}
