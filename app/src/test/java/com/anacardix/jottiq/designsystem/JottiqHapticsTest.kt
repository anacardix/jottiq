package com.anacardix.jottiq.designsystem

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class JottiqHapticsTest {

    private class RecordingHapticFeedback : HapticFeedback {
        val performed = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performed += hapticFeedbackType
        }
    }

    @Test
    fun `perform invokes the platform haptic when enabled`() {
        val feedback = RecordingHapticFeedback()
        val haptics = JottiqHaptics(feedback) { true }

        haptics.perform(JottiqHapticType.Confirm)

        assertThat(feedback.performed).containsExactly(HapticFeedbackType.Confirm)
    }

    @Test
    fun `perform is a no-op when disabled`() {
        val feedback = RecordingHapticFeedback()
        val haptics = JottiqHaptics(feedback) { false }

        haptics.perform(JottiqHapticType.Reject)

        assertThat(feedback.performed).isEmpty()
    }

    @Test
    fun `each JottiqHapticType maps to the expected platform HapticFeedbackType`() {
        val feedback = RecordingHapticFeedback()
        val haptics = JottiqHaptics(feedback) { true }

        haptics.perform(JottiqHapticType.ToggleOn)
        haptics.perform(JottiqHapticType.ToggleOff)
        haptics.perform(JottiqHapticType.Confirm)
        haptics.perform(JottiqHapticType.Reject)
        haptics.perform(JottiqHapticType.GestureThreshold)

        assertThat(feedback.performed).containsExactly(
            HapticFeedbackType.ToggleOn,
            HapticFeedbackType.ToggleOff,
            HapticFeedbackType.Confirm,
            HapticFeedbackType.Reject,
            HapticFeedbackType.GestureThresholdActivate,
        ).inOrder()
    }

    @Test
    fun `isEnabled is read fresh on every call, not cached at construction`() {
        val feedback = RecordingHapticFeedback()
        var enabled = false
        val haptics = JottiqHaptics(feedback) { enabled }

        haptics.perform(JottiqHapticType.Confirm)
        assertThat(feedback.performed).isEmpty()

        enabled = true
        haptics.perform(JottiqHapticType.Confirm)
        assertThat(feedback.performed).containsExactly(HapticFeedbackType.Confirm)
    }
}
