package one.mixin.android.ui.wallet.adapter

import android.annotation.SuppressLint
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemWalletSearchBinding
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.setQuoteText
import one.mixin.android.util.getChainNetwork
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal

abstract class ItemViewHolder(val binding: ItemWalletSearchBinding) : RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bindView(
        assetId: String,
        iconUrl: String,
        chainIconUrl: String?,
        chainId: String,
        name: String,
        symbol: String,
        balance: String,
        assetKey: String?,
        priceUsd: String,
        changeUsd: String,
        priceFiat: BigDecimal,
        collectionHash: String?,
    ) {
        binding.badgeCircleIv.loadToken(iconUrl, chainIconUrl, collectionHash)
        binding.nameTv.text = name
        binding.balanceTv.maxWidth = binding.root.resources.displayMetrics.widthPixels / 2
        binding.balanceTv.text = "$balance $symbol"
        val chainNetwork = getChainNetwork(assetId, chainId, assetKey)
        binding.networkTv.isVisible = chainNetwork != null && collectionHash.isNullOrEmpty()
        if (chainNetwork != null) {
            binding.networkTv.text = chainNetwork
        }
        if (priceUsd == "0") {
            binding.priceTv.setText(R.string.NA)
            updatePriceAsCentered()
            binding.changeTv.visibility = View.GONE
        } else {
            updatePriceAsNormal()
            binding.priceTv.text = "${Fiats.getSymbol()}${priceFiat.priceFormat()}"
            if (changeUsd.isNotEmpty()) {
                val bigChangeUsd = BigDecimal(changeUsd)
                val isRising = bigChangeUsd >= BigDecimal.ZERO
                binding.changeTv.setQuoteText("${(bigChangeUsd * BigDecimal(100)).numberFormat2()}%", isRising)
                binding.changeTv.visibility = View.VISIBLE
            } else{
                binding.changeTv.visibility = View.GONE
            }
        }
    }

    private fun updatePriceAsCentered() {
        val layoutParams: RelativeLayout.LayoutParams = binding.priceTv.layoutParams as? RelativeLayout.LayoutParams ?: return
        layoutParams.removeRule(RelativeLayout.ALIGN_BASELINE)
        layoutParams.removeRule(RelativeLayout.BELOW)
        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL)
        layoutParams.topMargin = 0
        binding.priceTv.layoutParams = layoutParams
    }

    private fun updatePriceAsNormal() {
        val layoutParams: RelativeLayout.LayoutParams = binding.priceTv.layoutParams as? RelativeLayout.LayoutParams ?: return
        layoutParams.removeRule(RelativeLayout.CENTER_VERTICAL)
        layoutParams.addRule(RelativeLayout.ALIGN_BASELINE, R.id.balance_tv)
        layoutParams.topMargin = getPriceTopMarginPx()
        binding.priceTv.layoutParams = layoutParams
    }

    private fun getPriceTopMarginPx(): Int {
        val density: Float = binding.root.resources.displayMetrics.density
        return (PRICE_TOP_MARGIN_DP * density).toInt()
    }

    private companion object {
        private const val PRICE_TOP_MARGIN_DP: Int = 2
    }
}

class AssetHolder(binding: ItemWalletSearchBinding) : ItemViewHolder(binding) {
    fun bind(
        asset: TokenItem,
        callback: WalletSearchCallback? = null,
        currentAssetId: String? = null,
    ) {
        bindView(
            asset.assetId,
            asset.iconUrl,
            asset.chainIconUrl,
            asset.chainId,
            asset.name,
            asset.symbol,
            asset.balance.numberFormat8(),
            asset.assetKey,
            asset.priceUsd,
            asset.changeUsd,
            asset.priceFiat(),
            asset.collectionHash,
        )
        binding.priceTv.isVisible = currentAssetId == null
        if (currentAssetId != null) {
            binding.changeTv.isVisible = false
        }
        binding.checkIv.isVisible = asset.assetId == currentAssetId
        itemView.setOnClickListener {
            callback?.onAssetClick(asset.assetId, asset)
        }
    }
}

interface WalletSearchCallback {
    fun onAssetClick(
        assetId: String,
        tokenItem: TokenItem? = null,
    )
}
