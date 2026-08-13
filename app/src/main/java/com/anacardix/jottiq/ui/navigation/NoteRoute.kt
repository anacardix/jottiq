package com.anacardix.jottiq.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe route for [com.anacardix.jottiq.ui.noteeditor.NoteEditorScreen], showing [noteId]. */
@Serializable
data class NoteRoute(val noteId: String)
