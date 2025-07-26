package me.hufman.androidautoidrive.navigation

import android.content.Context
import android.preference.PreferenceManager

enum class ExternalNavigationApp {
    OFF,
    GOOGLE_MAPS,
    WAZE
}

object ExternalNavigationSettings {
    private const val PREF_KEY = "Nav_External_App"

    fun getPreferredNavigationApp(context: Context): ExternalNavigationApp {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return when (prefs.getString(PREF_KEY, ExternalNavigationApp.OFF.name) ?: ExternalNavigationApp.OFF.name) {
            ExternalNavigationApp.GOOGLE_MAPS.name -> ExternalNavigationApp.GOOGLE_MAPS
            ExternalNavigationApp.WAZE.name -> ExternalNavigationApp.WAZE
            else -> ExternalNavigationApp.OFF
        }
    }
}
