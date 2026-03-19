package com.doodlu.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "doodlu_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val ROOM_ID = stringPreferencesKey("room_id")
        val USER_ID = stringPreferencesKey("user_id")
        val SYMBOL = stringPreferencesKey("symbol")
        val AUTO_WALLPAPER = booleanPreferencesKey("auto_wallpaper")
        val WALLPAPER_TARGET = stringPreferencesKey("wallpaper_target") // "lock" or "both"
    }

    val roomId: Flow<String?> = context.dataStore.data.map { it[ROOM_ID] }
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    val symbol: Flow<String?> = context.dataStore.data.map { it[SYMBOL] }
    val autoWallpaper: Flow<Boolean> = context.dataStore.data.map { it[AUTO_WALLPAPER] ?: false }
    val wallpaperTarget: Flow<String> = context.dataStore.data.map { it[WALLPAPER_TARGET] ?: "lock" }

    suspend fun saveRoom(roomId: String, userId: String, symbol: String) {
        context.dataStore.edit { prefs ->
            prefs[ROOM_ID] = roomId
            prefs[USER_ID] = userId
            prefs[SYMBOL] = symbol
        }
    }

    suspend fun clearRoom() {
        context.dataStore.edit { prefs ->
            prefs.remove(ROOM_ID)
            prefs.remove(USER_ID)
            prefs.remove(SYMBOL)
        }
    }

    suspend fun setAutoWallpaper(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_WALLPAPER] = enabled }
    }

    suspend fun setWallpaperTarget(target: String) {
        context.dataStore.edit { it[WALLPAPER_TARGET] = target }
    }
}
