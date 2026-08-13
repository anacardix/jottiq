plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.androidx.baselineprofile)
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.anacardix.jottiq"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.anacardix.jottiq"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Play Store screenshot capture tests live in their own package (see sourceSets below)
        // and must never run as part of the normal androidTest suite CI executes on PRs
        // (connectedDebugAndroidTest). Excluded here by default; the dedicated capture Gradle
        // invocation overrides `notPackage`/`package` on the command line to target only that
        // package instead. See play/README.md for the exact invocation.
        testInstrumentationRunnerArguments["notPackage"] = "com.anacardix.jottiq.playscreenshots"
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            // No custom NDK code — this only symbolicates prebuilt native libs pulled in
            // transitively by AndroidX (Compose's graphics-path, DataStore's shared counter),
            // so Play Console can deobfuscate their crash/ANR stack traces automatically.
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    sourceSets {
        // Robolectric's unit-test resource loading only reads the *main* variant's merged
        // assets (see AGP's generated test_config.properties: android_merged_assets points at
        // mergeDebugAssets, not a test-specific merge) — so MigrationTestHelper's schema JSONs
        // must live on the main asset path, not the "test" source set, to be visible in JVM tests.
        getByName("main") {
            assets.srcDirs("$projectDir/schemas")
        }
        // The Italian demo dataset consumed by the Play Store screenshot capture tests
        // (com.anacardix.jottiq.playscreenshots) lives at repo root under play/demo-data/ — one
        // canonical copy, shared with the later framing/caption phases — exposed here as an
        // androidTest asset instead of being duplicated under app/src/androidTest/assets.
        getByName("androidTest") {
            assets.srcDirs("$rootDir/play/demo-data")
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        managedDevices {
            localDevices {
                // Used only by the dedicated Play Store screenshot capture task, never by CI's
                // connectedDebugAndroidTest. "aosp" (no Google apps/services) keeps the status bar
                // free of a Google account / Play notification icons; Pixel 8 matches the Play
                // Console's 1080x2400 phone screenshot requirement.
                create("pixel8Api34Aosp") {
                    device = "Pixel 8"
                    apiLevel = 34
                    systemImageSource = "aosp"
                }
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// No managed devices configured — generation runs on whatever device/emulator is attached.

kover {
    reports {
        filters {
            includes {
                classes(
                    "com.anacardix.jottiq.domain.*",
                    "com.anacardix.jottiq.data.*",
                )
            }
            excludes {
                // Room- and Hilt-generated code lives alongside our own @Database/@Dao/@Inject
                // classes and would otherwise count our coverage requirement against unmodifiable
                // codegen (Room's *_Impl, Dagger's *_Factory dependency-injection factories).
                classes("*_Impl", "*_Impl\$*", "*_Factory", "*_Factory\$*")
            }
        }
        verify {
            rule("Domain and data coverage") {
                minBound(80)
            }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.profileinstaller)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Data
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.datastore.preferences)

    // Security
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment)

    // Editor
    implementation(libs.richeditor.compose)
    implementation(libs.ksoup.html)
    implementation(libs.ksoup.entities)

    detektPlugins(libs.detekt.formatting)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.tooling)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    // UiDevice, for the Play Store screenshot capture tests (com.anacardix.jottiq.playscreenshots):
    // UiDevice.takeScreenshot() captures the full display (status bar included), unlike Espresso's
    // Screenshot.capture() which only captures the Activity window.
    androidTestImplementation(libs.androidx.uiautomator)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    baselineProfile(project(":baselineprofile"))
}
