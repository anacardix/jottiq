package com.anacardix.jottiq.playscreenshots

import android.app.UiAutomation
import android.os.ParcelFileDescriptor
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Drives SystemUI Demo Mode + a handful of device-wide normalizations (animations, font scale,
 * light theme) entirely through [UiAutomation.executeShellCommand] — shell commands run with
 * shell-user privileges, which is what lets a Gradle Managed Device be normalized from *inside*
 * the instrumented test: the GMD emulator is created and torn down around the test task itself,
 * so there is no window to `adb shell` into it from outside (see the phase brief).
 */
object SystemUiDemoMode {

    fun enter(automation: UiAutomation) {
        automation.shell("settings put global sysui_demo_allowed 1")
        automation.shell("am broadcast -a com.android.systemui.demo -e command enter")
        automation.shell("am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1000")
        automation.shell(
            "am broadcast -a com.android.systemui.demo -e command battery " +
                "-e level 100 -e plugged false",
        )
        // "fully true" is what suppresses the "connected but not fully verified" wifi icon
        // (a bare "-e wifi show -e level 4" still rendered with an exclamation mark on this AVD —
        // see the phase report).
        automation.shell(
            "am broadcast -a com.android.systemui.demo -e command network " +
                "-e wifi show -e level 4 -e fully true",
        )
        automation.shell(
            "am broadcast -a com.android.systemui.demo -e command network " +
                "-e mobile show -e level 4 -e datatype none",
        )
        automation.shell("am broadcast -a com.android.systemui.demo -e command notifications -e visible false")
    }

    fun exit(automation: UiAutomation) {
        automation.shell("am broadcast -a com.android.systemui.demo -e command exit")
    }

    fun disableAnimations(automation: UiAutomation) {
        automation.shell("settings put global window_animation_scale 0")
        automation.shell("settings put global transition_animation_scale 0")
        automation.shell("settings put global animator_duration_scale 0")
    }

    fun setFontScale1x(automation: UiAutomation) {
        automation.shell("settings put system font_scale 1.0")
    }

    fun forceLightTheme(automation: UiAutomation) {
        automation.shell("cmd uimode night no")
    }

    /**
     * Sets a device-credential PIN so [androidx.biometric.BiometricPrompt]'s `DEVICE_CREDENTIAL`
     * fallback has something to authenticate against — a completely fresh AVD has neither an
     * enrolled biometric nor a credential set, and `BiometricPrompt` shows *no* UI at all (calls
     * `onAuthenticationError` immediately) when none of its allowed authenticators are available.
     * This is exactly the capturability question the phase brief flags for the "lock" shot.
     */
    fun setDeviceCredentialPin(automation: UiAutomation, pin: String) {
        automation.shell("locksettings set-pin $pin")
    }

    fun clearDeviceCredentialPin(automation: UiAutomation, pin: String) {
        automation.shell("locksettings clear --old $pin")
    }

    /** Runs [command] and blocks until it finishes, returning combined stdout. */
    fun UiAutomation.shell(command: String): String {
        val stream = ParcelFileDescriptor.AutoCloseInputStream(executeShellCommand(command))
        return BufferedReader(InputStreamReader(stream)).use { reader -> reader.readText() }
    }
}
