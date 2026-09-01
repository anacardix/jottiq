package com.anacardix.jottiq.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anacardix.jottiq.R
import com.anacardix.jottiq.designsystem.JottiqHapticType
import com.anacardix.jottiq.designsystem.JottiqSpacing
import com.anacardix.jottiq.designsystem.JottiqTheme
import com.anacardix.jottiq.designsystem.component.GroupedListRow
import com.anacardix.jottiq.designsystem.component.JottiqTopAppBar
import com.anacardix.jottiq.designsystem.component.rememberJottiqTopAppBarScrollBehavior
import com.anacardix.jottiq.designsystem.icon.AppIcon
import com.anacardix.jottiq.designsystem.icon.AppIcons
import com.anacardix.jottiq.designsystem.rememberJottiqHaptics
import com.anacardix.jottiq.domain.AppLanguage
import com.anacardix.jottiq.domain.SortOrder
import com.anacardix.jottiq.domain.ThemePref
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.onEvent(SettingsEvent.ScreenShown)
    }
    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collectLatest { event ->
            when (event) {
                SettingsNavigationEvent.Back -> onBackClick()
            }
        }
    }
    SettingsContent(uiState = uiState, onEvent = viewModel::onEvent, onBackClick = onBackClick, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = rememberJottiqTopAppBarScrollBehavior()
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JottiqTopAppBar(
                title = stringResource(R.string.settings_title),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        AppIcon(AppIcons.ArrowBack, contentDescription = stringResource(R.string.settings_back_action))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = JottiqSpacing.screenGutter),
        ) {
            SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                GroupedListRow(
                    index = 0,
                    count = 1,
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_theme_row_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    supportingContent = { Text(uiState.themePref.displayText()) },
                    leadingContent = { AppIcon(AppIcons.Contrast, contentDescription = null) },
                    trailingContent = { AppIcon(AppIcons.ChevronRight, contentDescription = null) },
                    onClick = { onEvent(SettingsEvent.ThemeRowClicked) },
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_notes)) {
                GroupedListRow(
                    index = 0,
                    count = 1,
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_sort_row_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    supportingContent = { Text(uiState.sortOrder.displayText()) },
                    leadingContent = { AppIcon(AppIcons.SwapVert, contentDescription = null) },
                    trailingContent = { AppIcon(AppIcons.ChevronRight, contentDescription = null) },
                    onClick = { onEvent(SettingsEvent.SortRowClicked) },
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_general)) {
                GroupedListRow(
                    index = 0,
                    count = 2,
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_language_row_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    supportingContent = { Text(uiState.language.displayText()) },
                    leadingContent = { AppIcon(AppIcons.Language, contentDescription = null) },
                    trailingContent = { AppIcon(AppIcons.ChevronRight, contentDescription = null) },
                    onClick = { onEvent(SettingsEvent.LanguageRowClicked) },
                )
                val haptics = rememberJottiqHaptics()
                GroupedListRow(
                    index = 1,
                    count = 2,
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_haptics_row_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    supportingContent = { Text(stringResource(R.string.settings_haptics_row_subtitle)) },
                    leadingContent = { AppIcon(AppIcons.Settings, contentDescription = null) },
                    trailingContent = {
                        Switch(checked = uiState.hapticsEnabled, onCheckedChange = null)
                    },
                    onClick = {
                        val enabled = !uiState.hapticsEnabled
                        haptics.perform(if (enabled) JottiqHapticType.ToggleOn else JottiqHapticType.ToggleOff)
                        onEvent(SettingsEvent.HapticsToggled(enabled))
                    },
                )
            }
        }
    }

    when (uiState.activeDialog) {
        SettingsDialog.Theme -> ThemePickerDialog(selected = uiState.themePref, onEvent = onEvent)
        SettingsDialog.Sort -> SortPickerDialog(selected = uiState.sortOrder, onEvent = onEvent)
        SettingsDialog.Language -> LanguagePickerDialog(selected = uiState.language, onEvent = onEvent)
        null -> Unit
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = JottiqSpacing.l, bottom = JottiqSpacing.s),
    )
    content()
}

@Composable
private fun ThemePickerDialog(selected: ThemePref, onEvent: (SettingsEvent) -> Unit) {
    SingleChoiceDialog(
        title = stringResource(R.string.settings_theme_row_title),
        options = listOf(
            ThemePref.System to stringResource(R.string.settings_theme_system),
            ThemePref.Light to stringResource(R.string.settings_theme_light),
            ThemePref.Dark to stringResource(R.string.settings_theme_dark),
        ),
        selected = selected,
        onSelected = { onEvent(SettingsEvent.ThemeSelected(it)) },
        onDismiss = { onEvent(SettingsEvent.DialogDismissed) },
    )
}

@Composable
private fun SortPickerDialog(selected: SortOrder, onEvent: (SettingsEvent) -> Unit) {
    SingleChoiceDialog(
        title = stringResource(R.string.settings_sort_row_title),
        options = listOf(
            SortOrder.DateEdited to stringResource(R.string.home_sort_date_edited),
            SortOrder.DateCreated to stringResource(R.string.home_sort_date_created),
            SortOrder.TitleAsc to stringResource(R.string.home_sort_title_az),
        ),
        selected = selected,
        onSelected = { onEvent(SettingsEvent.SortOrderSelected(it)) },
        onDismiss = { onEvent(SettingsEvent.DialogDismissed) },
    )
}

@Composable
private fun LanguagePickerDialog(selected: AppLanguage, onEvent: (SettingsEvent) -> Unit) {
    SingleChoiceDialog(
        title = stringResource(R.string.settings_language_row_title),
        options = listOf(
            AppLanguage.System to stringResource(R.string.settings_language_system),
            AppLanguage.English to stringResource(R.string.settings_language_english),
            AppLanguage.Italian to stringResource(R.string.settings_language_italian),
            AppLanguage.German to stringResource(R.string.settings_language_german),
            AppLanguage.French to stringResource(R.string.settings_language_french),
            AppLanguage.SpanishSpain to stringResource(R.string.settings_language_spanish_spain),
            AppLanguage.SpanishLatinAmerica to stringResource(R.string.settings_language_spanish_latin_america),
            AppLanguage.PortuguesePortugal to stringResource(R.string.settings_language_portuguese_portugal),
            AppLanguage.PortugueseBrazil to stringResource(R.string.settings_language_portuguese_brazil),
        ),
        selected = selected,
        onSelected = { onEvent(SettingsEvent.LanguageSelected(it)) },
        onDismiss = { onEvent(SettingsEvent.DialogDismissed) },
    )
}

@Composable
private fun <T> SingleChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberJottiqHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier
                    .selectableGroup()
                    .heightIn(max = JottiqSpacing.dialogOptionListMaxHeight)
                    .verticalScroll(rememberScrollState()),
            ) {
                options.forEach { (value, label) ->
                    val isSelected = value == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = {
                                    haptics.perform(JottiqHapticType.Confirm)
                                    onSelected(value)
                                },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = JottiqSpacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(Modifier.width(JottiqSpacing.m))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_dialog_cancel)) }
        },
    )
}

@Composable
private fun ThemePref.displayText(): String = when (this) {
    ThemePref.System -> stringResource(R.string.settings_theme_system)
    ThemePref.Light -> stringResource(R.string.settings_theme_light)
    ThemePref.Dark -> stringResource(R.string.settings_theme_dark)
}

@Composable
private fun SortOrder.displayText(): String = when (this) {
    SortOrder.DateEdited -> stringResource(R.string.home_sort_date_edited)
    SortOrder.DateCreated -> stringResource(R.string.home_sort_date_created)
    SortOrder.TitleAsc -> stringResource(R.string.home_sort_title_az)
}

@Composable
private fun AppLanguage.displayText(): String = when (this) {
    AppLanguage.System -> stringResource(R.string.settings_language_system)
    AppLanguage.English -> stringResource(R.string.settings_language_english)
    AppLanguage.Italian -> stringResource(R.string.settings_language_italian)
    AppLanguage.German -> stringResource(R.string.settings_language_german)
    AppLanguage.French -> stringResource(R.string.settings_language_french)
    AppLanguage.SpanishSpain -> stringResource(R.string.settings_language_spanish_spain)
    AppLanguage.SpanishLatinAmerica -> stringResource(R.string.settings_language_spanish_latin_america)
    AppLanguage.PortuguesePortugal -> stringResource(R.string.settings_language_portuguese_portugal)
    AppLanguage.PortugueseBrazil -> stringResource(R.string.settings_language_portuguese_brazil)
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun SettingsScreenPreviewLight() {
    JottiqTheme(darkTheme = false, dynamicColor = false) {
        SettingsContent(uiState = SettingsUiState(isLoading = false), onEvent = {}, onBackClick = {})
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun SettingsScreenPreviewDark() {
    JottiqTheme(darkTheme = true, dynamicColor = false) {
        SettingsContent(uiState = SettingsUiState(isLoading = false), onEvent = {}, onBackClick = {})
    }
}
