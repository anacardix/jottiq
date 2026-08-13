package com.anacardix.jottiq.ui.common

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/**
 * Unwraps a (possibly locale-overridden, see `LocalizedContent`) [Context] to find its host
 * [FragmentActivity]. A plain `context as? FragmentActivity` cast breaks once the context has been
 * wrapped (e.g. for per-language resources), since the wrapper itself isn't the Activity.
 */
tailrec fun Context.findActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
