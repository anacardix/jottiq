package com.anacardix.jottiq.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import com.anacardix.jottiq.designsystem.JottiqTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * [SwipeableGroupedRow] must fully commit a right-to-left swipe (calling `onDelete` once settled
 * dismissed, with the row sliding off-screen), while a left-to-right swipe is a toggle: it calls
 * `onToggleFavorite` and animates back to [SwipeToDismissBoxValue.Settled] rather than staying
 * dismissed. The resetSignal regression this guards is: if the caller's list re-includes the same
 * item (e.g. the user tapped Undo) while the `LazyColumn` is still mid-exit-animation and reuses this
 * composable's slot, the restored row must not stay stuck rendered at the dismissed, off-screen
 * position — bumping `resetSignal` is how the caller forces a fresh, visible swipe state for it.
 */
@RunWith(RobolectricTestRunner::class)
class SwipeableGroupedRowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `swiping left invokes onDelete and slides the row off-screen`() {
        var deleted = false
        composeTestRule.setContent {
            JottiqTheme(dynamicColor = false) {
                SwipeableGroupedRow(shape = RectangleShape, onDelete = { deleted = true }) {
                    Text("Groceries", modifier = Modifier.fillMaxWidth())
                }
            }
        }

        composeTestRule.onNodeWithText("Groceries").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertThat(deleted).isTrue()
    }

    /**
     * Regression test: rows carry their own horizontal inset (matching [GroupedListRow]'s
     * screenGutter padding in production, applied to the row instead of the LazyColumn per
     * [com.anacardix.jottiq.ui.common.noteRowGroup]'s kdoc) rather than being pre-shrunk by an
     * outer container. A committed swipe must still clear the *entire* screen, not just the row's
     * own already-inset width — otherwise a gutter-width sliver of the row is left visible at the
     * screen edge once settled.
     */
    @Test
    fun `swiping left clears the full screen despite the row's own horizontal inset`() {
        val screenWidth = 300.dp
        val gutter = 16.dp
        composeTestRule.setContent {
            JottiqTheme(dynamicColor = false) {
                Box(Modifier.width(screenWidth)) {
                    SwipeableGroupedRow(shape = RectangleShape, onDelete = {}) {
                        Box(Modifier.padding(horizontal = gutter)) {
                            Text("Groceries", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Groceries").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        val rowRight = composeTestRule.onNodeWithText("Groceries").getUnclippedBoundsInRoot().right
        assertThat(rowRight.value).isLessThan(1f)
    }

    /**
     * Regression test: [SwipeToDismissBox] always composes `backgroundContent` regardless of
     * swipe state, so without gating on `dismissDirection` the solid swipe-action color would sit
     * behind the row permanently and bleed through the row's clipped rounded corners' anti-aliased
     * edge pixels as a faint colored ring around every card (bug/corners.png). At rest — no swipe
     * yet — nothing should be painted at all.
     */
    @Test
    fun `delete background is not painted at rest`() {
        composeTestRule.setContent {
            JottiqTheme(dynamicColor = false) {
                SwipeableGroupedRow(shape = RectangleShape, onDelete = {}) {
                    Text("Groceries", modifier = Modifier.fillMaxWidth())
                }
            }
        }

        composeTestRule.onNodeWithTag(DELETE_SWIPE_BACKGROUND_TAG).assertDoesNotExist()
    }

    /**
     * Regression test: [DeleteSwipeBackground] fills the entire (edge-to-edge) box behind the row
     * so a committed swipe can clear the full screen (see the test above), but it must not *paint*
     * into the gutter margin outside where the row's own inset card sits.
     */
    @Test
    fun `delete background does not paint into the row's gutter margin while swiping`() {
        val screenWidth = 300.dp
        val gutter = 16.dp
        composeTestRule.setContent {
            JottiqTheme(dynamicColor = false) {
                Box(Modifier.width(screenWidth)) {
                    SwipeableGroupedRow(shape = RectangleShape, onDelete = {}) {
                        Box(Modifier.padding(horizontal = gutter)) {
                            Text("Groceries", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
        composeTestRule.onNodeWithText("Groceries").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        val backgroundBounds = composeTestRule.onNodeWithTag(DELETE_SWIPE_BACKGROUND_TAG).getUnclippedBoundsInRoot()
        assertThat(backgroundBounds.left.value).isAtLeast(gutter.value)
        assertThat(backgroundBounds.right.value).isAtMost(screenWidth.value - gutter.value)
    }

    @Test
    fun `bumping resetSignal after a delete swipe brings the row back on-screen`() {
        var resetSignal by mutableIntStateOf(0)
        composeTestRule.setContent {
            JottiqTheme(dynamicColor = false) {
                SwipeableGroupedRow(shape = RectangleShape, onDelete = {}, resetSignal = resetSignal) {
                    // fillMaxWidth so the swipe below traverses the whole row, matching production
                    // rows (GroupedListRow spans full width) rather than just the text's own size.
                    Text("Groceries", modifier = Modifier.fillMaxWidth())
                }
            }
        }
        composeTestRule.onNodeWithText("Groceries").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        // Simulates the caller restoring the item via Undo and bumping HomeUiState.undoNonce.
        resetSignal++
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Groceries").assertIsDisplayed()
    }

    @Test
    fun `swiping right invokes onToggleFavorite and the row stays on-screen`() {
        var toggled = false
        composeTestRule.setContent {
            JottiqTheme(dynamicColor = false) {
                SwipeableGroupedRow(
                    shape = RectangleShape,
                    onDelete = {},
                    onToggleFavorite = { toggled = true },
                ) {
                    Text("Groceries", modifier = Modifier.fillMaxWidth())
                }
            }
        }

        composeTestRule.onNodeWithText("Groceries").performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        assertThat(toggled).isTrue()
        composeTestRule.onNodeWithText("Groceries").assertIsDisplayed()
    }

    @Test
    fun `swiping right does nothing when onToggleFavorite is not provided`() {
        var deleted = false
        composeTestRule.setContent {
            JottiqTheme(dynamicColor = false) {
                SwipeableGroupedRow(shape = RectangleShape, onDelete = { deleted = true }) {
                    Text("Groceries", modifier = Modifier.fillMaxWidth())
                }
            }
        }

        composeTestRule.onNodeWithText("Groceries").performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        assertThat(deleted).isFalse()
        composeTestRule.onNodeWithText("Groceries").assertIsDisplayed()
    }
}
