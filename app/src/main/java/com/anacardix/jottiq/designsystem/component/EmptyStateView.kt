package com.anacardix.jottiq.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anacardix.jottiq.designsystem.JottiqSpacing
import com.anacardix.jottiq.designsystem.icon.AppIcon
import com.anacardix.jottiq.designsystem.icon.AppIconGlyph

private val ICON_CONTAINER_SIZE = 96.dp
private const val ICON_SIZE_SP = 40
private val MESSAGE_MAX_WIDTH = 260.dp

// Matches Roboto Flex's "Emphasized" weight bucket (design-tokens.png), the same one headlineLarge/
// Medium use — the title reads as bold, not the plain titleLarge weight.
@Suppress("MagicNumber")
private val TITLE_WEIGHT = FontWeight(650)

/**
 * Per-screen empty variant (Home/Folder-view/Trash) per `design/design-tokens.png`: a
 * `surfaceContainer` circle around a glyph, an optional bold [title], and a centered [message]
 * below (`design/18. No notes.png`, `design/19. No notes in folder.png`). [topSpacing] lets
 * screens with a taller header (Home/Folder) push the block further down to match those designs.
 */
@Composable
fun EmptyStateView(
    icon: AppIconGlyph,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    topSpacing: Dp = JottiqSpacing.xl,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(ICON_CONTAINER_SIZE)
                .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                sizeSp = ICON_SIZE_SP,
            )
        }
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = TITLE_WEIGHT),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = JottiqSpacing.l),
            )
        }
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = MESSAGE_MAX_WIDTH)
                    .padding(top = if (title != null) JottiqSpacing.xs else JottiqSpacing.l),
            )
        }
    }
}
