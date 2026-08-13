package com.anacardix.jottiq.di

import android.content.Context
import androidx.room.Room
import com.anacardix.jottiq.data.local.JottiqDatabase
import com.anacardix.jottiq.data.local.RoomTransactionRunner
import com.anacardix.jottiq.data.local.TransactionRunner
import com.anacardix.jottiq.data.local.dao.FolderDao
import com.anacardix.jottiq.data.local.dao.NoteDao
import com.anacardix.jottiq.data.local.migration.MIGRATION_1_2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "jottiq.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideJottiqDatabase(@ApplicationContext context: Context): JottiqDatabase =
        Room.databaseBuilder(context, JottiqDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideNoteDao(database: JottiqDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideFolderDao(database: JottiqDatabase): FolderDao = database.folderDao()

    @Provides
    @Singleton
    fun provideTransactionRunner(database: JottiqDatabase): TransactionRunner =
        RoomTransactionRunner(database)
}
