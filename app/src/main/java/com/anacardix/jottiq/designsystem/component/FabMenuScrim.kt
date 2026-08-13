package com.anacardix.jottiq.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics

/**
 * Invisible full-screen layer placed between a list and an expanded `FloatingActionButtonMenu`, so
 * tapping anywhere outside the menu collapses it (`FloatingActionButtonMenu` has no built-in
 * outside-tap dismissal, unlike a `Dialog`/`ModalBottomSheet`).
 */
@Composable
fun FabMenuScrim(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onDismiss)
            .clearAndSetSemantics { },
    )
}
