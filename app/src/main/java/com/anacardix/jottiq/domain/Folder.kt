package com.anacardix.jottiq.domain

/**
 * A folder; folders nest via [parentId] (`null` = top level). Locking a folder ([isLocked])
 * protects its entire subtree (CLAUDE.md). Same sync-ready invariants as [Note].
 */
data class Folder(
    val id: String,
    val parentId: String?,
    val name: String,
    val isLocked: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
