package com.anacardix.jottiq.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/** CPU-bound work off the main thread (e.g. JSON decode) — see [DispatchersModule]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** Blocking I/O off the main thread — see [DispatchersModule]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Coroutine dispatchers, injected (never referenced as `Dispatchers.xxx` directly in production
 * code) so tests can substitute a [kotlinx.coroutines.test.TestDispatcher].
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
