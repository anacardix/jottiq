package com.anacardix.jottiq.ui.unlockgate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anacardix.jottiq.R
import com.anacardix.jottiq.designsystem.JottiqShapeTokens
import com.anacardix.jottiq.designsystem.JottiqSpacing
import com.anacardix.jottiq.designsystem.JottiqTheme
import com.anacardix.jottiq.designsystem.icon.AppIcon
import com.anacardix.jottiq.designsystem.icon.AppIcons
import com.anacardix.jottiq.ui.common.findActivity
import com.anacardix.jottiq.ui.common.resolve
import kotlinx.coroutines.flow.collectLatest

@Composable
fun UnlockGateScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onUnlocked: (targetId: String, isFolder: Boolean) -> Unit = { _, _ -> },
    viewModel: UnlockGateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collectLatest { event ->
            when (event) {
                UnlockGateNavigationEvent.Back -> onBackClick()
                is UnlockGateNavigationEvent.Unlocked -> onUnlocked(event.targetId, event.isFolder)
            }
        }
    }
    UnlockGateContent(uiState = uiState, onEvent = viewModel::onEvent, onBackClick = onBackClick, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnlockGateContent(
    uiState: UnlockGateUiState,
    onEvent: (UnlockGateEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage = uiState.userMessage
    val resolvedUserMessage = userMessage?.resolve()
    LaunchedEffect(userMessage?.id) {
        if (resolvedUserMessage != null) {
            try {
                snackbarHostState.showSnackbar(resolvedUserMessage)
            } finally {
                // Runs even if navigating away cancels this coroutine mid-snackbar, so the
                // message is always marked consumed and never re-shown on the next recomposition.
                onEvent(UnlockGateEvent.UserMessageShown)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        val backDescription = stringResource(R.string.unlock_gate_back_action)
                        AppIcon(AppIcons.ArrowBack, contentDescription = backDescription)
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        },
    ) { innerPadding ->
        val context = LocalContext.current
        val displayName = uiState.targetName.ifBlank { stringResource(R.string.untitled_note) }
        val promptTitle = stringResource(R.string.unlock_gate_prompt_title, displayName)

        LaunchedEffect(Unit) {
            val activity = context.findActivity() ?: return@LaunchedEffect
            onEvent(UnlockGateEvent.UnlockClicked(activity, promptTitle))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = JottiqSpacing.xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LockHero()
            Text(
                text = if (uiState.isFolder) {
                    stringResource(R.string.unlock_gate_title_folder)
                } else {
                    stringResource(R.string.unlock_gate_title_note)
                },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = JottiqSpacing.l),
            )
            Button(
                onClick = {
                    val activity = context.findActivity() ?: return@Button
                    onEvent(UnlockGateEvent.UnlockClicked(activity, promptTitle))
                },
                enabled = !uiState.isAuthenticating,
                modifier = Modifier.padding(top = JottiqSpacing.xl),
            ) {
                AppIcon(AppIcons.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                Text(
                    text = stringResource(R.string.unlock_gate_button),
                    modifier = Modifier.padding(start = JottiqSpacing.xs),
                )
            }
        }
    }
}

@Composable
private fun LockHero() {
    Box(
        modifier = Modifier
            .size(HERO_SIZE)
            .background(MaterialTheme.colorScheme.primaryContainer, JottiqShapeTokens.lockGateHero),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            AppIcons.Lock,
            contentDescription = null,
            filled = true,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            sizeSp = HERO_ICON_SIZE,
        )
    }
}

private val HERO_SIZE: Dp = 96.dp
private const val HERO_ICON_SIZE = 40

@Preview(name = "Note - Light", showBackground = true)
@Composable
private fun UnlockGateScreenPreviewNoteLight() {
    JottiqTheme(darkTheme = false, dynamicColor = false) {
        UnlockGateContent(
            uiState = UnlockGateUiState(targetId = "1", targetName = "Gift ideas", isFolder = false),
            onEvent = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "Folder - Dark", showBackground = true)
@Composable
private fun UnlockGateScreenPreviewFolderDark() {
    JottiqTheme(darkTheme = true, dynamicColor = false) {
        UnlockGateContent(
            uiState = UnlockGateUiState(targetId = "1", targetName = "Journal", isFolder = true),
            onEvent = {},
            onBackClick = {},
        )
    }
}
