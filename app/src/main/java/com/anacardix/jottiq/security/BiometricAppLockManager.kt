package com.anacardix.jottiq.security

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

private val CANCELLED_ERROR_CODES = setOf(
    BiometricPrompt.ERROR_USER_CANCELED,
    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
    BiometricPrompt.ERROR_CANCELED,
)

/**
 * [AppLockManager] implemented against `BiometricPrompt`. Allows [BIOMETRIC_WEAK] (fingerprint/
 * face) or [DEVICE_CREDENTIAL] (PIN/pattern/password) — with both allowed, the system prompt
 * supplies its own "Use PIN"/"Cancel" affordances (`design/17. Unlocked.png`), so no negative
 * button text is set here (the two are mutually exclusive on [BiometricPrompt.PromptInfo]).
 */
class BiometricAppLockManager @Inject constructor() : AppLockManager {

    override suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
    ): AppLockResult = suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) continuation.resume(AppLockResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (!continuation.isActive) return
                val result = if (errorCode in CANCELLED_ERROR_CODES) {
                    AppLockResult.Cancelled
                } else {
                    AppLockResult.Failed(errString.toString())
                }
                continuation.resume(result)
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        if (subtitle.isNotBlank()) promptInfoBuilder.setSubtitle(subtitle)
        prompt.authenticate(promptInfoBuilder.build())
        continuation.invokeOnCancellation { prompt.cancelAuthentication() }
    }
}
