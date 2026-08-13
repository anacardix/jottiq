package com.anacardix.jottiq.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

/**
 * Generates `app/src/release/generated/baselineProfiles/baseline-prof.txt`: the classes/methods R8
 * should AOT-compile ahead of time on install, instead of letting ART interpret/JIT them on first
 * run. Run via `./gradlew :app:generateBaselineProfile` with a device/emulator attached (no
 * managed devices are configured, so it runs on whatever `adb devices` sees).
 *
 * Covers cold start through Home rendering — the highest-value, most common baseline profile
 * scenario. Deeper in-app navigation can be added once the UI exposes stable test hooks for
 * UiAutomator to target.
 *
 * Not passing `includeInStartupProfile = true`: the resulting startup-prof.txt's class names come
 * from this module's own "nonMinifiedRelease" instrumented build, which doesn't obfuscate
 * identically to the real `release` R8 mapping — merging it into `app`'s release build produced
 * thousands of "Startup class/method not found" mismatches. The baseline (AOT) profile below
 * doesn't have this problem since AGP recompiles it against the real release mapping.
 */
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(packageName = "com.anacardix.jottiq") {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()
    }
}
