package one.mixin.android.ui.conversation.location

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_location.view.*
import kotlinx.android.synthetic.main.item_search_location.view.title
import one.mixin.android.R
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.vo.Location
import one.mixin.android.vo.foursquare.Venues

class LocationSearchAdapter(val callback: (Location) -> Unit) : RecyclerView.Adapter<VenueHolder>() {
    var venues: List<Venues>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VenueHolder {
        return LayoutInflater.from(parent.context).inflate(R.layout.item_search_location, parent, false).run {
            VenueHolder(this)
        }
    }

    override fun getItemCount(): Int = venues.notNullWithElse({ it.size }, 0)

    override fun onBindViewHolder(holder: VenueHolder, position: Int) {
        val venue = venues?.get(position)
        holder.itemView.title.text = venue?.name
        holder.itemView.sub_title.text = venue?.location?.address ?: venue?.location?.formattedAddress?.toString()
        // holder.itemView.location_icon.loadImage(venue?.getImageUrl())
        holder.itemView.setOnClickListener {
            venue ?: return@setOnClickListener
            callback(Location(venue.location.lat, venue.location.lng))
        }
    }
}
