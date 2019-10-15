package one.mixin.android.ui.media

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_transaction_header.view.*
import one.mixin.android.R
import one.mixin.android.extension.timeAgoDay

class MediaHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(time: String) {
        itemView.date_tv.timeAgoDay(time, itemView.context.getString(R.string.media_date_patten))
    }
}
