package com.anacardix.jottiq.designsystem

import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Regression guard for [JottiqNoteBodyTextStyle]: the note body must render every line — whether
 * separated by a manual Enter (a new paragraph) or by an automatic soft wrap — with the same
 * spacing. richeditor-compose lays the note body out as a series of per-paragraph text blocks, and
 * Compose only trims a fixed `lineHeight`'s extra leading at each block's own top/bottom edge, not
 * between wrapped lines inside one block — so a fixed `lineHeight` (as `bodyLarge` has) makes
 * soft-wrapped lines look more spaced apart than manually broken ones. Leaving `lineHeight`
 * unspecified uses the font's natural metrics instead, which have no extra leading to trim, so
 * every line ends up spaced the same regardless of how the line break happened.
 */
class TypeTest {

    @Test
    fun `note body style has no fixed line height to trim inconsistently`() {
        assertThat(JottiqNoteBodyTextStyle.lineHeight.isUnspecified).isTrue()
    }

    @Test
    fun `note body style disables extra font padding`() {
        assertThat(JottiqNoteBodyTextStyle.platformStyle?.paragraphStyle?.includeFontPadding).isFalse()
    }

    @Test
    fun `note body style otherwise matches bodyLarge`() {
        assertThat(JottiqNoteBodyTextStyle.fontSize).isEqualTo(16.sp)
        assertThat(JottiqNoteBodyTextStyle.letterSpacing).isEqualTo(0.5.sp)
    }
}
