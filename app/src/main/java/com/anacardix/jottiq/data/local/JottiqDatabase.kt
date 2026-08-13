package com.anacardix.jottiq.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.anacardix.jottiq.data.local.dao.FolderDao
import com.anacardix.jottiq.data.local.dao.NoteDao
import com.anacardix.jottiq.data.local.entity.FolderEntity
import com.anacardix.jottiq.data.local.entity.NoteEntity

/**
 * Single Room database for the app — the sync-ready source of truth. Entities and DAOs never
 * leave the data layer (see CLAUDE.md).
 */
@Database(entities = [NoteEntity::class, FolderEntity::class], version = 2, exportSchema = true)
abstract class JottiqDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
}
