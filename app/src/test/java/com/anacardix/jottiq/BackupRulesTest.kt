package com.anacardix.jottiq

import android.content.Context
import android.content.res.XmlResourceParser
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.xmlpull.v1.XmlPullParser

private const val DATASTORE_INCLUDE_PATH = "datastore/jottiq_settings.preferences_pb"

/**
 * Guards the Auto Backup / data-extraction rules referenced from AndroidManifest.xml
 * (`android:fullBackupContent`, `android:dataExtractionRules`). Everything not explicitly
 * `<include>`d is excluded from backup, so a silently dropped entry here means silently dropped
 * user data on restore — see DatabaseModuleInstrumentedTest for the companion journal-mode guard.
 */
@RunWith(RobolectricTestRunner::class)
class BackupRulesTest {

    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun `backup_rules includes the database and the settings DataStore file`() {
        val includes = collectIncludePaths(R.xml.backup_rules)

        assertThat(includes).containsEntry("database", ".")
        assertThat(includes).containsEntry("file", DATASTORE_INCLUDE_PATH)
    }

    @Test
    fun `data_extraction_rules includes the database and settings file per section`() {
        val includesBySection = collectIncludesBySection(R.xml.data_extraction_rules)

        assertThat(includesBySection["cloud-backup"]).contains("database" to ".")
        assertThat(includesBySection["cloud-backup"]).contains("file" to DATASTORE_INCLUDE_PATH)
        assertThat(includesBySection["device-transfer"]).contains("database" to ".")
        assertThat(includesBySection["device-transfer"]).contains("file" to DATASTORE_INCLUDE_PATH)
    }

    private fun collectIncludePaths(xmlResId: Int): Map<String, String> {
        val parser: XmlResourceParser = resources.getXml(xmlResId)
        val includes = mutableMapOf<String, String>()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "include") {
                includes[parser.getAttributeValue(null, "domain")] = parser.getAttributeValue(null, "path")
            }
            event = parser.next()
        }
        parser.close()

        return includes
    }

    /** Maps each `<cloud-backup>`/`<device-transfer>` section to its `(domain, path)` includes. */
    private fun collectIncludesBySection(xmlResId: Int): Map<String, List<Pair<String, String>>> {
        val parser = resources.getXml(xmlResId)
        val includesBySection = mutableMapOf<String, MutableList<Pair<String, String>>>()
        var currentSection: String? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                currentSection = nextSection(parser, currentSection, includesBySection)
            }
            event = parser.next()
        }
        parser.close()

        return includesBySection
    }

    private fun nextSection(
        parser: XmlResourceParser,
        currentSection: String?,
        includesBySection: MutableMap<String, MutableList<Pair<String, String>>>,
    ): String? = when (parser.name) {
        "cloud-backup", "device-transfer" -> parser.name
        "include" -> {
            val entry = parser.getAttributeValue(null, "domain") to parser.getAttributeValue(null, "path")
            currentSection?.let { includesBySection.getOrPut(it) { mutableListOf() }.add(entry) }
            currentSection
        }
        else -> currentSection
    }
}
