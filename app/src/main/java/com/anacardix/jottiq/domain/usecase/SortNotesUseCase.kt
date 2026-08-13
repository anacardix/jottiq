package com.anacardix.jottiq.domain.usecase

import com.anacardix.jottiq.domain.NoteSummary
import com.anacardix.jottiq.domain.SortOrder
import javax.inject.Inject

/** Orders notes per Home/Folder-view's sort menu (`design/03. Sort.png`). */
class SortNotesUseCase @Inject constructor() {
    operator fun invoke(notes: List<NoteSummary>, order: SortOrder): List<NoteSummary> = when (order) {
        SortOrder.DateEdited -> notes.sortedByDescending { it.updatedAt }
        SortOrder.DateCreated -> notes.sortedByDescending { it.createdAt }
        SortOrder.TitleAsc -> notes.sortedBy { it.title.lowercase() }
    }
}
