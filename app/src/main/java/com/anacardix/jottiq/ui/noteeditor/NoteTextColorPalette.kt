package com.anacardix.jottiq.ui.noteeditor

import androidx.compose.ui.graphics.Color
import com.anacardix.jottiq.domain.NoteTextColor

/**
 * The fixed 5-swatch color popover palette, sampled from `design/07. Edit colors.png`.
 * [NoteTextColor.Default] deliberately has no [Color] here — it means "no color override," so
 * the surrounding theme's text color (and dark mode) keeps working.
 */
@Suppress("MagicNumber")
object NoteTextColorPalette {
    val Red = Color(0xFFA53328)
    val Blue = Color(0xFF356DEB)
    val Green = Color(0xFF36693A)
    val Gold = Color(0xFF835C1C)

    fun colorFor(noteTextColor: NoteTextColor): Color? = when (noteTextColor) {
        NoteTextColor.Default -> null
        NoteTextColor.Red -> Red
        NoteTextColor.Blue -> Blue
        NoteTextColor.Green -> Green
        NoteTextColor.Gold -> Gold
    }

    fun noteTextColorFor(color: Color): NoteTextColor? = when (color) {
        Red -> NoteTextColor.Red
        Blue -> NoteTextColor.Blue
        Green -> NoteTextColor.Green
        Gold -> NoteTextColor.Gold
        else -> null
    }
}
