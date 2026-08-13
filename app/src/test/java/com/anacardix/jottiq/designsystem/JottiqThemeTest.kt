package com.anacardix.jottiq.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JottiqThemeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `light theme resolves static seed color scheme when dynamic color is disabled`() {
        var primary: Color? = null

        composeTestRule.setContent {
            JottiqTheme(darkTheme = false, dynamicColor = false) {
                primary = MaterialTheme.colorScheme.primary
            }
        }

        assertThat(primary).isEqualTo(JottiqLightColors.Primary)
    }

    @Test
    fun `dark theme resolves static seed color scheme when dynamic color is disabled`() {
        var primary: Color? = null

        composeTestRule.setContent {
            JottiqTheme(darkTheme = true, dynamicColor = false) {
                primary = MaterialTheme.colorScheme.primary
            }
        }

        assertThat(primary).isEqualTo(JottiqDarkColors.Primary)
    }

    @Test
    fun `theme applies the expressive shape scale and Roboto Flex type scale`() {
        var shapeMedium: Shape? = null
        var headlineLargeSize: TextUnit? = null

        composeTestRule.setContent {
            JottiqTheme(darkTheme = false, dynamicColor = false) {
                shapeMedium = MaterialTheme.shapes.medium
                headlineLargeSize = MaterialTheme.typography.headlineLarge.fontSize
            }
        }

        assertThat(shapeMedium).isEqualTo(RoundedCornerShape(18.dp))
        assertThat(headlineLargeSize).isEqualTo(32.sp)
    }
}
