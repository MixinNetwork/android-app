package one.mixin.android.ui.search.holder

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_search_asset.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import one.mixin.android.R
import one.mixin.android.extension.formatPrice
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.max8
import one.mixin.android.extension.numberFormat
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.AssetItem
import org.jetbrains.anko.textColorResource

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
        itemView.avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        itemView.avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        itemView.setOnClickListener { onItemClickListener?.onAsset(asset) }

        itemView.balance.text = asset.balance.max8().numberFormat() + " " + asset.symbol
        itemView.balance_as.text = itemView.context.getString(R.string.wallet_unit_usd, "≈ ${asset.usd().toString().formatPrice().numberFormat()}")
        if (asset.priceUsd == "0") {
            itemView.price_tv.text = "N/A"
            itemView.change_tv.visibility = View.GONE
        } else {
            itemView.change_tv.visibility = View.VISIBLE
            itemView.price_tv.text = "$${asset.priceUsd.formatPrice().numberFormat()}"
            if (asset.changeUsd.isNotEmpty()) {
                val isPositive = asset.changeUsd.toFloat() > 0
                val t = "${String.format("%.2f", asset.changeUsd.toFloat() * 100)}%"
                itemView.change_tv.text = if (isPositive) "+$t" else t
                itemView.change_tv.textColorResource = if (isPositive) R.color.colorGreen else R.color.colorRed
            }
        }
    }
}