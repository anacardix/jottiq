package com.anacardix.jottiq.playscreenshots

import android.content.Context
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File

private const val TAG = "PlayScreenshots"

/**
 * Writes captured PNGs to the directory the Gradle Managed Device task pulls back to the host —
 * `InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")`, mirrored to
 * `app/build/outputs/managed_device_android_test_additional_output/<device>/<variant>/...` after
 * the run (enabled via `android.testoptions.manageddevices.enable-additional-test-output=true` in
 * gradle.properties). Falls back to app-external storage — logged either way, since which path
 * actually got used is itself something to verify against real Gradle output, not assume.
 */
object ScreenshotCapture {

    fun outputDir(context: Context): File {
        val fromRunner = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val dir = if (fromRunner != null) {
            File(fromRunner, "playscreenshots")
        } else {
            File(context.getExternalFilesDir(null), "playscreenshots")
        }
        dir.mkdirs()
        Log.i(TAG, "Screenshot output dir: ${dir.absolutePath} (from runner arg: ${fromRunner != null})")
        return dir
    }

    fun capture(device: UiDevice, outputDir: File, slug: String) {
        val file = File(outputDir, "$slug.png")
        val success = device.takeScreenshot(file)
        Log.i(TAG, "Captured $slug -> ${file.absolutePath} (success=$success, exists=${file.exists()})")
        check(success && file.exists()) { "Failed to capture screenshot for slug \"$slug\"" }
    }

    /**
     * The status bar's pixel height on this device/config, queried the same way the platform
     * itself does — used by the post-run status-bar-hash verification script so it crops the
     * identical region across all 5 shots without guessing a fixed pixel value.
     */
    fun writeStatusBarHeightSidecar(context: Context, outputDir: File) {
        val heightPx = dimenPx(context, "status_bar_height")
        File(outputDir, "status-bar-height-px.txt").writeText(heightPx.toString())
        Log.i(TAG, "status_bar_height = ${heightPx}px")
    }

    /**
     * The 3-button/gesture navigation bar's pixel height — used by the framing script to crop it
     * out (per the Play Store screenshot design) without guessing a fixed pixel value that would
     * drift across densities/devices.
     */
    fun writeNavigationBarHeightSidecar(context: Context, outputDir: File) {
        val heightPx = dimenPx(context, "navigation_bar_height")
        File(outputDir, "navigation-bar-height-px.txt").writeText(heightPx.toString())
        Log.i(TAG, "navigation_bar_height = ${heightPx}px")
    }

    private fun dimenPx(context: Context, name: String): Int {
        val resourceId = context.resources.getIdentifier(name, "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else -1
    }
}
