package me.hufman.androidautoidrive.navigation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.common.logger.MapboxLogger
import com.mapbox.maps.MapView
import com.mapbox.maps.Style

/**
 * Activity responsible for displaying a Mapbox map and routing to a
 * destination. When launched via [MapboxNavigationController], this
 * activity geocodes the provided destination name, calculates a route
 * using Mapbox Directions, and renders the route on the map. The
 * activity runs in a standard Android context but renders via the BMW
 * Apps UI when connected to the head unit.
 *
 * Note: this is a skeleton implementation. You will need to
 * integrate Mapbox Navigation and Search SDKs to perform geocoding,
 * route calculation, and turn‑by‑turn guidance. See Mapbox
 * documentation for details on obtaining an access token and enabling
 * the required dependencies.
 */
class MapboxNavigationActivity : AppCompatActivity() {
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate a simple layout containing a MapView. You will need
        // to create the corresponding layout file in res/layout.
        mapView = MapView(this)
        setContentView(mapView)

        // Load a basic map style. In production you may wish to
        // customize the style or use the Navigation Day/Night styles.
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->
            MapboxLogger.d("Map style loaded")
            // TODO: After style is loaded, geocode the destination and
            // calculate the route.
            startRouting()
        }
    }

    /**
     * Kick off geocoding and route calculation using Mapbox SDKs. This
     * method should obtain the destination name from the intent,
     * convert it into coordinates via the Mapbox Search SDK, then use
     * Mapbox Navigation to calculate a route. Finally, draw the
     * resulting polyline on the map.
     */
    private fun startRouting() {
        val destinationName = intent.getStringExtra(EXTRA_DESTINATION_NAME) ?: return
        // TODO: Implement geocoding and route calculation.
        // Example steps (pseudo-code):
        // 1. Initialize PlaceAutocomplete or geocoding client with your
        //    Mapbox access token.
        // 2. Request a single geocoding result for destinationName.
        // 3. Use current location (via LocationComponent or fused
        //    location provider) as origin.
        // 4. Call Mapbox NavigationRoute.builder() to build a route.
        // 5. Draw the route line on the map and animate the camera.
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    companion object {
        const val EXTRA_DESTINATION_NAME = "destination_name"
    }
}
