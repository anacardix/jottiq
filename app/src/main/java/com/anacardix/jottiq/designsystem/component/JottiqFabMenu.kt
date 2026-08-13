package com.anacardix.jottiq.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.anacardix.jottiq.designsystem.JottiqHapticType
import com.anacardix.jottiq.designsystem.JottiqSpacing
import com.anacardix.jottiq.designsystem.icon.AppIcon
import com.anacardix.jottiq.designsystem.icon.AppIcons
import com.anacardix.jottiq.designsystem.rememberJottiqHaptics

/**
 * Home/Folder's Expressive "speed-dial" FAB: an Add/Close toggle that expands into a column of
 * labeled actions. Wraps `FloatingActionButtonMenu` + `ToggleFloatingActionButton` so the
 * `ExperimentalMaterial3ExpressiveApi` opt-in stays inside `designsystem` (see the kdoc on
 * [com.anacardix.jottiq.designsystem.JottiqTheme]).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JottiqFabMenu(
    expanded: Boolean,
    onToggle: () -> Unit,
    toggleContentDescription: String,
    items: List<JottiqFabMenuItem>,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberJottiqHaptics()
    FloatingActionButtonMenu(
        modifier = modifier.padding(JottiqSpacing.screenGutter),
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = {
                    haptics.perform(if (expanded) JottiqHapticType.ToggleOff else JottiqHapticType.ToggleOn)
                    onToggle()
                },
            ) {
                val glyph by remember {
                    derivedStateOf { if (checkedProgress > FAB_ICON_SWAP_THRESHOLD) AppIcons.Close else AppIcons.Add }
                }
                AppIcon(
                    glyph,
                    contentDescription = toggleContentDescription,
                    modifier = Modifier.animateIcon({ checkedProgress }),
                )
            }
        },
    ) {
        items.forEach { item ->
            FloatingActionButtonMenuItem(
                onClick = item.onClick,
                text = { Text(item.label) },
                icon = { AppIcon(item.icon, contentDescription = null) },
                containerColor = item.containerColor,
            )
        }
    }
}

private const val FAB_ICON_SWAP_THRESHOLD = 0.5f
