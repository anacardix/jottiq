package com.anacardix.jottiq.fakes

import androidx.fragment.app.FragmentActivity
import com.anacardix.jottiq.security.AppLockManager
import com.anacardix.jottiq.security.AppLockResult

/** In-memory [AppLockManager] fake, reused across screen tests per CLAUDE.md's fakes-first policy. */
class FakeAppLockManager : AppLockManager {

    var nextResult: AppLockResult = AppLockResult.Success
    var lastTitle: String? = null
    var lastSubtitle: String? = null

    override suspend fun authenticate(activity: FragmentActivity, title: String, subtitle: String): AppLockResult {
        lastTitle = title
        lastSubtitle = subtitle
        return nextResult
    }
}
