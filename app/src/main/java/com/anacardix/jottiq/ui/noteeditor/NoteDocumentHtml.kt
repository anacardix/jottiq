package com.anacardix.jottiq.ui.noteeditor

import androidx.compose.ui.graphics.Color
import com.anacardix.jottiq.domain.FormatSpan
import com.anacardix.jottiq.domain.FormatStyle
import com.anacardix.jottiq.domain.HeadingLevel
import com.anacardix.jottiq.domain.NoteBlock
import java.util.Locale
import kotlin.math.roundToInt

/**
 * NoteDocument -> HTML synthesis (the encode half of the codec behind `NoteDocumentBridge.kt`):
 * builds the small, self-controlled HTML vocabulary (p/h1-3/ul/ol/li/b/i/u/a/span) fed to
 * `RichTextState.setHtml`. The decode half lives in `NoteDocumentHtmlParser.kt`.
 */

// A paragraph carrying both a heading and the bulleted/numbered flag is a legacy combination the
// current toolbar never produces (heading and list are mutually exclusive editor actions going
// forward, and so are bulleted and numbered); bulleted wins over numbered, and either wins over
// heading, rather than emitting invalid nested markup.
internal fun List<NoteBlock.Paragraph>.toRichHtml(): String = buildString {
    var i = 0
    while (i < this@toRichHtml.size) {
        val block = this@toRichHtml[i]
        i = when {
            block.bulleted -> appendListRun(this@toRichHtml, i, tag = "ul") { it.bulleted }
            block.numbered -> appendListRun(this@toRichHtml, i, tag = "ol") { it.numbered }
            else -> appendBlock(block, i, isLast = i == this@toRichHtml.lastIndex)
        }
    }
}

private fun StringBuilder.appendListRun(
    blocks: List<NoteBlock.Paragraph>,
    start: Int,
    tag: String,
    inRun: (NoteBlock.Paragraph) -> Boolean,
): Int {
    append("<$tag>")
    var i = start
    while (i < blocks.size && inRun(blocks[i])) {
        val item = blocks[i]
        append("<li>").append(encodeInlineHtml(item.text, item.spans)).append("</li>")
        i++
    }
    append("</$tag>")
    return i
}

private fun StringBuilder.appendBlock(block: NoteBlock.Paragraph, index: Int, isLast: Boolean): Int {
    // An empty line is what RichTextState.setHtml understands as a bare <br> — an empty <p></p>
    // gets silently dropped by it. A trailing empty line is omitted entirely instead, because
    // setHtml re-reads a trailing <br> as TWO blank lines. Every caller that turns this HTML back
    // into a RichTextState (EditorBlockHelpers.kt's richSegment) pads the resulting state back out
    // live to compensate, so this omission is purely an encoding detail, not a lossy trade-off.
    if (block.text.isEmpty() && block.heading == null) {
        if (!isLast) append("<br>")
        return index + 1
    }
    val tag = block.heading.htmlTag()
    append("<$tag>").append(encodeInlineHtml(block.text, block.spans)).append("</$tag>")
    return index + 1
}

private fun HeadingLevel?.htmlTag(): String = when (this) {
    HeadingLevel.H1 -> "h1"
    HeadingLevel.H2 -> "h2"
    HeadingLevel.H3 -> "h3"
    null -> "p"
}

/** Slices [text] into maximal runs of constant active-span-set, wrapping each run in its own tags. */
private fun encodeInlineHtml(text: String, spans: List<FormatSpan>): String {
    if (spans.isEmpty()) return escapeHtmlText(text)
    val cuts = (spans.flatMap { listOf(it.start, it.end) } + listOf(0, text.length))
        .filter { it in 0..text.length }
        .toSortedSet()
        .toList()
    return buildString {
        for (i in 0 until cuts.size - 1) {
            val segStart = cuts[i]
            val segEnd = cuts[i + 1]
            if (segStart >= segEnd) continue
            val active = spans.filter { it.start <= segStart && it.end >= segEnd }
            append(wrapWithTags(escapeHtmlText(text.substring(segStart, segEnd)), active.map { it.style }))
        }
    }
}

private fun wrapWithTags(text: String, styles: List<FormatStyle>): String {
    var result = text
    if (styles.any { it is FormatStyle.Bold }) result = "<b>$result</b>"
    if (styles.any { it is FormatStyle.Italic }) result = "<i>$result</i>"
    if (styles.any { it is FormatStyle.Underline }) result = "<u>$result</u>"
    styles.filterIsInstance<FormatStyle.TextColor>().firstOrNull()?.let { style -> result = result.wrapColor(style) }
    styles.filterIsInstance<FormatStyle.Link>().firstOrNull()?.let { style ->
        // Normalized here (not just at link-creation time) so a URL saved before normalizeLinkUrl
        // existed, or edited outside the app, still gets a valid scheme before it's fed into
        // RichTextState.setHtml — the only place read mode (BasicRichText) reads a link's target
        // from, since it opens the URL itself and has no hook for us to fix it up at tap time.
        result = "<a href=\"${escapeHtmlAttribute(normalizeLinkUrl(style.url))}\">$result</a>"
    }
    return result
}

private fun String.wrapColor(style: FormatStyle.TextColor): String {
    val color = NoteTextColorPalette.colorFor(style.color) ?: return this
    return "<span style=\"color:${color.toCssHex()}\">$this</span>"
}

private fun Color.toCssHex(): String {
    val r = (red * MAX_BYTE).roundToInt()
    val g = (green * MAX_BYTE).roundToInt()
    val b = (blue * MAX_BYTE).roundToInt()
    return String.format(Locale.ROOT, "#%02x%02x%02x", r, g, b)
}

private const val MAX_BYTE = 255

private fun escapeHtmlText(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

private fun escapeHtmlAttribute(value: String): String = escapeHtmlText(value).replace("\"", "&quot;")
