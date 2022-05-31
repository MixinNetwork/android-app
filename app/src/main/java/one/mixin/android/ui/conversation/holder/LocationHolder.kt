package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.MapStyleOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.ResourceOptionsManager
import com.mapbox.maps.plugin.gestures.gestures
import one.mixin.android.BuildConfig
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.ItemChatLocationBinding
import one.mixin.android.event.ExpiredEvent
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.maxItemWidth
import one.mixin.android.extension.round
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.ui.conversation.holder.base.Terminable
import one.mixin.android.ui.conversation.location.MixinLatLng
import one.mixin.android.ui.conversation.location.MixinMapView
import one.mixin.android.ui.conversation.location.useMapbox
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSecret
import one.mixin.android.websocket.LocationPayload
import one.mixin.android.websocket.toLocationData

class LocationHolder constructor(val binding: ItemChatLocationBinding) :
    BaseViewHolder(binding.root),
    OnMapReadyCallback,
    Terminable {

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
            ResourceOptionsManager.getDefault(itemView.context, BuildConfig.MAPBOX_PUBLIC_TOKEN)
            val stub = binding.mapboxStub
            mapBoxView = stub.inflate() as MapView
        }
        mixinMapView = MixinMapView(binding.root.context, binding.googleMap, mapBoxView)
        mixinMapView.onCreate(null)
        if (useMapbox) {
            initMapbox(mapBoxView)
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

    private fun initMapbox(mapboxView: MapView?) {
        val mapboxMap = mapboxView?.getMapboxMap() ?: return

        mixinMapView.mapboxMap = mapboxMap
        mapboxMap.loadStyleUri(mixinMapView.getMapboxStyle())
        mapboxView.gestures.updateSettings {
            rotateEnabled = false
            rotateEnabled = false
            pitchEnabled = false
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
            binding.chatTime.translationY = dp4
        } else {
            (binding.locationBottom.layoutParams as ViewGroup.MarginLayoutParams).topMargin = 0
            binding.chatTime.setBackgroundResource(0)
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

        binding.chatTime.load(
            isMe,
            messageItem.createdAt,
            messageItem.status,
            messageItem.isPin ?: false,
            isRepresentative = isRepresentative,
            isSecret = messageItem.isSecret(),
            isWhite = location?.name == null && location?.address == null
        )

        if (isFirst && !isMe) {
            binding.chatName.visibility = View.VISIBLE
            binding.chatName.text = messageItem.userFullName
            if (messageItem.appId != null) {
                binding.chatName.setCompoundDrawables(null, null, botIcon, null)
                binding.chatName.compoundDrawablePadding = 3.dp
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
            binding.chatName.compoundDrawablePadding = 3.dp
        } else {
            binding.chatName.setCompoundDrawables(null, null, null, null)
        }
        chatJumpLayout(binding.chatJump, isMe, messageItem.expireIn, R.id.chat_layout)
        chatLayout(isMe, isLast)
    }

    override fun onRead(messageItem: MessageItem) {
        if (messageItem.expireIn != null) {
            RxBus.publish(ExpiredEvent(messageItem.messageId, messageItem.expireIn))
        }
    }
}
