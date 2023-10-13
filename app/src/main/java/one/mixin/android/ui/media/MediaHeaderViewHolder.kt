package one.mixin.android.ui.media

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemTransactionHeaderBinding
import one.mixin.android.extension.timeAgoDay

class MediaHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val binding = ItemTransactionHeaderBinding.bind(itemView)
    fun bind(time: String) {
        binding.dateTv.timeAgoDay(time, itemView.context.getString(R.string.date_format_date))
    }
}
