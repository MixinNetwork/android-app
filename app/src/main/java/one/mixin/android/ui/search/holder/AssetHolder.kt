package one.mixin.android.ui.search.holder

import android.annotation.SuppressLint
import android.view.View
import one.mixin.android.R
import one.mixin.android.databinding.ItemSearchAssetBinding
import one.mixin.android.extension.highLight
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.textColorResource
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

class AssetHolder constructor(val binding: ItemSearchAssetBinding) : NormalHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(asset: AssetItem, target: String, onItemClickListener: SearchFragment.OnSearchClickListener?) {
        binding.avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        binding.avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        binding.root.setOnClickListener { onItemClickListener?.onAsset(asset) }

        binding.balance.text = asset.balance.numberFormat8() + " " + asset.symbol
        binding.balance.highLight(target)
        binding.balanceAs.text = "≈ ${Fiats.getSymbol()}${asset.fiat().numberFormat2()}"
        if (asset.priceUsd == "0") {
            binding.priceTv.setText(R.string.not_applicable)
            binding.changeTv.visibility = View.GONE
        } else {
            binding.changeTv.visibility = View.VISIBLE
            binding.priceTv.text = "${Fiats.getSymbol()}${asset.priceFiat().priceFormat()}"
            if (asset.changeUsd.isNotEmpty()) {
                val changeUsd = BigDecimal(asset.changeUsd)
                val isPositive = changeUsd > BigDecimal.ZERO
                val t = "${(changeUsd * BigDecimal(100)).numberFormat2()}%"
                binding.changeTv.text = if (isPositive) "+$t" else t
                binding.changeTv.textColorResource = if (isPositive) R.color.colorGreen else R.color.colorRed
            }
        }
    }
}
