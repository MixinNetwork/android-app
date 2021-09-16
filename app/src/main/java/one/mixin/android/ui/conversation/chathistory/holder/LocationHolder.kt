package one.mixin.android.ui.conversation.chathistory.holder

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.MapStyleOptions
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import one.mixin.android.BuildConfig
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatLocationBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.maxItemWidth
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.chathistory.TranscriptAdapter
import one.mixin.android.ui.conversation.location.MixinLatLng
import one.mixin.android.ui.conversation.location.MixinMapView
import one.mixin.android.ui.conversation.location.useMapbox
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.MessageStatus
import one.mixin.android.websocket.LocationPayload
import one.mixin.android.websocket.toLocationData
import org.jetbrains.anko.dip
import org.jetbrains.anko.textColorResource

class LocationHolder constructor(val binding: ItemChatLocationBinding) :
    BaseViewHolder(binding.root),
    OnMapReadyCallback,
    com.mapbox.mapboxsdk.maps.OnMapReadyCallback {
    private val dp16 = itemView.context.dpToPx(16f)

    private var mixinMapView: MixinMapView
    private var onResumeCalled = false

    private val dp36 by lazy {
        36.dp
    }

    private val dp4 by lazy {
        MixinApplication.appContext.dpToPx(4f).toFloat()
    }

    private val useMapbox = useMapbox()

    init {
        binding.chatName.maxWidth = itemView.context.maxItemWidth() - dp16
        binding.locationLayout.round(6.dp)
        var mapBoxView: MapView? = null
        if (useMapbox) {
            Mapbox.getInstance(itemView.context, BuildConfig.MAPBOX_PUBLIC_TOKEN)
            val stub = binding.mapboxStub
            mapBoxView = stub.inflate() as MapView
        }
        mixinMapView = MixinMapView(binding.root.context, binding.googleMap, mapBoxView)
        mixinMapView.onCreate(null)
        if (useMapbox) {
            mapBoxView?.getMapAsync(this)
        } else {
            binding.googleMap.getMapAsync(this)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        MapsInitializer.initialize(MixinApplication.appContext)
        mixinMapView.googleMap = googleMap
        if (isNightMode) {
            val style = MapStyleOptions.loadRawResourceStyle(MixinApplication.appContext, R.raw.mapstyle_night)
            googleMap.setMapStyle(style)
        }
        with(googleMap) {
            uiSettings.isZoomGesturesEnabled = false
            uiSettings.isScrollGesturesEnabled = false
            uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = false
        }
        if (onResumeCalled) {
            mixinMapView.onResume()
        }
        setMapLocation()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mixinMapView.mapboxMap = mapboxMap
        mapboxMap.setStyle(mixinMapView.getMapboxStyle())
        with(mapboxMap) {
            uiSettings.isZoomGesturesEnabled = false
            uiSettings.isScrollGesturesEnabled = false
        }
        if (onResumeCalled) {
            mixinMapView.onResume()
        }
        setMapLocation()
    }

    private fun setMapLocation() {
        if (useMapbox) {
            if (mixinMapView.mapboxMap == null) return
        } else {
            if (mixinMapView.googleMap == null) return
        }
        if (itemView.tag == location.hashCode()) return

        itemView.tag = location.hashCode()
        onResumeCalled = true
        mixinMapView.clear()
        mixinMapView.onResume()
        location?.let { data ->
            val position = MixinLatLng(data.latitude, data.longitude)
            if (!useMapbox) {
                mixinMapView.googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
            }
            mixinMapView.addMarker(position)
            mixinMapView.moveCamera(position)
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        val lp = (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams)
        if (isMe) {
            lp.horizontalBias = 1f
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_me_last,
                    R.drawable.chat_bubble_reply_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_me,
                    R.drawable.chat_bubble_reply_me_night
                )
            }
            (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
        } else {
            lp.horizontalBias = 0f
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_other_last,
                    R.drawable.chat_bubble_reply_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_other,
                    R.drawable.chat_bubble_reply_other_night
                )
            }
            (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = dp3
        }
    }

    private var location: LocationPayload? = null
    fun bind(
        messageItem: ChatHistoryMessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        onItemListener: TranscriptAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        location = toLocationData(messageItem.content)
        if (location?.name == null) {
            binding.locationSubTitle.isVisible = false
            binding.locationTitle.visibility = View.INVISIBLE
        } else {
            binding.locationSubTitle.isVisible = true
            binding.locationTitle.isVisible = true
            binding.locationTitle.text = location?.name
            binding.locationSubTitle.text = location?.address
        }
        if (location?.name == null && location?.address == null) {
            (binding.locationBottom.layoutParams as ViewGroup.MarginLayoutParams).topMargin = -dp36
            binding.chatTime.setBackgroundResource(R.drawable.bg_bubble_shadow)
            binding.chatTime.textColorResource = R.color.white
            binding.chatTime.translationY = dp4
        } else {
            (binding.locationBottom.layoutParams as ViewGroup.MarginLayoutParams).topMargin = 0
            binding.chatTime.setBackgroundResource(0)
            binding.chatTime.textColorResource = (R.color.color_chat_date)
            binding.chatTime.translationY = 0f
        }
        setMapLocation()

        val isMe = messageItem.userId == Session.getAccountId()

        binding.chatTime.timeAgoClock(messageItem.createdAt)
        setStatusIcon(
            isMe,
            MessageStatus.DELIVERED.name,
            isSecret = false,
            isRepresentative = false,
            isWhite = location?.name == null && location?.address == null
        ) { statusIcon, secretIcon, representativeIcon ->
            statusIcon?.setBounds(0, 0, dp12, dp12)
            secretIcon?.setBounds(0, 0, dp8, dp8)
            representativeIcon?.setBounds(0, 0, dp8, dp8)
            TextViewCompat.setCompoundDrawablesRelative(binding.chatTime, secretIcon ?: representativeIcon, null, statusIcon, null)
        }

        if (isFirst && !isMe) {
            binding.chatName.visibility = View.VISIBLE
            binding.chatName.text = messageItem.userFullName
            if (messageItem.appId != null) {
                binding.chatName.setCompoundDrawables(null, null, botIcon, null)
                binding.chatName.compoundDrawablePadding = itemView.dip(3)
            } else {
                binding.chatName.setCompoundDrawables(null, null, null, null)
            }
            binding.chatName.setTextColor(getColorById(messageItem.userId))
            binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            binding.chatName.visibility = View.GONE
        }

        if (messageItem.appId != null) {
            binding.chatName.setCompoundDrawables(null, null, botIcon, null)
            binding.chatName.compoundDrawablePadding = itemView.dip(3)
        } else {
            binding.chatName.setCompoundDrawables(null, null, null, null)
        }

        chatLayout(isMe, isLast)
        itemView.setOnClickListener {
            onItemListener.onLocationClick(messageItem)
        }
        if (messageItem.transcriptId == null) {
            binding.root.setOnLongClickListener {
                onItemListener.onMenu(binding.chatJump, messageItem)
                true
            }
            binding.chatLayout.setOnLongClickListener {
                onItemListener.onMenu(binding.chatJump, messageItem)
                true
            }
            chatJumpLayout(binding.chatJump, isMe, messageItem.messageId, R.id.chat_layout, onItemListener)
        }
    }
}
