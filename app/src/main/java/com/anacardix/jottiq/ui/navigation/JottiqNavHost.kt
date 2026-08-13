package com.anacardix.jottiq.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.anacardix.jottiq.ui.folder.FolderScreen
import com.anacardix.jottiq.ui.home.HomeScreen
import com.anacardix.jottiq.ui.noteeditor.NoteEditorScreen
import com.anacardix.jottiq.ui.settings.SettingsScreen
import com.anacardix.jottiq.ui.trash.TrashScreen
import com.anacardix.jottiq.ui.unlockgate.UnlockGateScreen

/**
 * App-wide navigation graph. Move-to-folder and note deletion are both handled entirely inside
 * NoteEditorScreen via a bottom sheet / AlertDialog — neither navigates directly.
 */
@Composable
fun JottiqNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        // Fills the gap between the outgoing and incoming screens while both are mid-fade during
        // JottiqTransitions' slide+fade animation — without this the transparent AnimatedContent
        // container briefly exposes the activity window's background underneath.
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        enterTransition = JottiqTransitions.enter,
        exitTransition = JottiqTransitions.exit,
        popEnterTransition = JottiqTransitions.popEnter,
        popExitTransition = JottiqTransitions.popExit,
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onFolderClick = { folderId -> navController.navigate(FolderRoute(folderId)) },
                onNoteClick = { noteId -> navController.navigate(NoteRoute(noteId)) },
                onLockedFolderClick = { folderId, name ->
                    navController.navigate(UnlockGateRoute(folderId, name, isFolder = true))
                },
                onLockedNoteClick = { noteId, title ->
                    navController.navigate(UnlockGateRoute(noteId, title, isFolder = false))
                },
                onTrashClick = { navController.navigate(TrashRoute) },
                onSettingsClick = { navController.navigate(SettingsRoute) },
            )
        }
        composable<FolderRoute> {
            FolderScreen(
                onBackClick = { navController.popBackStack() },
                onFolderClick = { folderId -> navController.navigate(FolderRoute(folderId)) },
                onNoteClick = { noteId -> navController.navigate(NoteRoute(noteId)) },
                onLockedFolderClick = { folderId, name ->
                    navController.navigate(UnlockGateRoute(folderId, name, isFolder = true))
                },
                onLockedNoteClick = { noteId, title ->
                    navController.navigate(UnlockGateRoute(noteId, title, isFolder = false))
                },
            )
        }
        composable<NoteRoute> {
            NoteEditorScreen(onBackClick = { navController.popBackStack() })
        }
        composable<TrashRoute> {
            TrashScreen(onBackClick = { navController.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable<UnlockGateRoute> { backStackEntry ->
            val route: UnlockGateRoute = backStackEntry.toRoute()
            UnlockGateScreen(
                onBackClick = { navController.popBackStack() },
                onUnlocked = { targetId, isFolder ->
                    val destination = if (isFolder) {
                        FolderRoute(targetId)
                    } else {
                        NoteRoute(targetId)
                    }
                    navController.navigate(destination) {
                        popUpTo(route) { inclusive = true }
                    }
                },
            )
        }
    }
}
