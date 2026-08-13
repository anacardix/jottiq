package com.anacardix.jottiq.domain.usecase

import com.anacardix.jottiq.domain.Folder
import com.anacardix.jottiq.domain.SortOrder
import javax.inject.Inject

/** Orders folders per Home/Folder-view's sort menu (`design/03. Sort.png`). */
class SortFoldersUseCase @Inject constructor() {
    operator fun invoke(folders: List<Folder>, order: SortOrder): List<Folder> = when (order) {
        SortOrder.DateEdited -> folders.sortedByDescending { it.updatedAt }
        SortOrder.DateCreated -> folders.sortedByDescending { it.createdAt }
        SortOrder.TitleAsc -> folders.sortedBy { it.name.lowercase() }
    }
}
