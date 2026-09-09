package one.mixin.android.ui.search.holder

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isVisible
import one.mixin.android.databinding.ItemSearchAssetBinding
import one.mixin.android.extension.highLight
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.setQuoteText
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.util.getChainNetwork
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.TokenGroup
import java.math.BigDecimal

class AssetHolder constructor(val binding: ItemSearchAssetBinding) : NormalHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(
        asset: TokenItem,
        target: String,
        onItemClickListener: SearchFragment.OnSearchClickListener?,
        group: TokenGroup? = null,
    ) {
        binding.avatar.loadToken(asset)
        binding.avatar.badge.isVisible = false
        binding.root.setOnClickListener { onItemClickListener?.onAssetClick(asset) }
        binding.balance.text = (group?.balance?.toPlainString() ?: asset.balance) + " " + asset.symbol
        binding.balance.highLight(target)
        binding.balanceAs.text = "≈ ${Fiats.getSymbol()}${(group?.fiat ?: asset.fiat()).numberFormat2()}"
        val chainNetwork = getChainNetwork(asset.assetId, asset.chainId, asset.assetKey)
        binding.networkTv.isVisible = chainNetwork != null
        if (chainNetwork != null) {
            binding.networkTv.text = chainNetwork
        }
        if (asset.priceUsd == "0") {
            binding.naTv.visibility = View.VISIBLE
            binding.priceTv.visibility = View.GONE
            binding.changeTv.visibility = View.GONE
        } else {
            binding.naTv.visibility = View.GONE
            binding.priceTv.visibility = View.VISIBLE
            binding.changeTv.visibility = View.VISIBLE
            binding.priceTv.text = "${Fiats.getSymbol()}${asset.priceFiat().priceFormat()}"
            if (asset.changeUsd.isNotEmpty()) {
                val changeUsd = BigDecimal(asset.changeUsd)
                val isPositive = changeUsd >= BigDecimal.ZERO
                val t = "${(changeUsd * BigDecimal(100)).numberFormat2()}%"
                binding.changeTv.setQuoteText(if (isPositive) "+$t" else t, isPositive)
            }
        }
    }
}
