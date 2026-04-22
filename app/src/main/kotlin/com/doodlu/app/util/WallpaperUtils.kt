package com.celestial.spire.util

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.celestial.spire.wallpaper.DoodluWallpaperService

/**
 * Returns true if DoodluWallpaperService is currently the active live wallpaper
 * on this device (checks either home or lock screen wallpaper info).
 */
fun isDoodluActiveWallpaper(context: Context): Boolean {
    return try {
        val wm   = WallpaperManager.getInstance(context)
        val info = wm.wallpaperInfo   // returns null for static wallpapers
        info?.component?.className == DoodluWallpaperService::class.java.name
    } catch (e: Exception) {
        false
    }
}

/**
 * Build the Intent that opens the Android live-wallpaper system picker,
 * pre-selecting Doodlu. Falls back to the generic chooser if unavailable.
 */
fun buildWallpaperPickerIntent(context: Context): Intent {
    return try {
        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(context, DoodluWallpaperService::class.java)
            )
        }
    } catch (e: Exception) {
        Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
    }
}
