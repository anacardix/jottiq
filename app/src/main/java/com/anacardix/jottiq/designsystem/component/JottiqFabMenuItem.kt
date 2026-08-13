package com.anacardix.jottiq.designsystem.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.anacardix.jottiq.designsystem.icon.AppIconGlyph

/** One action row of an expanded [JottiqFabMenu] (Home's "New folder" / "New note", ...). */
@Immutable
data class JottiqFabMenuItem(
    val label: String,
    val icon: AppIconGlyph,
    val containerColor: Color,
    val onClick: () -> Unit,
)
