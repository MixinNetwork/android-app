package one.mixin.android.ui.conversation.location

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_location.view.*
import one.mixin.android.R
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.vo.Location
import one.mixin.android.vo.foursquare.Venues

class LocationAdapter(val callback: (Location) -> Unit) : RecyclerView.Adapter<LocationHolder>() {
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
        val venue = venues?.get(position)
        holder.itemView.title.text = venue?.name
        holder.itemView.setOnClickListener {
            venue ?: return@setOnClickListener
            callback(Location(venue.location.lat, venue.location.lng))
        }
    }
}
