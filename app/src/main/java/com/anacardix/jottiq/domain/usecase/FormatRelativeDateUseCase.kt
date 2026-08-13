package com.anacardix.jottiq.domain.usecase

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * A note/folder row's date label, bucketed but not yet localized.
 */
sealed interface RelativeDateLabel {
    data class Time(val text: String) : RelativeDateLabel
    data class Date(val text: String) : RelativeDateLabel
}

/**
 * Buckets an epoch-millis timestamp into a row-friendly label: "HH:mm" for today or yesterday
 * (the list's Today/Yesterday section headers already convey which of the two it is), or an older
 * date ("d MMM") otherwise, per `design/01. Home.png`'s row date labels. [Clock] is injected so
 * "today" is deterministic under test.
 */
class FormatRelativeDateUseCase @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(epochMillis: Long, locale: Locale = Locale.getDefault()): RelativeDateLabel {
        val zonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(clock.zone)
        val date = zonedDateTime.toLocalDate()
        val today = LocalDate.now(clock)
        return if (date == today || date == today.minusDays(1)) {
            RelativeDateLabel.Time(TIME_FORMAT.withLocale(locale).format(zonedDateTime))
        } else {
            RelativeDateLabel.Date(DATE_FORMAT.withLocale(locale).format(zonedDateTime))
        }
    }

    /**
     * Formats [epochMillis] for the note editor's Created/Edited subtitle: just "HH:mm" for today,
     * or "d MMM, HH:mm" for any other day, so an older note's subtitle still says which day it was.
     */
    fun formatEditorTimestamp(epochMillis: Long, locale: Locale = Locale.getDefault()): String {
        val zonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(clock.zone)
        val date = zonedDateTime.toLocalDate()
        val today = LocalDate.now(clock)
        return if (date == today) {
            TIME_FORMAT.withLocale(locale).format(zonedDateTime)
        } else {
            EDITOR_DATE_TIME_FORMAT.withLocale(locale).format(zonedDateTime)
        }
    }

    private companion object {
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
        val EDITOR_DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm")
    }
}
