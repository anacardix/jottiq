package com.anacardix.jottiq.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * The app's large-title app bar (Home/Folder/Trash/Settings). Wraps `LargeFlexibleTopAppBar` so
 * the `ExperimentalMaterial3ExpressiveApi` opt-in stays inside `designsystem` (see the kdoc on
 * [com.anacardix.jottiq.designsystem.JottiqTheme]). Pinned via [rememberJottiqTopAppBarScrollBehavior]:
 * the title stays in its large state and never collapses into the small bar as content scrolls.
 *
 * [title] and [subtitle] are plain strings, not composable slots, forced to a single line
 * (`maxLines = 1`, ellipsized) rather than left free to wrap: `LargeFlexibleTopAppBar`'s collapsed
 * row measures to `max(collapsedHeight, titleColumnHeight)` (material3 `AppBar.kt`'s
 * `TopAppBarMeasurePolicy`), and that column is measured into whatever width [navigationIcon] and
 * [actions] leave over — a caller with a wide `actions` row (e.g. [SelectionTopBar]'s bulk-action
 * icons) can squeeze it enough that a normal wrapping title grows past 64dp and pushes the whole
 * bar, and everything scrolling under it, a few dp taller. Keeping both lines fixed-height instead
 * keeps the bar's total height constant across every caller and every mode, so switching between
 * a screen's normal bar and its [SelectionTopBar] never shifts the list underneath.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JottiqTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    LargeFlexibleTopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        subtitle = subtitle?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
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
