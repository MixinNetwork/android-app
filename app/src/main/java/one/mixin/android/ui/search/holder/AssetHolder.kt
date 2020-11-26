package one.mixin.android.ui.search.holder

import android.annotation.SuppressLint
import android.view.View
import kotlinx.android.synthetic.main.item_search_asset.view.*

import one.mixin.android.R
import one.mixin.android.extension.highLight
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Fiats
import org.jetbrains.anko.textColorResource
import java.math.BigDecimal

class AssetHolder constructor(containerView: View) : NormalHolder(containerView) {
    @SuppressLint("SetTextI18n")
    fun bind(asset: AssetItem, target: String, onItemClickListener: SearchFragment.OnSearchClickListener?) {
        itemView.avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        itemView.avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        itemView.setOnClickListener { onItemClickListener?.onAsset(asset) }

        itemView.balance.text = asset.balance.numberFormat8() + " " + asset.symbol
        itemView.balance.highLight(target)
        itemView.balance_as.text = "≈ ${Fiats.getSymbol()}${asset.fiat().numberFormat2()}"
        if (asset.priceUsd == "0") {
            itemView.price_tv.setText(R.string.asset_none)
            itemView.change_tv.visibility = View.GONE
        } else {
            itemView.change_tv.visibility = View.VISIBLE
            itemView.price_tv.text = "${Fiats.getSymbol()}${asset.priceFiat().priceFormat()}"
            if (asset.changeUsd.isNotEmpty()) {
                val changeUsd = BigDecimal(asset.changeUsd)
                val isPositive = changeUsd > BigDecimal.ZERO
                val t = "${(changeUsd * BigDecimal(100)).numberFormat2()}%"
                itemView.change_tv.text = if (isPositive) "+$t" else t
                itemView.change_tv.textColorResource = if (isPositive) R.color.colorGreen else R.color.colorRed
            }
        }
    }
}
