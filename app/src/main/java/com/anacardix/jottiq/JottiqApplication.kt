package com.anacardix.jottiq

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.anacardix.jottiq.security.LockSession
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class JottiqApplication : Application() {

    @Inject
    lateinit var lockSession: LockSession

    override fun onCreate() {
        super.onCreate()
        // Resets the app-wide unlock session (see LockSession's kdoc) whenever the app leaves the
        // foreground, so returning to it re-gates every locked note/folder.
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    lockSession.lock()
                }
            },
        )
    }
}
