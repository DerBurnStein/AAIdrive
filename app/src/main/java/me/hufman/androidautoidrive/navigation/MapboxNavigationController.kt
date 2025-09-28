package me.hufman.androidautoidrive.navigation

import android.content.Context
import android.content.Intent

/**
 * A helper object to launch the built‑in Mapbox navigation experience.
 *
 * When the user selects a destination on their phone, call
 * [startNavigation] with the application context and a human‑readable
 * destination name or address.  This will launch
 * [MapboxNavigationActivity] and display the route on the car’s screen
 * via the BMW Apps UI.
 */
object MapboxNavigationController {
    /**
     * Start navigation to the given destination name.  The destination
     * string will be geocoded and routed via Mapbox APIs inside the
     * [MapboxNavigationActivity].  Because this is launched from a
     * non‑activity context (e.g. a Service or BroadcastReceiver), the
     * `FLAG_ACTIVITY_NEW_TASK` flag is automatically set.
     *
     * @param context any valid context, typically the application context
     * @param destinationName a human‑readable address or place name
     */
    fun startNavigation(context: Context, destinationName: String) {
        val intent = Intent(context, MapboxNavigationActivity::class.java).apply {
            putExtra(MapboxNavigationActivity.EXTRA_DESTINATION_NAME, destinationName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
