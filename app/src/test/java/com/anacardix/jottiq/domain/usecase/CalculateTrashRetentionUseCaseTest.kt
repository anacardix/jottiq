package com.anacardix.jottiq.domain.usecase

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

private val FIXED_ZONE = ZoneOffset.UTC
private val NOW = Instant.parse("2026-07-17T14:02:00Z")

private const val SECONDS_PER_DAY = 24L * 60 * 60

class CalculateTrashRetentionUseCaseTest {

    private val clock = Clock.fixed(NOW, FIXED_ZONE)
    private val calculateRetention = CalculateTrashRetentionUseCase(clock)

    @Test
    fun `a note deleted today has the full 30 days left`() {
        assertThat(calculateRetention(NOW.toEpochMilli())).isEqualTo(30)
    }

    @Test
    fun `a note deleted 24 days ago has 6 days left`() {
        val deletedAt = NOW.minusSeconds(24 * SECONDS_PER_DAY)

        assertThat(calculateRetention(deletedAt.toEpochMilli())).isEqualTo(6)
    }

    @Test
    fun `a note deleted exactly 30 days ago has 0 days left`() {
        val deletedAt = NOW.minusSeconds(30 * SECONDS_PER_DAY)

        assertThat(calculateRetention(deletedAt.toEpochMilli())).isEqualTo(0)
    }

    @Test
    fun `a note past the retention window clamps to 0, never negative`() {
        val deletedAt = NOW.minusSeconds(90 * SECONDS_PER_DAY)

        assertThat(calculateRetention(deletedAt.toEpochMilli())).isEqualTo(0)
    }
}
