package one.mixin.android.ui.conversation.location

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_location.view.*
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.vo.foursquare.Venue
import one.mixin.android.vo.foursquare.getImageUrl
import one.mixin.android.vo.foursquare.getVenueType
import one.mixin.android.websocket.LocationPayload

class LocationAdapter(val currentCallback: () -> Unit, val callback: (LocationPayload) -> Unit) : RecyclerView.Adapter<VenueHolder>() {
    var venues: List<Venue>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var accurate: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VenueHolder {
        return LayoutInflater.from(parent.context).inflate(R.layout.item_location, parent, false).run {
            VenueHolder(this)
        }
    }

    override fun getItemCount(): Int = venues.notNullWithElse({ it.size + 1 }, 1)

    override fun onBindViewHolder(holder: VenueHolder, position: Int) {
        if (position == 0) {
            holder.itemView.title.setText(R.string.location_send_current_location)
            holder.itemView.sub_title.text = accurate
            holder.itemView.location_icon.setBackgroundResource(R.drawable.ic_current_location)
            holder.itemView.location_icon.setImageDrawable(null)
            holder.itemView.location_icon.imageTintList = null
            holder.itemView.setOnClickListener {
                currentCallback()
            }
        } else {
            val venue = venues?.get(position - 1)
            holder.itemView.title.text = venue?.name
            holder.itemView.location_icon.loadImage(venue?.getImageUrl())
            holder.itemView.location_icon.setBackgroundResource(R.drawable.bg_menu)
            holder.itemView.location_icon.imageTintList = ColorStateList.valueOf(holder.itemView.context.colorFromAttribute(R.attr.icon_default))
            holder.itemView.sub_title.text = venue?.location?.address ?: venue?.location?.formattedAddress?.get(0)
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
}
