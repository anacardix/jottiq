package com.anacardix.jottiq.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Minimum width for a text field inside a [JottiqInputDialog], so short input doesn't collapse it. */
val DIALOG_FIELD_MIN_WIDTH: Dp = 240.dp

/**
 * Single-purpose input dialog (name a folder, insert a link, ...) styled to
 * `design/09. Add link.png` and the dialog row in `design/design-tokens.png`: `shapes.large` (28dp,
 * not the theme's default `extraLarge` 36dp AlertDialog corner), `titleLarge` heading, a filled
 * primary pill to confirm, and a text button to dismiss. Callers supply their own text field(s) via
 * [content] — using `placeholder`, not `label`, to match the mockup.
 */
@Composable
fun JottiqInputDialog(
    title: String,
    confirmLabel: String,
    dismissLabel: String,
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        text = { Column(content = content) },
        confirmButton = {
            Button(onClick = onConfirm, enabled = confirmEnabled) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel)
            }
        },
    )
}

/**
 * Single-line text field for a [JottiqInputDialog]'s content slot.
 *
 * A singleLine text field measures shorter than a [Text] with the same style (it collapses toward
 * glyph height and drops the style's line leading — see the title field in `NoteEditorScreen.kt`
 * for the same fix applied there). A plain `OutlinedTextField(placeholder = { Text(...) })` is
 * therefore taller while the placeholder shows than once the user types, which reads as the field
 * shrinking. Normalizing font padding and line-height trim on both the field's `textStyle` and the
 * placeholder keeps the field's height identical in the empty and typed states.
 */
@Composable
fun JottiqDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val fieldStyle = MaterialTheme.typography.bodyLarge.merge(
        TextStyle(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
        ),
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(text = placeholder, style = fieldStyle) },
        textStyle = fieldStyle,
        singleLine = true,
        modifier = modifier.widthIn(min = DIALOG_FIELD_MIN_WIDTH),
    )
}
