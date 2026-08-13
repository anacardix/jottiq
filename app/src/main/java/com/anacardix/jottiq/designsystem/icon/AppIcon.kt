package com.anacardix.jottiq.designsystem.icon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anacardix.jottiq.R

@Suppress("MagicNumber") // default axis values are the design tokens themselves
private object IconAxisDefault {
    const val OPTICAL_SIZE = 24
    const val WEIGHT = 400
    const val GRADE = 0f
}

/**
 * Renders one glyph from the bundled, subsetted Material Symbols Rounded variable font, matching
 * `design/design-tokens.png` ("Icons — Material Symbols Rounded, 24dp default, wght 400").
 *
 * @param filled sets the `FILL` axis: `false` = outline (default), `true` = solid — the tokens
 * page calls for this on toggle states (favorite star, lock, folder). This swaps the glyph
 * immediately; wrap the call site in [androidx.compose.animation.Crossfade] or
 * [androidx.compose.animation.AnimatedContent] if a screen wants the swap to transition.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun AppIcon(
    glyph: AppIconGlyph,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    tint: Color = LocalContentColor.current,
    sizeSp: Int = IconAxisDefault.OPTICAL_SIZE,
) {
    val family = remember(filled, sizeSp) {
        FontFamily(
            Font(
                R.font.material_symbols_rounded,
                variationSettings = FontVariation.Settings(
                    FontVariation.Setting("FILL", if (filled) 1f else 0f),
                    FontVariation.Setting("GRAD", IconAxisDefault.GRADE),
                    FontVariation.Setting("opsz", sizeSp.toFloat()),
                    FontVariation.weight(IconAxisDefault.WEIGHT),
                ),
            ),
        )
    }
    val a11yModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier.clearAndSetSemantics { }
    }
    // A single-line Text's natural height is ~1.2x its font size (the font's ascent+descent,
    // which is where the glyph ink is actually centered — verified against the font's own
    // metrics). A Box constrained to exactly sizeSp would clip that extra ~0.2x off the bottom
    // only, which shifts the ink down instead of centering it (most visible on the FAB menu's
    // Add "+" / Close "X"). `wrapContentSize(unbounded = true)` lets Text report and lay out at
    // its true natural size — letting the harmless top/bottom padding overflow the box equally —
    // so Box's contentAlignment centers the actual ink, not a clipped fragment of it.
    Box(
        modifier = modifier.then(a11yModifier).size(sizeSp.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph.asString(),
            color = tint,
            textAlign = TextAlign.Center,
            softWrap = false,
            modifier = Modifier.wrapContentSize(unbounded = true),
            style = TextStyle(
                fontFamily = family,
                fontSize = sizeSp.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        )
    }
}
