package one.mixin.android.ui.conversation.location

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.service.FoursquareService
import one.mixin.android.databinding.ActivityLocationBinding
import one.mixin.android.extension.REQUEST_LOCATION
import one.mixin.android.extension.dp
import one.mixin.android.extension.getParcelableExtraCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.notEmptyWithElse
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.util.calculationByDistance
import one.mixin.android.util.distanceFormat
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.websocket.LocationPayload
import one.mixin.android.websocket.getImageUrl
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import org.osmdroid.views.MapView
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

@AndroidEntryPoint
class LocationActivity : BaseActivity(), OnMapReadyCallback {
    @Inject
    lateinit var foursquareService: FoursquareService

    private var currentPosition: MixinLatLng? = null
    private var selfPosition: MixinLatLng? = null
        set(value) {
            if (value != null) {
                location?.let { location ->
                    calculationByDistance(LatLng(value.latitude, value.longitude), LatLng(location.latitude, location.longitude)).distanceFormat()
                }?.let {
                    if (binding.locationSubTitle.text == null) {
                        binding.locationSubTitle.text = getString(R.string.location_distance_away, it.first, getString(it.second))
                    }
                }
            }
            field = value
        }

    private var mapsInitialized = false
    private var onResumeCalled = false

    private val location: LocationPayload? by lazy {
        intent.getParcelableExtraCompat(LOCATION, LocationPayload::class.java)
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
            },
        )
    }

    private val locationSearchAdapter by lazy {
        LocationSearchAdapter {
            setResult(it)
        }
    }

    private val mLocationListener: LocationListener =
        object : LocationListener {
            override fun onLocationChanged(location: android.location.Location) {
                currentPosition = MixinLatLng(location.latitude, location.longitude)
                selfPosition = MixinLatLng(location.latitude, location.longitude)
                if (this@LocationActivity.location == null) {
                    currentPosition?.let { currentPosition ->
                        mixinMapView.moveCamera(currentPosition)
                        isInit = false
                    }
                    locationAdapter.accurate = resources.getQuantityString(R.plurals.location_accurate, location.accuracy.toInt(), location.accuracy.toInt())
                }
            }

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {}
        }

    private lateinit var mixinMapView: MixinMapView
    private var osmMapView: org.osmdroid.views.MapView? = null
    private val useOsm = useOpenStreetMap()

    private lateinit var binding: ActivityLocationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            if (location != null) {
                mentionLocation.motion.loadLayoutDescription(R.xml.scene_location_none)
            }
            if (useOsm) {
                val stub = mentionLocation.osmStub
                osmMapView = stub.inflate() as org.osmdroid.views.MapView
            } else {
                try {
                    MapsInitializer.initialize(MixinApplication.appContext)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
            mixinMapView = MixinMapView(
                this@LocationActivity,
                mentionLocation.googleMapView,
                osmMapView
            ).apply {
                onCreate(savedInstanceState)
            }
            if (useOsm) {
                initOsmMap(osmMapView)
            } else {
                mentionLocation.googleMapView.getMapAsync(this@LocationActivity)
            }
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
                    mixinMapView.moveCamera(selfPosition)
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
                override fun onEditorAction(
                    v: TextView?,
                    actionId: Int,
                    event: KeyEvent?,
                ): Boolean {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        binding.searchEt.hideKeyboard()
                        return true
                    }
                    return false
                }
            },
        )

        val currentLocation = location
        if (currentLocation != null) {
            binding.locationTitle.text = currentLocation.name ?: getString(R.string.Unnamed_location)
            currentLocation.address?.let { address ->
                binding.locationSubTitle.text = address
            }
            val image = currentLocation.getImageUrl()
            if (image != null) {
                binding.locationIcon.loadImage(data = image)
            } else {
                binding.locationIcon.setBackgroundResource(R.drawable.ic_current_location)
            }
            binding.icLocationShared.setOnClickListener {
                try {
                    startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("geo:${currentLocation.latitude},${currentLocation.longitude}?q=${currentLocation.latitude},${currentLocation.longitude}")),
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
                                    "http://maps.google.com/maps?saddr=${selfPosition.latitude},${selfPosition.longitude}&daddr=${currentLocation.latitude},${currentLocation.longitude}",
                                ),
                            ),
                        )
                    } catch (e: ActivityNotFoundException) {
                        toast(R.string.error_open_location)
                    }
                }
            }
        } else {
            binding.mentionLocation.locationRecycler.adapter = locationAdapter
        }
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

    override fun onStart() {
        super.onStart()
        if (mapsInitialized) {
            mixinMapView.onStart()
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
            mixinMapView.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        onResumeCalled = false
        getSystemService<LocationManager>()?.removeUpdates(mLocationListener)
        if (mapsInitialized) {
            mixinMapView.onPause()
        }
    }

    override fun onStop() {
        super.onStop()
        if (locationAlert != null && locationAlert?.isShowing == true) {
            locationAlert?.dismiss()
        }
        if (mapsInitialized) {
            mixinMapView.onStop()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (mapsInitialized) {
            mixinMapView.onLowMemory()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mapsInitialized) {
            mixinMapView.onDestroy()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mapsInitialized) {
            mixinMapView.onSaveInstanceState(outState)
        }
    }

    @SuppressLint("MissingPermission")
    private fun findMyLocation() {
        val locationManager = getSystemService<LocationManager>()
        if (locationManager != null && !checkLocationEnable(locationManager)) {
            return
        }
        locationManager?.getProviders(true)?.let { providers ->
            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider)
                if (l != null) {
                    currentPosition = MixinLatLng(l.latitude, l.longitude)
                    selfPosition = MixinLatLng(l.latitude, l.longitude)
                    if (this@LocationActivity.location == null) {
                        currentPosition?.let { currentPosition ->
                            mixinMapView.moveCamera(currentPosition)
                            isInit = false
                        }
                        locationAdapter.accurate = resources.getQuantityString(R.plurals.location_accurate, l.accuracy.toInt(), l.accuracy.toInt())
                    }
                }
            }
        }
        locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 100f, mLocationListener)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mixinMapView.googleMap = googleMap
        if (this.isNightMode()) {
            val style = MapStyleOptions.loadRawResourceStyle(applicationContext, R.raw.mapstyle_night)
            googleMap.setMapStyle(style)
        }
        initGoogleMap(googleMap)
        afterInit()
    }

    private fun setResult(location: LocationPayload) {
        setResult(Activity.RESULT_OK, Intent().putExtra(LOCATION_NAME, location))
        finish()
    }

    private fun afterInit() {
        val l = location
        val sp = selfPosition
        if (l != null) {
            val mixinLatLng = MixinLatLng(l.latitude, l.longitude)
            mixinMapView.addMarker(mixinLatLng)
            mixinMapView.moveCamera(mixinLatLng)
        } else if (sp != null) {
            mixinMapView.moveCamera(sp)
        }
        mapsInitialized = true
        if (onResumeCalled) {
            mixinMapView.onResume()
        }
    }

    private var isInit = true

    @SuppressLint("MissingPermission")
    private fun initGoogleMap(googleMap: GoogleMap) {
        try {
            googleMap.isMyLocationEnabled = true
        } catch (e: Exception) {
            Timber.e(e)
        }
        with(googleMap) {
            uiSettings.isMyLocationButtonEnabled = false
            uiSettings.isZoomControlsEnabled = false
            uiSettings.isCompassEnabled = false
            uiSettings.isIndoorLevelPickerEnabled = false
            uiSettings.isRotateGesturesEnabled = false
        }
        googleMap.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                val cameraPosition = googleMap.cameraPosition
                CameraUpdateFactory.newLatLngZoom(cameraPosition.target, cameraPosition.zoom)
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
            locationSearchAdapter.setMark(marker.position.latitude, marker.position.longitude)
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
                    val mixinLatLng = MixinLatLng(lastLang.latitude, lastLang.longitude)
                    currentPosition = mixinLatLng
                    search(mixinLatLng)
                }
            }
        }
        googleMap.setOnCameraMoveCanceledListener {}
    }

    private fun initOsmMap(mapView: org.osmdroid.views.MapView?) {
        mapView?.apply {
            setMultiTouchControls(true)
            overlayManager.tilesOverlay.loadingBackgroundColor = Color.TRANSPARENT
            overlayManager.tilesOverlay.loadingLineColor = Color.TRANSPARENT
            
            addMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean {
                    if (!binding.mentionLocation.icMarker.isVisible && !isInit) {
                        binding.mentionLocation.icMarker.isVisible = true
                        locationSearchAdapter.setMark()
                    }
                    
                    markerAnimatorSet?.cancel()
                    markerAnimatorSet = AnimatorSet()
                    markerAnimatorSet?.playTogether(
                        ObjectAnimator.ofFloat(binding.mentionLocation.icMarker, View.TRANSLATION_Y, -8.dp.toFloat())
                    )
                    markerAnimatorSet?.duration = 200
                    markerAnimatorSet?.start()

                    val center = projection.fromPixels(width/2, height/2) as GeoPoint
                    currentPosition = MixinLatLng(center.latitude, center.longitude)
                    if (location == null) {
                        search(currentPosition!!)
                    }

                    markerAnimatorSet?.cancel()
                    markerAnimatorSet = AnimatorSet()
                    markerAnimatorSet?.playTogether(
                        ObjectAnimator.ofFloat(binding.mentionLocation.icMarker, View.TRANSLATION_Y, 0f)
                    )
                    markerAnimatorSet?.duration = 200
                    markerAnimatorSet?.start()
                    
                    return true
                }

                override fun onZoom(event: ZoomEvent?): Boolean {
                    return false
                }
            })
        }
        afterInit()
    }

    private var lastSearchJob: Job? = null

    fun search(latLng: MixinLatLng) {
        binding.mentionLocation.locationPb.isVisible = locationAdapter.venues == null
        if (lastSearchJob != null && lastSearchJob?.isActive == true) {
            lastSearchJob?.cancel()
        }
        lastSearchJob =
            lifecycleScope.launch(errorHandler) {
                val result = foursquareService.searchVenues("${latLng.latitude},${latLng.longitude}")

                result.response?.venues.let { list ->
                    list.let { data ->
                        locationAdapter.venues = data
                        binding.mentionLocation.locationPb.isVisible = data == null
                    }
                }
            }
    }

    private val errorHandler =
        CoroutineExceptionHandler { _, error ->
            Timber.e(error)
        }

    private fun checkLocationEnable(locationManager: LocationManager): Boolean {
        var gpsEnable = false
        try {
            gpsEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            Timber.w(e)
        }
        if (!gpsEnable) {
            locationAlert =
                AlertDialog.Builder(this)
                    .setMessage(R.string.location_enable_title)
                    .setPositiveButton(R.string.Settings) { dialog, _ ->
                        try {
                            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                        dialog.dismiss()
                    }.show()
            return false
        }
        return true
    }

    private var locationAlert: AlertDialog? = null

    private var lastSearchQueryJob: Job? = null

    fun search(query: String) {
        binding.mentionLocation.locationPb.isVisible = locationSearchAdapter.venues == null
        val currentPosition = this.currentPosition ?: return
        if (lastSearchQueryJob != null && lastSearchQueryJob?.isActive == true) {
            lastSearchQueryJob?.cancel()
        }
        lastSearchQueryJob =
            lifecycleScope.launch(errorHandler) {
                val result = foursquareService.searchVenues("${currentPosition.latitude},${currentPosition.longitude}", query)
                result.response?.venues.let { data ->
                    locationSearchAdapter.venues = data
                    binding.mentionLocation.apply {
                        locationEmpty.isVisible = data.isNullOrEmpty()
                        locationEmptyTv.text = getString(R.string.location_empty, query)
                        locationPb.isVisible = data == null
                    }
                    mixinMapView.clear()
                    var south: Double? = null
                    var west: Double? = null
                    var north: Double? = null
                    var east: Double? = null
                    data?.forEachIndexed { index, item ->
                        south =
                            if (south == null) {
                                item.location.lat
                            } else {
                                min(south!!, item.location.lat)
                            }
                        west =
                            if (west == null) {
                                item.location.lng
                            } else {
                                min(west!!, item.location.lng)
                            }
                        north =
                            if (north == null) {
                                item.location.lat
                            } else {
                                max(north!!, item.location.lat)
                            }
                        east =
                            if (east == null) {
                                item.location.lng
                            } else {
                                max(east!!, item.location.lng)
                            }
                        mixinMapView.addMarker(index, item)

                        if (south != null && west != null && north != null && east != null) {
                            val bound = MixinLatLngBounds(MixinLatLng(south!!, west!!), MixinLatLng(north!!, east!!))
                            mixinMapView.moveBounds(bound)
                        }
                    }
                }
            }
    }

    private var markerAnimatorSet: AnimatorSet? = null

    private val textWatcher =
        object : TextWatcher {
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
                        mixinMapView.clear()
                    },
                )
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int,
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int,
            ) {}
        }

    companion object {
        private const val LOCATION_NAME = "location_name"
        private const val LOCATION = "location"

        fun getResult(intent: Intent): LocationPayload? {
            return intent.getParcelableExtraCompat(LOCATION_NAME, LocationPayload::class.java)
        }

        fun show(fragment: Fragment) {
            Intent(fragment.requireContext(), LocationActivity::class.java).run {
                @Suppress("DEPRECATION")
                fragment.startActivityForResult(this, REQUEST_LOCATION)
            }
        }

        fun show(
            context: Context,
            location: LocationPayload,
        ) {
            Intent(context, LocationActivity::class.java).run {
                putExtra(LOCATION, location)
                context.startActivity(this)
            }
        }
    }
}
