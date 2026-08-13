package com.anacardix.jottiq.ui.app

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.anacardix.jottiq.domain.AppLanguage
import com.anacardix.jottiq.domain.toLocaleOrNull

/**
 * Overrides [LocalContext]'s [Resources]/[AssetManager] with locale-specific ones for [language],
 * so every `stringResource()` call below resolves against the chosen locale instead of the device
 * default. [AppLanguage.System] passes the original context through unchanged.
 *
 * The provided context is a [ContextWrapper] whose `baseContext` is the *original* context
 * unchanged — only [Context.getResources]/[Context.getAssets]/[Context.getTheme] are overridden to
 * a locale-specific shadow context. This matters: a bare `createConfigurationContext()` result is
 * not a [ContextWrapper] over the original context, so code that unwraps [LocalContext] looking for
 * the host [android.app.Activity] (e.g. Hilt's `hiltViewModel()`, or [FragmentActivity][
 * androidx.fragment.app.FragmentActivity] casts for `BiometricPrompt`) would fail to find it and
 * crash — that was the bug this wrapper fixes. Compose-only (no [android.app.LocaleManager] /
 * `AppCompatDelegate` integration, which would need a new dependency and an `AppCompatActivity`
 * base) — in-app resources switch immediately, but the app's own display name / OS surfaces (e.g.
 * notification strings, if any are ever added) would not follow this preference.
 */
@Composable
fun LocalizedContent(language: AppLanguage, content: @Composable () -> Unit) {
    val baseContext = LocalContext.current
    val ambientConfiguration = LocalConfiguration.current
    val localizedContext = remember(baseContext, ambientConfiguration, language) {
        val locale = language.toLocaleOrNull() ?: return@remember baseContext
        val configuration = Configuration(ambientConfiguration)
        configuration.setLocale(locale)
        val resourcesContext = baseContext.createConfigurationContext(configuration)
        LocalizedContextWrapper(base = baseContext, resourcesContext = resourcesContext)
    }
    CompositionLocalProvider(LocalContext provides localizedContext) {
        content()
    }
}

/**
 * Serves resources/assets/theme from [resourcesContext] while keeping [base] as `baseContext`, so
 * unwrapping this wrapper's `baseContext` chain still reaches the original host (Activity).
 */
private class LocalizedContextWrapper(
    base: Context,
    private val resourcesContext: Context,
) : ContextWrapper(base) {
    override fun getResources(): Resources = resourcesContext.resources
    override fun getAssets(): AssetManager = resourcesContext.assets
    override fun getTheme(): Resources.Theme = resourcesContext.theme
}
