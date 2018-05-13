package one.mixin.android.ui.search.holder

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_search_asset.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.AssetItem

class AssetHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    fun bind(user: AssetItem, onItemClickListener: SearchFragment.OnSearchClickListener?) {
        bind(user, onItemClickListener, false)
    }

    @SuppressLint("SetTextI18n")
    fun bind(asset: AssetItem, onItemClickListener: SearchFragment.OnSearchClickListener?, isEnd: Boolean) {
        if (isEnd) {
            itemView.divider.visibility = View.GONE
        } else {
            itemView.divider.visibility = View.VISIBLE
        }
        itemView.name.text = asset.name
        itemView.balance.text = asset.balance + " " + asset.symbol
        itemView.usd.text = itemView.context.getString(R.string.wallet_unit_usd,
            "≈ ${String.format("%.2f", asset.usd())}")
        itemView.avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        itemView.avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        itemView.setOnClickListener { onItemClickListener?.onAsset(asset) }
    }
}