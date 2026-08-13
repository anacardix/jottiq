package com.anacardix.jottiq.domain

import kotlinx.coroutines.flow.Flow

/**
 * Source of truth for user preferences. Implemented in `data/` against DataStore. Sort order is
 * shared by Home's sort menu and Settings' "Default sorting" row — both read/write the same value.
 */
interface SettingsRepository {
    fun observeSortOrder(): Flow<SortOrder>
    suspend fun setSortOrder(order: SortOrder): DataResult<Unit>

    fun observeThemePref(): Flow<ThemePref>
    suspend fun setThemePref(pref: ThemePref): DataResult<Unit>

    fun observeLanguage(): Flow<AppLanguage>
    suspend fun setLanguage(language: AppLanguage): DataResult<Unit>

    fun observeHapticsEnabled(): Flow<Boolean>
    suspend fun setHapticsEnabled(enabled: Boolean): DataResult<Unit>
}
