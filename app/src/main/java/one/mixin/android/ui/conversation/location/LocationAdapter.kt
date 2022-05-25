package one.mixin.android.ui.conversation.location

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemLocationBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.vo.foursquare.Venue
import one.mixin.android.vo.foursquare.getImageUrl
import one.mixin.android.vo.foursquare.getVenueType
import one.mixin.android.websocket.LocationPayload

class LocationAdapter(val currentCallback: () -> Unit, val callback: (LocationPayload) -> Unit) : RecyclerView.Adapter<VenueHolder>() {
    var venues: List<Venue>? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var accurate: String? = null
        @SuppressLint("NotifyDataSetChanged")
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
        val binding = ItemLocationBinding.bind(holder.itemView)
        if (position == 0) {
            binding.title.setText(R.string.Send_your_Current_Location)
            binding.subTitle.text = accurate
            binding.locationIcon.setBackgroundResource(R.drawable.ic_current_location)
            binding.locationIcon.setImageDrawable(null)
            binding.locationIcon.imageTintList = null
            holder.itemView.setOnClickListener {
                currentCallback()
            }
        } else {
            val venue = venues?.get(position - 1)
            binding.title.text = venue?.name
            binding.locationIcon.loadImage(venue?.getImageUrl())
            binding.locationIcon.setBackgroundResource(R.drawable.bg_menu)
            binding.locationIcon.imageTintList = ColorStateList.valueOf(holder.itemView.context.colorFromAttribute(R.attr.icon_default))
            binding.subTitle.text = venue?.location?.address ?: venue?.location?.formattedAddress?.get(0)
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
