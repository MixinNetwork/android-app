package one.mixin.android.ui.conversation.location

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.service.FoursquareService
import one.mixin.android.databinding.ActivityLocationBinding
import one.mixin.android.extension.REQUEST_LOCATION
import one.mixin.android.extension.dp
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.notEmptyWithElse
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.util.calculationByDistance
import one.mixin.android.util.distanceFormat
import one.mixin.android.websocket.LocationPayload
import one.mixin.android.websocket.getImageUrl
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@AndroidEntryPoint
class LocationActivity : BaseActivity(), OnMapReadyCallback {

    @Inject
    lateinit var foursquareService: FoursquareService

    private val ZOOM_LEVEL = 13f
    private var currentPosition: LatLng? = null
    private var selfPosition: LatLng? = null
        set(value) {
            if (value != null) {
                location?.let { location ->
                    calculationByDistance(value, LatLng(location.latitude, location.longitude)).distanceFormat()
                }?.let {
                    if (binding.locationSubTitle.text == null)
                        binding.locationSubTitle.text = getString(R.string.location_distance, it.first, getString(it.second))
                }
            }
            field = value
        }

    private var mapsInitialized = false
    private var onResumeCalled = false
    private var forceUpdate: CameraUpdate? = null

    private val location: LocationPayload? by lazy {
        intent.getParcelableExtra(LOCATION)
    }

    private val locationAdapter by lazy {
        LocationAdapter(
            {
                currentPosition?.let { currentPosition ->
                    setResult(LocationPayload(currentPosition.latitude, currentPosition.longitude, null, null))
                }
            },
            {
                setResult(it)
            }
        )
    }

    private val locationSearchAdapter by lazy {
        LocationSearchAdapter {
            setResult(it)
        }
    }

    private val mLocationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: android.location.Location) {
            currentPosition = LatLng(location.latitude, location.longitude)
            selfPosition = LatLng(location.latitude, location.longitude)
            if (this@LocationActivity.location == null) {
                currentPosition?.let { currentPosition ->
                    moveCamera(currentPosition)
                    isInit = false
                }
                locationAdapter.accurate = getString(R.string.location_accurate, location.accuracy.toInt())
            }
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}
    }

    private lateinit var binding: ActivityLocationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            if (location != null) {
                mentionLocation.motion.loadLayoutDescription(R.xml.scene_location_none)
            }
            MapsInitializer.initialize(MixinApplication.appContext)
            mentionLocation.mapView.onCreate(savedInstanceState)
            mentionLocation.mapView.getMapAsync(this@LocationActivity)
            icBack.setOnClickListener {
                if (searchVa.displayedChild == 1) {
                    searchVa.showPrevious()
                    searchEt.text = null
                    searchEt.hideKeyboard()
                    mentionLocation.locationEmpty.isVisible = false
                } else {
                    finish()
                }
            }
            icSearch.isVisible = location == null
            icLocationShared.isVisible = location != null
            mentionLocation.locationPb.isVisible = location == null
            locationGo.isVisible = location != null
            mentionLocation.locationRecycler.isVisible = location == null
            icSearch.setOnClickListener {
                searchVa.showNext()
                searchEt.requestFocus()
                searchEt.showKeyboard()
            }
            mentionLocation.myLocation.setOnClickListener {
                selfPosition?.let { selfPosition ->
                    moveCamera(selfPosition)
                }
            }
            icClose.setOnClickListener {
                searchVa.showPrevious()
                searchEt.text = null
                searchEt.hideKeyboard()
                mentionLocation.locationEmpty.isVisible = false
            }
        }
        binding.searchEt.addTextChangedListener(textWatcher)
        binding.searchEt.setOnEditorActionListener(
            object : TextView.OnEditorActionListener {
                override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        binding.searchEt.hideKeyboard()
                        return true
                    }
                    return false
                }
            }
        )

        location.notNullWithElse(
            { location ->
                binding.locationTitle.text = location.name ?: getString(R.string.location_unnamed)
                location.address?.let { address ->
                    binding.locationSubTitle.text = address
                }
                location.getImageUrl().notNullWithElse(
                    {
                        binding.locationIcon.loadImage(it)
                    },
                    {
                        binding.locationIcon.setBackgroundResource(R.drawable.ic_current_location)
                    }
                )
                binding.icLocationShared.setOnClickListener {
                    try {
                        startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}"))
                        )
                    } catch (e: ActivityNotFoundException) {
                        toast(R.string.error_open_location)
                    }
                }
                binding.locationGoIv.setOnClickListener {
                    selfPosition?.let { selfPosition ->
                        try {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(
                                        "http://maps.google.com/maps?saddr=${selfPosition.latitude},${selfPosition.longitude}&daddr=${location.latitude},${location.longitude}"
                                    )
                                )
                            )
                        } catch (e: ActivityNotFoundException) {
                            toast(R.string.error_open_location)
                        }
                    }
                }
            },
            {
                binding.mentionLocation.locationRecycler.adapter = locationAdapter
            }
        )
    }

    override fun onBackPressed() {
        if (binding.searchVa.displayedChild == 1) {
            binding.searchVa.showPrevious()
            binding.searchEt.text = null
            binding.searchEt.hideKeyboard()
            binding.mentionLocation.locationEmpty.isVisible = false
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        onResumeCalled = true
        RxPermissions(this)
            .request(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            .autoDispose(stopScope)
            .subscribe { granted ->
                if (granted) {
                    findMyLocation()
                } else {
                    openPermissionSetting()
                }
            }
        if (mapsInitialized) {
            binding.mentionLocation.mapView.onResume()
        }
    }

    @SuppressLint("MissingPermission")
    private fun findMyLocation() {
        val locationManager = getSystemService<LocationManager>()
        locationManager?.getProviders(true)?.let { providers ->
            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider)
                if (l != null) {
                    currentPosition = LatLng(l.latitude, l.longitude)
                    selfPosition = LatLng(l.latitude, l.longitude)
                    if (this@LocationActivity.location == null) {
                        currentPosition?.let { currentPosition ->
                            moveCamera(currentPosition)
                            isInit = false
                        }
                        locationAdapter.accurate = getString(R.string.location_accurate, l.accuracy.toInt())
                    }
                }
            }
        }
        locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 100f, mLocationListener)
    }

    override fun onPause() {
        super.onPause()
        onResumeCalled = false
        getSystemService<LocationManager>()?.removeUpdates(mLocationListener)
        if (mapsInitialized) {
            binding.mentionLocation.mapView.onPause()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (mapsInitialized) {
            binding.mentionLocation.mapView.onLowMemory()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mapsInitialized) {
            binding.mentionLocation.mapView.onDestroy()
        }
    }

    private var googleMap: GoogleMap? = null

    private fun moveCamera(latlng: LatLng) {
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, ZOOM_LEVEL))
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap ?: return
        this.googleMap = googleMap
        if (this.isNightMode()) {
            val style = MapStyleOptions.loadRawResourceStyle(applicationContext, R.raw.mapstyle_night)
            googleMap.setMapStyle(style)
        }
        mapInit(googleMap)

        with(googleMap) {
            if (location != null) {
                addMarker(
                    MarkerOptions().position(
                        LatLng(location!!.latitude, location!!.longitude)
                    ).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map_marker))
                )
                moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location!!.latitude, location!!.longitude), ZOOM_LEVEL))
            } else if (selfPosition != null) {
                moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(selfPosition!!.latitude, selfPosition!!.longitude), ZOOM_LEVEL))
            }
        }
        mapsInitialized = true
        if (onResumeCalled) {
            binding.mentionLocation.mapView.onResume()
        }
    }

    private fun setResult(location: LocationPayload) {
        setResult(Activity.RESULT_OK, Intent().putExtra(LOCATION_NAME, location))
        finish()
    }

    private var isInit = true
    fun mapInit(googleMap: GoogleMap) {
        try {
            googleMap.isMyLocationEnabled = true
        } catch (e: Exception) {
            Timber.e(e)
        }
        with(googleMap) {
            uiSettings?.isMyLocationButtonEnabled = false
            uiSettings?.isZoomControlsEnabled = false
            uiSettings?.isCompassEnabled = false
            uiSettings?.isIndoorLevelPickerEnabled = false
            uiSettings?.isRotateGesturesEnabled = false
        }
        googleMap.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                val cameraPosition = googleMap.cameraPosition
                forceUpdate = CameraUpdateFactory.newLatLngZoom(cameraPosition.target, cameraPosition.zoom)
                if (!binding.mentionLocation.icMarker.isVisible && !isInit) {
                    binding.mentionLocation.icMarker.isVisible = true
                    locationSearchAdapter.setMark()
                }
            }
            markerAnimatorSet?.cancel()
            markerAnimatorSet = AnimatorSet()
            markerAnimatorSet?.playTogether(ObjectAnimator.ofFloat(binding.mentionLocation.icMarker, View.TRANSLATION_Y, -8.dp.toFloat()))
            markerAnimatorSet?.duration = 200
            markerAnimatorSet?.start()
        }
        googleMap.setOnCameraMoveListener {}
        googleMap.setOnMarkerClickListener { marker ->
            locationSearchAdapter.setMark(marker.zIndex)
            binding.mentionLocation.icMarker.isVisible = true
            false
        }

        googleMap.setOnCameraIdleListener {
            markerAnimatorSet?.cancel()
            markerAnimatorSet = AnimatorSet()
            markerAnimatorSet?.playTogether(ObjectAnimator.ofFloat(binding.mentionLocation.icMarker, View.TRANSLATION_Y, 0f))
            markerAnimatorSet?.duration = 200
            markerAnimatorSet?.start()
            googleMap.cameraPosition.target.let { lastLang ->
                if (location == null) {
                    currentPosition = lastLang
                    search(lastLang)
                }
            }
        }

        googleMap.setOnCameraMoveCanceledListener {}
    }

    private var lastSearchJob: Job? = null
    fun search(latlng: LatLng) {
        binding.mentionLocation.locationPb.isVisible = locationAdapter.venues == null
        if (lastSearchJob != null && lastSearchJob?.isActive == true) {
            lastSearchJob?.cancel()
        }
        lastSearchJob = lifecycleScope.launch(errorHandler) {
            val result = foursquareService.searchVenues("${latlng.latitude},${latlng.longitude}")

            result.response?.venues.let { list ->
                list.let { data ->
                    locationAdapter.venues = data
                    binding.mentionLocation.locationPb.isVisible = data == null
                }
            }
        }
    }

    private val errorHandler = CoroutineExceptionHandler { _, error ->
        Timber.e(error)
    }

    private var lastSearchQueryJob: Job? = null
    fun search(query: String) {
        binding.mentionLocation.locationPb.isVisible = locationSearchAdapter.venues == null
        val currentPosition = this.currentPosition ?: return
        if (lastSearchQueryJob != null && lastSearchQueryJob?.isActive == true) {
            lastSearchQueryJob?.cancel()
        }
        lastSearchQueryJob = lifecycleScope.launch(errorHandler) {
            val result = foursquareService.searchVenues("${currentPosition.latitude},${currentPosition.longitude}", query)
            result.response?.venues.let { data ->
                locationSearchAdapter.venues = data
                binding.mentionLocation.apply {
                    locationEmpty.isVisible = data.isNullOrEmpty()
                    locationEmptyTv.text = getString(R.string.location_empty, query)
                    locationPb.isVisible = data == null
                }
                googleMap?.clear()
                var south: Double? = null
                var west: Double? = null
                var north: Double? = null
                var east: Double? = null
                data?.forEachIndexed { index, item ->
                    south = if (south == null) {
                        item.location.lat
                    } else {
                        min(south!!, item.location.lat)
                    }
                    west = if (west == null) {
                        item.location.lng
                    } else {
                        min(west!!, item.location.lng)
                    }
                    north = if (north == null) {
                        item.location.lat
                    } else {
                        max(north!!, item.location.lat)
                    }
                    east = if (east == null) {
                        item.location.lng
                    } else {
                        max(east!!, item.location.lng)
                    }
                    googleMap?.addMarker(
                        MarkerOptions().zIndex(index.toFloat()).position(
                            LatLng(
                                item.location.lat,
                                item.location.lng
                            )
                        ).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_search_maker))
                    )

                    if (south != null && west != null && north != null && east != null) {
                        val bound = LatLngBounds(LatLng(south!!, west!!), LatLng(north!!, east!!))
                        moveBounds(bound)
                    }
                }
            }
        }
    }

    private fun moveBounds(bound: LatLngBounds) {
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bound, 64.dp))
    }

    private var markerAnimatorSet: AnimatorSet? = null

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            s.notEmptyWithElse(
                { charSequence ->
                    binding.mentionLocation.locationRecycler.adapter = locationSearchAdapter
                    val content = charSequence.toString()
                    locationSearchAdapter.keyword = content
                    search(content)
                },
                {
                    binding.mentionLocation.locationRecycler.adapter = locationAdapter
                    if (lastSearchQueryJob?.isActive == true) {
                        lastSearchQueryJob?.cancel()
                    }
                    locationSearchAdapter.keyword = null
                    locationSearchAdapter.venues = null
                    locationSearchAdapter.setMark()
                    googleMap?.clear()
                }
            )
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    companion object {

        private val LOCATION_NAME = "location_name"
        private val LOCATION = "location"

        fun getResult(intent: Intent): LocationPayload? {
            return intent.getParcelableExtra(LOCATION_NAME)
        }

        fun show(fragment: Fragment) {
            Intent(fragment.requireContext(), LocationActivity::class.java).run {
                fragment.startActivityForResult(this, REQUEST_LOCATION)
            }
        }

        fun show(context: Context, location: LocationPayload) {
            Intent(context, LocationActivity::class.java).run {
                putExtra(LOCATION, location)
                context.startActivity(this)
            }
        }
    }
}
