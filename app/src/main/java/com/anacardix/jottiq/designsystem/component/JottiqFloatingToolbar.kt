package com.anacardix.jottiq.designsystem.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The note editor's bottom-docked formatting dock. Wraps `HorizontalFloatingToolbar` so the
 * `ExperimentalMaterial3ExpressiveApi` opt-in stays inside `designsystem` (see the kdoc on
 * [com.anacardix.jottiq.designsystem.JottiqTheme]).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JottiqFloatingToolbar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    HorizontalFloatingToolbar(
        expanded = true,
        // `modifier` already caps the toolbar itself to the screen width at the call site; letting
        // wide content overflow rather than scroll internally is the caller's call, made via
        // [content]. `heightIn(max = ...)` guards against a measurement quirk in this M3
        // 1.5.0-alpha08 build: under the loose height constraints this bottom-docked, Box-aligned
        // toolbar receives, its container can measure many times taller than its content needs (a
        // near screen-height box), which — combined with the pill's percent-based corner shape —
        // renders as a giant circle instead of a slim bar. Capping to the M3 floating-toolbar
        // container token keeps it at its intended size regardless.
        modifier = modifier.heightIn(max = FloatingToolbarDefaults.ContainerSize),
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    ) {
        content()
    }
}
