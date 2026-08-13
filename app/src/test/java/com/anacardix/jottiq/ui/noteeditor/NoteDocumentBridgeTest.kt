package com.anacardix.jottiq.ui.noteeditor

import androidx.compose.ui.text.TextRange
import com.anacardix.jottiq.domain.FormatSpan
import com.anacardix.jottiq.domain.FormatStyle
import com.anacardix.jottiq.domain.HeadingLevel
import com.anacardix.jottiq.domain.NoteBlock
import com.anacardix.jottiq.domain.NoteDocument
import com.anacardix.jottiq.domain.NoteTextColor
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NoteDocumentBridgeTest {

    @Test
    fun `empty document round-trips to a single blank rich segment`() {
        val segments = NoteDocument().toSegments()

        assertThat(segments).hasSize(1)
        assertThat(segments.single()).isInstanceOf(EditorSegment.Rich::class.java)

        val roundTripped = segments.toNoteDocument()
        assertThat(roundTripped.blocks).hasSize(1)
        val block = roundTripped.blocks.single() as NoteBlock.Paragraph
        assertThat(block.text).isEmpty()
        assertThat(block.heading).isNull()
        assertThat(block.bulleted).isFalse()
    }

    @Test
    fun `plain paragraph text round-trips`() {
        val document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Hello world")))

        val result = document.toSegments().toNoteDocument()

        assertThat(result.blocks).hasSize(1)
        val block = result.blocks.single() as NoteBlock.Paragraph
        assertThat(block.text).isEqualTo("Hello world")
    }

    @Test
    fun `heading level round-trips`() {
        val document = NoteDocument(
            blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Title", heading = HeadingLevel.H1)),
        )

        val result = document.toSegments().toNoteDocument()

        val block = result.blocks.single() as NoteBlock.Paragraph
        assertThat(block.text).isEqualTo("Title")
        assertThat(block.heading).isEqualTo(HeadingLevel.H1)
    }

    // Regression test: a previous implementation kept a second, hand-rolled absolute-size span
    // layered on top of the library's own heading styling, computed from a caret-offset range per
    // heading paragraph. In a multi-paragraph segment that range bookkeeping could drift and bleed
    // onto a sibling paragraph (e.g. setting H1 on one line visibly resized a neighboring H2 line).
    // Heading level is now carried purely by the domain block + the `<h1|h2|h3>` HTML tag — no
    // per-paragraph span juggling — so each paragraph's heading level must survive the round-trip
    // independently of its siblings, regardless of how many other headings share the segment.
    @Test
    fun `each paragraph in a multi-heading segment keeps its own heading level independent of its siblings`() {
        val document = NoteDocument(
            blocks = listOf(
                NoteBlock.Paragraph(id = "p0", text = "sbn"),
                NoteBlock.Paragraph(id = "p1", text = "dhhd", heading = HeadingLevel.H3),
                NoteBlock.Paragraph(id = "p2", text = "hdhshsdhduhsusdh", heading = HeadingLevel.H1),
                NoteBlock.Paragraph(id = "p3", text = "hhshs", heading = HeadingLevel.H2),
            ),
        )

        val result = document.toSegments().toNoteDocument()

        val blocks = result.blocks.map { it as NoteBlock.Paragraph }
        assertThat(blocks.map { it.text }).containsExactly("sbn", "dhhd", "hdhshsdhduhsusdh", "hhshs").inOrder()
        assertThat(blocks[0].heading).isNull()
        assertThat(blocks[1].heading).isEqualTo(HeadingLevel.H3)
        assertThat(blocks[2].heading).isEqualTo(HeadingLevel.H1)
        assertThat(blocks[3].heading).isEqualTo(HeadingLevel.H2)
    }

    @Test
    fun `consecutive bulleted paragraphs round-trip as separate bulleted blocks`() {
        val document = NoteDocument(
            blocks = listOf(
                NoteBlock.Paragraph(id = "p1", text = "Milk", bulleted = true),
                NoteBlock.Paragraph(id = "p2", text = "Eggs", bulleted = true),
            ),
        )

        val result = document.toSegments().toNoteDocument()

        assertThat(result.blocks).hasSize(2)
        val blocks = result.blocks.map { it as NoteBlock.Paragraph }
        assertThat(blocks.map { it.text }).containsExactly("Milk", "Eggs").inOrder()
        assertThat(blocks.all { it.bulleted }).isTrue()
    }

    @Test
    fun `consecutive numbered paragraphs round-trip as separate numbered blocks`() {
        val document = NoteDocument(
            blocks = listOf(
                NoteBlock.Paragraph(id = "p1", text = "Milk", numbered = true),
                NoteBlock.Paragraph(id = "p2", text = "Eggs", numbered = true),
            ),
        )

        val result = document.toSegments().toNoteDocument()

        assertThat(result.blocks).hasSize(2)
        val blocks = result.blocks.map { it as NoteBlock.Paragraph }
        assertThat(blocks.map { it.text }).containsExactly("Milk", "Eggs").inOrder()
        assertThat(blocks.all { it.numbered }).isTrue()
    }

    // Numbered-list counterpart of the bulleted "blank paragraph after an exited list" regression
    // below — exercises the same `richSegment` padding path, but through the variable-width "N. "
    // prefix instead of the fixed-width "• " one (see `EditorBlockHelpers.kt`'s `sumAnnotatedLength`).
    @Test
    fun `blank paragraph after an exited numbered list stays unnumbered across reload`() {
        val document = NoteDocument(
            blocks = listOf(
                NoteBlock.Paragraph(id = "p1", text = "Milk", numbered = true),
                NoteBlock.Paragraph(id = "p2", text = ""),
            ),
        )

        val segments = document.toSegments()
        val state = (segments.single() as EditorSegment.Rich).state
        state.selection = TextRange(state.annotatedString.text.length)
        assertThat(state.isOrderedList).isFalse()

        val result = segments.toNoteDocument()
        assertThat(result.blocks).hasSize(2)
        val blocks = result.blocks.map { it as NoteBlock.Paragraph }
        assertThat(blocks[0].numbered).isTrue()
        assertThat(blocks[1].text).isEmpty()
        assertThat(blocks[1].numbered).isFalse()
    }

    // Regression test: pressing Enter on an empty bulleted list item exits the list, leaving a
    // blank, non-bulleted paragraph after the bulleted run (`config.exitListOnEmptyItem` in
    // `NoteDocumentBridge.kt`'s `newRichTextState`). That trailing blank paragraph is exactly the
    // one `toRichHtml`'s `appendBlock` omits from the encoded HTML (see `NoteDocumentHtml.kt`), so
    // `richSegment` has to pad it back in with a live newline typed after the bulleted item. Left
    // uncorrected, the editor's own "Enter continues the list" behavior retypes that padded
    // paragraph as a new bulleted item instead of the plain one it stands for -- an HTML round-trip
    // through `toNoteDocument()` alone doesn't reveal this (the library's `toHtml()` renders the
    // stray empty list item the same either way), but the live paragraph type is what the editor
    // actually renders a bullet glyph from, so it's asserted directly here.
    @Test
    fun `blank paragraph after an exited bulleted list stays unbulleted across reload`() {
        val document = NoteDocument(
            blocks = listOf(
                NoteBlock.Paragraph(id = "p1", text = "Milk", bulleted = true),
                NoteBlock.Paragraph(id = "p2", text = ""),
            ),
        )

        val segments = document.toSegments()
        val state = (segments.single() as EditorSegment.Rich).state
        state.selection = TextRange(state.annotatedString.text.length)
        assertThat(state.isUnorderedList).isFalse()

        val result = segments.toNoteDocument()
        assertThat(result.blocks).hasSize(2)
        val blocks = result.blocks.map { it as NoteBlock.Paragraph }
        assertThat(blocks[0].bulleted).isTrue()
        assertThat(blocks[1].text).isEmpty()
        assertThat(blocks[1].bulleted).isFalse()
    }

    // Known upstream bug, not yet fixable: pressing Enter twice in a row on a bulleted list item
    // (the second Enter lands on the now-empty item and exits the list, per `exitListOnEmptyItem`
    // above) corrupts a link sitting in the very next paragraph of the same Rich segment. Root
    // cause is inside compose-rich-editor's `RichTextState.checkForParagraphs()`: the
    // `exitListOnEmptyItem` branch does `index--; continue` to skip re-registering a split
    // paragraph, which leaves the just-typed '\n' in the raw text buffer without a matching
    // `RichParagraph`/`RichSpan` boundary — desyncing every span index after that point, including
    // the link's. Confirmed present in 1.0.0-rc14 (this project's pinned version). The 1.0.0 stable
    // release's changelog (diff-based edit pipeline, link-edge-typing fixes) looked promising but
    // can't be adopted: richeditor-compose:1.0.0 requires Kotlin 2.4.0 metadata, unreadable by AGP
    // 9.2.1's built-in Kotlin 2.2.x compiler -- upgrading past that is a separate, larger toolchain
    // change (AGP 9.3.0 + Gradle 9.5.0+) tracked independently of this bug. Re-check on the next
    // richeditor bump: un-@Ignore this test and see if it passes.
    @Ignore(
        "Upstream compose-rich-editor bug (checkForParagraphs' exitListOnEmptyItem branch); not " +
            "fixable without a larger AGP/Kotlin toolchain bump than richeditor-compose:1.0.0 " +
            "requires. Re-test on next richeditor upgrade.",
    )
    @Test
    fun `link after an exited bulleted list survives pressing Enter twice on the last bullet`() {
        val linkText = "Visit example"
        val linkSpan = FormatSpan(6, 13, FormatStyle.Link("https://example.com"))
        val document = NoteDocument(
            blocks = listOf(
                NoteBlock.Paragraph(id = "p1", text = "Milk", bulleted = true),
                NoteBlock.Paragraph(id = "p2", text = linkText, spans = listOf(linkSpan)),
            ),
        )

        val segment = document.toSegments().single() as EditorSegment.Rich
        val state = segment.state

        // Enter #1: cursor at the end of the non-empty "Milk" bullet -> new empty bullet item.
        val afterMilk = state.annotatedString.text.indexOf("Milk") + "Milk".length
        state.addTextAtIndex(afterMilk, "\n")
        assertThat(state.isUnorderedList).isTrue()

        // Enter #2: cursor on that now-empty bullet item -> exits the list.
        state.addTextAtIndex(state.selection.min, "\n")
        assertThat(state.isUnorderedList).isFalse()

        val result = listOf<EditorSegment>(segment).toNoteDocument()
        val linkBlock = result.blocks.last() as NoteBlock.Paragraph
        assertThat(linkBlock.text).isEqualTo(linkText)
        assertThat(linkBlock.spans).containsExactly(linkSpan)
    }

    @Test
    fun `bold italic and underline spans round-trip`() {
        val text = "Bold Italic Underline"
        val spans = listOf(
            FormatSpan(0, 4, FormatStyle.Bold),
            FormatSpan(5, 11, FormatStyle.Italic),
            FormatSpan(12, 21, FormatStyle.Underline),
        )
        val document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = text, spans = spans)))

        val result = document.toSegments().toNoteDocument()

        val block = result.blocks.single() as NoteBlock.Paragraph
        assertThat(block.text).isEqualTo(text)
        assertThat(block.spans).containsExactlyElementsIn(spans)
    }

    @Test
    fun `overlapping bold and italic spans round-trip`() {
        val text = "BoldItalicBoth"
        val spans = listOf(
            FormatSpan(0, 10, FormatStyle.Bold),
            FormatSpan(4, 14, FormatStyle.Italic),
        )
        val document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = text, spans = spans)))

        val result = document.toSegments().toNoteDocument()

        val block = result.blocks.single() as NoteBlock.Paragraph
        assertThat(block.text).isEqualTo(text)
        val bold = block.spans.filter { it.style is FormatStyle.Bold }
        val italic = block.spans.filter { it.style is FormatStyle.Italic }
        assertThat(coveredRange(bold)).isEqualTo(0 to 10)
        assertThat(coveredRange(italic)).isEqualTo(4 to 14)
    }

    @Test
    fun `text color span round-trips`() {
        val text = "Red text"
        val spans = listOf(FormatSpan(0, 3, FormatStyle.TextColor(NoteTextColor.Red)))
        val document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = text, spans = spans)))

        val result = document.toSegments().toNoteDocument()

        val block = result.blocks.single() as NoteBlock.Paragraph
        assertThat(block.spans).containsExactly(FormatSpan(0, 3, FormatStyle.TextColor(NoteTextColor.Red)))
    }

    @Test
    fun `link span round-trips`() {
        val text = "Visit example"
        val spans = listOf(FormatSpan(6, 13, FormatStyle.Link("https://example.com")))
        val document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = text, spans = spans)))

        val result = document.toSegments().toNoteDocument()

        val block = result.blocks.single() as NoteBlock.Paragraph
        assertThat(block.spans).containsExactly(FormatSpan(6, 13, FormatStyle.Link("https://example.com")))
    }

    @Test
    fun `schemeless link normalizes to https when it round-trips through the html bridge`() {
        val text = "Visit example"
        val spans = listOf(FormatSpan(6, 13, FormatStyle.Link("example.com")))
        val document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = text, spans = spans)))

        val result = document.toSegments().toNoteDocument()

        val block = result.blocks.single() as NoteBlock.Paragraph
        assertThat(block.spans).containsExactly(FormatSpan(6, 13, FormatStyle.Link("https://example.com")))
    }

    @Test
    fun `a blank line between paragraphs survives the round-trip`() {
        val document = NoteDocument(
            blocks = listOf(
                NoteBlock.Paragraph(id = "p1", text = "One"),
                NoteBlock.Paragraph(id = "p2", text = ""),
                NoteBlock.Paragraph(id = "p3", text = "Two"),
            ),
        )

        val result = document.toSegments().toNoteDocument()

        assertThat(result.blocks.map { (it as NoteBlock.Paragraph).text })
            .containsExactly("One", "", "Two")
            .inOrder()
    }

    @Test
    fun `a live line break inside a paragraph parses back as two blocks with split spans`() {
        val document = NoteDocument(
            blocks = listOf(
                NoteBlock.Paragraph(
                    id = "p1",
                    text = "Milkoat",
                    spans = listOf(FormatSpan(0, 4, FormatStyle.Bold)),
                ),
            ),
        )
        val segment = document.toSegments().single() as EditorSegment.Rich
        // Enter mid-word, exactly as the IME delivers it.
        segment.state.addTextAtIndex(2, "\n")

        val result = listOf<EditorSegment>(segment).toNoteDocument()

        assertThat(result.blocks).hasSize(2)
        val first = result.blocks[0] as NoteBlock.Paragraph
        val second = result.blocks[1] as NoteBlock.Paragraph
        assertThat(first.text).isEqualTo("Mi")
        assertThat(first.spans).containsExactly(FormatSpan(0, 2, FormatStyle.Bold))
        // (How much of the bold run carries over to the second line is the library's business —
        // only the text split itself is pinned here.)
        assertThat(second.text).isEqualTo("lkoat")
    }

    @Test
    fun `ampersand and angle bracket characters in text survive round-trip`() {
        val text = "Tom & Jerry <3 code"
        val document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = text)))

        val result = document.toSegments().toNoteDocument()

        assertThat((result.blocks.single() as NoteBlock.Paragraph).text).isEqualTo(text)
    }

    private fun coveredRange(spans: List<FormatSpan>): Pair<Int, Int> {
        val start = spans.minOf { it.start }
        val end = spans.maxOf { it.end }
        return start to end
    }
}
