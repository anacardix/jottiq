package com.anacardix.jottiq.domain

/**
 * A note's rich-text body: an ordered list of content blocks. Extend by adding new [NoteBlock]
 * subtypes (e.g. future `Image`, `Table`); never rename existing serialized fields (CLAUDE.md).
 */
data class NoteDocument(val blocks: List<NoteBlock> = emptyList())

/** v1 block kinds: [Paragraph], optionally a heading, bulleted, or numbered. */
sealed interface NoteBlock {
    val id: String

    data class Paragraph(
        override val id: String,
        val text: String = "",
        val heading: HeadingLevel? = null,
        val bulleted: Boolean = false,
        val numbered: Boolean = false,
        val spans: List<FormatSpan> = emptyList(),
    ) : NoteBlock
}

enum class HeadingLevel { H1, H2, H3 }

/** An inline formatting range over a block's text, `[start, end)`. */
data class FormatSpan(val start: Int, val end: Int, val style: FormatStyle)

sealed interface FormatStyle {
    data object Bold : FormatStyle
    data object Italic : FormatStyle
    data object Underline : FormatStyle
    data class TextColor(val color: NoteTextColor) : FormatStyle
    data class Link(val url: String) : FormatStyle
}

/** The fixed 5-swatch palette from the note-editor color popover (`design/07. Edit colors.png`). */
enum class NoteTextColor { Default, Red, Blue, Green, Gold }
