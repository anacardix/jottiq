package com.anacardix.jottiq.ui.noteeditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichText

/**
 * Read-mode rendering of the note body: [BasicRichText] displays the exact same [RichTextState]
 * (and base style) that [BasicRichTextEditor] renders in edit mode, so formatting — including
 * heading size, a relative `em` span the library computes internally — can never drift between
 * modes the way a hand-rolled, domain-model-based renderer could (a previous version of this file
 * mirrored the library's heading math independently, which only matched at the system's default
 * font scale and visibly diverged from edit mode once a user raised their font size).
 *
 * [BasicRichText] resolves a tap on a link itself (opens the URL) and does not consume a tap
 * anywhere else, so the outer [onTap] still fires for "tap the rest of the note to start editing",
 * matching Apple Notes. See `NoteDocumentHtml.kt` for why every link's URL is normalized before it
 * ever reaches the library's state, keeping that built-in link handling working even for URLs
 * saved before normalization existed. See [interceptDeadZoneTaps] for why a tap on a row's blank
 * trailing space still needs to be steered away from that built-in link handling.
 */
@OptIn(ExperimentalRichTextApi::class)
@Composable
internal fun ReadRichSegment(state: RichTextState, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val currentOnTap by rememberUpdatedState(onTap)
    BasicRichText(
        state = state,
        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        onTextLayout = { layoutResult = it },
        modifier = modifier
            .interceptDeadZoneTaps({ layoutResult }) { currentOnTap() }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onTap),
    )
}

/**
 * [BasicRichText] resolves a tapped [Offset] to a link by asking the text layout for the nearest
 * character offset, which — unlike real hit-testing — always returns *some* character even when
 * the tap lands past the last rendered glyph on a line. A paragraph that's just a short link (e.g.
 * a note whose whole body is a URL) still spans the full width of its read-mode row, so any tap in
 * that row's blank trailing space snaps to the link's last character and the library opens it —
 * exactly the "clicking the row opens the link" bug this guards against.
 *
 * Intercepts the down event one pass earlier than the library's own tap handling
 * ([PointerEventPass.Main]) whenever it falls outside every candidate line's actual horizontal
 * extent, and drives the tap itself instead. It doesn't matter whether the nearest character the
 * library would have picked belongs to a link or plain text: a tap past the real glyphs always
 * means "start editing", never "open link", so this can steer every such tap the same way without
 * needing to ask the library (an internal implementation detail it doesn't expose) what's actually
 * under the cursor.
 */
@Composable
private fun Modifier.interceptDeadZoneTaps(
    layoutResult: () -> TextLayoutResult?,
    onTap: () -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val layout = layoutResult() ?: return@awaitEachGesture
        if (!down.position.isPastRenderedText(layout)) return@awaitEachGesture
        down.consume()
        val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
        if (up != null) {
            up.consume()
            onTap()
        }
    }
}

private fun Offset.isPastRenderedText(layout: TextLayoutResult): Boolean {
    if (layout.lineCount == 0) return false
    val line = layout.getLineForVerticalPosition(y).coerceIn(0, layout.lineCount - 1)
    return x < layout.getLineLeft(line) || x > layout.getLineRight(line)
}
