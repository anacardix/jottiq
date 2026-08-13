package com.anacardix.jottiq.di

import com.anacardix.jottiq.data.FoldersRepositoryImpl
import com.anacardix.jottiq.data.NotesRepositoryImpl
import com.anacardix.jottiq.data.SettingsRepositoryImpl
import com.anacardix.jottiq.domain.FoldersRepository
import com.anacardix.jottiq.domain.NotesRepository
import com.anacardix.jottiq.domain.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNotesRepository(impl: NotesRepositoryImpl): NotesRepository

    @Binds
    @Singleton
    abstract fun bindFoldersRepository(impl: FoldersRepositoryImpl): FoldersRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
