package com.anacardix.jottiq.designsystem

import androidx.compose.ui.unit.dp

/**
 * 4dp spacing grid per `design/design-tokens.png`, plus the semantic values screens are built
 * from directly (screen gutter, row padding, list bottom inset for FAB clearance, minimum touch
 * target). Prefer the semantic names in screen code; fall back to the raw grid steps only when no
 * semantic token fits.
 */
@Suppress("MagicNumber") // dp values below are the design tokens themselves
object JottiqSpacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp

    /** Horizontal screen edge padding. */
    val screenGutter = 16.dp

    /** Grouped list row padding. */
    val rowPaddingHorizontal = 16.dp
    val rowPaddingVertical = 15.dp

    /** Gap between rows within the same grouped list. */
    val groupGap = 2.dp

    /** Gap between distinct sections (e.g. Favorites vs. folders vs. loose notes). */
    val sectionGap = 20.dp

    /** Bottom content padding reserved so scrollable lists don't sit under the FAB. */
    val listBottomInset = 130.dp

    /** Minimum interactive touch target; icon buttons render at 48dp. */
    val touchTarget = 48.dp

    /** Caps single-choice dialog option lists (e.g. Language) so they scroll instead of overflowing. */
    val dialogOptionListMaxHeight = 320.dp
}
