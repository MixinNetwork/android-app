package one.mixin.android.ui.search.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_search_header.view.*

class HeaderHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    fun bind(text: String, showMore: Boolean, action: () -> Unit) {
        itemView.search_header_tv.text = text
        itemView.search_header_more.visibility = if (showMore) View.VISIBLE else View.GONE
        itemView.search_header_more.setOnClickListener {
            action.invoke()
        }
    }
}