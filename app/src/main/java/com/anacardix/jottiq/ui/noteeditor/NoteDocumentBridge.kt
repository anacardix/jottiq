package com.anacardix.jottiq.ui.noteeditor

import com.anacardix.jottiq.domain.NoteBlock
import com.anacardix.jottiq.domain.NoteDocument
import com.mohamedrejeb.richeditor.model.RichTextState
import java.util.UUID

/**
 * Bridges the sync-ready [NoteDocument] block model to/from the editor's [EditorSegment] list.
 *
 * compose-rich-editor's [RichTextState] doesn't expose its internal per-paragraph structure
 * (heading level, list membership, spans) through public API — only [RichTextState.toHtml] does,
 * via the library's own tested HTML serializer. So HTML is used as the lossless intermediate
 * format in both directions (see `NoteDocumentHtml.kt`): [NoteDocument] blocks are synthesized
 * into a small, self-controlled HTML vocabulary and fed to [RichTextState.setHtml]; conversely
 * [RichTextState.toHtml]'s output is parsed back into [NoteBlock]s.
 *
 * Splitting a [NoteBlock.Paragraph] run into one shared [RichTextState] (and re-splitting it back
 * into paragraphs on every save) means individual paragraph ids are NOT stable across edits — the
 * library exposes no per-paragraph identity to preserve. This is an accepted trade-off of adopting
 * the library; ids remain client-generated UUIDs (CLAUDE.md's sync-ready invariant), just not
 * stable ones for rich-text paragraphs.
 */

/**
 * [NoteDocument] blocks -> segments: every block is a [NoteBlock.Paragraph], all grouped into one
 * [EditorSegment.Rich].
 */
fun NoteDocument.toSegments(): List<EditorSegment> {
    val paragraphs = blocks.map { block -> when (block) { is NoteBlock.Paragraph -> block } }
    if (paragraphs.isEmpty()) return listOf(blankRichSegment())
    return listOf(richSegment(paragraphs))
}

private fun blankRichSegment() = EditorSegment.Rich(id = UUID.randomUUID().toString(), state = newRichTextState())

/** Segments -> [NoteDocument] blocks; each [EditorSegment.Rich] splits back into one [NoteBlock.Paragraph] per line. */
fun List<EditorSegment>.toNoteDocument(): NoteDocument =
    toNoteDocumentCached(cachedBlocks = emptyMap(), dirtySegmentIds = emptySet()).first

/**
 * Segments -> [NoteDocument], like [toNoteDocument], but reuses [cachedBlocks] for any segment
 * whose id is present there and *not* in [dirtySegmentIds] — skipping its HTML round-trip
 * ([EditorSegment.toBlocks]) entirely. This is what makes autosave only pay the parse cost for the
 * segment(s) the user actually touched since the last save, instead of re-parsing every segment on
 * every debounce tick.
 *
 * Returns the built document together with a fresh cache — one entry per segment currently in
 * [this], so a segment removed by a structural edit naturally drops out instead of leaking.
 */
fun List<EditorSegment>.toNoteDocumentCached(
    cachedBlocks: Map<String, List<NoteBlock>>,
    dirtySegmentIds: Set<String>,
): Pair<NoteDocument, Map<String, List<NoteBlock>>> {
    val freshCache = mutableMapOf<String, List<NoteBlock>>()
    val blocks = flatMap { segment ->
        val reusable = cachedBlocks[segment.id]?.takeIf { segment.id !in dirtySegmentIds }
        val parsed = reusable ?: segment.toBlocks()
        freshCache[segment.id] = parsed
        parsed
    }
    return NoteDocument(blocks = blocks) to freshCache
}

private fun EditorSegment.toBlocks(): List<NoteBlock> = when (this) {
    is EditorSegment.Rich -> parseRichHtml(state.toHtml())
}

/**
 * Creates a freshly configured [RichTextState], optionally seeded with [html] (see `toRichHtml`).
 *
 * Heading size is deliberately left entirely to compose-rich-editor's own per-paragraph
 * `HeadingStyle` (applied natively when [RichTextState.setHtml] parses an `<h1>`/`<h2>`/`<h3>` tag,
 * or when [RichTextState.setHeadingStyle] is called) — there is no absolute-size override layered on
 * top. `NoteReadText.kt`'s read mode renders this very same [RichTextState] through the library's
 * own [com.mohamedrejeb.richeditor.ui.BasicRichText], so edit and read mode land on the same size
 * by construction — including under non-linear font scaling — without any cross-paragraph span
 * bookkeeping that could bleed onto a sibling heading.
 */
fun newRichTextState(html: String = ""): RichTextState {
    val state = RichTextState()
    state.config.exitListOnEmptyItem = true
    state.config.preserveStyleOnEmptyLine = true
    if (html.isNotEmpty()) {
        state.setHtml(html)
    }
    return state
}
