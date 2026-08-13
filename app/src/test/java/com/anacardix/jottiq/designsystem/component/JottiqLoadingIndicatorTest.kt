package com.anacardix.jottiq.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.anacardix.jottiq.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JottiqLoadingIndicatorTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `renders with a discoverable content description`() {
        composeTestRule.setContent {
            JottiqLoadingIndicator()
        }

        val description = composeTestRule.activity.getString(R.string.loading_indicator_description)
        composeTestRule.onNodeWithContentDescription(description).assertExists()
    }
}
