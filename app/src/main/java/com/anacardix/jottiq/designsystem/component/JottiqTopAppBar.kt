package com.anacardix.jottiq.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The app's large-title app bar (Home/Folder/Trash/Settings). Wraps `LargeFlexibleTopAppBar` so
 * the `ExperimentalMaterial3ExpressiveApi` opt-in stays inside `designsystem` (see the kdoc on
 * [com.anacardix.jottiq.designsystem.JottiqTheme]). Pinned via [rememberJottiqTopAppBarScrollBehavior]:
 * the title stays in its large state and never collapses into the small bar as content scrolls.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JottiqTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    LargeFlexibleTopAppBar(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
    )
}

/**
 * Scroll behavior for [JottiqTopAppBar]: the bar stays pinned in its large state as content
 * scrolls underneath it — the title never collapses into a small nav bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberJottiqTopAppBarScrollBehavior(): TopAppBarScrollBehavior =
    TopAppBarDefaults.pinnedScrollBehavior()
