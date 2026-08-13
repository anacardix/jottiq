package com.anacardix.jottiq.domain.usecase

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val RETENTION_DAYS = 30L

/**
 * Days remaining before a trashed note is eligible for permanent deletion
 * (`design/14. Trash.png`'s "N days left" and the 30-day policy banner). Clamped to 0 rather than
 * going negative once the retention window has passed — purging itself is a separate, explicit
 * action (CLAUDE.md: hard-delete only happens on trash purge).
 */
class CalculateTrashRetentionUseCase @Inject constructor(private val clock: Clock) {
    operator fun invoke(deletedAt: Long): Int {
        val deletedDate = Instant.ofEpochMilli(deletedAt).atZone(clock.zone).toLocalDate()
        val today = LocalDate.now(clock)
        val daysSinceDeleted = ChronoUnit.DAYS.between(deletedDate, today)
        return (RETENTION_DAYS - daysSinceDeleted).coerceIn(0, RETENTION_DAYS).toInt()
    }
}
