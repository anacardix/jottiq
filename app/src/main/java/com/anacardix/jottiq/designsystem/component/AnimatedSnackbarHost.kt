package com.anacardix.jottiq.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import kotlinx.coroutines.delay

/**
 * Snackbar host that slides its content up from off-screen instead of Material3's default
 * fade/scale-in-place ([androidx.compose.material3.SnackbarHost]), so it reads as "coming up from
 * the bottom" (Gmail-style). Shares [SNACKBAR_MOTION_DURATION_MS]/[SnackbarMotionEasing] with
 * [rememberFabSnackbarLift] so the FAB rises in the same slow, synchronized motion.
 */
@Composable
fun AnimatedSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData) -> Unit,
) {
    val current = hostState.currentSnackbarData
    val accessibilityManager = LocalAccessibilityManager.current
    var lastData by remember { mutableStateOf<SnackbarData?>(null) }
    LaunchedEffect(current) {
        if (current != null) {
            lastData = current
            // Material3's stock SnackbarHost auto-dismisses on a timeout; this custom host
            // replaces that composable for the slide-up animation, so it must replicate the
            // timeout itself or snackbars only ever close via their action button.
            val timeoutMillis = current.visuals.duration.toDismissTimeoutMillis(
                hasAction = current.visuals.actionLabel != null,
                accessibilityManager = accessibilityManager,
            )
            delay(timeoutMillis)
            current.dismiss()
        }
    }
    AnimatedVisibility(
        visible = current != null,
        modifier = modifier,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = SNACKBAR_MOTION_DURATION_MS, easing = SnackbarMotionEasing),
            initialOffsetY = { fullHeight -> fullHeight },
        ) + fadeIn(tween(durationMillis = SNACKBAR_MOTION_DURATION_MS)),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = SNACKBAR_MOTION_DURATION_MS, easing = SnackbarMotionEasing),
            targetOffsetY = { fullHeight -> fullHeight },
        ) + fadeOut(tween(durationMillis = SNACKBAR_MOTION_DURATION_MS)),
    ) {
        lastData?.let { data -> snackbar(data) }
    }
}

/**
 * Base timeout values for [SnackbarDuration] (Material3's own `SnackbarDuration.toMillis` isn't
 * public, and [SNACKBAR_SHORT_TIMEOUT_MS] is shortened from Material3's 4s default), then defers
 * to [AccessibilityManager] to extend the timeout for touch-exploration users, same as the stock
 * [androidx.compose.material3.SnackbarHost].
 */
private fun SnackbarDuration.toDismissTimeoutMillis(
    hasAction: Boolean,
    accessibilityManager: AccessibilityManager?,
): Long {
    val original = when (this) {
        SnackbarDuration.Indefinite -> Long.MAX_VALUE
        SnackbarDuration.Long -> SNACKBAR_LONG_TIMEOUT_MS
        SnackbarDuration.Short -> SNACKBAR_SHORT_TIMEOUT_MS
    }
    if (accessibilityManager == null) return original
    return accessibilityManager.calculateRecommendedTimeoutMillis(
        originalTimeoutMillis = original,
        containsIcons = true,
        containsText = true,
        containsControls = hasAction,
    )
}

private const val SNACKBAR_SHORT_TIMEOUT_MS = 3_000L
private const val SNACKBAR_LONG_TIMEOUT_MS = 10_000L
