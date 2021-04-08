package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.location.MixinLatLng
import one.mixin.android.ui.conversation.location.MixinMapView
import one.mixin.android.ui.conversation.location.useGoogleMap
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSignal
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

    private val useGoogleMap = useGoogleMap()

    init {
        binding.chatName.maxWidth = itemView.context.maxItemWidth() - dp16
        binding.locationLayout.round(6.dp)
        var mapBoxView: MapView? = null
        if (!useGoogleMap) {
            Mapbox.getInstance(itemView.context, BuildConfig.MAPBOX_PUBLIC_TOKEN)
            val stub = binding.mapboxStub
            mapBoxView = stub.inflate() as MapView
        }
        mixinMapView = MixinMapView(binding.root.context, binding.googleMap, mapBoxView)
        mixinMapView.onCreate(null)
        if (useGoogleMap) {
            binding.googleMap.getMapAsync(this)
        } else {
            mapBoxView?.getMapAsync(this)
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
        if (useGoogleMap) {
            if (mixinMapView.googleMap == null) return
        } else {
            if (mixinMapView.mapboxMap == null) return
        }
        if (itemView.tag == location.hashCode()) return

        itemView.tag = location.hashCode()
        onResumeCalled = true
        mixinMapView.clear()
        mixinMapView.onResume()
        location?.let { data ->
            val position = MixinLatLng(data.latitude, data.longitude)
            if (useGoogleMap) {
                mixinMapView.googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
            }
            mixinMapView.addMarker(position)
            mixinMapView.moveCamera(position)
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        val lp = (binding.chatLayout.layoutParams as FrameLayout.LayoutParams)
        if (isMe) {
            lp.gravity = Gravity.END
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
            lp.gravity = Gravity.START
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

    private var onItemListener: ConversationAdapter.OnItemListener? = null

    private var location: LocationPayload? = null
    fun bind(
        messageItem: MessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        hasSelect: Boolean,
        isSelect: Boolean,
        isRepresentative: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        this.onItemListener = onItemListener
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
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        itemView.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }
        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onLocationClick(messageItem)
            }
        }
        binding.locationLayout.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onLocationClick(messageItem)
            }
        }
        binding.chatContentLayout.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onLocationClick(messageItem)
            }
        }
        binding.locationLayout.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }
        binding.chatContentLayout.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }
        val isMe = meId == messageItem.userId

        binding.chatTime.timeAgoClock(messageItem.createdAt)
        setStatusIcon(isMe, messageItem.status, messageItem.isSignal(), isRepresentative, location?.name == null && location?.address == null) { statusIcon, secretIcon, representativeIcon ->
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
    }
}
