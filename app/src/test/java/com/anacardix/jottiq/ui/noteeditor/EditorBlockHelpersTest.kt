package com.anacardix.jottiq.ui.noteeditor

import com.anacardix.jottiq.domain.NoteBlock
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditorBlockHelpersTest {

    @Test
    fun `a scheme-less url gets an https prefix`() {
        assertThat(normalizeLinkUrl("example.com")).isEqualTo("https://example.com")
    }

    @Test
    fun `a scheme-less url with leading and trailing whitespace is trimmed then prefixed`() {
        assertThat(normalizeLinkUrl("  example.com  ")).isEqualTo("https://example.com")
    }

    @Test
    fun `an http url is left unchanged`() {
        assertThat(normalizeLinkUrl("http://example.com")).isEqualTo("http://example.com")
    }

    @Test
    fun `an https url is left unchanged`() {
        assertThat(normalizeLinkUrl("https://example.com")).isEqualTo("https://example.com")
    }

    @Test
    fun `a mailto url is left unchanged`() {
        assertThat(normalizeLinkUrl("mailto:a@b.com")).isEqualTo("mailto:a@b.com")
    }

    @Test
    fun `a tel url is left unchanged`() {
        assertThat(normalizeLinkUrl("tel:123456")).isEqualTo("tel:123456")
    }

    @Test
    fun `a blank url normalizes to an empty string`() {
        assertThat(normalizeLinkUrl("   ")).isEqualTo("")
    }

    @Test
    fun `paragraphBlocks restores a trailing blank line RichTextState toHtml drops`() {
        // "One", Enter, Enter: row 2 and row 3 both end up blank. RichTextState.toHtml() (the
        // library's own HTML encoder) then silently drops the <br> for the very last paragraph
        // when the paragraph before it is blank too, so a naive parseRichHtml(toHtml()) would
        // report only 2 rows even though the live editor (and its caret) still has 3.
        val state = newRichTextState("<p>One</p>")
        state.addTextAtIndex(state.annotatedString.text.length, "\n")
        state.addTextAtIndex(state.annotatedString.text.length, "\n")

        val blocks = state.paragraphBlocks()

        assertThat(blocks.map { it.text }).containsExactly("One", "", "").inOrder()
    }

    @Test
    fun `richSegment keeps a lone trailing blank paragraph toRichHtml would otherwise drop`() {
        // toRichHtml() intentionally omits the <br> for a lone trailing blank paragraph, since
        // setHtml would misread it as two blank lines. richSegment must restore it live so a
        // structural edit that makes a mid-note blank line "last" in the rebuilt half doesn't
        // silently lose that row.
        val blocks = listOf(
            NoteBlock.Paragraph(id = "p1", text = "One"),
            NoteBlock.Paragraph(id = "p2", text = ""),
        )

        val segment = richSegment(blocks)

        assertThat(segment.state.paragraphBlocks().map { it.text }).containsExactly("One", "").inOrder()
    }

    @Test
    fun `richSegment on a single blank block does not double the blank line`() {
        val blocks = listOf(NoteBlock.Paragraph(id = "p1", text = ""))

        val segment = richSegment(blocks)

        assertThat(segment.state.paragraphBlocks().map { it.text }).containsExactly("")
    }

    // A numbered item's "N. " prefix grows by a digit once the run passes 9 items, unlike the
    // bullet's fixed-width "• " — this pins that the annotated-length caret math in
    // `sumAnnotatedLength` tracks each item's actual position within its run (1..10) rather than
    // assuming a constant prefix width, across the single- to double-digit boundary.
    @Test
    fun `richSegment keeps every item once a numbered run crosses the double-digit boundary`() {
        val blocks = (1..10).map { NoteBlock.Paragraph(id = "p$it", text = "x", numbered = true) }

        val segment = richSegment(blocks)

        val restored = segment.state.paragraphBlocks()
        assertThat(restored).hasSize(10)
        assertThat(restored.map { it.text }).containsExactlyElementsIn(List(10) { "x" })
        assertThat(restored.all { it.numbered }).isTrue()
    }
}
