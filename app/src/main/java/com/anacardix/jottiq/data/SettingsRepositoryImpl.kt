package com.anacardix.jottiq.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.anacardix.jottiq.domain.AppLanguage
import com.anacardix.jottiq.domain.DataResult
import com.anacardix.jottiq.domain.SettingsRepository
import com.anacardix.jottiq.domain.SortOrder
import com.anacardix.jottiq.domain.ThemePref
import com.anacardix.jottiq.domain.runCatchingDataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val SORT_ORDER_KEY = stringPreferencesKey("sort_order")
private val THEME_PREF_KEY = stringPreferencesKey("theme_pref")
private val LANGUAGE_KEY = stringPreferencesKey("language")
private val HAPTICS_ENABLED_KEY = booleanPreferencesKey("haptics_enabled")

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun observeSortOrder(): Flow<SortOrder> = dataStore.data.map { preferences ->
        preferences[SORT_ORDER_KEY]?.let { name ->
            SortOrder.entries.firstOrNull { it.name == name }
        } ?: SortOrder.DateEdited
    }.distinctUntilChanged()

    override suspend fun setSortOrder(order: SortOrder): DataResult<Unit> = runCatchingDataResult {
        dataStore.edit { preferences -> preferences[SORT_ORDER_KEY] = order.name }
    }

    override fun observeThemePref(): Flow<ThemePref> = dataStore.data.map { preferences ->
        preferences[THEME_PREF_KEY]?.let { name ->
            ThemePref.entries.firstOrNull { it.name == name }
        } ?: ThemePref.System
    }.distinctUntilChanged()

    override suspend fun setThemePref(pref: ThemePref): DataResult<Unit> = runCatchingDataResult {
        dataStore.edit { preferences -> preferences[THEME_PREF_KEY] = pref.name }
    }

    override fun observeLanguage(): Flow<AppLanguage> = dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY]?.let { name ->
            AppLanguage.entries.firstOrNull { it.name == name }
        } ?: AppLanguage.System
    }.distinctUntilChanged()

    override suspend fun setLanguage(language: AppLanguage): DataResult<Unit> = runCatchingDataResult {
        dataStore.edit { preferences -> preferences[LANGUAGE_KEY] = language.name }
    }

    override fun observeHapticsEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HAPTICS_ENABLED_KEY] ?: true
    }.distinctUntilChanged()

    override suspend fun setHapticsEnabled(enabled: Boolean): DataResult<Unit> = runCatchingDataResult {
        dataStore.edit { preferences -> preferences[HAPTICS_ENABLED_KEY] = enabled }
    }
}
