package com.anacardix.jottiq.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.anacardix.jottiq.R

/**
 * Full-bleed, centered Expressive loading indicator for a screen's first-emission window — the
 * gap between a screen appearing and its ViewModel delivering its first `isLoading = false` state,
 * which otherwise renders as a blank frame. Wraps `LoadingIndicator` so the
 * `ExperimentalMaterial3ExpressiveApi` opt-in stays inside `designsystem` (see the kdoc on
 * [com.anacardix.jottiq.designsystem.JottiqTheme]).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JottiqLoadingIndicator(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.loading_indicator_description)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // LoadingIndicator has no contentDescription param of its own (unlike, say, an Icon) —
        // attach it via semantics so screen readers announce the load instead of staying silent.
        LoadingIndicator(modifier = Modifier.semantics { contentDescription = description })
    }
}
