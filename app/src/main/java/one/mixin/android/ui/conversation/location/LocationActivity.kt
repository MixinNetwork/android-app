package one.mixin.android.ui.conversation.location

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import javax.inject.Inject
import kotlinx.android.synthetic.main.activity_location.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.service.FoursquareService
import one.mixin.android.extension.REQUEST_LOCATION
import one.mixin.android.extension.dp
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.showKeyboard
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.vo.Location
import timber.log.Timber

class LocationActivity : BaseActivity(), OnMapReadyCallback {

    @Inject
    lateinit var foursquareService: FoursquareService

    // todo delete test position
    private val ZOOM_LEVEL = 13f
    private var currentPosition = LatLng(39.9967, 116.4805)

    private var mapsInitialized = false
    private var onResumeCalled = false
    private var forceUpdate: CameraUpdate? = null

    private val location: Location? by lazy {
        intent.getParcelableExtra<Location>(LOCATION)
    }

    private val adapter by lazy {
        LocationAdapter({
            setResult(Location(currentPosition.latitude, currentPosition.longitude))
        }, {
            setResult(it)
        })
    }

    private val locationSearchAdapter by lazy {
        LocationSearchAdapter {
            setResult(it)
        }
    }

    private val mLocationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: android.location.Location) {
            Timber.d("${location.latitude} ${location.longitude}")
            currentPosition = LatLng(location.latitude, location.longitude)
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        }

        override fun onProviderEnabled(provider: String) {
        }

        override fun onProviderDisabled(provider: String) {
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)
        val locationManager = getSystemService<LocationManager>()
        locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 100f, mLocationListener)
        map_view.onCreate(savedInstanceState)
        ic_back.setOnClickListener {
            if (search_va.displayedChild == 1) {
                search_va.showPrevious()
                search_et.hideKeyboard()
            } else {
                finish()
            }
        }
        marker.isVisible = location == null
        ic_search.isVisible = location == null
        ic_search.setOnClickListener {
            search_va.showNext()
            search_et.requestFocus()
            search_et.showKeyboard()
        }
        ic_close.setOnClickListener {
            search_va.showPrevious()
            search_et.hideKeyboard()
        }
        search_et.addTextChangedListener(textWatcher)
        MapsInitializer.initialize(MixinApplication.appContext)
        map_view.getMapAsync(this)
        recycler_view.isVisible = location == null
        if (location == null) {
            recycler_view.adapter = adapter
            search_recycler.adapter = locationSearchAdapter
        }
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
            val style = MapStyleOptions.loadRawResourceStyle(applicationContext, R.raw.mapstyle_night)
            googleMap.setMapStyle(style)
        }
        mapInit(googleMap)

        with(googleMap) {
            if (location != null) {

                addMarker(
                    MarkerOptions().position(
                        LatLng(location!!.latitude, location!!.longitude)
                    ).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map_pin))
                )
                moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location!!.latitude, location!!.longitude), ZOOM_LEVEL))
            } else {
                moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, ZOOM_LEVEL))
            }
        }
        mapsInitialized = true
        if (onResumeCalled) {
            map_view.onResume()
        }
    }

    private fun setResult(location: Location) {
        setResult(Activity.RESULT_OK, Intent().putExtra(LOCATION_NAME, location))
        finish()
    }

    fun mapInit(googleMap: GoogleMap) {
        try {
            googleMap.isMyLocationEnabled = true
        } catch (e: Exception) {
            Timber.e(e)
        }
        googleMap.uiSettings?.isMyLocationButtonEnabled = false
        googleMap.uiSettings?.isZoomControlsEnabled = false
        googleMap.uiSettings?.isCompassEnabled = false
        googleMap.setOnCameraMoveStartedListener { reason ->
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
        googleMap.setOnCameraMoveListener {}

        googleMap.setOnCameraIdleListener {
            markerAnimatorSet?.cancel()
            markerAnimatorSet = AnimatorSet()
            markerAnimatorSet?.playTogether(ObjectAnimator.ofFloat(marker, View.TRANSLATION_Y, 0f))
            markerAnimatorSet?.duration = 200
            markerAnimatorSet?.start()
            googleMap.cameraPosition.target.let { lastLang ->
                if (location == null) {
                    currentPosition = lastLang
                    search(lastLang)
                }
            }
        }

        googleMap.setOnCameraMoveCanceledListener {
            Timber.d("cancel")
        }
    }

    private var lastSearchJob: Job? = null
    fun search(latlng: LatLng) {
        if (lastSearchJob != null && lastSearchJob?.isActive == true) {
            lastSearchJob?.cancel()
        }
        lastSearchJob = lifecycleScope.launch {
            val result = try {
                foursquareService.searchVenues("${latlng.latitude},${latlng.longitude}")
            } catch (e: Exception) {
                Timber.e(e)
                return@launch
            }
            result.response?.venues.let {
                adapter.venues = it?.filter { item ->
                    item.location.address != null
                }
            }
        }
    }

    fun search(query: String) {
        lifecycleScope.launch {
            val result = try {
                foursquareService.searchVenues("${currentPosition.latitude},${currentPosition.longitude}", query)
            } catch (e: Exception) {
                Timber.e(e)
                return@launch
            }
            locationSearchAdapter.venues = result.response?.venues?.filter { item ->
                item.location.address != null
            }
        }
    }

    private var markerAnimatorSet: AnimatorSet? = null

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            search_recycler.isVisible = s?.isNotEmpty() == true
            s?.let {
                search(it.toString())
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    companion object {

        private val LOCATION_NAME = "location_name"
        private val LOCATION = "location"

        fun getResult(intent: Intent): Location? {
            return intent.getParcelableExtra(LOCATION_NAME)
        }

        fun show(fragment: Fragment) {
            Intent(fragment.requireContext(), LocationActivity::class.java).run {
                fragment.startActivityForResult(this, REQUEST_LOCATION)
            }
        }

        fun show(context: Context, location: Location) {
            Intent(context, LocationActivity::class.java).run {
                putExtra(LOCATION, location)
                context.startActivity(this)
            }
        }
    }
}
