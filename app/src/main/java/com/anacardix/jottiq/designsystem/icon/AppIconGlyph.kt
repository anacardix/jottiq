package com.anacardix.jottiq.designsystem.icon

/**
 * A single Material Symbols Rounded glyph, addressed by its Private Use Area codepoint (the
 * font's `cmap` maps codepoints straight to icon outlines — no ligature/GSUB substitution needed).
 */
@JvmInline
value class AppIconGlyph(val codepoint: Int) {
    /** The glyph as a renderable string, for use as [androidx.compose.material3.Text] content. */
    fun asString(): String = String(Character.toChars(codepoint))
}

/**
 * Codepoint catalog for every Material Symbols Rounded glyph the app uses. `res/font/material_symbols_rounded.ttf`
 * is subsetted to exactly these glyphs (see the font-subsetting note in the designsystem README) —
 * add new icons here *and* re-subset the font, they won't render otherwise.
 *
 * Codepoints sourced from the upstream `MaterialSymbolsRounded[FILL,GRAD,opsz,wght].codepoints` map.
 */
@Suppress("MagicNumber") // codepoints are the glyph identity, not arbitrary numbers
object AppIcons {
    val ArrowBack = AppIconGlyph(0xE5C4)
    val SwapVert = AppIconGlyph(0xE8D5)
    val Delete = AppIconGlyph(0xE92E)
    val Settings = AppIconGlyph(0xE8B8)
    val Folder = AppIconGlyph(0xE2C7)
    val FolderOpen = AppIconGlyph(0xE2C8)
    val Lock = AppIconGlyph(0xE899)
    val LockOpen = AppIconGlyph(0xE898)
    val Star = AppIconGlyph(0xF09A)
    val ChevronRight = AppIconGlyph(0xE5CC)
    val Add = AppIconGlyph(0xE145)
    val Close = AppIconGlyph(0xE5CD)
    val NoteAdd = AppIconGlyph(0xE89C)
    val EditNote = AppIconGlyph(0xE745)
    val CreateNewFolder = AppIconGlyph(0xE2CC)
    val DriveFileMove = AppIconGlyph(0xE9A1)
    val Create = AppIconGlyph(0xF097)
    val FormatBold = AppIconGlyph(0xE238)
    val FormatItalic = AppIconGlyph(0xE23F)
    val FormatUnderlined = AppIconGlyph(0xE249)
    val Palette = AppIconGlyph(0xE40A)
    val Title = AppIconGlyph(0xE264)
    val FormatListBulleted = AppIconGlyph(0xE241)
    val FormatListNumbered = AppIconGlyph(0xE242)
    val Link = AppIconGlyph(0xE250)
    val Restore = AppIconGlyph(0xE8B3)
    val Info = AppIconGlyph(0xE88E)
    val Language = AppIconGlyph(0xEA07)
    val Contrast = AppIconGlyph(0xEB37)
    val Fingerprint = AppIconGlyph(0xE90D)
    val Inventory2 = AppIconGlyph(0xE1A1)
    val Check = AppIconGlyph(0xE668)
    val DragHandle = AppIconGlyph(0xE25D)
    val CalendarToday = AppIconGlyph(0xE935)
    val SortByAlpha = AppIconGlyph(0xE053)
    val Description = AppIconGlyph(0xE873)
}
