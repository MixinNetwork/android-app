package one.mixin.android.ui.conversation.location

import android.os.Bundle
import androidx.core.view.isVisible
import com.amap.api.maps.AMap
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
import one.mixin.android.util.language.Lingver
import one.mixin.android.vo.foursquare.Venue

class MixinMapView(
    private val googleMapView: MapView,
    private val aMapView: com.amap.api.maps.TextureMapView,
) {
    private val useGoogleMap = useGoogleMap()

    init {
        googleMapView.isVisible = useGoogleMap
        aMapView.isVisible = !useGoogleMap
    }

    var googleMap: GoogleMap? = null
    var aMap: AMap? = null

    fun addMarker(latlng: MixinLatLng) {
        if (useGoogleMap) {
            googleMap?.addMarker(
                MarkerOptions().position(
                    LatLng(latlng.latitude, latlng.longitude)
                ).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_search_maker))
            )
        } else {
            aMap?.addMarker(
                com.amap.api.maps.model.MarkerOptions().position(
                    com.amap.api.maps.model.LatLng(latlng.latitude, latlng.longitude)
                ).icon(com.amap.api.maps.model.BitmapDescriptorFactory.fromResource(R.drawable.ic_location_search_maker))
            )
        }
    }

    fun addMarker(index: Int, venue: Venue) {
        if (useGoogleMap) {
            googleMap?.addMarker(
                MarkerOptions().zIndex(index.toFloat()).position(
                    LatLng(
                        venue.location.lat,
                        venue.location.lng
                    )
                ).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_search_maker))
            )
        } else {
            aMap?.addMarker(
                com.amap.api.maps.model.MarkerOptions().zIndex(index.toFloat()).position(
                    com.amap.api.maps.model.LatLng(
                        venue.location.lat,
                        venue.location.lng
                    )
                ).icon(com.amap.api.maps.model.BitmapDescriptorFactory.fromResource(R.drawable.ic_location_search_maker))
            )
        }
    }

    fun moveBounds(bound: MixinLatLngBounds) {
        if (useGoogleMap) {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bound.toGoogleMap(), 64.dp))
        } else {
            aMap?.animateCamera(com.amap.api.maps.CameraUpdateFactory.newLatLngBounds(bound.toAMap(), 64.dp))
        }
    }

    fun moveCamera(latlng: MixinLatLng) {
        if (useGoogleMap) {
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng.toGoogleMap(), ZOOM_LEVEL))
        } else {
            aMap?.moveCamera(com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(latlng.toAMap(), ZOOM_LEVEL))
        }
    }

    fun clear() {
        if (useGoogleMap) {
            googleMap?.clear()
        } else {
            aMap?.clear()
        }
    }

    fun onCreate(savedInstanceState: Bundle?) {
        if (useGoogleMap) {
            googleMapView.onCreate(savedInstanceState)
        } else {
            aMapView.onCreate(savedInstanceState)
        }
    }

    fun onResume() {
        if (useGoogleMap) {
            googleMapView.onResume()
        } else {
            aMapView.onResume()
        }
    }

    fun onPause() {
        if (useGoogleMap) {
            googleMapView.onPause()
        } else {
            aMapView.onPause()
        }
    }

    fun onDestroy() {
        if (useGoogleMap) {
            googleMapView.onDestroy()
        } else {
            aMapView.onDestroy()
        }
    }

    fun onLowMemory() {
        if (useGoogleMap) {
            googleMapView.onLowMemory()
        } else {
            aMapView.onLowMemory()
        }
    }

    fun onSaveInstanceState(savedInstanceState: Bundle) {
        if (useGoogleMap) {
            googleMapView.onSaveInstanceState(savedInstanceState)
        } else {
            aMapView.onSaveInstanceState(savedInstanceState)
        }
    }

    companion object {
        private const val ZOOM_LEVEL = 13f
    }
}

fun useGoogleMap() = Lingver.getInstance().getLanguage() != "zh" &&
    MixinApplication.appContext.isGooglePlayServicesAvailable()

data class MixinLatLng(val latitude: Double, val longitude: Double) {
    fun toGoogleMap() = LatLng(latitude, longitude)
    fun toAMap() = com.amap.api.maps.model.LatLng(latitude, longitude)
}

data class MixinLatLngBounds(val southwest: MixinLatLng, val northeast: MixinLatLng) {
    fun toGoogleMap() = LatLngBounds(LatLng(southwest.latitude, southwest.longitude), LatLng(northeast.latitude, northeast.longitude))
    fun toAMap() = com.amap.api.maps.model.LatLngBounds(com.amap.api.maps.model.LatLng(southwest.latitude, southwest.longitude), com.amap.api.maps.model.LatLng(northeast.latitude, northeast.longitude))
}
