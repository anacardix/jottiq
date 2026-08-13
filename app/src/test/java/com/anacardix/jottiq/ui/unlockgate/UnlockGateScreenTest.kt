package com.anacardix.jottiq.ui.unlockgate

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.fragment.app.FragmentActivity
import com.anacardix.jottiq.R
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UnlockGateScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `shows the generic note title without the target name`() {
        composeTestRule.setContent {
            UnlockGateContent(
                uiState = UnlockGateUiState(targetId = "1", targetName = "Gift ideas", isFolder = false),
                onEvent = {},
                onBackClick = {},
            )
        }

        val title = composeTestRule.activity.getString(R.string.unlock_gate_title_note)
        composeTestRule.onNodeWithText(title).assertExists()
        composeTestRule.onNodeWithText("Gift ideas", substring = true).assertDoesNotExist()
    }

    @Test
    fun `shows the folder title when the target is a folder`() {
        composeTestRule.setContent {
            UnlockGateContent(
                uiState = UnlockGateUiState(targetId = "1", targetName = "Journal", isFolder = true),
                onEvent = {},
                onBackClick = {},
            )
        }

        val title = composeTestRule.activity.getString(R.string.unlock_gate_title_folder)
        composeTestRule.onNodeWithText(title).assertExists()
    }

    @Test
    fun `entering the screen triggers unlock without a tap`() {
        var unlockClicked = false
        val fragmentActivity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides fragmentActivity) {
                UnlockGateContent(
                    uiState = UnlockGateUiState(targetId = "1", targetName = "Journal", isFolder = true),
                    onEvent = { event -> if (event is UnlockGateEvent.UnlockClicked) unlockClicked = true },
                    onBackClick = {},
                )
            }
        }

        assertThat(unlockClicked).isTrue()
    }

    @Test
    fun `back button triggers onBackClick`() {
        var backClicked = false
        composeTestRule.setContent {
            UnlockGateContent(
                uiState = UnlockGateUiState(targetId = "1", targetName = "Journal", isFolder = true),
                onEvent = {},
                onBackClick = { backClicked = true },
            )
        }

        val description = composeTestRule.activity.getString(R.string.unlock_gate_back_action)
        composeTestRule.onNodeWithContentDescription(description).performClick()

        assertThat(backClicked).isTrue()
    }
}
