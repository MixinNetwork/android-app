package one.mixin.android.ui.search.holder

import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_search_header.view.*

class HeaderHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    fun bind(text: String) {
        itemView.search_header_tv.text = text
    }
}