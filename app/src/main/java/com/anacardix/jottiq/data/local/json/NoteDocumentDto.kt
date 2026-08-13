package com.anacardix.jottiq.data.local.json

import com.anacardix.jottiq.domain.FormatSpan
import com.anacardix.jottiq.domain.FormatStyle
import com.anacardix.jottiq.domain.HeadingLevel
import com.anacardix.jottiq.domain.NoteBlock
import com.anacardix.jottiq.domain.NoteDocument
import com.anacardix.jottiq.domain.NoteTextColor
import kotlinx.serialization.Serializable

/**
 * JSON mirror of [NoteDocument], stored in [com.anacardix.jottiq.data.local.entity.NoteEntity.documentJson].
 * Kept separate from the domain model so `kotlinx.serialization` stays a data-layer concern —
 * mapping happens at the repository boundary via [toDomain]/[toDto] (CLAUDE.md).
 */
@Serializable
data class NoteDocumentDto(val blocks: List<NoteBlockDto> = emptyList())

@Serializable
sealed interface NoteBlockDto {
    val id: String

    @Serializable
    data class Paragraph(
        override val id: String,
        val text: String = "",
        val heading: HeadingLevel? = null,
        val bulleted: Boolean = false,
        val spans: List<FormatSpanDto> = emptyList(),
        val numbered: Boolean = false,
    ) : NoteBlockDto
}

@Serializable
data class FormatSpanDto(val start: Int, val end: Int, val style: FormatStyleDto)

@Serializable
sealed interface FormatStyleDto {
    @Serializable
    data object Bold : FormatStyleDto

    @Serializable
    data object Italic : FormatStyleDto

    @Serializable
    data object Underline : FormatStyleDto

    @Serializable
    data class TextColor(val color: NoteTextColor) : FormatStyleDto

    @Serializable
    data class Link(val url: String) : FormatStyleDto
}

fun NoteDocument.toDto(): NoteDocumentDto = NoteDocumentDto(blocks = blocks.map { it.toDto() })

fun NoteDocumentDto.toDomain(): NoteDocument = NoteDocument(blocks = blocks.map { it.toDomain() })

private fun NoteBlock.toDto(): NoteBlockDto = when (this) {
    is NoteBlock.Paragraph -> NoteBlockDto.Paragraph(
        id = id,
        text = text,
        heading = heading,
        bulleted = bulleted,
        spans = spans.map { it.toDto() },
        numbered = numbered,
    )
}

private fun NoteBlockDto.toDomain(): NoteBlock = when (this) {
    is NoteBlockDto.Paragraph -> NoteBlock.Paragraph(
        id = id,
        text = text,
        heading = heading,
        bulleted = bulleted,
        numbered = numbered,
        spans = spans.map { it.toDomain() },
    )
}

private fun FormatSpan.toDto(): FormatSpanDto = FormatSpanDto(start, end, style.toDto())

private fun FormatSpanDto.toDomain(): FormatSpan = FormatSpan(start, end, style.toDomain())

private fun FormatStyle.toDto(): FormatStyleDto = when (this) {
    FormatStyle.Bold -> FormatStyleDto.Bold
    FormatStyle.Italic -> FormatStyleDto.Italic
    FormatStyle.Underline -> FormatStyleDto.Underline
    is FormatStyle.TextColor -> FormatStyleDto.TextColor(color)
    is FormatStyle.Link -> FormatStyleDto.Link(url)
}

private fun FormatStyleDto.toDomain(): FormatStyle = when (this) {
    FormatStyleDto.Bold -> FormatStyle.Bold
    FormatStyleDto.Italic -> FormatStyle.Italic
    FormatStyleDto.Underline -> FormatStyle.Underline
    is FormatStyleDto.TextColor -> FormatStyle.TextColor(color)
    is FormatStyleDto.Link -> FormatStyle.Link(url)
}
