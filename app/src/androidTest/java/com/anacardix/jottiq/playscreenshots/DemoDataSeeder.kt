package com.anacardix.jottiq.playscreenshots

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.anacardix.jottiq.data.local.JottiqDatabase
import com.anacardix.jottiq.data.local.entity.FolderEntity
import com.anacardix.jottiq.data.local.entity.NoteEntity
import com.anacardix.jottiq.data.local.json.NoteDocumentDto
import com.anacardix.jottiq.data.local.json.toDto
import com.anacardix.jottiq.data.local.migration.MIGRATION_1_2
import com.anacardix.jottiq.domain.FormatSpan
import com.anacardix.jottiq.domain.FormatStyle
import com.anacardix.jottiq.domain.HeadingLevel
import com.anacardix.jottiq.domain.NoteBlock
import com.anacardix.jottiq.domain.NoteDocument
import com.anacardix.jottiq.domain.NoteTextColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val DATABASE_NAME = "jottiq.db"
private const val SETTINGS_DATASTORE_NAME = "jottiq_settings"
private val LANGUAGE_KEY = stringPreferencesKey("language")
private val THEME_PREF_KEY = stringPreferencesKey("theme_pref")
private val LENIENT_JSON = Json { ignoreUnknownKeys = true }

/**
 * Seeds the app's real Room database + settings DataStore directly through the app's own DAOs —
 * no precomputed `.db` file — so the seeded data always matches the app's current schema and
 * migrations (see the phase brief). Runs in the same process as the app under test (this is a
 * self-instrumenting androidTest APK, not `androidx.test.orchestrator`), so writing to these
 * on-disk files here is visible to the app's own Hilt-provided [JottiqDatabase]/DataStore the
 * moment `MainActivity` is launched afterward — both sides open the exact same files by name.
 *
 * Every note/folder id is a freshly generated UUID (CLAUDE.md's sync-ready invariant); the human
 * -readable ids in the JSON asset are only used to resolve folder parent/child references while
 * seeding.
 */
object DemoDataSeeder {

    /**
     * [targetContext] must be `InstrumentationRegistry.getInstrumentation().targetContext` (the
     * app under test — its Room database/DataStore are what get seeded); [assetContext] must be
     * `InstrumentationRegistry.getInstrumentation().context` (this androidTest APK's own context
     * — `it.json` is bundled into *its* assets, not the target app's, so reading it through
     * [targetContext] fails with `FileNotFoundException` even though the file is right there in
     * the merged androidTest assets on the build side).
     */
    fun seed(
        targetContext: Context,
        assetContext: Context,
        assetFileName: String,
        language: String,
        themePref: String,
    ): DemoDataJson {
        val demoData = loadDemoData(assetContext, assetFileName)
        seedDatabase(targetContext, demoData)
        runBlocking { seedSettings(targetContext, language, themePref) }
        return demoData
    }

    private fun loadDemoData(assetContext: Context, assetFileName: String): DemoDataJson {
        val json = assetContext.assets.open(assetFileName).use { it.readBytes().decodeToString() }
        return LENIENT_JSON.decodeFromString(DemoDataJson.serializer(), json)
    }

    private fun seedDatabase(context: Context, demoData: DemoDataJson) {
        // A fresh id per seed call (rather than a real UUID literal in the JSON) keeps the demo
        // JSON human-readable while still respecting the "client-generated UUID" invariant.
        val idMap = mutableMapOf<String, String>()
        fun resolveId(jsonId: String) = idMap.getOrPut(jsonId) { UUID.randomUUID().toString() }

        val now = System.currentTimeMillis()
        fun minutesAgo(minutes: Long) = now - TimeUnit.MINUTES.toMillis(minutes)

        val database = Room.databaseBuilder(context, JottiqDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()
        try {
            val folderDao = database.folderDao()
            val noteDao = database.noteDao()
            runBlocking {
                demoData.folders.forEach { folder ->
                    folderDao.upsert(
                        FolderEntity(
                            id = resolveId(folder.id),
                            parentId = folder.parentId?.let(::resolveId),
                            name = folder.name,
                            isLocked = folder.locked,
                            createdAt = now,
                            updatedAt = now,
                            deletedAt = null,
                        ),
                    )
                }
                (demoData.notes + demoData.trashedNotes).forEach { note ->
                    noteDao.upsert(
                        NoteEntity(
                            id = resolveId(note.id),
                            folderId = note.folderId?.let(::resolveId),
                            title = note.title,
                            documentJson = note.toDocumentJson(),
                            isFavorite = note.favorite,
                            isLocked = note.locked,
                            createdAt = minutesAgo(note.createdAtOffsetMinutes),
                            updatedAt = minutesAgo(note.updatedAtOffsetMinutes),
                            deletedAt = note.deletedAtOffsetMinutes?.let(::minutesAgo),
                        ),
                    )
                }
            }
        } finally {
            // Closed before MainActivity opens its own Room connection to the same file below —
            // avoids two live SQLiteOpenHelper connections overlapping during the handoff.
            database.close()
        }
    }

    private suspend fun seedSettings(context: Context, language: String, themePref: String) {
        // DataStore guards against two live instances for the same file existing in one process
        // (throws IllegalStateException) — without an explicit scope to cancel here, this
        // instance stays "active" indefinitely and collides with the app's own Hilt-provided
        // DataStore singleton for the same file the moment MainActivity launches right after.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { context.preferencesDataStoreFile(SETTINGS_DATASTORE_NAME) },
        )
        // Pinned to Light rather than left on the System default: System also turns on
        // Android 12+ dynamic color (wallpaper-derived Material You), which would make the
        // captured palette depend on whatever wallpaper this AVD happens to have instead of the
        // app's own brand beige/clay scheme (see `design/design-tokens.png`) — not what a Play
        // Store screenshot should show.
        dataStore.edit {
            it[LANGUAGE_KEY] = language
            it[THEME_PREF_KEY] = themePref
        }
        scope.cancel()
    }

    private fun DemoNoteJson.toDocumentJson(): String {
        val document = NoteDocument(
            blocks = blocks.map { block ->
                NoteBlock.Paragraph(
                    id = UUID.randomUUID().toString(),
                    text = block.text,
                    heading = block.heading?.let { HeadingLevel.valueOf(it) },
                    bulleted = block.bulleted,
                    numbered = block.numbered,
                    spans = block.spans.map { it.toFormatSpan(block.text) },
                )
            },
        )
        return LENIENT_JSON.encodeToString(NoteDocumentDto.serializer(), document.toDto())
    }

    private fun DemoSpanJson.toFormatSpan(blockText: String): FormatSpan {
        val start = blockText.indexOf(match)
        check(start >= 0) { "Span match \"$match\" not found in block text \"$blockText\"" }
        val style = when (style) {
            "bold" -> FormatStyle.Bold
            "italic" -> FormatStyle.Italic
            "underline" -> FormatStyle.Underline
            "color" -> FormatStyle.TextColor(NoteTextColor.valueOf(requireNotNull(color)))
            "link" -> FormatStyle.Link(requireNotNull(url))
            else -> error("Unknown span style \"$style\"")
        }
        return FormatSpan(start = start, end = start + match.length, style = style)
    }
}
