package com.anacardix.jottiq.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe route for [com.anacardix.jottiq.ui.folder.FolderScreen], showing [folderId]'s
 * contents.
 */
@Serializable
data class FolderRoute(val folderId: String)
