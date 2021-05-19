package one.mixin.android.ui.conversation.location

import android.content.Context
import android.os.Bundle
import androidx.core.view.isVisible
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import one.mixin.android.BuildConfig
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.extension.isGooglePlayServicesAvailable
import one.mixin.android.extension.isNightMode
import one.mixin.android.util.language.Lingver
import one.mixin.android.vo.foursquare.Venue

class MixinMapView(
    private val context: Context,
    private val googleMapView: MapView,
    private val mapboxView: com.mapbox.mapboxsdk.maps.MapView?
) {
    private val useMapbox = useMapbox()

    init {
        googleMapView.isVisible = !useMapbox
        mapboxView?.isVisible = useMapbox
    }

    var googleMap: GoogleMap? = null
    var mapboxMap: MapboxMap? = null

    @Suppress("DEPRECATION")
    fun addMarker(latLng: MixinLatLng) {
        if (useMapbox) {
            mapboxMap?.addMarker(
                com.mapbox.mapboxsdk.annotations.MarkerOptions().position(
                    com.mapbox.mapboxsdk.geometry.LatLng(latLng.latitude, latLng.longitude)
                ).icon(IconFactory.getInstance(context).fromResource(R.drawable.ic_location_search_maker))
            )
        } else {
            googleMap?.addMarker(
                MarkerOptions().position(
                    LatLng(latLng.latitude, latLng.longitude)
                ).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_search_maker))
            )
        }
    }

    @Suppress("DEPRECATION")
    fun addMarker(index: Int, venue: Venue) {
        if (useMapbox) {
            mapboxMap?.addMarker(
                com.mapbox.mapboxsdk.annotations.MarkerOptions().position(
                    com.mapbox.mapboxsdk.geometry.LatLng(venue.location.lat, venue.location.lng)
                ).icon(IconFactory.getInstance(context).fromResource(R.drawable.ic_location_search_maker))
            )
        } else {
            googleMap?.addMarker(
                MarkerOptions().zIndex(index.toFloat()).position(
                    LatLng(
                        venue.location.lat,
                        venue.location.lng
                    )
                ).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_search_maker))
            )
        }
    }

    fun moveBounds(bound: MixinLatLngBounds) {
        if (useMapbox) {
            mapboxMap?.animateCamera(com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLngBounds(bound.toMapbox(), 64.dp))
        } else {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bound.toGoogleMap(), 64.dp))
        }
    }

    fun moveCamera(latLng: MixinLatLng) {
        if (useMapbox) {
            mapboxMap?.animateCamera(com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLngZoom(latLng.toMapbox(), MAPBOX_ZOOM_LEVEL.toDouble()))
        } else {
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng.toGoogleMap(), GOOGLE_MAP_ZOOM_LEVEL))
        }
    }

    fun clear() {
        if (useMapbox) {
            @Suppress("DEPRECATION")
            mapboxMap?.clear()
        } else {
            googleMap?.clear()
        }
    }

    fun getMapboxStyle(): Style.Builder = Style.Builder().fromUri(
        if (context.isNightMode()) {
            MAPBOX_STYLE_NIGHT
        } else {
            MAPBOX_STYLE
        }
    )

    fun onCreate(savedInstanceState: Bundle?) {
        if (useMapbox) {
            mapboxView?.onCreate(savedInstanceState)
        } else {
            googleMapView.onCreate(savedInstanceState)
        }
    }

    fun onStart() {
        if (useMapbox) {
            mapboxView?.onStart()
        } else {
            googleMapView.onStart()
        }
    }

    fun onResume() {
        if (useMapbox) {
            mapboxView?.onResume()
        } else {
            googleMapView.onResume()
        }
    }

    fun onPause() {
        if (useMapbox) {
            mapboxView?.onPause()
        } else {
            googleMapView.onPause()
        }
    }

    fun onStop() {
        if (useMapbox) {
            mapboxView?.onStop()
        } else {
            googleMapView.onStop()
        }
    }

    fun onDestroy() {
        if (useMapbox) {
            mapboxView?.onDestroy()
        } else {
            googleMapView.onDestroy()
        }
    }

    fun onLowMemory() {
        if (useMapbox) {
            mapboxView?.onLowMemory()
        } else {
            googleMapView.onLowMemory()
        }
    }

    fun onSaveInstanceState(savedInstanceState: Bundle) {
        if (useMapbox) {
            mapboxView?.onSaveInstanceState(savedInstanceState)
        } else {
            googleMapView.onSaveInstanceState(savedInstanceState)
        }
    }

    companion object {
        private const val GOOGLE_MAP_ZOOM_LEVEL = 13f
        private const val MAPBOX_ZOOM_LEVEL = 12f

        const val MAPBOX_STYLE = "mapbox://styles/mixinmap/cknfubdn445p217o7er2mwy4s"
        const val MAPBOX_STYLE_NIGHT = "mapbox://styles/mixinmap/cknfudbop45rj17o6kadhrduz"
    }
}

fun useMapbox() = Lingver.getInstance().getLanguage() == "zh" &&
    !MixinApplication.appContext.isGooglePlayServicesAvailable() &&
    BuildConfig.MAPBOX_PUBLIC_TOKEN.isNotBlank()

data class MixinLatLng(val latitude: Double, val longitude: Double) {
    fun toGoogleMap() = LatLng(latitude, longitude)
    fun toMapbox() = com.mapbox.mapboxsdk.geometry.LatLng(latitude, longitude)
}

data class MixinLatLngBounds(val southwest: MixinLatLng, val northeast: MixinLatLng) {
    fun toGoogleMap() = LatLngBounds(LatLng(southwest.latitude, southwest.longitude), LatLng(northeast.latitude, northeast.longitude))
    fun toMapbox(): com.mapbox.mapboxsdk.geometry.LatLngBounds = com.mapbox.mapboxsdk.geometry.LatLngBounds.from(
        northeast.latitude, northeast.longitude, southwest.latitude, southwest.longitude
    )
}
