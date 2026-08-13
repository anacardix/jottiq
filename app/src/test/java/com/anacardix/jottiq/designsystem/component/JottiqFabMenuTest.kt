package com.anacardix.jottiq.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.anacardix.jottiq.designsystem.icon.AppIcons
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JottiqFabMenuTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `clicking the toggle button fires onToggle`() {
        var toggled = false
        composeTestRule.setContent {
            JottiqFabMenu(
                expanded = false,
                onToggle = { toggled = true },
                toggleContentDescription = "Toggle menu",
                items = emptyList(),
            )
        }

        composeTestRule.onNodeWithContentDescription("Toggle menu").performClick()
        assertThat(toggled).isTrue()
    }

    @Test
    fun `expanded menu shows each item's label`() {
        composeTestRule.setContent {
            JottiqFabMenu(
                expanded = true,
                onToggle = {},
                toggleContentDescription = "Toggle menu",
                items = listOf(
                    JottiqFabMenuItem(
                        label = "New folder",
                        icon = AppIcons.CreateNewFolder,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        onClick = {},
                    ),
                    JottiqFabMenuItem(
                        label = "New note",
                        icon = AppIcons.EditNote,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        onClick = {},
                    ),
                ),
            )
        }

        composeTestRule.onNodeWithText("New folder").assertExists()
        composeTestRule.onNodeWithText("New note").assertExists()
    }

    @Test
    fun `clicking an expanded item fires its onClick`() {
        var clicked = false
        composeTestRule.setContent {
            JottiqFabMenu(
                expanded = true,
                onToggle = {},
                toggleContentDescription = "Toggle menu",
                items = listOf(
                    JottiqFabMenuItem(
                        label = "New folder",
                        icon = AppIcons.CreateNewFolder,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        onClick = { clicked = true },
                    ),
                ),
            )
        }

        composeTestRule.onNodeWithText("New folder").performClick()
        assertThat(clicked).isTrue()
    }
}
