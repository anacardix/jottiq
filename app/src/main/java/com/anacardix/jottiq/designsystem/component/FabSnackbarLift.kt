package com.anacardix.jottiq.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.anacardix.jottiq.designsystem.JottiqSpacing

/** Shared timing so the FAB lift and the snackbar's own slide-in ([AnimatedSnackbarHost]) move together. */
internal const val SNACKBAR_MOTION_DURATION_MS = 400
internal val SnackbarMotionEasing = FastOutSlowInEasing

/**
 * Coordinates a manually-positioned FAB with a Scaffold's `snackbarHost` so the FAB slides up
 * above the snackbar while it is visible, then eases back down once it dismisses (Gmail-style).
 *
 * The FAB is deliberately kept out of the Scaffold's `floatingActionButton` slot: registering it
 * there would make the Scaffold push the *snackbar* above the FAB instead, and it would fight with
 * the expandable `FloatingActionButtonMenu` + `FabMenuScrim` structure the screens already use.
 * Instead, the snackbar reports its own measured height via [FabSnackbarLift.onSnackbarSized], and
 * the FAB reads back an animated [FabSnackbarLift.offset] to apply as a vertical `Modifier.offset`.
 * The offset animates with the same duration/easing as the snackbar's slide-in ([AnimatedSnackbarHost])
 * so the two read as a single, slow, coordinated motion rather than two independent animations.
 */
@Stable
class FabSnackbarLift(val offset: Dp, val onSnackbarSized: (IntSize) -> Unit)

@Composable
fun rememberFabSnackbarLift(
    hostState: SnackbarHostState,
    gap: Dp = JottiqSpacing.m,
): FabSnackbarLift {
    val density = LocalDensity.current
    var snackbarHeightPx by remember { mutableIntStateOf(0) }
    val isSnackbarVisible = hostState.currentSnackbarData != null
    val targetOffset = if (isSnackbarVisible && snackbarHeightPx > 0) {
        with(density) { snackbarHeightPx.toDp() + gap }
    } else {
        0.dp
    }
    val animatedOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = tween(durationMillis = SNACKBAR_MOTION_DURATION_MS, easing = SnackbarMotionEasing),
        label = "fabSnackbarLift",
    )
    return remember(animatedOffset) {
        FabSnackbarLift(
            offset = animatedOffset,
            onSnackbarSized = { size -> snackbarHeightPx = size.height },
        )
    }
}
