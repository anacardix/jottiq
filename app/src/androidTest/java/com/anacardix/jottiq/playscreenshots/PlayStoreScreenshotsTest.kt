package com.anacardix.jottiq.playscreenshots

import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.anacardix.jottiq.MainActivity
import com.anacardix.jottiq.R
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

private const val TAG = "PlayScreenshots"
private const val DEVICE_PIN = "1234"
private const val SCREEN_WAIT_MS = 20_000L
private const val PROMPT_WAIT_MS = 3_000L
private const val SETTLE_WAIT_MS = 400L
private const val DEFAULT_LOCALE_TAG = "it"

/**
 * Maps a `play/demo-data/<tag>.json` locale tag to the app's own `AppLanguage` enum name (the
 * DataStore value [DemoDataSeeder.seed] expects) and the Java [Locale] used to read that
 * language's string resources.
 */
private data class LocaleConfig(val tag: String, val appLanguage: String, val javaLocale: Locale)

private val LOCALE_CONFIGS = listOf(
    LocaleConfig("it", "Italian", Locale.ITALIAN),
    LocaleConfig("en", "English", Locale.ENGLISH),
    LocaleConfig("fr", "French", Locale.FRENCH),
    LocaleConfig("de", "German", Locale.GERMAN),
    LocaleConfig("es-ES", "SpanishSpain", Locale.forLanguageTag("es-ES")),
    LocaleConfig("es-419", "SpanishLatinAmerica", Locale.forLanguageTag("es-419")),
    LocaleConfig("pt-PT", "PortuguesePortugal", Locale.forLanguageTag("pt-PT")),
    LocaleConfig("pt-BR", "PortugueseBrazil", Locale.forLanguageTag("pt-BR")),
).associateBy { it.tag }

// Ids from play/demo-data/<tag>.json — identical across every locale file (see DemoData.kt).
private const val ID_FOLDER_PERSONAL = "folder-personale"
private const val ID_FOLDER_RECIPES = "folder-ricette"
private const val ID_NOTE_TRAVEL = "note-lisbona"
private const val ID_NOTE_BREAD = "note-pane"
private const val ID_NOTE_LOCKED = "note-documenti"
private const val ID_NOTE_TRASHED = "note-lista-spesa"

/**
 * Captures the 5 phone screenshots for the Play Store listing, for whichever locale is passed
 * via the `locale` instrumentation runner argument (one of [LOCALE_CONFIGS]'s tags; defaults to
 * `it`). See the phase brief for the full shot list and non-negotiable constraints. Lives outside
 * the normal androidTest suite CI runs (excluded via `notPackage` in defaultConfig, see
 * app/build.gradle.kts); run explicitly against the `pixel8Api34Aosp` Gradle Managed Device:
 *
 * ```
 * ./gradlew pixel8Api34AospDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.notPackage= \
 *   -Pandroid.testInstrumentationRunnerArguments.package=com.anacardix.jottiq.playscreenshots \
 *   -Pandroid.testInstrumentationRunnerArguments.locale=fr
 * ```
 */
class PlayStoreScreenshotsTest {

    private val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    // This androidTest APK's own context — `<locale>.json` is bundled into its assets, not the
    // app under test's (see DemoDataSeeder's kdoc).
    private val instrumentationContext = InstrumentationRegistry.getInstrumentation().context

    private val localeTag = InstrumentationRegistry.getArguments().getString("locale") ?: DEFAULT_LOCALE_TAG
    private val localeConfig = requireNotNull(LOCALE_CONFIGS[localeTag]) {
        "Unknown locale \"$localeTag\" — expected one of ${LOCALE_CONFIGS.keys}"
    }
    private val uiLocaleContext = targetContext.createConfigurationContext(
        Configuration(targetContext.resources.configuration).apply { setLocale(localeConfig.javaLocale) },
    )

    private lateinit var demoData: DemoDataJson

    @Before
    fun setUp() {
        SystemUiDemoMode.disableAnimations(automation)
        SystemUiDemoMode.setFontScale1x(automation)
        SystemUiDemoMode.forceLightTheme(automation)
        SystemUiDemoMode.setDeviceCredentialPin(automation, DEVICE_PIN)
        SystemUiDemoMode.enter(automation)
        demoData = DemoDataSeeder.seed(
            targetContext,
            instrumentationContext,
            "$localeTag.json",
            language = localeConfig.appLanguage,
            themePref = "Light",
        )
    }

    @After
    fun tearDown() {
        // Always runs, even if capture() below throws — see SystemUiDemoMode's kdoc on why this
        // can't be driven from outside the GMD-managed emulator.
        SystemUiDemoMode.exit(automation)
        SystemUiDemoMode.clearDeviceCredentialPin(automation, DEVICE_PIN)
    }

    @Test
    fun capture() {
        val outputDir = ScreenshotCapture.outputDir(targetContext)
        ScreenshotCapture.writeStatusBarHeightSidecar(targetContext, outputDir)
        ScreenshotCapture.writeNavigationBarHeightSidecar(targetContext, outputDir)

        val personalFolder = demoData.folderNamed(ID_FOLDER_PERSONAL)
        val recipesFolder = demoData.folderNamed(ID_FOLDER_RECIPES)
        val travelNote = demoData.noteTitled(ID_NOTE_TRAVEL)
        val breadNote = demoData.noteTitled(ID_NOTE_BREAD)
        val breadFirstHeading = demoData.noteBlockText(ID_NOTE_BREAD, 0)
        val lockedNote = demoData.noteTitled(ID_NOTE_LOCKED)
        val trashedNote = demoData.noteTitled(ID_NOTE_TRASHED)

        launchApp()

        // 1. folders — Home: top-level folders + Favorites (cross-folder, includes a note nested
        // two levels down inside Personal > Travel — see the phase report on what this does and
        // does not show about the folder hierarchy).
        waitForText(personalFolder)
        waitForText(travelNote)
        ScreenshotCapture.capture(device, outputDir, "folders")

        // 2. editor — Home > Personal > Recipes > the bread note.
        clickText(personalFolder)
        waitForText(recipesFolder)
        clickText(recipesFolder)
        waitForText(breadNote)
        clickText(breadNote)
        // compose-rich-editor merges the whole note body into one accessibility node (its content
        // isn't split into a per-line Text like the plain list rows above), so an exact By.text()
        // match against just the heading never matches the merged node's full text — see the
        // phase report.
        waitForTextContains(breadFirstHeading)
        ScreenshotCapture.capture(device, outputDir, "editor")
        device.pressBack()
        device.pressBack()
        device.pressBack()
        waitForText(personalFolder)

        // 3. lock — Home > locked note triggers BiometricPrompt automatically (UnlockGateScreen's
        // own LaunchedEffect). The system prompt itself is not capturable (its window is
        // FLAG_SECURE-protected — confirmed empirically: the resulting screenshot is a solid
        // black 1080x2400 frame, status bar included; see the phase report). Per product
        // decision: dismiss the system prompt with back (resolves to AppLockResult.Cancelled, no
        // navigation) and capture the in-app UnlockGateScreen ("this note is locked" / Unlock
        // button) underneath instead.
        clickText(lockedNote)
        device.waitForIdle()
        Thread.sleep(PROMPT_WAIT_MS)
        // The system prompt here is a PIN pad (ConfirmDeviceCredential), not just a fingerprint
        // chooser — logcat showed it opening the soft keyboard (id/lockPassword). A single back
        // press only dismisses that keyboard (HIDE_SOFT_INPUT_BY_BACK_KEY); the PIN dialog itself
        // needs a second one.
        device.pressBack()
        device.waitForIdle()
        device.pressBack()
        waitForLocalizedString(R.string.unlock_gate_title_note)
        ScreenshotCapture.capture(device, outputDir, "lock")
        returnHome(personalFolder)

        // 4. trash — Home > Trash icon > the trashed note.
        clickContentDescription(R.string.home_trash_action)
        waitForText(trashedNote)
        ScreenshotCapture.capture(device, outputDir, "trash")
        device.pressBack()
        waitForText(personalFolder)

        // 5. theme — Home > Settings icon.
        clickContentDescription(R.string.home_settings_action)
        waitForLocalizedString(R.string.settings_theme_row_title)
        ScreenshotCapture.capture(device, outputDir, "theme")
    }

    private fun launchApp() {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(targetContext, MainActivity::class.java.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        targetContext.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(targetContext.packageName).depth(0)), SCREEN_WAIT_MS)
    }

    private fun localizedString(@StringRes resId: Int): String = uiLocaleContext.getString(resId)

    private fun waitForLocalizedString(@StringRes resId: Int) = waitForText(localizedString(resId))

    private fun waitForText(text: String) {
        val found = device.wait(Until.hasObject(By.text(text)), SCREEN_WAIT_MS)
        check(found) { "Timed out waiting for text \"$text\"" }
        settle()
    }

    private fun waitForTextContains(text: String) {
        val found = device.wait(Until.hasObject(By.textContains(text)), SCREEN_WAIT_MS)
        check(found) { "Timed out waiting for text containing \"$text\"" }
        settle()
    }

    /**
     * `device.waitForIdle()` alone isn't always enough before a screenshot: it waits for the
     * main looper to go idle, not for Compose's own animation/recomposition frames (e.g.
     * `LazyColumn`'s `Modifier.animateItem()`, unaffected by the animator-duration-scale=0 we set
     * device-wide, since Compose animations run on their own clock, not `ValueAnimator`). One
     * "trash" capture out of 8 locale runs briefly caught the list mid-settle with the row not
     * yet composed — reproduced once, gone on retry — hence this extra fixed buffer.
     */
    private fun settle() {
        device.waitForIdle()
        Thread.sleep(SETTLE_WAIT_MS)
    }

    private fun clickText(text: String) {
        val target = device.findObject(By.text(text))
        checkNotNull(target) { "Could not find text \"$text\" to click" }
        target.click()
    }

    /**
     * Presses back up to 3 times, stopping as soon as [homeMarkerText] (a top-level folder name)
     * is visible again. Used after the "lock" shot specifically because how many windows sit
     * between the current screen and Home depends on whether a system dialog (BiometricPrompt /
     * ConfirmDeviceCredential) actually appeared — the very thing this phase is empirically
     * checking (see the phase report) — so a fixed number of back presses would either strand the
     * test off-screen or over-pop past Home.
     */
    private fun returnHome(homeMarkerText: String, maxPresses: Int = 3) {
        repeat(maxPresses) {
            if (device.hasObject(By.text(homeMarkerText))) return
            device.pressBack()
            device.waitForIdle()
        }
        check(device.wait(Until.hasObject(By.text(homeMarkerText)), SCREEN_WAIT_MS)) {
            "Could not return to Home after $maxPresses back presses"
        }
    }

    private fun clickContentDescription(@StringRes resId: Int) {
        val description = localizedString(resId)
        val target = device.findObject(By.desc(description))
        checkNotNull(target) { "Could not find content description \"$description\" to click" }
        target.click()
    }

    init {
        Log.i(TAG, "Locale under test: $localeTag")
    }
}
