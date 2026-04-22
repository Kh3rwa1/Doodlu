package com.celestial.spire

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.celestial.spire.data.PreferencesManager
import com.celestial.spire.sync.ConnectionState
import com.celestial.spire.sync.SyncManager
import com.celestial.spire.ui.navigation.DoodluNavGraph
import com.celestial.spire.ui.navigation.Screen
import com.celestial.spire.ui.theme.DoodluTheme
import com.celestial.spire.wallpaper.DoodluWallpaperService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private lateinit var prefsManager: PreferencesManager
    // The real post-splash destination (drawing or pairing)
    private lateinit var postSplashDestination: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full edge-to-edge — let Compose handle insets
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        prefsManager = PreferencesManager(this)

        // Determine where to go after splash
        postSplashDestination = runBlocking {
            val roomId = prefsManager.roomId.first()
            val userId = prefsManager.userId.first()
            if (!roomId.isNullOrEmpty() && !userId.isNullOrEmpty()) {
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
                    navController      = navController,
                    // Always show splash first; splash navigates to postSplashDestination
                    startDestination   = Screen.Splash.route,
                    onSetWallpaper     = { launchWallpaperPicker() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SyncManager.registerClient()

        val roomId = runBlocking { prefsManager.roomId.first() }
        val userId = runBlocking { prefsManager.userId.first() }
        if (!roomId.isNullOrEmpty() && !userId.isNullOrEmpty()) {
            if (SyncManager.connectionState.value == ConnectionState.DISCONNECTED) {
                SyncManager.connect(roomId, userId)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        SyncManager.unregisterClient()
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
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }
}
