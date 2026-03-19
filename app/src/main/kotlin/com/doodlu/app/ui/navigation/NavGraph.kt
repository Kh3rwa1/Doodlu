package com.doodlu.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.doodlu.app.ui.screens.*

sealed class Screen(val route: String) {
    object Pairing : Screen("pairing")
    object Drawing : Screen("drawing")
    object TicTacToe : Screen("tictactoe")
    object Settings : Screen("settings")
}

@Composable
fun DoodluNavGraph(
    navController: NavHostController,
    startDestination: String,
    onSetWallpaper: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Pairing.route) {
            PairingScreen(
                onPaired = {
                    navController.navigate(Screen.Drawing.route) {
                        popUpTo(Screen.Pairing.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Drawing.route) {
            DrawingScreen(
                onNavigateToTicTacToe = {
                    navController.navigate(Screen.TicTacToe.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onSetWallpaper = onSetWallpaper
            )
        }

        composable(Screen.TicTacToe.route) {
            TicTacToeScreen(
                onBackToDrawing = {
                    navController.navigate(Screen.Drawing.route) {
                        popUpTo(Screen.TicTacToe.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLeaveRoom = {
                    navController.navigate(Screen.Pairing.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
