package com.anacardix.jottiq.ui.app

import com.anacardix.jottiq.domain.ThemePref
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppRootTest {

    @Test
    fun `System theme enables dynamic color`() {
        assertThat(resolveDynamicColor(ThemePref.System)).isTrue()
    }

    @Test
    fun `Light theme disables dynamic color in favor of design tokens`() {
        assertThat(resolveDynamicColor(ThemePref.Light)).isFalse()
    }

    @Test
    fun `Dark theme disables dynamic color in favor of design tokens`() {
        assertThat(resolveDynamicColor(ThemePref.Dark)).isFalse()
    }
}
