package com.anacardix.jottiq.designsystem

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Regression guard for the roles pinned in `design/design-tokens.png`: they must match the
 * documented hex exactly. These values must never be re-tuned by eye against a single display —
 * emulator, OLED, and LCD panels all render whites/darks differently, so the token image is the
 * only source of truth. See [JottiqLightColors] and [JottiqDarkColors].
 */
class JottiqLightColorsTest {

    @Test
    fun `light pinned roles match design tokens`() {
        assertThat(JottiqLightColors.Primary).isEqualTo(Color(0xFF9E4A2E))
        assertThat(JottiqLightColors.OnPrimary).isEqualTo(Color(0xFFFFFFFF))
        assertThat(JottiqLightColors.PrimaryContainer).isEqualTo(Color(0xFFD97757))
        assertThat(JottiqLightColors.OnPrimaryContainer).isEqualTo(Color(0xFFFFF3EC))
        assertThat(JottiqLightColors.Secondary).isEqualTo(Color(0xFF77574B))
        assertThat(JottiqLightColors.SecondaryContainer).isEqualTo(Color(0xFFF5DFD4))
        assertThat(JottiqLightColors.Error).isEqualTo(Color(0xFFBA1A1A))
        assertThat(JottiqLightColors.Surface).isEqualTo(Color(0xFFFCF8F4))
        assertThat(JottiqLightColors.OnSurface).isEqualTo(Color(0xFF221A16))
        assertThat(JottiqLightColors.OnSurfaceVariant).isEqualTo(Color(0xFF53433C))
        assertThat(JottiqLightColors.SurfaceContainerLow).isEqualTo(Color(0xFFF7F1EB))
        assertThat(JottiqLightColors.SurfaceContainer).isEqualTo(Color(0xFFF2EBE4))
        assertThat(JottiqLightColors.SurfaceContainerHigh).isEqualTo(Color(0xFFECE5DE))
        assertThat(JottiqLightColors.SurfaceContainerHighest).isEqualTo(Color(0xFFE6DFD8))
        assertThat(JottiqLightColors.Outline).isEqualTo(Color(0xFF85736B))
        assertThat(JottiqLightColors.InverseSurface).isEqualTo(Color(0xFF382E29))
        assertThat(JottiqLightColors.Scrim).isEqualTo(Color(0xFF000000))
    }

    @Test
    fun `light surface and background stay equal`() {
        assertThat(JottiqLightColors.Surface).isEqualTo(JottiqLightColors.Background)
    }

    @Test
    fun `dark pinned roles match design tokens`() {
        assertThat(JottiqDarkColors.Primary).isEqualTo(Color(0xFFFFB59E))
        assertThat(JottiqDarkColors.OnPrimary).isEqualTo(Color(0xFF5A1B04))
        assertThat(JottiqDarkColors.PrimaryContainer).isEqualTo(Color(0xFFC0603F))
        assertThat(JottiqDarkColors.OnPrimaryContainer).isEqualTo(Color(0xFFFFF0E8))
        assertThat(JottiqDarkColors.Secondary).isEqualTo(Color(0xFFE7BFAF))
        assertThat(JottiqDarkColors.SecondaryContainer).isEqualTo(Color(0xFF5D4034))
        assertThat(JottiqDarkColors.Error).isEqualTo(Color(0xFFFFB4AB))
        assertThat(JottiqDarkColors.Surface).isEqualTo(Color(0xFF191411))
        assertThat(JottiqDarkColors.OnSurface).isEqualTo(Color(0xFFEFE0DA))
        assertThat(JottiqDarkColors.OnSurfaceVariant).isEqualTo(Color(0xFFD8C3B9))
        assertThat(JottiqDarkColors.SurfaceContainerLow).isEqualTo(Color(0xFF221A16))
        assertThat(JottiqDarkColors.SurfaceContainer).isEqualTo(Color(0xFF261E1A))
        assertThat(JottiqDarkColors.SurfaceContainerHigh).isEqualTo(Color(0xFF312824))
        assertThat(JottiqDarkColors.SurfaceContainerHighest).isEqualTo(Color(0xFF3C332E))
        assertThat(JottiqDarkColors.Outline).isEqualTo(Color(0xFFA08D84))
        assertThat(JottiqDarkColors.InverseSurface).isEqualTo(Color(0xFFEFE0DA))
        assertThat(JottiqDarkColors.Scrim).isEqualTo(Color(0xFF000000))
    }

    @Test
    fun `dark surface and background stay equal`() {
        assertThat(JottiqDarkColors.Surface).isEqualTo(JottiqDarkColors.Background)
    }

    @Test
    fun `light and dark inverse primary match each other's primary`() {
        assertThat(JottiqLightColors.InversePrimary).isEqualTo(JottiqDarkColors.Primary)
        assertThat(JottiqDarkColors.InversePrimary).isEqualTo(JottiqLightColors.Primary)
    }
}
