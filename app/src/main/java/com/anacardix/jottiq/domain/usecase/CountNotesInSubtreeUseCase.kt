package com.anacardix.jottiq.domain.usecase

import com.anacardix.jottiq.domain.Folder
import com.anacardix.jottiq.domain.NoteSummary
import javax.inject.Inject

/**
 * Counts non-deleted notes inside each folder's subtree: its own notes plus every descendant
 * folder's notes, recursively. This is what Home's folder rows display (`design/01. Home.png`)
 * — e.g. "Personal 3" rolls up its one direct note plus the two notes nested under
 * Personal → Travel → Japan 2026.
 *
 * Computed for every folder in one bottom-up pass (each folder's count is resolved once and
 * memoized) rather than per folder, so the whole result is O(folders + notes) instead of
 * rescanning [allNotes] once per folder visited.
 */
class CountNotesInSubtreeUseCase @Inject constructor() {
    operator fun invoke(allFolders: List<Folder>, allNotes: List<NoteSummary>): Map<String, Int> {
        val notesByFolderId = allNotes.groupingBy { it.folderId }.eachCount()
        val childrenByParentId = allFolders.groupBy { it.parentId }
        val counts = mutableMapOf<String, Int>()

        fun resolve(folderId: String): Int = counts.getOrPut(folderId) {
            val ownCount = notesByFolderId[folderId] ?: 0
            val childCount = childrenByParentId[folderId].orEmpty().sumOf { child -> resolve(child.id) }
            ownCount + childCount
        }

        allFolders.forEach { resolve(it.id) }
        return counts
    }
}
