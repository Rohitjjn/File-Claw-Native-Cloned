package com.example.feature.navigation

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.SettingEntity
import com.example.domain.preview.FileContentState
import com.example.feature.browser.AllFilesScreen
import com.example.feature.editor.EditorScreen
import com.example.feature.home.HomeScreen
import com.example.feature.home.SplashScreen
import com.example.feature.preview.FilePreviewScreen
import com.example.feature.search.SearchScreen
import com.example.feature.settings.SettingsScreen
import com.example.platform.shortcuts.FileClawShortcuts
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Owns app routes, shortcut navigation, and file-open navigation events. */
@Composable
fun AppNavigation(
    activity: ComponentActivity,
    viewModel: MainViewModel,
    settings: SettingEntity,
    pendingShortcutIntent: Intent?
) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        FileClawShortcuts.register(activity)
    }

    LaunchedEffect(pendingShortcutIntent) {
        pendingShortcutIntent?.let { shortcutIntent ->
            when (shortcutIntent.action) {
                FileClawShortcuts.ACTION_LAST_OPENED -> {
                    try {
                        val lastFile = withContext(Dispatchers.IO) { viewModel.getMostRecentFile() }
                        if (lastFile != null) {
                            viewModel.openFile(lastFile)
                            if (lastFile.extension.lowercase() == "txt" && settings.defaultToEditOnOpen) {
                                navController.navigate("editor") { launchSingleTop = true }
                            } else {
                                navController.navigate("preview") { launchSingleTop = true }
                            }
                        } else {
                            Toast.makeText(activity.applicationContext, "No history found", Toast.LENGTH_SHORT).show()
                        }
                    } catch (error: Exception) {
                        error.printStackTrace()
                    }
                }
                FileClawShortcuts.ACTION_HISTORY -> navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
                FileClawShortcuts.ACTION_SETTINGS -> navController.navigate("settings") {
                    launchSingleTop = true
                }
            }
        }
    }

    val navigateToPreviewWithEditOnOpen: () -> Unit = {
        val fileState = viewModel.currentFileState.value
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentRoute != "preview" && currentRoute != "editor") {
            if (fileState.opensInEditor() && settings.defaultToEditOnOpen) {
                navController.navigate("editor") { launchSingleTop = true }
            } else {
                navController.navigate("preview") { launchSingleTop = true }
            }
        }
    }

    LaunchedEffect(viewModel, settings.defaultToEditOnOpen) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is MainViewModel.NavigationEvent.NavigateToPreview -> {
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    if (currentRoute != "preview" && currentRoute != "editor") {
                        if (event.fileState.opensInEditor() && settings.defaultToEditOnOpen) {
                            navController.navigate("editor") { launchSingleTop = true }
                        } else {
                            navController.navigate("preview") { launchSingleTop = true }
                        }
                    } else if (currentRoute == "preview" && event.fileState.opensInEditor() && settings.defaultToEditOnOpen) {
                        navController.navigate("editor") {
                            popUpTo("preview") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
                is MainViewModel.NavigationEvent.ShowError ->
                    Toast.makeText(activity, event.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    NavigationGraph(
        activity = activity,
        navController = navController,
        viewModel = viewModel,
        navigateToPreview = navigateToPreviewWithEditOnOpen
    )
}

@Composable
private fun NavigationGraph(
    activity: ComponentActivity,
    navController: NavHostController,
    viewModel: MainViewModel,
    navigateToPreview: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            scaleIn(initialScale = 0.97f, animationSpec = tween(80, easing = FastOutSlowInEasing)) +
                fadeIn(animationSpec = tween(80))
        },
        exitTransition = {
            scaleOut(targetScale = 1.03f, animationSpec = tween(60, easing = FastOutSlowInEasing)) +
                fadeOut(animationSpec = tween(60))
        },
        popEnterTransition = {
            scaleIn(initialScale = 1.03f, animationSpec = tween(80, easing = FastOutSlowInEasing)) +
                fadeIn(animationSpec = tween(80))
        },
        popExitTransition = {
            scaleOut(targetScale = 0.97f, animationSpec = tween(60, easing = FastOutSlowInEasing)) +
                fadeOut(animationSpec = tween(60))
        }
    ) {
        composable("splash") {
            SplashScreen(onNavigateToHome = {
                navController.navigate("home") { popUpTo("splash") { inclusive = true } }
            })
        }
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToPreview = navigateToPreview,
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToAllFiles = { navController.navigate("all_files") },
                onNavigateToSearch = { navController.navigate("search") }
            )
        }
        composable("all_files") {
            AllFilesScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToPreview = navigateToPreview
            )
        }
        composable("search") {
            SearchScreen(
                viewModel = viewModel,
                onBackClick = {
                    viewModel.updateSearchQuery("")
                    navController.popBackStack()
                }
            )
        }
        composable("preview") {
            FilePreviewScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToEditor = { navController.navigate("editor") }
            )
        }
        composable("editor") {
            EditorScreen(viewModel = viewModel, onBackClick = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(viewModel = viewModel, onBackClick = { navController.popBackStack() })
        }
    }
}

/** Only complete, writable text states are eligible for the direct-to-editor default. */
private fun FileContentState.opensInEditor(): Boolean =
    this is FileContentState.TextSuccess && editable
