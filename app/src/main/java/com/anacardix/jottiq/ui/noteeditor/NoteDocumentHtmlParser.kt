package com.anacardix.jottiq.ui.noteeditor

import com.anacardix.jottiq.domain.FormatSpan
import com.anacardix.jottiq.domain.FormatStyle
import com.anacardix.jottiq.domain.HeadingLevel
import com.anacardix.jottiq.domain.NoteBlock
import com.anacardix.jottiq.domain.NoteTextColor
import com.mohamedrejeb.ksoup.entities.KsoupEntities
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import java.util.UUID
import kotlin.math.roundToInt

/**
 * HTML -> NoteDocument parsing (the decode half of the codec behind `NoteDocumentBridge.kt`):
 * parses the vocabulary `NoteDocumentHtml.kt`'s `toRichHtml` produces back out of
 * `RichTextState.toHtml`'s output into [NoteBlock.Paragraph]s.
 */

private class ParagraphBuilder(val heading: HeadingLevel?, val bulleted: Boolean, val numbered: Boolean) {
    val text = StringBuilder()
    val spans = mutableListOf<FormatSpan>()
}

private class OpenSpan(val tag: String, val start: Int, val extra: String?)

/** Parses the constrained HTML vocabulary `RichTextState.toHtml` emits (p/h1-3/ul/ol/li/b/i/u/a/span). */
internal fun parseRichHtml(html: String): List<NoteBlock.Paragraph> = RichHtmlParser().parse(html)

private class RichHtmlParser {
    private val paragraphs = mutableListOf<NoteBlock.Paragraph>()
    private var current: ParagraphBuilder? = null
    private val openSpans = ArrayDeque<OpenSpan>()
    private val openListTags = ArrayDeque<String>()

    fun parse(html: String): List<NoteBlock.Paragraph> {
        val handler = KsoupHtmlHandler.Builder()
            .onOpenTag { name, attributes, _ -> openTag(name, attributes) }
            .onText { text -> current?.text?.append(KsoupEntities.decodeHtml(text)) }
            .onCloseTag { name, _ -> closeTag(name) }
            .build()
        KsoupHtmlParser(handler = handler).parseComplete(html)
        finishParagraph()
        return paragraphs
    }

    private fun openTag(name: String, attributes: Map<String, String>) {
        when (name) {
            "h1" -> current = ParagraphBuilder(HeadingLevel.H1, bulleted = false, numbered = false)
            "h2" -> current = ParagraphBuilder(HeadingLevel.H2, bulleted = false, numbered = false)
            "h3" -> current = ParagraphBuilder(HeadingLevel.H3, bulleted = false, numbered = false)
            "p" -> current = ParagraphBuilder(heading = null, bulleted = false, numbered = false)
            "ul", "ol" -> openListTags.addLast(name)
            "li" -> current = ParagraphBuilder(
                heading = null,
                bulleted = openListTags.lastOrNull() == "ul",
                numbered = openListTags.lastOrNull() == "ol",
            )
            "br" -> lineBreak()
            "b", "strong" -> openSpan("b")
            "i", "em" -> openSpan("i")
            "u", "ins" -> openSpan("u")
            "a" -> openSpan("a", attributes["href"].orEmpty())
            "span" -> openSpan("span", parseCssColor(attributes["style"])?.name)
        }
    }

    private fun openSpan(tag: String, extra: String? = null) {
        openSpans.addLast(OpenSpan(tag, current?.text?.length ?: 0, extra))
    }

    // RichTextState.toHtml serializes an empty line as a bare <br> between blocks, and a mid-
    // paragraph line break (one live Enter keystroke) as <br> inside the enclosing <p> — both must
    // become their own paragraph block or blank lines silently vanish on save.
    private fun lineBreak() {
        val builder = current
        if (builder == null) {
            paragraphs += NoteBlock.Paragraph(id = UUID.randomUUID().toString())
            return
        }
        // Close every open inline span at the break, then reopen it at the continuation's start.
        val reopened = openSpans.map { OpenSpan(it.tag, start = 0, extra = it.extra) }
        openSpans.forEach { open ->
            val end = builder.text.length
            spanStyleFor(open.tag, open.extra)?.let { builder.spans.add(FormatSpan(open.start, end, it)) }
        }
        finishParagraph()
        current = ParagraphBuilder(builder.heading, builder.bulleted, builder.numbered)
        openSpans.clear()
        openSpans.addAll(reopened)
    }

    private fun closeTag(name: String) {
        when (name) {
            "h1", "h2", "h3", "p", "li" -> finishParagraph()
            "ul", "ol" -> openListTags.removeLastOrNull()
            "b", "strong" -> closeSpan("b")
            "i", "em" -> closeSpan("i")
            "u", "ins" -> closeSpan("u")
            "a" -> closeSpan("a")
            "span" -> closeSpan("span")
        }
    }

    private fun finishParagraph() {
        val builder = current ?: return
        paragraphs += NoteBlock.Paragraph(
            id = UUID.randomUUID().toString(),
            text = builder.text.toString(),
            heading = builder.heading,
            bulleted = builder.bulleted,
            numbered = builder.numbered,
            spans = builder.spans.toList(),
        )
        current = null
    }

    private fun closeSpan(tag: String) {
        val open = openSpans.removeLastMatching { it.tag == tag } ?: return
        val end = current?.text?.length ?: open.start
        spanStyleFor(tag, open.extra)?.let { current?.spans?.add(FormatSpan(open.start, end, it)) }
    }

    private fun spanStyleFor(tag: String, extra: String?): FormatStyle? = when (tag) {
        "b" -> FormatStyle.Bold
        "i" -> FormatStyle.Italic
        "u" -> FormatStyle.Underline
        "a" -> FormatStyle.Link(extra.orEmpty())
        "span" -> extra?.let { FormatStyle.TextColor(NoteTextColor.valueOf(it)) }
        else -> null
    }
}

private fun <T> ArrayDeque<T>.removeLastMatching(predicate: (T) -> Boolean): T? {
    val index = indexOfLast(predicate)
    if (index < 0) return null
    return removeAt(index)
}

private val RgbaColorRegex = Regex("""color:\s*rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)""")
private val HexColorRegex = Regex("""color:\s*#([0-9a-fA-F]{6})""")
private const val HEX_COMPONENT_LENGTH = 2
private const val HEX_RADIX = 16
private const val MAX_BYTE_F = 255f

private fun parseCssColor(style: String?): NoteTextColor? {
    if (style == null) return null
    val hexMatch = HexColorRegex.find(style)
    val rgbaMatch = RgbaColorRegex.find(style)
    return when {
        hexMatch != null -> {
            val hex = hexMatch.groupValues[1]
            matchNoteTextColor(hexComponent(hex, 0), hexComponent(hex, 1), hexComponent(hex, 2))
        }
        rgbaMatch != null -> matchNoteTextColor(
            r = rgbaMatch.groupValues[1].toFloat().roundToInt(),
            g = rgbaMatch.groupValues[2].toFloat().roundToInt(),
            b = rgbaMatch.groupValues[3].toFloat().roundToInt(),
        )
        else -> null
    }
}

private fun hexComponent(hex: String, index: Int): Int {
    val start = index * HEX_COMPONENT_LENGTH
    return hex.substring(start, start + HEX_COMPONENT_LENGTH).toInt(HEX_RADIX)
}

private fun matchNoteTextColor(r: Int, g: Int, b: Int): NoteTextColor? {
    val candidates = mapOf(
        NoteTextColor.Red to NoteTextColorPalette.Red,
        NoteTextColor.Blue to NoteTextColorPalette.Blue,
        NoteTextColor.Green to NoteTextColorPalette.Green,
        NoteTextColor.Gold to NoteTextColorPalette.Gold,
    )
    return candidates.entries.firstOrNull { (_, color) ->
        (color.red * MAX_BYTE_F).roundToInt() == r &&
            (color.green * MAX_BYTE_F).roundToInt() == g &&
            (color.blue * MAX_BYTE_F).roundToInt() == b
    }?.key
}
