package com.anacardix.jottiq.ui.noteeditor

import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EmojiRangesTest {

    @Test
    fun `plain text returns the whole range as a single run`() {
        val ranges = nonEmojiSubRanges("Hello world", TextRange(0, 11))

        assertThat(ranges).containsExactly(TextRange(0, 11))
    }

    @Test
    fun `emoji-only selection returns no ranges`() {
        val ranges = nonEmojiSubRanges("😀", TextRange(0, 2))

        assertThat(ranges).isEmpty()
    }

    @Test
    fun `emoji in the middle splits the selection around it`() {
        // "Hi😀!" — H(0) i(1) 😀(2..4, surrogate pair) !(4)
        val ranges = nonEmojiSubRanges("Hi😀!", TextRange(0, 5))

        assertThat(ranges).containsExactly(TextRange(0, 2), TextRange(4, 5)).inOrder()
    }

    @Test
    fun `leading emoji is excluded but trailing text is kept`() {
        val ranges = nonEmojiSubRanges("😀Hi", TextRange(0, 4))

        assertThat(ranges).containsExactly(TextRange(2, 4))
    }

    @Test
    fun `trailing emoji is excluded but leading text is kept`() {
        val ranges = nonEmojiSubRanges("Hi😀", TextRange(0, 4))

        assertThat(ranges).containsExactly(TextRange(0, 2))
    }

    @Test
    fun `a zero width joiner family sequence is excluded as one unit`() {
        // Man + ZWJ + Woman + ZWJ + Girl, surrounded by plain text.
        val family = "👨‍👩‍👧"
        val text = "a${family}b"

        val ranges = nonEmojiSubRanges(text, TextRange(0, text.length))

        assertThat(ranges).containsExactly(TextRange(0, 1), TextRange(text.length - 1, text.length)).inOrder()
    }

    @Test
    fun `a flag built from two regional indicators is excluded as one unit`() {
        // U+1F1EE U+1F1F9 = the Italian flag (two regional-indicator code points).
        val flag = "🇮🇹"
        val text = "a${flag}b"

        val ranges = nonEmojiSubRanges(text, TextRange(0, text.length))

        assertThat(ranges).containsExactly(TextRange(0, 1), TextRange(text.length - 1, text.length)).inOrder()
    }

    @Test
    fun `a keycap emoji sequence is excluded as one unit`() {
        // "1" + Variation Selector-16 + Combining Enclosing Keycap = 1️⃣
        val keycap = "1️⃣"
        val text = "a${keycap}b"

        val ranges = nonEmojiSubRanges(text, TextRange(0, text.length))

        assertThat(ranges).containsExactly(TextRange(0, 1), TextRange(text.length - 1, text.length)).inOrder()
    }

    @Test
    fun `an empty or collapsed range returns no sub-ranges`() {
        assertThat(nonEmojiSubRanges("Hello", TextRange(2, 2))).isEmpty()
        assertThat(nonEmojiSubRanges("", TextRange(0, 0))).isEmpty()
    }

    @Test
    fun `a reversed range is treated the same as its normalized form`() {
        val ranges = nonEmojiSubRanges("Hi😀!", TextRange(5, 0))

        assertThat(ranges).containsExactly(TextRange(0, 2), TextRange(4, 5)).inOrder()
    }
}
