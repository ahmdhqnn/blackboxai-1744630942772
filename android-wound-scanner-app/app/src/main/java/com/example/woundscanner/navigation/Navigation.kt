package com.example.woundscanner.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.woundscanner.ui.screens.*

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object History : Screen("history")
    object Analysis : Screen("analysis/{imageUri}") {
        fun createRoute(imageUri: String) = "analysis/$imageUri"
    }
    object Camera : Screen("camera")

    companion object {
        const val IMAGE_URI_ARG = "imageUri"
    }
}

@Composable
fun WoundScannerNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToAnalysis = { imageUri ->
                    if (imageUri.isEmpty()) {
                        navController.navigate(Screen.Camera.route)
                    } else {
                        navController.navigate(Screen.Analysis.createRoute(imageUri))
                    }
                }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateUp = {
                    navController.navigateUp()
                },
                onWoundSelected = { imageUri ->
                    navController.navigate(Screen.Analysis.createRoute(imageUri))
                }
            )
        }

        composable(
            route = Screen.Analysis.route,
            arguments = listOf(
                navArgument(Screen.IMAGE_URI_ARG) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val imageUri = backStackEntry.arguments?.getString(Screen.IMAGE_URI_ARG) ?: ""
            AnalysisResultScreen(
                imageUri = imageUri,
                onNavigateUp = {
                    navController.navigateUp()
                },
                onSaveComplete = {
                    // Pop back to Home screen
                    navController.popBackStack(Screen.Home.route, false)
                }
            )
        }

        composable(Screen.Camera.route) {
            CameraScreen(
                onImageCaptured = { imageUri ->
                    navController.navigate(Screen.Analysis.createRoute(imageUri)) {
                        // Remove Camera screen from back stack
                        popUpTo(Screen.Camera.route) { inclusive = true }
                    }
                },
                onNavigateUp = {
                    navController.navigateUp()
                }
            )
        }
    }
}

// Navigation actions
class NavigationActions(private val navController: NavHostController) {
    val navigateToHome: () -> Unit = {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) { inclusive = true }
        }
    }

    val navigateToHistory: () -> Unit = {
        navController.navigate(Screen.History.route)
    }

    val navigateToCamera: () -> Unit = {
        navController.navigate(Screen.Camera.route)
    }

    fun navigateToAnalysis(imageUri: String) {
        navController.navigate(Screen.Analysis.createRoute(imageUri))
    }

    val navigateUp: () -> Unit = {
        navController.navigateUp()
    }
}

// Navigation state
sealed class NavigationState {
    object Idle : NavigationState()
    data class NavigateToAnalysis(val imageUri: String) : NavigationState()
    object NavigateToCamera : NavigationState()
    object NavigateToHistory : NavigationState()
    object NavigateUp : NavigationState()
}

// Navigation effects
sealed class NavigationEffect {
    object NavigateBack : NavigationEffect()
    data class ShowSnackbar(val message: String) : NavigationEffect()
    data class ShowDialog(
        val title: String,
        val message: String,
        val onConfirm: () -> Unit,
        val onDismiss: () -> Unit
    ) : NavigationEffect()
}

// Navigation arguments
object NavArgs {
    const val IMAGE_URI = "imageUri"
    const val WOUND_ID = "woundId"
}

// Deep link handling
object DeepLinks {
    const val SCHEME = "woundscanner"
    const val HOST = "app"
    
    // Deep link patterns
    const val ANALYSIS = "$SCHEME://$HOST/analysis"
    const val HISTORY = "$SCHEME://$HOST/history"
    const val WOUND_DETAILS = "$SCHEME://$HOST/wound"
}

// Navigation extensions
fun NavHostController.navigateAndPopUp(
    route: String,
    popUpRoute: String? = null,
    inclusive: Boolean = false
) {
    if (popUpRoute == null) {
        navigate(route)
    } else {
        navigate(route) {
            popUpTo(popUpRoute) { this.inclusive = inclusive }
        }
    }
}

fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

fun NavHostController.clearBackStackAndNavigate(route: String) {
    navigate(route) {
        popUpTo(0) { inclusive = true }
    }
}
