package one.mixin.android.ui.conversation.location

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.core.view.isVisible
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
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
    mapboxView: com.mapbox.maps.MapView?
) {
    private val p = 64.dp.toDouble()
    private val mapboxBoundsEdgeInsets = EdgeInsets(p, p, p, p)

    private val useMapbox = useMapbox()
    private val annotationApi = mapboxView?.annotations
    var pointAnnotationManager: PointAnnotationManager? = null

    init {
        googleMapView.isVisible = !useMapbox
        mapboxView?.isVisible = useMapbox

        if (useMapbox) {
            pointAnnotationManager = annotationApi?.createPointAnnotationManager()
        }
    }

    var googleMap: GoogleMap? = null
    var mapboxMap: MapboxMap? = null

    fun addMarker(latLng: MixinLatLng) {
        if (useMapbox) {
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(latLng.toMapbox())
                .withIconImage(BitmapFactory.decodeResource(context.resources, R.drawable.ic_location_search_maker))
            pointAnnotationManager?.create(pointAnnotationOptions)
        } else {
            googleMap?.addMarker(
                MarkerOptions()
                    .position(latLng.toGoogleMap())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_search_maker))
            )
        }
    }

    fun addMarker(index: Int, venue: Venue) {
        if (useMapbox) {
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(venue.location.lng, venue.location.lat))
                .withIconImage(BitmapFactory.decodeResource(context.resources, R.drawable.ic_location_search_maker))
            pointAnnotationManager?.create(pointAnnotationOptions)
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
            mapboxMap?.cameraForCoordinateBounds(bound.toMapbox(), mapboxBoundsEdgeInsets)?.let { mapboxMap?.setCamera(it) }
        } else {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bound.toGoogleMap(), 64.dp))
        }
    }

    fun moveCamera(latLng: MixinLatLng) {
        if (useMapbox) {
            mapboxMap?.flyTo(CameraOptions.Builder().center(latLng.toMapbox()).zoom(MAPBOX_ZOOM_LEVEL.toDouble()).build())
        } else {
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng.toGoogleMap(), GOOGLE_MAP_ZOOM_LEVEL))
        }
    }

    fun clear() {
        if (useMapbox) {
            pointAnnotationManager?.deleteAll()
        } else {
            googleMap?.clear()
        }
    }

    fun getMapboxStyle() =
        if (context.isNightMode()) {
            MAPBOX_STYLE_NIGHT
        } else {
            MAPBOX_STYLE
        }

    fun onCreate(savedInstanceState: Bundle?) {
        if (!useMapbox) {
            googleMapView.onCreate(savedInstanceState)
        }
    }

    fun onStart() {
        if (!useMapbox) {
            googleMapView.onStart()
        }
    }

    fun onResume() {
        if (!useMapbox) {
            googleMapView.onResume()
        }
    }

    fun onPause() {
        if (!useMapbox) {
            googleMapView.onPause()
        }
    }

    fun onStop() {
        if (!useMapbox) {
            googleMapView.onStart()
        }
    }

    fun onDestroy() {
        if (!useMapbox) {
            googleMapView.onDestroy()
        }
    }

    fun onLowMemory() {
        if (!useMapbox) {
            googleMapView.onLowMemory()
        }
    }

    fun onSaveInstanceState(savedInstanceState: Bundle) {
        if (!useMapbox) {
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
    fun toMapbox(): Point = Point.fromLngLat(longitude, latitude)
}

data class MixinLatLngBounds(val southwest: MixinLatLng, val northeast: MixinLatLng) {
    fun toGoogleMap() = LatLngBounds(LatLng(southwest.latitude, southwest.longitude), LatLng(northeast.latitude, northeast.longitude))
    fun toMapbox() = CoordinateBounds(Point.fromLngLat(southwest.longitude, southwest.latitude), Point.fromLngLat(northeast.longitude, northeast.latitude))
}
