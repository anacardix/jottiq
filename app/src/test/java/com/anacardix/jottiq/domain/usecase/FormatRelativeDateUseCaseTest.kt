package com.anacardix.jottiq.domain.usecase

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

private val FIXED_ZONE = ZoneOffset.UTC
private val NOW = Instant.parse("2026-07-17T14:02:00Z")

class FormatRelativeDateUseCaseTest {

    private val clock = Clock.fixed(NOW, FIXED_ZONE)
    private val formatRelativeDate = FormatRelativeDateUseCase(clock)

    @Test
    fun `timestamp from today formats as HH-mm`() {
        val label = formatRelativeDate(NOW.toEpochMilli(), Locale.US)

        assertThat(label).isEqualTo(RelativeDateLabel.Time("14:02"))
    }

    @Test
    fun `timestamp from yesterday formats as HH-mm`() {
        val yesterday = NOW.minusSeconds(SECONDS_PER_DAY)

        val label = formatRelativeDate(yesterday.toEpochMilli(), Locale.US)

        assertThat(label).isEqualTo(RelativeDateLabel.Time("14:02"))
    }

    @Test
    fun `timestamp from two days ago formats as d MMM`() {
        val twoDaysAgo = NOW.minusSeconds(2 * SECONDS_PER_DAY)

        val label = formatRelativeDate(twoDaysAgo.toEpochMilli(), Locale.US)

        assertThat(label).isEqualTo(RelativeDateLabel.Date("15 Jul"))
    }

    @Test
    fun `formatEditorTimestamp returns HH-mm for a timestamp from today`() {
        val text = formatRelativeDate.formatEditorTimestamp(NOW.toEpochMilli(), Locale.US)

        assertThat(text).isEqualTo("14:02")
    }

    @Test
    fun `formatEditorTimestamp returns date and time for a timestamp from yesterday`() {
        val yesterday = NOW.minusSeconds(SECONDS_PER_DAY)

        val text = formatRelativeDate.formatEditorTimestamp(yesterday.toEpochMilli(), Locale.US)

        assertThat(text).isEqualTo("16 Jul, 14:02")
    }

    @Test
    fun `formatEditorTimestamp returns date and time for a timestamp from long ago`() {
        val longAgo = NOW.minusSeconds(365 * SECONDS_PER_DAY)

        val text = formatRelativeDate.formatEditorTimestamp(longAgo.toEpochMilli(), Locale.US)

        assertThat(text).isEqualTo("17 Jul, 14:02")
    }

    private companion object {
        const val SECONDS_PER_DAY = 24L * 60 * 60
    }
}
