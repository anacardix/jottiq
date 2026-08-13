package com.anacardix.jottiq.ui.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.anacardix.jottiq.R
import com.anacardix.jottiq.domain.AppLanguage
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private tailrec fun Context.unwrapToActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.unwrapToActivity()
    else -> null
}

@RunWith(RobolectricTestRunner::class)
class LocalizedContentTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `English forces the English resources regardless of device locale`() {
        var homeTitle: String? = null

        composeTestRule.setContent {
            LocalizedContent(language = AppLanguage.English) {
                homeTitle = stringResource(R.string.home_title)
            }
        }

        assertThat(homeTitle).isEqualTo("Notes")
    }

    @Test
    fun `Italian forces the Italian resources`() {
        var homeTitle: String? = null

        composeTestRule.setContent {
            LocalizedContent(language = AppLanguage.Italian) {
                homeTitle = stringResource(R.string.home_title)
            }
        }

        assertThat(homeTitle).isEqualTo("Note")
    }

    @Test
    fun `German forces the German resources`() {
        var homeTitle: String? = null

        composeTestRule.setContent {
            LocalizedContent(language = AppLanguage.German) {
                homeTitle = stringResource(R.string.home_title)
            }
        }

        assertThat(homeTitle).isEqualTo("Notizen")
    }

    @Test
    fun `French forces the French resources`() {
        var homeTitle: String? = null

        composeTestRule.setContent {
            LocalizedContent(language = AppLanguage.French) {
                homeTitle = stringResource(R.string.home_title)
            }
        }

        assertThat(homeTitle).isEqualTo("Notes")
    }

    @Test
    fun `SpanishSpain forces the Spanish resources`() {
        var homeTitle: String? = null

        composeTestRule.setContent {
            LocalizedContent(language = AppLanguage.SpanishSpain) {
                homeTitle = stringResource(R.string.home_title)
            }
        }

        assertThat(homeTitle).isEqualTo("Notas")
    }

    @Test
    fun `SpanishLatinAmerica forces the Latin American Spanish resources`() {
        var settingsAction: String? = null

        composeTestRule.setContent {
            LocalizedContent(language = AppLanguage.SpanishLatinAmerica) {
                settingsAction = stringResource(R.string.home_settings_action)
            }
        }

        assertThat(settingsAction).isEqualTo("Configuración")
    }

    @Test
    fun `PortuguesePortugal forces the European Portuguese resources`() {
        var undoAction: String? = null

        composeTestRule.setContent {
            LocalizedContent(language = AppLanguage.PortuguesePortugal) {
                undoAction = stringResource(R.string.undo_action)
            }
        }

        assertThat(undoAction).isEqualTo("Anular")
    }

    @Test
    fun `PortugueseBrazil forces the Brazilian Portuguese resources`() {
        var undoAction: String? = null

        composeTestRule.setContent {
            LocalizedContent(language = AppLanguage.PortugueseBrazil) {
                undoAction = stringResource(R.string.undo_action)
            }
        }

        assertThat(undoAction).isEqualTo("Desfazer")
    }

    @Test
    fun `English keeps the host Activity reachable through the wrapped context`() {
        var reachedActivity: Activity? = null

        composeTestRule.setContent {
            LocalizedContent(language = AppLanguage.English) {
                reachedActivity = LocalContext.current.unwrapToActivity()
            }
        }

        assertThat(reachedActivity).isInstanceOf(ComponentActivity::class.java)
    }

    @Test
    fun `System passes the ambient context through unchanged`() {
        var passedThroughContext: Boolean? = null

        composeTestRule.setContent {
            val ambientContext = LocalContext.current
            LocalizedContent(language = AppLanguage.System) {
                passedThroughContext = LocalContext.current === ambientContext
            }
        }

        assertThat(passedThroughContext).isTrue()
    }
}
