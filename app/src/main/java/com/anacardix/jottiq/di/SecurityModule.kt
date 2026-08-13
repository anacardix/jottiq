package com.anacardix.jottiq.di

import com.anacardix.jottiq.security.AppLockManager
import com.anacardix.jottiq.security.BiometricAppLockManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindAppLockManager(impl: BiometricAppLockManager): AppLockManager
}
