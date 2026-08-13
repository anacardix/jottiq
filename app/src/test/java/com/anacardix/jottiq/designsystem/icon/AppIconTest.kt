package com.anacardix.jottiq.designsystem.icon

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.anacardix.jottiq.designsystem.JottiqTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppIconTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `outlined and filled glyph render and expose their content description`() {
        composeTestRule.setContent {
            JottiqTheme(dynamicColor = false) {
                AppIcon(glyph = AppIcons.Star, contentDescription = "Favorite", filled = false)
                AppIcon(glyph = AppIcons.Star, contentDescription = "Favorited", filled = true)
            }
        }

        composeTestRule.onNodeWithContentDescription("Favorite").assertExists()
        composeTestRule.onNodeWithContentDescription("Favorited").assertExists()
    }

    @Test
    fun `decorative icon without a description still renders`() {
        composeTestRule.setContent {
            JottiqTheme(dynamicColor = false) {
                AppIcon(glyph = AppIcons.ChevronRight, contentDescription = null)
            }
        }

        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `icon occupies a fixed square matching its size, regardless of glyph metrics`() {
        composeTestRule.setContent {
            JottiqTheme(dynamicColor = false) {
                AppIcon(glyph = AppIcons.Add, contentDescription = "Add")
                AppIcon(glyph = AppIcons.Close, contentDescription = "Close")
            }
        }

        // Different glyphs (differing side-bearings) must still report the same, exact
        // 24dp x 24dp bounds — this is what keeps them centered inside a round FAB.
        composeTestRule.onNodeWithContentDescription("Add")
            .assertWidthIsEqualTo(24.dp)
            .assertHeightIsEqualTo(24.dp)
        composeTestRule.onNodeWithContentDescription("Close")
            .assertWidthIsEqualTo(24.dp)
            .assertHeightIsEqualTo(24.dp)
    }
}
