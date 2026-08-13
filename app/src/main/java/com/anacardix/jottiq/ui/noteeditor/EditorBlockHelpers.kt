package com.anacardix.jottiq.ui.noteeditor

import com.anacardix.jottiq.domain.NoteBlock
import com.mohamedrejeb.richeditor.model.RichTextState
import java.util.UUID

/** Shared block helpers behind `NoteDocumentBridge.kt`'s HTML round-trip. */

/**
 * The affected segment's live state, parsed back into paragraph blocks (never empty).
 *
 * [RichTextState.toHtml] drops one or more trailing blank paragraphs' `<br>` when the paragraph
 * before them is also blank (a library quirk), so a bare `parseRichHtml(toHtml())` can silently
 * return fewer rows than the live editor's [RichTextState.selection]/`annotatedString` actually
 * has. Pad the parsed blocks back out with empty paragraphs until their combined annotated length
 * matches the live text, so a caret sitting on one of the dropped rows never mis-maps onto an
 * earlier one.
 */
internal fun RichTextState.paragraphBlocks(): List<NoteBlock.Paragraph> {
    val blocks = parseRichHtml(toHtml()).ifEmpty { listOf(NoteBlock.Paragraph(id = UUID.randomUUID().toString())) }
        .toMutableList()
    var length = blocks.sumAnnotatedLength() + (blocks.size - 1) * PARAGRAPH_SEPARATOR_LENGTH
    val liveLength = annotatedString.text.length
    while (length < liveLength) {
        blocks += NoteBlock.Paragraph(id = UUID.randomUUID().toString())
        length += PARAGRAPH_SEPARATOR_LENGTH
    }
    return blocks
}

/**
 * Builds a fresh [EditorSegment.Rich] for [blocks]. Used by `NoteDocumentBridge.kt`'s
 * `toSegments()` to build the note's Rich segment on load.
 *
 * [toRichHtml] intentionally omits the `<br>` for a lone trailing blank paragraph (see
 * `NoteDocumentHtml.kt`'s `appendBlock`), since [RichTextState.setHtml] would otherwise misread a
 * trailing `<br>` as two blank lines. Left uncompensated, that drops the last blank row of any Rich
 * run ending in a blank paragraph. Pad the constructed state back out with live newlines until its
 * length matches [blocks]' expected layout, mirroring [paragraphBlocks]'s read-side fix.
 *
 * The block that omission ever drops is always a plain, non-list paragraph (`toRichHtml`'s
 * `appendBlock` only omits when `heading == null` and it never runs for a bulleted/numbered block
 * in the first place — those go through `appendListRun`, which never omits). But typing the
 * compensating newline lands at the end of whatever paragraph precedes it, so if that paragraph is
 * a list item, the live editor's own "Enter continues the list" behavior resurrects the padding as
 * a new list item instead of the plain one it stands for (the "blank bullet/number reappears after
 * reopening the note" bug). Strip that back off when it happens.
 */
internal fun richSegment(blocks: List<NoteBlock.Paragraph>): EditorSegment.Rich {
    val nonEmpty = blocks.ifEmpty { listOf(NoteBlock.Paragraph(id = UUID.randomUUID().toString())) }
    val state = newRichTextState(nonEmpty.toRichHtml())
    val expectedLength = nonEmpty.sumAnnotatedLength() + (nonEmpty.size - 1) * PARAGRAPH_SEPARATOR_LENGTH
    var padded = false
    while (state.annotatedString.text.length < expectedLength) {
        state.addTextAtIndex(state.annotatedString.text.length, "\n")
        padded = true
    }
    if (padded) {
        if (state.isUnorderedList) state.removeUnorderedList()
        if (state.isOrderedList) state.removeOrderedList()
    }
    return EditorSegment.Rich(id = UUID.randomUUID().toString(), state = state)
}

// Matches an explicit URI scheme (e.g. "https:", "mailto:", "tel:") per RFC 3986.
private val URL_SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")

/**
 * A URL typed without a scheme (e.g. "example.com") can't be opened by [android.net.Uri]/
 * `UriHandler` — normalize it to an `https://` URL so links always resolve. URLs that already
 * carry a scheme (`http:`, `mailto:`, `tel:`, …) are left untouched.
 */
internal fun normalizeLinkUrl(raw: String): String {
    val trimmed = raw.trim()
    return when {
        trimmed.isEmpty() -> trimmed
        URL_SCHEME_REGEX.containsMatchIn(trimmed) -> trimmed
        else -> "https://$trimmed"
    }
}

// The annotated-text layout RichTextState renders: paragraphs joined by ONE space, bulleted lines
// prefixed with "• " (2 chars), numbered lines prefixed with "N. " (RichTextState's default
// OrderedListStyleType.Decimal: the item's 1-based position within its consecutive numbered run,
// plus the ". " suffix — so the prefix length varies with both the item's position and its digit
// count, unlike the bullet's fixed width). Pinned by NoteEditorViewModelTest's caret assertions.
private const val PARAGRAPH_SEPARATOR_LENGTH = 1
private const val BULLET_PREFIX_LENGTH = 2
private const val NUMBERED_SUFFIX_LENGTH = 2 // ". "

/** Sums [NoteBlock.Paragraph.annotatedLength], resolving each numbered item's position within its run. */
private fun List<NoteBlock.Paragraph>.sumAnnotatedLength(): Int {
    var positionInRun = 0
    var total = 0
    for (block in this) {
        positionInRun = if (block.numbered) positionInRun + 1 else 0
        total += block.annotatedLength(positionInRun)
    }
    return total
}

private fun NoteBlock.Paragraph.prefixLength(positionInRun: Int): Int = when {
    bulleted -> BULLET_PREFIX_LENGTH
    numbered -> positionInRun.toString().length + NUMBERED_SUFFIX_LENGTH
    else -> 0
}

private fun NoteBlock.Paragraph.annotatedLength(positionInRun: Int): Int = prefixLength(positionInRun) + text.length
