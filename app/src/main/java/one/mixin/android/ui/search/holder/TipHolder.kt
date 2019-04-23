package one.mixin.android.ui.search.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_search_tip.view.*
import one.mixin.android.R
import one.mixin.android.extension.highLight
import one.mixin.android.ui.search.SearchFragment

class TipHolder(containerView: View) : RecyclerView.ViewHolder(containerView) {

    fun bind(target: String, onItemClickListener: SearchFragment.OnSearchClickListener?) {
        itemView.search_tip_tv.text = itemView.context.getString(R.string.search_tip, target)
        itemView.search_tip_tv.highLight(target)
        itemView.search_tip_tv.setOnClickListener {
            onItemClickListener?.onTipClick()
        }
    }
}

class TipItem