package com.celestial.spire.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.celestial.spire.ui.screens.*

sealed class Screen(val route: String) {
    object Splash          : Screen("splash")
    object Pairing         : Screen("pairing")
    object WallpaperSetup  : Screen("wallpaper_setup")
    object Drawing         : Screen("drawing")
    object TicTacToe       : Screen("tictactoe")
    object Settings        : Screen("settings")
}

@Composable
fun DoodluNavGraph(
    navController: NavHostController,
    startDestination: String,
    onSetWallpaper: () -> Unit
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
        enterTransition  = {
            fadeIn(tween(300)) + slideInVertically(tween(300)) { (it * 0.04f).toInt() }
        },
        exitTransition   = { fadeOut(tween(220)) },
        popEnterTransition  = { fadeIn(tween(280)) },
        popExitTransition   = {
            fadeOut(tween(220)) + slideOutVertically(tween(280)) { (it * 0.04f).toInt() }
        }
    ) {

        // ── Splash ──────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashDone = {
                    // Use the passed startDestination to determine real target,
                    // but always clear the splash from back-stack
                    val dest = startDestination
                        .takeIf { it != Screen.Splash.route }
                        ?: Screen.Pairing.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Pairing ─────────────────────────────────────────────────────
        composable(Screen.Pairing.route) {
            PairingScreen(
                onPaired = { showSetup ->
                    val dest = if (showSetup) Screen.WallpaperSetup.route
                               else            Screen.Drawing.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Pairing.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Wallpaper Setup (one-time onboarding) ───────────────────────
        composable(Screen.WallpaperSetup.route) {
            WallpaperSetupScreen(
                onContinue = {
                    navController.navigate(Screen.Drawing.route) {
                        popUpTo(Screen.WallpaperSetup.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Drawing ─────────────────────────────────────────────────────
        composable(Screen.Drawing.route) {
            DrawingScreen(
                onNavigateToTicTacToe = {
                    navController.navigate(Screen.TicTacToe.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onSetWallpaper = onSetWallpaper,
                onKicked = {
                    navController.navigate(Screen.Pairing.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Tic-Tac-Toe ─────────────────────────────────────────────────
        composable(Screen.TicTacToe.route) {
            TicTacToeScreen(
                onBackToDrawing = {
                    navController.popBackStack()
                }
            )
        }

        // ── Settings ────────────────────────────────────────────────────
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
