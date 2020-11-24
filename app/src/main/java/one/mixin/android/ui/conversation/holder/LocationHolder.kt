package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.item_chat_location.view.*
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.isGooglePlayServicesAvailable
import one.mixin.android.extension.maxItemWidth
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSignal
import one.mixin.android.websocket.LocationPayload
import one.mixin.android.websocket.toLocationData
import org.jetbrains.anko.dip
import org.jetbrains.anko.textColorResource

class LocationHolder constructor(containerView: View) : BaseViewHolder(containerView), OnMapReadyCallback {
    private val dp16 = itemView.context.dpToPx(16f)

    private lateinit var map: GoogleMap
    private var onResumeCalled = false

    companion object {
        val isGooglePlayServicesAvailable by lazy { MixinApplication.appContext.isGooglePlayServicesAvailable() }
    }

    private val dp36 by lazy {
        36.dp
    }

    private val dp4 by lazy {
        MixinApplication.appContext.dpToPx(4f).toFloat()
    }

    init {
        itemView.chat_name.maxWidth = itemView.context.maxItemWidth() - dp16
        itemView.location_layout.round(6.dp)
        itemView.location_map.onCreate(null)
        itemView.location_map.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        MapsInitializer.initialize(MixinApplication.appContext)
        map = googleMap
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
            itemView.location_map.onResume()
        }
        setMapLocation()
    }

    private fun setMapLocation() {
        if (!::map.isInitialized) return
        if (itemView.tag == location.hashCode()) return
        itemView.tag = location.hashCode()
        onResumeCalled = true
        map.clear()
        itemView.location_map.onResume()
        location?.let { data ->
            val position = LatLng(data.latitude, data.longitude)
            with(map) {
                mapType = GoogleMap.MAP_TYPE_NORMAL
                addMarker(MarkerOptions().position(position).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map_marker)))
                moveCamera(CameraUpdateFactory.newLatLngZoom(position, 13f))
            }
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        val lp = (itemView.chat_msg_layout.layoutParams as FrameLayout.LayoutParams)
        if (isMe) {
            lp.gravity = Gravity.END
            if (isLast) {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_reply_me_last,
                    R.drawable.chat_bubble_reply_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_reply_me,
                    R.drawable.chat_bubble_reply_me_night
                )
            }
            (itemView.chat_time.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
        } else {
            lp.gravity = Gravity.START
            if (isLast) {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_reply_other_last,
                    R.drawable.chat_bubble_reply_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_reply_other,
                    R.drawable.chat_bubble_reply_other_night
                )
            }
            (itemView.chat_time.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = dp3
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
            itemView.location_sub_title.isVisible = false
            itemView.location_title.visibility = View.INVISIBLE
        } else {
            itemView.location_sub_title.isVisible = true
            itemView.location_title.isVisible = true
            itemView.location_title.text = location?.name
            itemView.location_sub_title.text = location?.address
        }
        if (location?.name == null && location?.address == null) {
            (itemView.location_bottom.layoutParams as ViewGroup.MarginLayoutParams).topMargin = -dp36
            itemView.chat_time.setBackgroundResource(R.drawable.bg_bubble_shadow)
            itemView.chat_time.textColorResource = R.color.white
            itemView.chat_time.translationY = dp4
        } else {
            (itemView.location_bottom.layoutParams as ViewGroup.MarginLayoutParams).topMargin = 0
            itemView.chat_time.setBackgroundResource(0)
            itemView.chat_time.textColorResource = (R.color.color_chat_date)
            itemView.chat_time.translationY = 0f
        }
        if (isGooglePlayServicesAvailable) {
            itemView.location_holder.isVisible = false
            itemView.location_map.isVisible = true
            setMapLocation()
        } else {
            itemView.location_holder.isVisible = true
            itemView.location_map.isVisible = false
        }
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
        itemView.location_layout.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onLocationClick(messageItem)
            }
        }
        itemView.chat_layout.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onLocationClick(messageItem)
            }
        }
        itemView.location_layout.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }
        itemView.chat_layout.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }
        val isMe = meId == messageItem.userId

        itemView.chat_time.timeAgoClock(messageItem.createdAt)
        setStatusIcon(isMe, messageItem.status, messageItem.isSignal(), isRepresentative, location?.name == null && location?.address == null) { statusIcon, secretIcon, representativeIcon ->
            statusIcon?.setBounds(0, 0, dp12, dp12)
            secretIcon?.setBounds(0, 0, dp8, dp8)
            representativeIcon?.setBounds(0, 0, dp8, dp8)
            TextViewCompat.setCompoundDrawablesRelative(itemView.chat_time, secretIcon ?: representativeIcon, null, statusIcon, null)
        }

        if (isFirst && !isMe) {
            itemView.chat_name.visibility = View.VISIBLE
            itemView.chat_name.text = messageItem.userFullName
            if (messageItem.appId != null) {
                itemView.chat_name.setCompoundDrawables(null, null, botIcon, null)
                itemView.chat_name.compoundDrawablePadding = itemView.dip(3)
            } else {
                itemView.chat_name.setCompoundDrawables(null, null, null, null)
            }
            itemView.chat_name.setTextColor(getColorById(messageItem.userId))
            itemView.chat_name.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            itemView.chat_name.visibility = View.GONE
        }

        if (messageItem.appId != null) {
            itemView.chat_name.setCompoundDrawables(null, null, botIcon, null)
            itemView.chat_name.compoundDrawablePadding = itemView.dip(3)
        } else {
            itemView.chat_name.setCompoundDrawables(null, null, null, null)
        }

        chatLayout(isMe, isLast)
    }
}
