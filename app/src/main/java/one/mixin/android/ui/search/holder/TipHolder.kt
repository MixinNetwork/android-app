package one.mixin.android.ui.search.holder

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_search_tip.view.*
import one.mixin.android.R
import one.mixin.android.extension.highLight
import one.mixin.android.ui.search.SearchFragment

class TipHolder(containerView: View) : RecyclerView.ViewHolder(containerView) {

    fun bind(target: String, searching: Boolean, onItemClickListener: SearchFragment.OnSearchClickListener?, isLast: Boolean) {
        if (isLast) {
            itemView.bottom_divider.setBackgroundResource(R.drawable.ic_shadow_bottom)
        } else {
            itemView.bottom_divider.setBackgroundResource(R.drawable.ic_shadow_divider)
        }
        itemView.search_tip_tv.text = itemView.context.getString(R.string.search_tip, target)
        itemView.search_tip_tv.highLight(target)
        if (searching) {
            itemView.pb.isVisible = true
            itemView.search_tip_tv.isEnabled = false
        } else {
            itemView.pb.isVisible = false
            itemView.search_tip_tv.isEnabled = true
        }
        itemView.search_tip_tv.setOnClickListener {
            onItemClickListener?.onTipClick()
        }
    }
}

class TipItem