package me.hufman.androidautoidrive.carapp.navigation

import android.content.Context
import android.location.Address
import me.hufman.androidautoidrive.navigation.ExternalNavigationLauncher
import me.hufman.androidautoidrive.navigation.ExternalNavigationLauncher.NavigationApp

/**
 * A [NavigationTrigger] implementation that launches external navigation apps.
 *
 * This class makes it easy to integrate third‑party navigation applications
 * like Google Maps or Waze into the AAIDrive navigation workflow.  When
 * [triggerNavigation] is invoked the selected navigation app is opened and
 * pointed at the given destination.  Existing navigation triggers in the
 * project send messages to the car's built‑in navigation system or the
 * custom map.  By contrast, this implementation hands off responsibility
 * entirely to the external app specified during construction.
 *
 * @param context a [Context] used to launch external activities.
 * @param app which external navigation application should be launched.
 */
class NavigationTriggerExternal(
    private val context: Context,
    private val app: NavigationApp
) : NavigationTrigger {
    override fun triggerNavigation(destination: Address) {
        ExternalNavigationLauncher.launchNavigation(context, destination, app)
    }
}
