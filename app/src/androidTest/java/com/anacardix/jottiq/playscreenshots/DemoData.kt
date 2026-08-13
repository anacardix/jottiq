package com.anacardix.jottiq.playscreenshots

import kotlinx.serialization.Serializable

/**
 * Mirrors the schema of `play/demo-data/<locale>.json` (bundled into this androidTest APK as an
 * asset — see the `androidTest` source set's `assets.srcDirs` in app/build.gradle.kts). One file
 * per locale (it, en, fr, de, es-ES, es-419, pt-PT, pt-BR); ids match across all of them, only
 * the visible strings differ.
 *
 * Span offsets are given as a literal substring to match ([DemoSpanJson.match]) rather than raw
 * character indices, so hand-authoring demo copy doesn't require counting characters — see
 * [DemoDataSeeder] for how a match is resolved to a [com.anacardix.jottiq.domain.FormatSpan].
 */
@Serializable
data class DemoDataJson(
    val folders: List<DemoFolderJson> = emptyList(),
    val notes: List<DemoNoteJson> = emptyList(),
    val trashedNotes: List<DemoNoteJson> = emptyList(),
) {
    // Ids are the one thing that stay constant across every locale's JSON (only the visible
    // strings are translated) — the capture test navigates by looking up the current locale's
    // translated text through these, instead of hardcoding any one language's copy.
    fun folderNamed(id: String): String = folders.first { it.id == id }.name
    fun noteTitled(id: String): String = (notes + trashedNotes).first { it.id == id }.title
    fun noteBlockText(id: String, blockIndex: Int): String =
        notes.first { it.id == id }.blocks[blockIndex].text
}

@Serializable
data class DemoFolderJson(
    val id: String,
    val parentId: String? = null,
    val name: String,
    val locked: Boolean = false,
)

@Serializable
data class DemoNoteJson(
    val id: String,
    val folderId: String? = null,
    val title: String,
    val favorite: Boolean = false,
    val locked: Boolean = false,
    val createdAtOffsetMinutes: Long,
    val updatedAtOffsetMinutes: Long,
    val deletedAtOffsetMinutes: Long? = null,
    val blocks: List<DemoBlockJson> = emptyList(),
)

@Serializable
data class DemoBlockJson(
    val heading: String? = null,
    val bulleted: Boolean = false,
    val numbered: Boolean = false,
    val text: String,
    val spans: List<DemoSpanJson> = emptyList(),
)

@Serializable
data class DemoSpanJson(
    val match: String,
    val style: String,
    val color: String? = null,
    val url: String? = null,
)
