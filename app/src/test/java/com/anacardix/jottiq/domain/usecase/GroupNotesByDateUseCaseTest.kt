package com.anacardix.jottiq.domain.usecase

import com.anacardix.jottiq.domain.NoteSummary
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

private val FIXED_ZONE = ZoneOffset.UTC
private val NOW = Instant.parse("2026-07-17T14:02:00Z")

class GroupNotesByDateUseCaseTest {

    private val clock = Clock.fixed(NOW, FIXED_ZONE)
    private val groupNotesByDate = GroupNotesByDateUseCase(clock)

    @Test
    fun `timestamp from today buckets under Today`() {
        val groups = group(note(id = "n1", timestamp = NOW))

        assertThat(groups.map { it.group }).containsExactly(NoteDateGroup.Today)
    }

    @Test
    fun `timestamp from yesterday buckets under Yesterday`() {
        val yesterday = NOW.minusSeconds(SECONDS_PER_DAY)

        val groups = group(note(id = "n1", timestamp = yesterday))

        assertThat(groups.map { it.group }).containsExactly(NoteDateGroup.Yesterday)
    }

    @Test
    fun `timestamps two and seven days ago bucket under Previous7Days`() {
        val twoDaysAgo = NOW.minusSeconds(2 * SECONDS_PER_DAY)
        val sevenDaysAgo = NOW.minusSeconds(7 * SECONDS_PER_DAY)

        val groups = group(note(id = "n1", timestamp = twoDaysAgo), note(id = "n2", timestamp = sevenDaysAgo))

        assertThat(groups.map { it.group }).containsExactly(NoteDateGroup.Previous7Days)
        assertThat(groups.single().notes.map { it.id }).containsExactly("n1", "n2").inOrder()
    }

    @Test
    fun `timestamps eight and thirty days ago bucket under Previous30Days`() {
        val eightDaysAgo = NOW.minusSeconds(8 * SECONDS_PER_DAY)
        val thirtyDaysAgo = NOW.minusSeconds(30 * SECONDS_PER_DAY)

        val groups = group(note(id = "n1", timestamp = eightDaysAgo), note(id = "n2", timestamp = thirtyDaysAgo))

        assertThat(groups.map { it.group }).containsExactly(NoteDateGroup.Previous30Days)
    }

    @Test
    fun `an older timestamp still in the current year buckets under its month`() {
        val thirtyOneDaysAgo = NOW.minusSeconds(31 * SECONDS_PER_DAY) // 2026-06-16, current year

        val groups = group(note(id = "n1", timestamp = thirtyOneDaysAgo))

        assertThat(groups.map { it.group }).containsExactly(NoteDateGroup.Month("June"))
    }

    @Test
    fun `a timestamp from a prior year buckets under its year`() {
        val lastYear = Instant.parse("2025-03-01T10:00:00Z")

        val groups = group(note(id = "n1", timestamp = lastYear))

        assertThat(groups.map { it.group }).containsExactly(NoteDateGroup.Year("2025"))
    }

    @Test
    fun `month labels are formatted with the requested locale`() {
        val thirtyOneDaysAgo = NOW.minusSeconds(31 * SECONDS_PER_DAY)

        val groups = groupNotesByDate(
            listOf(note(id = "n1", timestamp = thirtyOneDaysAgo)),
            locale = Locale.ITALIAN,
            timestampOf = NoteSummary::updatedAt,
        )

        assertThat(groups.map { it.group }).containsExactly(NoteDateGroup.Month("giugno"))
    }

    @Test
    fun `sections are ordered most-recent-first regardless of incoming note order`() {
        val today = note(id = "today", timestamp = NOW)
        val lastYear = note(id = "last-year", timestamp = Instant.parse("2025-03-01T10:00:00Z"))
        val previous7Days = note(id = "previous-7", timestamp = NOW.minusSeconds(2 * SECONDS_PER_DAY))

        val groups = group(lastYear, today, previous7Days)

        assertThat(groups.map { it.group }).containsExactly(
            NoteDateGroup.Today,
            NoteDateGroup.Previous7Days,
            NoteDateGroup.Year("2025"),
        ).inOrder()
    }

    @Test
    fun `within-group order follows the incoming note order, such as an alphabetical sort`() {
        val apple = note(id = "n1", title = "Apple", timestamp = NOW)
        val banana = note(id = "n2", title = "Banana", timestamp = NOW)

        val groups = group(apple, banana)

        assertThat(groups.single().notes.map { it.title }).containsExactly("Apple", "Banana").inOrder()
    }

    @Test
    fun `createdAt is used when timestampOf selects it instead of updatedAt`() {
        val createdLastYear = note(id = "n1", timestamp = Instant.parse("2025-03-01T10:00:00Z"), updatedTimestamp = NOW)

        val groups = groupNotesByDate(listOf(createdLastYear), locale = Locale.US, timestampOf = NoteSummary::createdAt)

        assertThat(groups.map { it.group }).containsExactly(NoteDateGroup.Year("2025"))
    }

    @Test
    fun `empty input produces no sections`() {
        assertThat(group()).isEmpty()
    }

    private fun group(vararg notes: NoteSummary) =
        groupNotesByDate(notes.toList(), locale = Locale.US, timestampOf = NoteSummary::updatedAt)

    @Suppress("LongParameterList")
    private fun note(
        id: String,
        timestamp: Instant,
        title: String = "Note $id",
        updatedTimestamp: Instant = timestamp,
    ) = NoteSummary(
        id = id,
        folderId = null,
        title = title,
        isFavorite = false,
        isLocked = false,
        createdAt = timestamp.toEpochMilli(),
        updatedAt = updatedTimestamp.toEpochMilli(),
        deletedAt = null,
    )

    private companion object {
        const val SECONDS_PER_DAY = 24L * 60 * 60
    }
}
