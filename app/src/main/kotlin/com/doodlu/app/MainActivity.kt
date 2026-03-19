package com.doodlu.app

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.doodlu.app.data.PreferencesManager
import com.doodlu.app.sync.SyncManager
import com.doodlu.app.ui.navigation.DoodluNavGraph
import com.doodlu.app.ui.navigation.Screen
import com.doodlu.app.ui.theme.DoodluTheme
import com.doodlu.app.wallpaper.DoodluWallpaperService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if user already has a room
        val prefsManager = PreferencesManager(this)
        val startDestination = runBlocking {
            val roomId = prefsManager.roomId.first()
            val userId = prefsManager.userId.first()
            if (!roomId.isNullOrEmpty() && !userId.isNullOrEmpty()) {
                // Reconnect existing room
                SyncManager.connect(roomId, userId)
                Screen.Drawing.route
            } else {
                Screen.Pairing.route
            }
        }

        setContent {
            DoodluTheme {
                val navController = rememberNavController()
                DoodluNavGraph(
                    navController = navController,
                    startDestination = startDestination,
                    onSetWallpaper = { launchWallpaperPicker() }
                )
            }
        }
    }

    private fun launchWallpaperPicker() {
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, DoodluWallpaperService::class.java)
                )
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to generic wallpaper picker
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }

    override fun onResume() {
        super.onResume()
        // Reconnect if disconnected
        val prefsManager = PreferencesManager(this)
        runBlocking {
            val roomId = prefsManager.roomId.first()
            val userId = prefsManager.userId.first()
            if (!roomId.isNullOrEmpty() && !userId.isNullOrEmpty()) {
                if (SyncManager.connectionState.value != com.doodlu.app.sync.ConnectionState.CONNECTED) {
                    SyncManager.connect(roomId, userId)
                }
            }
        }
    }
}
