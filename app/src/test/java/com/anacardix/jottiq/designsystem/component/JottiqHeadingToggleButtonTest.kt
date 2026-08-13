package com.anacardix.jottiq.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JottiqHeadingToggleButtonTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `reflects the checked state`() {
        composeTestRule.setContent {
            JottiqHeadingToggleButton(checked = true, onCheckedChange = {}) {
                Text("H1")
            }
        }

        composeTestRule.onNodeWithText("H1").assertIsOn()
    }

    @Test
    fun `reflects the unchecked state`() {
        composeTestRule.setContent {
            JottiqHeadingToggleButton(checked = false, onCheckedChange = {}) {
                Text("H1")
            }
        }

        composeTestRule.onNodeWithText("H1").assertIsOff()
    }

    @Test
    fun `clicking fires onCheckedChange`() {
        var clicked = false
        composeTestRule.setContent {
            JottiqHeadingToggleButton(checked = false, onCheckedChange = { clicked = true }) {
                Text("H1")
            }
        }

        composeTestRule.onNodeWithText("H1").performClick()
        assertThat(clicked).isTrue()
    }
}
