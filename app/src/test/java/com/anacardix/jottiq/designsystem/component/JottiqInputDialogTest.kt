package com.anacardix.jottiq.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JottiqInputDialogTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `title and content render`() {
        composeTestRule.setContent {
            JottiqInputDialog(
                title = "New folder",
                confirmLabel = "Create",
                dismissLabel = "Cancel",
                confirmEnabled = true,
                onConfirm = {},
                onDismiss = {},
            ) {
                OutlinedTextField(value = "Recipes", onValueChange = {})
            }
        }

        composeTestRule.onNodeWithText("New folder").assertExists()
        composeTestRule.onNodeWithText("Recipes").assertExists()
    }

    @Test
    fun `confirm button reflects confirmEnabled`() {
        composeTestRule.setContent {
            JottiqInputDialog(
                title = "Title",
                confirmLabel = "Confirm",
                dismissLabel = "Cancel",
                confirmEnabled = false,
                onConfirm = {},
                onDismiss = {},
            ) {}
        }

        composeTestRule.onNodeWithText("Confirm").assertIsNotEnabled()
    }

    @Test
    fun `confirm button is enabled when confirmEnabled is true`() {
        composeTestRule.setContent {
            JottiqInputDialog(
                title = "Title",
                confirmLabel = "Confirm",
                dismissLabel = "Cancel",
                confirmEnabled = true,
                onConfirm = {},
                onDismiss = {},
            ) {}
        }

        composeTestRule.onNodeWithText("Confirm").assertIsEnabled()
    }

    @Test
    fun `clicking confirm fires onConfirm`() {
        var confirmed = false
        composeTestRule.setContent {
            JottiqInputDialog(
                title = "Title",
                confirmLabel = "Confirm",
                dismissLabel = "Cancel",
                confirmEnabled = true,
                onConfirm = { confirmed = true },
                onDismiss = {},
            ) {}
        }

        composeTestRule.onNodeWithText("Confirm").performClick()
        assertThat(confirmed).isTrue()
    }

    @Test
    fun `clicking dismiss fires onDismiss`() {
        var dismissed = false
        composeTestRule.setContent {
            JottiqInputDialog(
                title = "Title",
                confirmLabel = "Confirm",
                dismissLabel = "Cancel",
                confirmEnabled = true,
                onConfirm = {},
                onDismiss = { dismissed = true },
            ) {}
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        assertThat(dismissed).isTrue()
    }
}
