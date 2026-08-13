package com.anacardix.jottiq.fakes

import com.anacardix.jottiq.domain.AppLanguage
import com.anacardix.jottiq.domain.DataResult
import com.anacardix.jottiq.domain.SettingsRepository
import com.anacardix.jottiq.domain.SortOrder
import com.anacardix.jottiq.domain.ThemePref
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [SettingsRepository] fake, reused across screen tests per CLAUDE.md's fakes-first policy. */
class FakeSettingsRepository : SettingsRepository {

    private val sortOrderFlow = MutableStateFlow(SortOrder.DateEdited)
    private val themePrefFlow = MutableStateFlow(ThemePref.System)
    private val languageFlow = MutableStateFlow(AppLanguage.System)
    private val hapticsEnabledFlow = MutableStateFlow(true)

    override fun observeSortOrder() = sortOrderFlow

    override suspend fun setSortOrder(order: SortOrder): DataResult<Unit> {
        sortOrderFlow.update { order }
        return DataResult.Success(Unit)
    }

    override fun observeThemePref() = themePrefFlow

    override suspend fun setThemePref(pref: ThemePref): DataResult<Unit> {
        themePrefFlow.update { pref }
        return DataResult.Success(Unit)
    }

    override fun observeLanguage() = languageFlow

    override suspend fun setLanguage(language: AppLanguage): DataResult<Unit> {
        languageFlow.update { language }
        return DataResult.Success(Unit)
    }

    override fun observeHapticsEnabled() = hapticsEnabledFlow

    override suspend fun setHapticsEnabled(enabled: Boolean): DataResult<Unit> {
        hapticsEnabledFlow.update { enabled }
        return DataResult.Success(Unit)
    }
}
