package one.mixin.android.ui.search.holder

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_search_asset.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import one.mixin.android.R
import one.mixin.android.extension.highLight
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.AssetItem
import org.jetbrains.anko.textColorResource
import java.math.BigDecimal

class AssetHolder constructor(containerView: View) :NormalHolder(containerView) {

    @SuppressLint("SetTextI18n")
    fun bind(asset: AssetItem, target: String, onItemClickListener: SearchFragment.OnSearchClickListener?, isEnd: Boolean = false) {
        itemView.ph1.isVisible = isEnd
        itemView.ph2.isVisible = isEnd
        itemView.balance.text = try {
            if (asset.balance.numberFormat8().toFloat() == 0f) {
                "0.00"
            } else {
                asset.balance.numberFormat8()
            }
        } catch (ignored: NumberFormatException) {
            asset.balance.numberFormat8()
        }
        itemView.balance.highLight(target)
        itemView.symbol_tv.text = asset.symbol
        itemView.balance_as.text = "≈ $${asset.usd().numberFormat2()}"
        if (asset.priceUsd == "0") {
            itemView.price_tv.setText(R.string.asset_none)
            itemView.change_tv.visibility = View.GONE
        } else {
            itemView.change_tv.visibility = View.VISIBLE
            itemView.price_tv.text = "$${asset.priceUsd.numberFormat()}"
            if (asset.changeUsd.isNotEmpty()) {
                val changeUsd = BigDecimal(asset.changeUsd)
                val isPositive = changeUsd > BigDecimal.ZERO
                itemView.change_tv.text = "${(changeUsd * BigDecimal(100)).numberFormat2()}%"
                itemView.change_tv.textColorResource = if (isPositive) R.color.wallet_green else R.color.wallet_pink
            }
        }
        itemView.avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        itemView.avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        itemView.setOnClickListener { onItemClickListener?.onAsset(asset) }
    }
}