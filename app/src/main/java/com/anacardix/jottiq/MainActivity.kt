package com.anacardix.jottiq

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.anacardix.jottiq.ui.app.AppRoot
import dagger.hilt.android.AndroidEntryPoint

/** [FragmentActivity], not [androidx.activity.ComponentActivity], because BiometricPrompt (used
 * by [com.anacardix.jottiq.security.BiometricAppLockManager]) requires a FragmentManager host. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}
