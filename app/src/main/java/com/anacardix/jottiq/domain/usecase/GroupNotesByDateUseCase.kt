package com.anacardix.jottiq.domain.usecase

import com.anacardix.jottiq.domain.NoteSummary
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * A note-list section header bucketing notes by recency, per `design/01. Home.png`'s Apple-Notes-
 * style sectioning. [Today]/[Yesterday]/[Previous7Days]/[Previous30Days] are fixed buckets resolved
 * to a string resource at the UI layer, per CLAUDE.md's "user-visible messages via string resource
 * ids" — mirroring [RelativeDateLabel]. [Month]/[Year] carry pre-formatted, locale-aware text
 * instead, since their content (month/year names) varies by locale rather than just their wording.
 */
sealed interface NoteDateGroup {
    data object Today : NoteDateGroup
    data object Yesterday : NoteDateGroup
    data object Previous7Days : NoteDateGroup
    data object Previous30Days : NoteDateGroup
    data class Month(val label: String) : NoteDateGroup
    data class Year(val label: String) : NoteDateGroup
}

/** One [NoteDateGroup] header and the notes under it, in their incoming order. */
data class NoteDateSection(val group: NoteDateGroup, val notes: List<NoteSummary>)

/**
 * Buckets [notes] into [NoteDateSection]s the way Apple Notes groups its note list: Today,
 * Yesterday, Previous 7 Days, Previous 30 Days, then one section per month for older notes still in
 * the current year, then one section per year for anything older. [notes] is expected to already be
 * sorted by the screen's [com.anacardix.jottiq.domain.SortOrder] — that order is preserved within
 * each section, since sections are only reordered relative to each other (by their most recent
 * [timestampOf] value), so the result reads most-recent-first even when [notes] arrives sorted by
 * title. [Clock] is injected so "today" is deterministic under test, matching
 * [FormatRelativeDateUseCase].
 */
class GroupNotesByDateUseCase @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(
        notes: List<NoteSummary>,
        locale: Locale = Locale.getDefault(),
        timestampOf: (NoteSummary) -> Long,
    ): List<NoteDateSection> {
        val today = LocalDate.now(clock)
        return notes
            .groupBy { groupFor(timestampOf(it), today, locale) }
            .entries
            .sortedByDescending { (_, groupNotes) -> groupNotes.maxOf(timestampOf) }
            .map { (group, groupNotes) -> NoteDateSection(group, groupNotes) }
    }

    private fun groupFor(epochMillis: Long, today: LocalDate, locale: Locale): NoteDateGroup {
        val date = Instant.ofEpochMilli(epochMillis).atZone(clock.zone).toLocalDate()
        return when {
            // Future timestamps (or clock skew) fall into Today rather than an else-branch bucket.
            !date.isBefore(today) -> NoteDateGroup.Today
            date == today.minusDays(1) -> NoteDateGroup.Yesterday
            !date.isBefore(today.minusDays(DAYS_IN_WEEK)) -> NoteDateGroup.Previous7Days
            !date.isBefore(today.minusDays(DAYS_IN_MONTH)) -> NoteDateGroup.Previous30Days
            date.year == today.year ->
                NoteDateGroup.Month(date.format(MONTH_FORMAT.withLocale(locale)))
            else -> NoteDateGroup.Year(date.year.toString())
        }
    }

    private companion object {
        const val DAYS_IN_WEEK = 7L
        const val DAYS_IN_MONTH = 30L
        val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL")
    }
}
