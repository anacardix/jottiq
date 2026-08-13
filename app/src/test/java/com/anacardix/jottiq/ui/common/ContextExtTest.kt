package com.anacardix.jottiq.ui.common

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContextExtTest {

    @Test
    fun `findActivity returns the Activity itself`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).get()

        assertThat(activity.findActivity()).isSameInstanceAs(activity)
    }

    @Test
    fun `findActivity unwraps nested ContextWrapper layers to reach the Activity`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).get()
        val wrapped = ContextWrapper(ContextWrapper(activity))

        assertThat(wrapped.findActivity()).isSameInstanceAs(activity)
    }

    @Test
    fun `findActivity returns null when there is no host Activity`() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()

        assertThat(appContext.findActivity()).isNull()
    }
}
