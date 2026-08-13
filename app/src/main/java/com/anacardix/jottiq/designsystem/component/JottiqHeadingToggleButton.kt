package com.anacardix.jottiq.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import com.anacardix.jottiq.designsystem.JottiqSpacing

/**
 * The note editor's H1/H2/H3 heading picker chip. Wraps `ToggleButton` so the
 * `ExperimentalMaterial3ExpressiveApi` opt-in stays inside `designsystem` (see the kdoc on
 * [com.anacardix.jottiq.designsystem.JottiqTheme]).
 *
 * Two fixes baked in, both required for correct use inside the note editor's formatting popovers:
 * - `focusProperties { canFocus = false }`: this control must never steal keyboard/input focus
 *   from the note's text field — doing so ends and restarts its text-input session, collapsing the
 *   selection and risking dropped formatting spans.
 * - A single fixed [ToggleButtonDefaults.shapes] across all interaction states: the default shapes
 *   morph to a squarish container when checked, the same jarring shape-swap the editor toolbar's
 *   B/I/U toggles were fixed to avoid.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JottiqHeadingToggleButton(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = { onCheckedChange() },
        modifier = modifier.focusProperties { canFocus = false },
        shapes = ToggleButtonDefaults.shapes(
            shape = MaterialTheme.shapes.small,
            pressedShape = MaterialTheme.shapes.small,
            checkedShape = MaterialTheme.shapes.small,
        ),
        colors = ToggleButtonDefaults.toggleButtonColors(
            containerColor = Color.Transparent,
            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        // Flat within the popover surface, matching the original pill — no button shadow.
        elevation = null,
        contentPadding = PaddingValues(horizontal = JottiqSpacing.m, vertical = JottiqSpacing.s),
    ) {
        content()
    }
}
