package com.example.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun OmniApp(viewModel: ReaderViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController, 
        startDestination = "library",
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
        exitTransition = { fadeOut() + slideOutHorizontally(targetOffsetX = { -it }) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
        popExitTransition = { fadeOut() + slideOutHorizontally(targetOffsetX = { it }) }
    ) {
        composable("library") {
            LibraryScreen(
                viewModel = viewModel,
                onNavigateToReader = { id -> navController.navigate("reader/$id") },
                onNavigateToExplore = { navController.navigate("explore") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("explore") {
            ExploreScreen(
                viewModel = viewModel,
                onNavigateToLibrary = { navController.navigate("library") { popUpTo(0) } },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("reader/{bookId}") { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId")?.toIntOrNull()
            if (bookId != null) {
                ReaderScreen(
                    bookId = bookId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
