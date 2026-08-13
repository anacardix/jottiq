package com.anacardix.jottiq.domain

import java.util.Locale

/** App-wide locale preference — Settings' "Language" row (`design/15. Settings.png`). */
enum class AppLanguage {
    System,
    English,
    Italian,
    German,
    French,
    SpanishSpain,
    SpanishLatinAmerica,
    PortuguesePortugal,
    PortugueseBrazil,
}

/** Maps to a concrete [Locale], or `null` for [AppLanguage.System] (meaning: use the device default). */
fun AppLanguage.toLocaleOrNull(): Locale? = when (this) {
    AppLanguage.System -> null
    AppLanguage.English -> Locale.forLanguageTag("en")
    AppLanguage.Italian -> Locale.forLanguageTag("it")
    AppLanguage.German -> Locale.forLanguageTag("de")
    AppLanguage.French -> Locale.forLanguageTag("fr")
    AppLanguage.SpanishSpain -> Locale.forLanguageTag("es-ES")
    AppLanguage.SpanishLatinAmerica -> Locale.forLanguageTag("es-419")
    AppLanguage.PortuguesePortugal -> Locale.forLanguageTag("pt-PT")
    AppLanguage.PortugueseBrazil -> Locale.forLanguageTag("pt-BR")
}

/**
 * Same as [toLocaleOrNull], falling back to the device default locale for [AppLanguage.System] — the
 * locale that date-formatting use cases ([com.anacardix.jottiq.domain.usecase.FormatRelativeDateUseCase],
 * [com.anacardix.jottiq.domain.usecase.GroupNotesByDateUseCase]) should actually format against,
 * since they run in ViewModels with no access to Compose's [LocalConfiguration][
 * androidx.compose.ui.platform.LocalConfiguration]-based in-app language override.
 */
fun AppLanguage.toLocale(): Locale = toLocaleOrNull() ?: Locale.getDefault()
