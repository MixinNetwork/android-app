package one.mixin.android.ui.conversation.location

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.core.view.isVisible
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.extension.isGooglePlayServicesAvailable
import one.mixin.android.vo.foursquare.Venue
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class MixinMapView(
    private val context: Context,
    private val googleMapView: MapView,
    private val osmMapView: org.osmdroid.views.MapView?
) {
    private val p = 64.dp.toDouble()
    private val useOsm = useOpenStreetMap()

    init {
        googleMapView.isVisible = !useOsm
        osmMapView?.isVisible = useOsm

        if (useOsm) {
            Configuration.getInstance().load(context, context.getSharedPreferences("osm_prefs", Context.MODE_PRIVATE))
            osmMapView?.setTileSource(TileSourceFactory.MAPNIK)
            osmMapView?.setMultiTouchControls(true)
        }
    }

    var googleMap: GoogleMap? = null
    var osmMapController: IMapController? = null

    fun addMarker(latLng: MixinLatLng) {
        if (useOsm) {
            val marker = Marker(osmMapView)
            marker.position = GeoPoint(latLng.latitude, latLng.longitude)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = context.resources.getDrawable(R.drawable.ic_location_search_maker)
            osmMapView?.overlays?.add(marker)
            osmMapView?.invalidate()
        } else {
            googleMap?.addMarker(
                MarkerOptions()
                    .position(latLng.toGoogleMap())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_search_maker))
            )
        }
    }

    fun addMarker(index: Int, venue: Venue) {
        if (useOsm) {
            val marker = Marker(osmMapView)
            marker.position = GeoPoint(venue.location.lat, venue.location.lng)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = context.resources.getDrawable(R.drawable.ic_location_search_maker)
            osmMapView?.overlays?.add(marker)
            osmMapView?.invalidate()
        } else {
            googleMap?.addMarker(
                MarkerOptions().zIndex(index.toFloat()).position(
                    LatLng(venue.location.lat, venue.location.lng)
                ).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_search_maker))
            )
        }
    }

    fun moveBounds(bound: MixinLatLngBounds) {
        if (useOsm) {
            osmMapView?.zoomToBoundingBox(
                org.osmdroid.util.BoundingBox(
                    bound.northeast.latitude,
                    bound.northeast.longitude,
                    bound.southwest.latitude,
                    bound.southwest.longitude
                ),
                true,
                p.toInt()
            )
        } else {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bound.toGoogleMap(), 64.dp))
        }
    }

    fun moveCamera(latLng: MixinLatLng) {
        if (useOsm) {
            osmMapView?.controller?.apply {
                setZoom(OSM_ZOOM_LEVEL)
                setCenter(GeoPoint(latLng.latitude, latLng.longitude))
            }
        } else {
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng.toGoogleMap(), GOOGLE_MAP_ZOOM_LEVEL))
        }
    }

    fun clear() {
        if (useOsm) {
            osmMapView?.overlays?.clear()
            osmMapView?.invalidate()
        } else {
            googleMap?.clear()
        }
    }

    fun onCreate(savedInstanceState: Bundle?) {
        if (!useOsm) {
            googleMapView.onCreate(savedInstanceState)
        }
    }

    fun onStart() {
        if (!useOsm) {
            googleMapView.onStart()
        }
    }

    fun onResume() {
        if (!useOsm) {
            googleMapView.onResume()
        }
    }

    fun onPause() {
        if (!useOsm) {
            googleMapView.onPause()
        }
    }

    fun onStop() {
        if (!useOsm) {
            googleMapView.onStart()
        }
    }

    fun onDestroy() {
        if (!useOsm) {
            googleMapView.onDestroy()
        }
    }

    fun onLowMemory() {
        if (!useOsm) {
            googleMapView.onLowMemory()
        }
    }

    fun onSaveInstanceState(savedInstanceState: Bundle) {
        if (!useOsm) {
            googleMapView.onSaveInstanceState(savedInstanceState)
        }
    }

    companion object {
        private const val GOOGLE_MAP_ZOOM_LEVEL = 13f
        private const val OSM_ZOOM_LEVEL = 16f.toDouble()
    }
}

fun useOpenStreetMap() = !MixinApplication.appContext.isGooglePlayServicesAvailable()

data class MixinLatLng(val latitude: Double, val longitude: Double) {
    fun toGoogleMap() = LatLng(latitude, longitude)
}

data class MixinLatLngBounds(val southwest: MixinLatLng, val northeast: MixinLatLng) {
    fun toGoogleMap() = LatLngBounds(LatLng(southwest.latitude, southwest.longitude), LatLng(northeast.latitude, northeast.longitude))
}
