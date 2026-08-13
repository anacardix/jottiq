package com.anacardix.jottiq.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: retires the scaffold `placeholder` table and introduces the real `notes`/`folders`
 * tables (see [com.anacardix.jottiq.data.local.entity.NoteEntity] and
 * [com.anacardix.jottiq.data.local.entity.FolderEntity]).
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `placeholder`")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `notes` (" +
                "`id` TEXT NOT NULL, `folderId` TEXT, `title` TEXT NOT NULL, " +
                "`documentJson` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, " +
                "`isLocked` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_folderId` ON `notes` (`folderId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_deletedAt` ON `notes` (`deletedAt`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `folders` (" +
                "`id` TEXT NOT NULL, `parentId` TEXT, `name` TEXT NOT NULL, " +
                "`isLocked` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_folders_parentId` ON `folders` (`parentId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_folders_deletedAt` ON `folders` (`deletedAt`)")
    }
}
