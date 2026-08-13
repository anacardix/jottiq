package com.anacardix.jottiq.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Corner radii below are the design tokens themselves (design-tokens.png "Shape scale").
@Suppress("MagicNumber")
private object ShapeRadius {
    val ExtraSmall = 8.dp
    val Small = 14.dp
    val Medium = 18.dp
    val Large = 28.dp
    val ExtraLarge = 36.dp
    val GroupedRowInner = 5.dp
}

/**
 * M3 shape scale per `design/design-tokens.png`: `XS 8` (checkbox), `S 14` (menus),
 * `M 18` (cards, FAB closed), `L 28` (sheets, dialogs), and an `extraLarge` bucket for the
 * `36`-radius lock-gate hero. Passed to [JottiqTheme]'s `MaterialExpressiveTheme(shapes = ...)`.
 */
val JottiqShapes = Shapes(
    extraSmall = RoundedCornerShape(ShapeRadius.ExtraSmall),
    small = RoundedCornerShape(ShapeRadius.Small),
    medium = RoundedCornerShape(ShapeRadius.Medium),
    large = RoundedCornerShape(ShapeRadius.Large),
    extraLarge = RoundedCornerShape(ShapeRadius.ExtraLarge),
)

/**
 * Corner values the 5-slot [Shapes] scale doesn't cover: the tokens page calls out shapes that
 * *morph* between two radii as component state changes (e.g. the FAB going 18→28-full when its
 * menu opens). Components animate between these with
 * [androidx.compose.material3.MotionScheme.expressive] rather than picking a single static
 * [androidx.compose.foundation.shape.CornerBasedShape].
 */
object JottiqShapeTokens {
    /** Inner corner between two rows inside the same grouped list (ends use [JottiqShapes.medium]). */
    val groupedRowInnerCorner = RoundedCornerShape(ShapeRadius.GroupedRowInner)

    /** Unlock-gate hero icon container. */
    val lockGateHero = RoundedCornerShape(ShapeRadius.ExtraLarge)

    /** Closed FAB corner — morphs to a full circle when its menu opens. */
    val fabClosed = RoundedCornerShape(ShapeRadius.Medium)
}

/**
 * Corner shape for one row inside a grouped list card (Home/Folder-view/Trash's rounded row
 * groups): "grouped list rows use 18 on group ends, 5 between" per `design/design-tokens.png`.
 * Only the outward-facing corners of the first and last row in the group get the full radius.
 */
fun groupedRowShape(index: Int, count: Int): RoundedCornerShape {
    val top = if (index == 0) ShapeRadius.Medium else ShapeRadius.GroupedRowInner
    val bottom = if (index == count - 1) ShapeRadius.Medium else ShapeRadius.GroupedRowInner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomEnd = bottom, bottomStart = bottom)
}
