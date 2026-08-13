package com.anacardix.jottiq.data.local.json

import com.anacardix.jottiq.domain.FormatSpan
import com.anacardix.jottiq.domain.FormatStyle
import com.anacardix.jottiq.domain.HeadingLevel
import com.anacardix.jottiq.domain.NoteBlock
import com.anacardix.jottiq.domain.NoteDocument
import com.anacardix.jottiq.domain.NoteTextColor
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class NoteDocumentDtoTest {

    private val json = Json

    @Test
    fun `document with every block and span kind round-trips through JSON unchanged`() {
        val document = NoteDocument(
            blocks = listOf(
                NoteBlock.Paragraph(
                    id = "block-1",
                    text = "For Saturday — farmers market first, then the co-op.",
                    heading = HeadingLevel.H1,
                    bulleted = false,
                    spans = listOf(
                        FormatSpan(4, 12, FormatStyle.Bold),
                        FormatSpan(4, 12, FormatStyle.Italic),
                        FormatSpan(0, 3, FormatStyle.Underline),
                        FormatSpan(0, 3, FormatStyle.TextColor(NoteTextColor.Red)),
                        FormatSpan(20, 30, FormatStyle.Link("https://example.com")),
                    ),
                ),
                NoteBlock.Paragraph(id = "block-2", text = "Milk (oat)"),
                NoteBlock.Paragraph(id = "block-3", text = "Eggs", numbered = true),
            ),
        )

        val roundTripped = json.decodeFromString<NoteDocumentDto>(
            json.encodeToString(document.toDto()),
        ).toDomain()

        assertThat(roundTripped).isEqualTo(document)
    }

    @Test
    fun `empty document round-trips to an empty document`() {
        val document = NoteDocument()

        val roundTripped = json.decodeFromString<NoteDocumentDto>(
            json.encodeToString(document.toDto()),
        ).toDomain()

        assertThat(roundTripped).isEqualTo(document)
    }

    @Test
    fun `paragraph JSON without a numbered field deserializes as unnumbered`() {
        // Simulates a note persisted before the numbered field existed (CLAUDE.md: never rename or
        // require existing serialized fields) — the field must default rather than fail to parse.
        val legacyJson = """
            {"blocks":[{"type":"com.anacardix.jottiq.data.local.json.NoteBlockDto.Paragraph",
            "id":"block-1","text":"Milk","bulleted":true}]}
        """.trimIndent()

        val decoded = json.decodeFromString<NoteDocumentDto>(legacyJson).toDomain()

        val paragraph = decoded.blocks.single() as NoteBlock.Paragraph
        assertThat(paragraph.bulleted).isTrue()
        assertThat(paragraph.numbered).isFalse()
    }
}
