package com.anacardix.jottiq.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.anacardix.jottiq.R

// Weight-axis values (400/500/550/600/650) are the tokens themselves — see design-tokens.png.
@Suppress("MagicNumber")
private object RobotoWeight {
    val Normal = FontWeight.Normal
    val Medium = FontWeight.Medium
    val Title = FontWeight(550)
    val SemiBold = FontWeight.SemiBold
    val Emphasized = FontWeight(650)
}

/**
 * Roboto (variable font, `res/font/roboto.ttf`, Google's official `[ital,wdth,wght]` build) pinned
 * at the weight buckets the design tokens use. Each [Font] entry sets its own `wght` axis via
 * [FontVariation] so Compose renders the exact weight a [TextStyle.fontWeight] asks for, instead of
 * synthesizing bold.
 *
 * The [FontWeight.Bold] (700) entry isn't a design-token bucket — it exists only so the note
 * editor's Bold toggle (which applies `SpanStyle(fontWeight = FontWeight.Bold)`, matching
 * richeditor-compose's own HTML export, which serializes a `<b>` tag only for exactly
 * `FontWeight.Bold`) renders a true 700-weight glyph instead of Compose synthesizing one on top of
 * the nearest pinned bucket (650).
 *
 * The two [FontStyle.Italic] entries exist for the same reason, but for the editor's Italic toggle —
 * and unlike the Roboto Flex font this app used previously, this build has a real `ital` axis (true
 * italic letterforms, not just an upright glyph sheared on the fly), so both bold weight and italic
 * style render as designed instances instead of synthesized ones.
 */
@OptIn(ExperimentalTextApi::class)
private val Roboto = FontFamily(
    Font(
        R.font.roboto,
        weight = RobotoWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(RobotoWeight.Normal.weight)),
    ),
    Font(
        R.font.roboto,
        weight = RobotoWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(RobotoWeight.Medium.weight)),
    ),
    Font(
        R.font.roboto,
        weight = RobotoWeight.Title,
        variationSettings = FontVariation.Settings(FontVariation.weight(RobotoWeight.Title.weight)),
    ),
    Font(
        R.font.roboto,
        weight = RobotoWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(RobotoWeight.SemiBold.weight)),
    ),
    Font(
        R.font.roboto,
        weight = RobotoWeight.Emphasized,
        variationSettings = FontVariation.Settings(FontVariation.weight(RobotoWeight.Emphasized.weight)),
    ),
    Font(
        R.font.roboto,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Bold.weight)),
    ),
    Font(
        R.font.roboto,
        weight = RobotoWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(RobotoWeight.Normal.weight),
            FontVariation.italic(1f),
        ),
    ),
    Font(
        R.font.roboto,
        weight = FontWeight.Bold,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Bold.weight),
            FontVariation.italic(1f),
        ),
    ),
)

private val materialBaseline = Typography()

/** Keeps an undocumented M3 slot's default size/line-height but switches it to Roboto. */
private fun TextStyle.onRoboto(weight: FontWeight) = copy(fontFamily = Roboto, fontWeight = weight)

/**
 * App-wide type scale — Roboto mapped onto M3 roles per `design/design-tokens.png`. The eight
 * documented roles (headlineLarge/Medium, titleLarge/Medium, bodyLarge, labelLarge, bodySmall,
 * labelMedium) use the exact size/line-height/weight from the tokens page. Undocumented M3 slots
 * keep Material's default size/line-height/letter-spacing but adopt Roboto at the nearest documented
 * weight bucket.
 */
@Suppress("MagicNumber") // sp sizes and letter-spacing below are the design tokens themselves
val JottiqTypography = Typography(
    displayLarge = materialBaseline.displayLarge.onRoboto(RobotoWeight.Emphasized),
    displayMedium = materialBaseline.displayMedium.onRoboto(RobotoWeight.Emphasized),
    displaySmall = materialBaseline.displaySmall.onRoboto(RobotoWeight.Emphasized),
    headlineLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = RobotoWeight.Emphasized,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = RobotoWeight.Emphasized,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineSmall = materialBaseline.headlineSmall.onRoboto(RobotoWeight.Emphasized),
    titleLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = RobotoWeight.Title,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = RobotoWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = materialBaseline.titleSmall.onRoboto(RobotoWeight.Title),
    bodyLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = RobotoWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = materialBaseline.bodyMedium.onRoboto(RobotoWeight.Normal),
    bodySmall = TextStyle(
        fontFamily = Roboto,
        fontWeight = RobotoWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = RobotoWeight.Title,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = RobotoWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = materialBaseline.labelSmall.onRoboto(RobotoWeight.SemiBold),
)

/**
 * Note editor body text: same 16sp/w400 as [Typography.bodyLarge], but with the font's natural
 * line height instead of `bodyLarge`'s fixed 24sp leading — not a standard M3 Typography slot, so
 * it's exposed here rather than overriding the shared `bodyLarge` role used elsewhere in the UI.
 *
 * richeditor-compose gives each paragraph its own `ParagraphStyle`, and Compose trims the extra
 * leading a fixed `lineHeight` adds at paragraph boundaries but not between wrapped lines inside
 * one paragraph — so a fixed `lineHeight` makes soft-wrapped lines look more spaced apart than
 * lines separated by a manual Enter. Leaving `lineHeight` unspecified (natural font metrics, no
 * leading to trim) removes that leading altogether, so every line — wrapped or manual — gets the
 * same tight spacing.
 */
@Suppress("MagicNumber")
val JottiqNoteBodyTextStyle = TextStyle(
    fontFamily = Roboto,
    fontWeight = RobotoWeight.Normal,
    fontSize = 16.sp,
    lineHeight = TextUnit.Unspecified,
    letterSpacing = 0.5.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)
