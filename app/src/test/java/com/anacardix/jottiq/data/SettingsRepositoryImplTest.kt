package com.anacardix.jottiq.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.anacardix.jottiq.domain.AppLanguage
import com.anacardix.jottiq.domain.DataResult
import com.anacardix.jottiq.domain.SortOrder
import com.anacardix.jottiq.domain.ThemePref
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class SettingsRepositoryImplTest {

    private lateinit var file: File
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun createRepository() {
        file = File.createTempFile("settings-repository-test", ".preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { file })
        repository = SettingsRepositoryImpl(dataStore)
    }

    @After
    fun deleteFile() {
        file.delete()
    }

    @Test
    fun `sort order defaults to date edited before anything is set`() = runTest {
        assertThat(repository.observeSortOrder().first()).isEqualTo(SortOrder.DateEdited)
    }

    @Test
    fun `setSortOrder persists and is reflected by observeSortOrder`() = runTest {
        val result = repository.setSortOrder(SortOrder.TitleAsc)

        assertThat(result).isEqualTo(DataResult.Success(Unit))
        assertThat(repository.observeSortOrder().first()).isEqualTo(SortOrder.TitleAsc)
    }

    @Test
    fun `observeSortOrder does not re-emit when an unrelated setting changes`() = runTest {
        repository.setSortOrder(SortOrder.TitleAsc)

        repository.observeSortOrder().test {
            assertThat(awaitItem()).isEqualTo(SortOrder.TitleAsc)

            // An unrelated preference write must not cause a duplicate SortOrder.TitleAsc emission
            // before the real change below — awaitItem() would otherwise see it first and fail.
            repository.setThemePref(ThemePref.Dark)
            repository.setSortOrder(SortOrder.DateCreated)

            assertThat(awaitItem()).isEqualTo(SortOrder.DateCreated)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `theme pref defaults to System before anything is set`() = runTest {
        assertThat(repository.observeThemePref().first()).isEqualTo(ThemePref.System)
    }

    @Test
    fun `setThemePref persists and is reflected by observeThemePref`() = runTest {
        val result = repository.setThemePref(ThemePref.Dark)

        assertThat(result).isEqualTo(DataResult.Success(Unit))
        assertThat(repository.observeThemePref().first()).isEqualTo(ThemePref.Dark)
    }

    @Test
    fun `language defaults to System before anything is set`() = runTest {
        assertThat(repository.observeLanguage().first()).isEqualTo(AppLanguage.System)
    }

    @Test
    fun `setLanguage persists and is reflected by observeLanguage`() = runTest {
        val result = repository.setLanguage(AppLanguage.Italian)

        assertThat(result).isEqualTo(DataResult.Success(Unit))
        assertThat(repository.observeLanguage().first()).isEqualTo(AppLanguage.Italian)
    }

    @Test
    fun `haptics enabled defaults to true before anything is set`() = runTest {
        assertThat(repository.observeHapticsEnabled().first()).isTrue()
    }

    @Test
    fun `setHapticsEnabled persists and is reflected by observeHapticsEnabled`() = runTest {
        val result = repository.setHapticsEnabled(false)

        assertThat(result).isEqualTo(DataResult.Success(Unit))
        assertThat(repository.observeHapticsEnabled().first()).isFalse()
    }
}
