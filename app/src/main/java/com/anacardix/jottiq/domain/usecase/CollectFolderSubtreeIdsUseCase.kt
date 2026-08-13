package com.anacardix.jottiq.domain.usecase

import com.anacardix.jottiq.domain.Folder
import javax.inject.Inject

/**
 * Collects [rootFolderId] itself plus every folder beneath it in [folders], walking [Folder.parentId].
 * A folder's lock/trash fate applies to its whole subtree (CLAUDE.md), so trashing or restoring a
 * folder needs this set to cascade to descendant folders (and, at the repository boundary, to the
 * notes living in any of them).
 */
class CollectFolderSubtreeIdsUseCase @Inject constructor() {
    operator fun invoke(folders: List<Folder>, rootFolderId: String): Set<String> {
        val childrenByParentId = folders.groupBy { it.parentId }
        val subtreeIds = mutableSetOf(rootFolderId)
        val pending = ArrayDeque(listOf(rootFolderId))
        while (pending.isNotEmpty()) {
            val currentId = pending.removeFirst()
            childrenByParentId[currentId].orEmpty().forEach { child ->
                if (subtreeIds.add(child.id)) {
                    pending.addLast(child.id)
                }
            }
        }
        return subtreeIds
    }
}
