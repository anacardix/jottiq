package com.anacardix.jottiq.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.anacardix.jottiq.R
import com.anacardix.jottiq.designsystem.JottiqHapticType
import com.anacardix.jottiq.designsystem.JottiqSpacing
import com.anacardix.jottiq.designsystem.SwipeActionColors
import com.anacardix.jottiq.designsystem.icon.AppIcon
import com.anacardix.jottiq.designsystem.icon.AppIcons
import com.anacardix.jottiq.designsystem.rememberJottiqHaptics
import kotlinx.coroutines.launch

/**
 * Fraction of the row's width the user must drag past before release commits to an action, rather
 * than springing back to [SwipeToDismissBoxValue.Settled]. This only gates whether a release counts
 * as commit *intent* — once [SwipeToDismissBox] decides to commit (by distance or fling velocity), it
 * always animates the row the rest of the way to its full anchor offset (the row's own width) before
 * `onDismiss` fires, regardless of this fraction. So it just needs to comfortably exceed an
 * accidental nudge while staying well within what an ordinary deliberate swipe covers — too high
 * (0.9f was tried and reverted) makes normal swipes spring back instead of committing.
 */
private const val FULL_SWIPE_THRESHOLD_FRACTION = 0.4f

internal const val DELETE_SWIPE_BACKGROUND_TAG = "deleteSwipeBackground"
internal const val FAVORITE_SWIPE_BACKGROUND_TAG = "favoriteSwipeBackground"

/**
 * Wraps [content] (one [GroupedListRow]) with swipe gestures: swiping right-to-left past the
 * threshold slides the row all the way off-screen and calls [onDelete] once it settles dismissed.
 * The row itself is then removed once its item leaves the list the caller renders from (trashing is
 * soft-delete, so this happens as soon as the active-items flow re-emits).
 *
 * When [onToggleFavorite] is non-null, swiping left-to-right instead calls it and animates the row
 * back to [SwipeToDismissBoxValue.Settled] — favouriting is a toggle, not a dismissal, so the row
 * never leaves the list. Passing `null` (the default) disables that direction entirely, matching
 * folders, which only support delete.
 *
 * [resetSignal] must change whenever the caller's list re-includes this row's item after it was
 * dismissed (e.g. the user tapped Undo) — the `LazyColumn` may still be mid-exit-animation for the
 * old occurrence and reuse this composable's slot for the restored one, which would otherwise leave
 * it stuck rendering at its dismissed, off-screen position. Changing [resetSignal] discards the old
 * swipe state so the restored row starts fresh at [SwipeToDismissBoxValue.Settled].
 *
 * This composable doesn't attempt to reset the swipe animation if [onDelete] turns out to fail,
 * since that's an exceptional case for a local, single-device write; the caller is still expected to
 * surface a user-visible error message for it (CLAUDE.md: never swallow failures).
 */
@Composable
fun SwipeableGroupedRow(
    shape: Shape,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    resetSignal: Any = Unit,
    content: @Composable () -> Unit,
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnToggleFavorite by rememberUpdatedState(onToggleFavorite)
    val currentIsFavorite by rememberUpdatedState(isFavorite)
    val haptics = rememberJottiqHaptics()
    val currentHaptics by rememberUpdatedState(haptics)
    val dismissState = key(resetSignal) {
        rememberSwipeToDismissBoxState(
            positionalThreshold = { totalDistance -> totalDistance * FULL_SWIPE_THRESHOLD_FRACTION },
        )
    }
    val coroutineScope = rememberCoroutineScope()
    // Buzzes once the drag first crosses the commit threshold, so the user feels the point of no
    // return rather than only the eventual settle. Tracked separately from onDismiss (which only
    // fires once the row has finished animating to its dismissed anchor).
    LaunchedEffect(dismissState) {
        var previousTarget = dismissState.targetValue
        snapshotFlow { dismissState.targetValue }.collect { target ->
            if (target != SwipeToDismissBoxValue.Settled && previousTarget == SwipeToDismissBoxValue.Settled) {
                currentHaptics.perform(JottiqHapticType.GestureThreshold)
            }
            previousTarget = target
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = onToggleFavorite != null,
        enableDismissFromEndToStart = true,
        // SwipeToDismissBox always composes/paints backgroundContent, even when settled at
        // offset zero — gating on dismissDirection (Settled only when there's no offset) keeps
        // nothing painted at rest. Without this, the solid swipe-action color sits behind the
        // row permanently, and bleeds through the row's clipped rounded corners' anti-aliased
        // edge pixels as a faint colored ring around every card (see bug/corners.png).
        backgroundContent = {
            when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd ->
                    if (onToggleFavorite != null) {
                        FavoriteSwipeBackground(shape = shape, isFavorite = isFavorite)
                    } else {
                        DeleteSwipeBackground(shape = shape)
                    }
                SwipeToDismissBoxValue.EndToStart -> DeleteSwipeBackground(shape = shape)
                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
        onDismiss = { direction ->
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    currentOnToggleFavorite?.invoke()
                    currentHaptics.perform(
                        if (currentIsFavorite) JottiqHapticType.ToggleOff else JottiqHapticType.ToggleOn,
                    )
                    // Favoriting is a toggle, not a dismissal: animate back rather than staying
                    // slid out, matching the "confirmValueChange veto" pattern for the alpha API
                    // this designsystem-wrapped call is stuck on (see class kdoc / CLAUDE.md's
                    // "one package" note on the eventual stable-1.5.0 upgrade).
                    coroutineScope.launch { dismissState.reset() }
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    currentOnDelete()
                    currentHaptics.perform(JottiqHapticType.Reject)
                }
                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
        content = { content() },
    )
}

@Composable
private fun DeleteSwipeBackground(shape: Shape) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // The row's own card (GroupedListRow, via NoteListRows) is inset by screenGutter within
            // this box rather than this box itself being shrunk (that's what lets a committed swipe
            // clear the whole physical screen — see SwipeableGroupedRowTest). Mirror that same inset
            // here so this background's painted color lines up with the card's edges instead of
            // bleeding into the gutter margin at rest, when this box is fully visible either way.
            .padding(horizontal = JottiqSpacing.screenGutter)
            .testTag(DELETE_SWIPE_BACKGROUND_TAG)
            .clip(shape)
            .background(SwipeActionColors.Delete)
            .padding(end = JottiqSpacing.l),
        contentAlignment = Alignment.CenterEnd,
    ) {
        AppIcon(
            AppIcons.Delete,
            contentDescription = stringResource(R.string.row_swipe_delete_action),
            tint = SwipeActionColors.OnAction,
        )
    }
}

@Composable
private fun FavoriteSwipeBackground(shape: Shape, isFavorite: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // See DeleteSwipeBackground's comment on the added screenGutter inset.
            .padding(horizontal = JottiqSpacing.screenGutter)
            .testTag(FAVORITE_SWIPE_BACKGROUND_TAG)
            .clip(shape)
            .background(SwipeActionColors.Favorite)
            .padding(start = JottiqSpacing.l),
        contentAlignment = Alignment.CenterStart,
    ) {
        AppIcon(
            AppIcons.Star,
            contentDescription = stringResource(R.string.row_swipe_favorite_action),
            // Previews the state the swipe is about to produce, not the current one.
            filled = !isFavorite,
            tint = SwipeActionColors.OnAction,
        )
    }
}
