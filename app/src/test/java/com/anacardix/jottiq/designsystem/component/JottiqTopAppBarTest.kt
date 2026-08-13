package com.anacardix.jottiq.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// JottiqTopAppBar's `scrollBehavior` param type (TopAppBarScrollBehavior) is itself annotated
// @ExperimentalMaterial3Api, which propagates the opt-in requirement to any caller of the
// function — even these tests, which don't pass a scrollBehavior.
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
class JottiqTopAppBarTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `renders the title`() {
        composeTestRule.setContent {
            JottiqTopAppBar(title = { Text("Notes") })
        }

        composeTestRule.onNodeWithText("Notes").assertExists()
    }

    @Test
    fun `renders the subtitle when provided`() {
        composeTestRule.setContent {
            JottiqTopAppBar(title = { Text("Personal") }, subtitle = { Text("2 items") })
        }

        composeTestRule.onNodeWithText("Personal").assertExists()
        composeTestRule.onNodeWithText("2 items").assertExists()
    }

    @Test
    fun `renders the navigation icon and actions`() {
        composeTestRule.setContent {
            JottiqTopAppBar(
                title = { Text("Trash") },
                navigationIcon = { IconButton(onClick = {}) { Text("Back") } },
                actions = { Text("Empty trash") },
            )
        }

        composeTestRule.onNodeWithText("Back").assertExists()
        composeTestRule.onNodeWithText("Empty trash").assertExists()
    }
}
