package com.anacardix.jottiq.ui.noteeditor

import androidx.compose.ui.text.TextRange
import java.text.BreakIterator

/**
 * Splits [range] of [text] into the maximal sub-ranges that contain no emoji grapheme clusters, so
 * formatting like underline or text color can be applied to the surrounding text without visibly
 * smearing across an emoji glyph. Walks by grapheme cluster (via [BreakIterator]), not UTF-16 char,
 * so multi-code-point sequences (ZWJ families, flags, skin-tone modifiers) are excluded as one unit
 * even though a single emoji can span several `Char`s.
 */
fun nonEmojiSubRanges(text: String, range: TextRange): List<TextRange> {
    val start = range.min.coerceIn(0, text.length)
    val end = range.max.coerceIn(0, text.length)
    if (start >= end) return emptyList()

    val boundary = BreakIterator.getCharacterInstance()
    boundary.setText(text)

    val result = mutableListOf<TextRange>()
    var runStart = -1
    var index = start
    while (index < end) {
        // following(index) is the next boundary after index, i.e. the end of whichever cluster
        // contains index — correct whether or not index itself sits on a boundary.
        val next = boundary.following(index)
        val clusterEnd = if (next == BreakIterator.DONE) end else minOf(next, end)
        if (isEmojiCluster(text, index, clusterEnd)) {
            if (runStart != -1) {
                result += TextRange(runStart, index)
                runStart = -1
            }
        } else if (runStart == -1) {
            runStart = index
        }
        index = clusterEnd
    }
    if (runStart != -1) result += TextRange(runStart, end)
    return result
}

private fun isEmojiCluster(text: String, start: Int, end: Int): Boolean {
    var i = start
    while (i < end) {
        val codePoint = text.codePointAt(i)
        if (isEmojiCodePoint(codePoint)) return true
        i += Character.charCount(codePoint)
    }
    return false
}

// Code points below are Unicode block boundaries and combining-mark values, not arbitrary magic
// numbers — there is no framework constant for "is this an emoji" on JDK 17 (Character.isEmoji
// only exists from JDK 21), so ranges are checked explicitly. This is a heuristic covering the
// common emoji blocks plus the combining marks used to build multi-code-point emoji sequences; it
// is not a full Unicode emoji-property implementation.
@Suppress("MagicNumber")
private fun isEmojiCodePoint(codePoint: Int): Boolean = when (codePoint) {
    in 0x1F300..0x1F5FF, // Miscellaneous Symbols and Pictographs
    in 0x1F600..0x1F64F, // Emoticons
    in 0x1F680..0x1F6FF, // Transport and Map Symbols
    in 0x1F900..0x1F9FF, // Supplemental Symbols and Pictographs
    in 0x1FA70..0x1FAFF, // Symbols and Pictographs Extended-A
    in 0x2600..0x26FF, // Miscellaneous Symbols
    in 0x2700..0x27BF, // Dingbats
    in 0x1F1E6..0x1F1FF, // Regional Indicator Symbols (flag letter pairs)
    in 0x1F3FB..0x1F3FF, // Emoji skin-tone modifiers
    0x200D, // Zero Width Joiner (chains emoji into one glyph, e.g. family sequences)
    0xFE0F, // Variation Selector-16 (forces emoji presentation)
    0x20E3, // Combining Enclosing Keycap (e.g. 1️⃣)
    -> true
    else -> false
}
