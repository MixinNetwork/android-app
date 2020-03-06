package one.mixin.android.ui.conversation.location

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import kotlinx.android.synthetic.main.activity_location.*
import kotlinx.android.synthetic.main.item_location.view.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.service.FoursquareService
import one.mixin.android.extension.dp
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.vo.foursquare.Venues
import timber.log.Timber
import javax.inject.Inject

class LocationActivity : BaseActivity(), OnMapReadyCallback {

    @Inject
    lateinit var foursquareService: FoursquareService

    // todo delete test position
    private val SYDNEY = LatLng(39.9967, 116.4805)
    private val ZOOM_LEVEL = 13f

    private var mapsInitialized = false
    private var onResumeCalled = false
    private var forceUpdate: CameraUpdate? = null

    private val adapter by lazy {
        LocationAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)
        map_view.onCreate(savedInstanceState)
        MapsInitializer.initialize(MixinApplication.appContext)
        map_view.getMapAsync(this)
        recycler_view.adapter = adapter
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
            googleMap.isMyLocationEnabled = true
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
                search(lastLang)
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
            val result = foursquareService.searchVenues("${latlng.latitude},${latlng.longitude}")
            if (result.isSuccess()) {
                result.response?.venues.let {
                    adapter.venues = it
                }
            }
        }
    }

    class LocationAdapter : RecyclerView.Adapter<LocationHolder>() {
        var venues: List<Venues>? = null
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationHolder {
            return LayoutInflater.from(parent.context).inflate(R.layout.item_location, parent, false).run {
                LocationHolder(this)
            }
        }

        override fun getItemCount(): Int = venues.notNullWithElse({ it.size }, 0)

        override fun onBindViewHolder(holder: LocationHolder, position: Int) {
            holder.itemView.title.text = venues?.get(position)?.name
        }
    }

    class LocationHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private var markerAnimatorSet: AnimatorSet? = null

    companion object {
        fun show(context: Context) {
            Intent(context, LocationActivity::class.java).run {
                context.startActivity(this)
            }
        }
    }
}
