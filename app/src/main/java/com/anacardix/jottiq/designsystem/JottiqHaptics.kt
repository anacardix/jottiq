package com.anacardix.jottiq.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Semantic vocabulary for the app's haptic cues, kept independent of [HapticFeedbackType] so call
 * sites express *why* they buzz (a toggle flipped, a destructive action fired) rather than which
 * platform constant that maps to.
 */
enum class JottiqHapticType {
    ToggleOn,
    ToggleOff,
    Confirm,
    Reject,
    GestureThreshold,
}

private fun JottiqHapticType.toPlatformType(): HapticFeedbackType = when (this) {
    JottiqHapticType.ToggleOn -> HapticFeedbackType.ToggleOn
    JottiqHapticType.ToggleOff -> HapticFeedbackType.ToggleOff
    JottiqHapticType.Confirm -> HapticFeedbackType.Confirm
    JottiqHapticType.Reject -> HapticFeedbackType.Reject
    JottiqHapticType.GestureThreshold -> HapticFeedbackType.GestureThresholdActivate
}

/** Whether the user has haptic feedback enabled in Settings; defaults to on until [ui.app.AppRoot]
 * provides the persisted value, matching [ui.settings.SettingsUiState]'s own `true` default. */
val LocalHapticsEnabled = compositionLocalOf { true }

/**
 * Fires [JottiqHapticType] cues through the platform's [HapticFeedback], honoring both the OS-level
 * haptics setting (via [HapticFeedback.performHapticFeedback] itself) and this app's own
 * Settings > General > "Haptic feedback" toggle (via [isEnabled]). Constructed with a lambda rather
 * than a plain `Boolean` so it always reads the latest value without needing to be recreated.
 */
class JottiqHaptics(private val haptic: HapticFeedback, private val isEnabled: () -> Boolean) {
    fun perform(type: JottiqHapticType) {
        if (!isEnabled()) return
        haptic.performHapticFeedback(type.toPlatformType())
    }
}

/** Remembers a [JottiqHaptics] wired to [LocalHapticFeedback] and [LocalHapticsEnabled]. */
@Composable
fun rememberJottiqHaptics(): JottiqHaptics {
    val haptic = LocalHapticFeedback.current
    val enabledState = rememberUpdatedState(LocalHapticsEnabled.current)
    return remember(haptic) { JottiqHaptics(haptic) { enabledState.value } }
}
