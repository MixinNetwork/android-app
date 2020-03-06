package one.mixin.android.ui.conversation.location

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import kotlinx.android.synthetic.main.activity_location.*
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.ui.common.BaseActivity
import timber.log.Timber

class LocationActivity : BaseActivity(), OnMapReadyCallback {

    // todo delete test position
    private val SYDNEY = LatLng(39.9967, 116.4805)
    private val ZOOM_LEVEL = 13f

    private var mapsInitialized = false
    private var onResumeCalled = false
    private var forceUpdate: CameraUpdate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)
        map_view.onCreate(savedInstanceState)
        MapsInitializer.initialize(MixinApplication.appContext)
        map_view.getMapAsync(this)
    }

    override fun onResume() {
        super.onResume()
        onResumeCalled = true
        if (mapsInitialized) {
            map_view.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        onResumeCalled = false
        if (mapsInitialized) {
            map_view.onPause()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (mapsInitialized) {
            map_view.onLowMemory()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mapsInitialized) {
            map_view.onDestroy()
        }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap ?: return

        if (isNightMode()) {
            val style = MapStyleOptions.loadRawResourceStyle(applicationContext, R.raw.mapstyle_night);
            googleMap.setMapStyle(style);
        }
        mapInit(googleMap)

        with(googleMap) {
            moveCamera(CameraUpdateFactory.newLatLngZoom(SYDNEY, ZOOM_LEVEL))
            // addMarker(MarkerOptions().position(SYDNEY))
        }
        mapsInitialized = true
        if (onResumeCalled) {
            map_view.onResume()
        }
    }

    fun mapInit(googleMap: GoogleMap) {
        try {
            googleMap.isMyLocationEnabled = true;
        } catch (e: Exception) {
            Timber.e(e)
        }
        googleMap.uiSettings?.isMyLocationButtonEnabled = false
        googleMap.uiSettings?.isZoomControlsEnabled = false
        googleMap.uiSettings?.isCompassEnabled = false
        googleMap.setOnCameraMoveStartedListener { reason ->
            Timber.d("started")
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                val cameraPosition = googleMap.cameraPosition
                forceUpdate = CameraUpdateFactory.newLatLngZoom(cameraPosition.target, cameraPosition.zoom)
            }
            markerAnimatorSet?.cancel()
            markerAnimatorSet = AnimatorSet()
            markerAnimatorSet?.playTogether(ObjectAnimator.ofFloat(marker, View.TRANSLATION_Y, -8.dp.toFloat()))
            markerAnimatorSet?.duration = 200
            markerAnimatorSet?.start()
        }
        googleMap.setOnCameraMoveListener {
            Timber.d("OnCameraMove")
        }

        googleMap.setOnCameraIdleListener {
            markerAnimatorSet?.cancel()
            markerAnimatorSet = AnimatorSet()
            markerAnimatorSet?.playTogether(ObjectAnimator.ofFloat(marker, View.TRANSLATION_Y, 0f))
            markerAnimatorSet?.duration = 200
            markerAnimatorSet?.start()
            googleMap.cameraPosition.target.let { lastLang ->
                Timber.d("${lastLang.latitude}")
                Timber.d("${lastLang.longitude}")
            }
        }

        googleMap.setOnCameraMoveCanceledListener {
            Timber.d("cancel")
        }
    }

    private var markerAnimatorSet: AnimatorSet? = null

    companion object {
        fun show(context: Context) {
            Intent(context, LocationActivity::class.java).run {
                context.startActivity(this)
            }
        }
    }
}
