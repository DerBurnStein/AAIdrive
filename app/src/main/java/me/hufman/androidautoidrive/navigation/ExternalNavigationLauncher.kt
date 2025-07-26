package me.hufman.androidautoidrive.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Address
import android.net.Uri

/**
 * A helper class to launch external navigation applications such as
 * Google Maps and Waze from within the AAIDrive environment.  Many BMW iDrive
 * users prefer the familiar interface of their favourite mapping app.  This
 * launcher builds Intents for the supported navigation apps and includes
 * fall‑backs to open the appropriate Play Store listing if the app isn't
 * installed.  When integrating this class into your project you can decide
 * which app to launch based on user preferences.
 */
object ExternalNavigationLauncher {
    /**
     * Supported navigation applications.  Use [GOOGLE_MAPS] to launch the
     * official Google Maps app or [WAZE] to launch Waze.  Additional apps
     * can be added here in the future.
     */
    enum class NavigationApp {
        GOOGLE_MAPS,
        WAZE
    }

    /**
     * Launches turn‑by‑turn navigation in the selected navigation app.  The
     * destination is provided as an [Address] so you can pass in results
     * returned by Android's geocoding APIs.  If the requested app is not
     * installed the user is taken to the corresponding Play Store page.
     *
     * Under the hood this method builds an implicit [Intent] using
     * documented navigation URI schemes.  For Google Maps the URI
     * `google.navigation:q=latitude,longitude` is used.  Google Maps
     * interprets the `q` parameter as the navigation end point and will
     * initiate navigation automatically, as described in the Android
     * Developers documentation【825356325138157†L996-L1039】.  For Waze the
     * launcher builds a deep link of the form
     * `https://waze.com/ul?ll=latitude,longitude&navigate=yes` which
     * instructs Waze to begin navigation immediately【533818015611783†L52-L107】.
     *
     * @param context A [Context] used to start the external activity.
     * @param destination The [Address] to navigate to.  The latitude and
     * longitude fields must be populated.
     * @param app The desired navigation app to launch.
     */
    fun launchNavigation(context: Context, destination: Address, app: NavigationApp) {
        val lat = destination.latitude
        val lng = destination.longitude
        when (app) {
            NavigationApp.GOOGLE_MAPS -> {
                // Construct the Google Maps navigation URI.  The `q` parameter accepts
                // either a latitude/longitude pair or a plain address.  Here we use
                // coordinates since they are unambiguous【825356325138157†L996-L1039】.
                val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                    // Restrict the intent to the Google Maps package so that other apps
                    // don't intercept it and to avoid showing a chooser dialog.
                    setPackage("com.google.android.apps.maps")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(mapIntent)
                } catch (e: ActivityNotFoundException) {
                    // If Google Maps isn't installed, send the user to its Play Store page.
                    val playIntent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=com.google.android.apps.maps")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(playIntent)
                }
            }
            NavigationApp.WAZE -> {
                // Build the Waze deep link.  The `ll` parameter sets the
                // destination coordinates and `navigate=yes` instructs Waze to
                // immediately start navigation【533818015611783†L52-L107】.
                val wazeUrl = "https://waze.com/ul?ll=${lat},${lng}&navigate=yes"
                val wazeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(wazeUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(wazeIntent)
                } catch (e: ActivityNotFoundException) {
                    // If Waze isn't installed, open its Play Store listing【533818015611783†L181-L189】.
                    val playIntent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=com.waze")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(playIntent)
                }
            }
        }
    }
}
