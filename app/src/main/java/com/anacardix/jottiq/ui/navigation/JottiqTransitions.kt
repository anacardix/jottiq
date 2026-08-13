package com.anacardix.jottiq.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/**
 * Shared horizontal-push navigation motion (the iOS/Apple-Notes drill-in feel the app is going
 * for): navigating forward slides the new screen in from the right while the current one slides
 * part-way to the left and fades out underneath it; `popBackStack()` reverses this. Applied once
 * as [androidx.navigation.compose.NavHost]'s default transitions, so every destination shares the
 * same feel instead of falling back to Navigation Compose's default cross-fade.
 */
object JottiqTransitions {
    private const val DURATION_MS = 320
    private const val UNDERLYING_SCREEN_PARALLAX = 0.25f

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(tween(DURATION_MS)) { fullWidth -> fullWidth } +
            fadeIn(tween(DURATION_MS))
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(tween(DURATION_MS)) { fullWidth -> -(fullWidth * UNDERLYING_SCREEN_PARALLAX).toInt() } +
            fadeOut(tween(DURATION_MS))
    }

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(tween(DURATION_MS)) { fullWidth -> -(fullWidth * UNDERLYING_SCREEN_PARALLAX).toInt() } +
            fadeIn(tween(DURATION_MS))
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(tween(DURATION_MS)) { fullWidth -> fullWidth } +
            fadeOut(tween(DURATION_MS))
    }
}
