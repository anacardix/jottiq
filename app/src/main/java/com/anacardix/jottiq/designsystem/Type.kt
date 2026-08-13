package com.anacardix.jottiq.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.anacardix.jottiq.R

// Weight-axis values (400/500/550/600/650) are the tokens themselves — see design-tokens.png.
@Suppress("MagicNumber")
private object FlexWeight {
    val Normal = FontWeight.Normal
    val Medium = FontWeight.Medium
    val Title = FontWeight(550)
    val SemiBold = FontWeight.SemiBold
    val Emphasized = FontWeight(650)
}

/**
 * Roboto Flex (variable font, `res/font/roboto_flex.ttf`) pinned at the weight buckets the design
 * tokens use. Each [Font] entry sets its own `wght` axis via [FontVariation] so Compose renders the
 * exact weight a [TextStyle.fontWeight] asks for, instead of synthesizing bold.
 */
@OptIn(ExperimentalTextApi::class)
private val RobotoFlex = FontFamily(
    Font(
        R.font.roboto_flex,
        weight = FlexWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(FlexWeight.Normal.weight)),
    ),
    Font(
        R.font.roboto_flex,
        weight = FlexWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(FlexWeight.Medium.weight)),
    ),
    Font(
        R.font.roboto_flex,
        weight = FlexWeight.Title,
        variationSettings = FontVariation.Settings(FontVariation.weight(FlexWeight.Title.weight)),
    ),
    Font(
        R.font.roboto_flex,
        weight = FlexWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FlexWeight.SemiBold.weight)),
    ),
    Font(
        R.font.roboto_flex,
        weight = FlexWeight.Emphasized,
        variationSettings = FontVariation.Settings(FontVariation.weight(FlexWeight.Emphasized.weight)),
    ),
)

private val materialBaseline = Typography()

/** Keeps an undocumented M3 slot's default size/line-height but switches it to Roboto Flex. */
private fun TextStyle.onRobotoFlex(weight: FontWeight) = copy(fontFamily = RobotoFlex, fontWeight = weight)

/**
 * App-wide type scale — Roboto Flex mapped onto M3 roles per `design/design-tokens.png`. The eight
 * documented roles (headlineLarge/Medium, titleLarge/Medium, bodyLarge, labelLarge, bodySmall,
 * labelMedium) use the exact size/line-height/weight from the tokens page. Undocumented M3 slots
 * keep Material's default size/line-height/letter-spacing but adopt Roboto Flex at the nearest
 * documented weight bucket.
 */
@Suppress("MagicNumber") // sp sizes and letter-spacing below are the design tokens themselves
val JottiqTypography = Typography(
    displayLarge = materialBaseline.displayLarge.onRobotoFlex(FlexWeight.Emphasized),
    displayMedium = materialBaseline.displayMedium.onRobotoFlex(FlexWeight.Emphasized),
    displaySmall = materialBaseline.displaySmall.onRobotoFlex(FlexWeight.Emphasized),
    headlineLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FlexWeight.Emphasized,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FlexWeight.Emphasized,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineSmall = materialBaseline.headlineSmall.onRobotoFlex(FlexWeight.Emphasized),
    titleLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FlexWeight.Title,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FlexWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = materialBaseline.titleSmall.onRobotoFlex(FlexWeight.Title),
    bodyLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FlexWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = materialBaseline.bodyMedium.onRobotoFlex(FlexWeight.Normal),
    bodySmall = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FlexWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FlexWeight.Title,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FlexWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = materialBaseline.labelSmall.onRobotoFlex(FlexWeight.SemiBold),
)

/**
 * Note editor body text: same 16sp/w400 as [Typography.bodyLarge], but the wider 26sp line height
 * the tokens page calls out specifically for note content — not a standard M3 Typography slot, so
 * it's exposed here rather than overriding the shared `bodyLarge` role used elsewhere in the UI.
 */
@Suppress("MagicNumber")
val JottiqNoteBodyTextStyle = TextStyle(
    fontFamily = RobotoFlex,
    fontWeight = FlexWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.5.sp,
)
