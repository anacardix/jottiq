package com.anacardix.jottiq.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
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
            JottiqTopAppBar(title = "Notes")
        }

        composeTestRule.onNodeWithText("Notes").assertExists()
    }

    @Test
    fun `renders the subtitle when provided`() {
        composeTestRule.setContent {
            JottiqTopAppBar(title = "Personal", subtitle = "2 items")
        }

        composeTestRule.onNodeWithText("Personal").assertExists()
        composeTestRule.onNodeWithText("2 items").assertExists()
    }

    @Test
    fun `renders the navigation icon and actions`() {
        composeTestRule.setContent {
            JottiqTopAppBar(
                title = "Trash",
                navigationIcon = { IconButton(onClick = {}) { Text("Back") } },
                actions = { Text("Empty trash") },
            )
        }

        composeTestRule.onNodeWithText("Back").assertExists()
        composeTestRule.onNodeWithText("Empty trash").assertExists()
    }

    @Test
    fun `bar height is unchanged whether the title is short or long enough to wrap`() {
        var measuredHeight = 0
        val title = mutableStateOf("Notes")
        val subtitle = mutableStateOf("12 items")
        composeTestRule.setContent {
            val currentTitle by title
            val currentSubtitle by subtitle
            JottiqTopAppBar(
                title = currentTitle,
                subtitle = currentSubtitle,
                navigationIcon = { IconButton(onClick = {}) { Text("Back") } },
                actions = {
                    Text("Select all")
                    IconButton(onClick = {}) { Text("A") }
                    IconButton(onClick = {}) { Text("B") }
                    IconButton(onClick = {}) { Text("C") }
                },
                modifier = Modifier.onSizeChanged { measuredHeight = it.height },
            )
        }
        composeTestRule.waitForIdle()
        val shortTitleHeight = measuredHeight

        title.value = "A very long title that would otherwise wrap onto multiple lines"
        subtitle.value = "A very long subtitle that would otherwise wrap too"
        composeTestRule.waitForIdle()
        val longTitleHeight = measuredHeight

        assertThat(longTitleHeight).isEqualTo(shortTitleHeight)
    }

    @Test
    fun `a blank single-space subtitle keeps the same bar height as a real subtitle`() {
        // Regression test: Text("") measures a shorter line than any non-blank string in this
        // material3 version (verified empirically — its LargeFlexibleTopAppBar bottom-row baseline
        // placement differs), so SelectionTopBar's blank-subtitle workaround must use a single
        // space, not an empty string, or it silently shrinks the bar and shifts the list under it.
        var measuredHeight = 0
        val subtitle = mutableStateOf("12 items")
        composeTestRule.setContent {
            val currentSubtitle by subtitle
            JottiqTopAppBar(
                title = "Notes",
                subtitle = currentSubtitle,
                modifier = Modifier.onSizeChanged { measuredHeight = it.height },
            )
        }
        composeTestRule.waitForIdle()
        val realSubtitleHeight = measuredHeight

        subtitle.value = " "
        composeTestRule.waitForIdle()
        val blankSubtitleHeight = measuredHeight

        assertThat(blankSubtitleHeight).isEqualTo(realSubtitleHeight)
    }
}
