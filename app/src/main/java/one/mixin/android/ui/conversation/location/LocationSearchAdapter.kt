package one.mixin.android.ui.conversation.location

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_location.view.*
import one.mixin.android.R
import one.mixin.android.extension.highLight
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.vo.foursquare.Venue
import one.mixin.android.vo.foursquare.getImageUrl
import one.mixin.android.vo.foursquare.getVenueType
import one.mixin.android.websocket.LocationPayload

class LocationSearchAdapter(val callback: (LocationPayload) -> Unit) : RecyclerView.Adapter<VenueHolder>() {
    var venues: List<Venue>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var keyword: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun setMark(index: Float = -1f) {
        if (index < 0) {
            currentVenues = null
            return
        }
        index.toInt().let { i ->
            if (i < venues?.size!!) {
                currentVenues = venues!![i]
            }
        }
    }

    private var currentVenues: Venue? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VenueHolder {
        return LayoutInflater.from(parent.context).inflate(R.layout.item_location, parent, false).run {
            VenueHolder(this)
        }
    }

    override fun getItemCount(): Int = venues.notNullWithElse(
        {
            it.size + if (currentVenues == null) {
                0
            } else {
                1
            }
        },
        if (currentVenues == null) {
            0
        } else {
            1
        }
    )

    override fun getItemViewType(position: Int): Int {
        if (currentVenues == null) {
            return 0
        } else if (position == 0) {
            return 1
        } else {
            return 0
        }
    }

    private fun getItem(position: Int): Venue? {
        return when {
            currentVenues == null -> {
                venues?.get(position)
            }
            position == 0 -> {
                currentVenues
            }
            else -> {
                venues?.get(position - 1)
            }
        }
    }

    override fun onBindViewHolder(holder: VenueHolder, position: Int) {
        val venue = getItem(position)
        if (getItemViewType(position) == 1) {
            holder.itemView.title.setText(R.string.location_send_current_location)
            holder.itemView.sub_title.text = venue?.name
            holder.itemView.location_icon.setBackgroundResource(R.drawable.ic_current_location)
            holder.itemView.location_icon.setImageDrawable(null)
            holder.itemView.location_icon.imageTintList = null
            holder.itemView.setOnClickListener {
                venue ?: return@setOnClickListener
                LocationPayload(
                    venue.location.lat,
                    venue.location.lng,
                    venue.name,
                    venue.location.address,
                    venue.getVenueType()
                )
            }
            return
        }
        holder.itemView.title.text = venue?.name
        if (keyword != null) {
            holder.itemView.title.highLight(keyword)
        }
        holder.itemView.sub_title.text = venue?.location?.address ?: venue?.location?.formattedAddress?.get(0)
        holder.itemView.location_icon.loadImage(venue?.getImageUrl())
        holder.itemView.location_icon.setBackgroundResource(R.drawable.bg_menu)
        holder.itemView.setOnClickListener {
            venue ?: return@setOnClickListener
            callback(
                LocationPayload(
                    venue.location.lat,
                    venue.location.lng,
                    venue.name,
                    venue.location.address ?: venue.location.formattedAddress?.get(0),
                    venue.getVenueType()
                )
            )
        }
    }
}
