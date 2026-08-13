package com.anacardix.jottiq.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.anacardix.jottiq.designsystem.icon.AppIcons
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EmptyStateViewTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `renders the message`() {
        composeTestRule.setContent {
            EmptyStateView(icon = AppIcons.NoteAdd, message = "Nothing here yet")
        }

        composeTestRule.onNodeWithText("Nothing here yet").assertExists()
    }

    @Test
    fun `renders the title above the message when provided`() {
        composeTestRule.setContent {
            EmptyStateView(icon = AppIcons.NoteAdd, title = "No notes yet", message = "Tap + to start")
        }

        composeTestRule.onNodeWithText("No notes yet").assertExists()
        composeTestRule.onNodeWithText("Tap + to start").assertExists()
    }
}
