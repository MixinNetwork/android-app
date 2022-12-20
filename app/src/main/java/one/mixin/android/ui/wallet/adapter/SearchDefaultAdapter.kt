package one.mixin.android.ui.wallet.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.ItemContactHeaderBinding
import one.mixin.android.databinding.ItemWalletSearchBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.textColorResource
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.TopAssetItem
import java.math.BigDecimal

class SearchDefaultAdapter : RecyclerView.Adapter<ItemViewHolder>(), StickyRecyclerHeadersAdapter<SearchDefaultAdapter.HeaderViewHolder> {
    companion object {
        const val TYPE_RECENT = 0
        const val TYPE_TOP = 1
    }

    var recentAssets: List<AssetItem>? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (value == field) return

            field = value
            notifyDataSetChanged()
        }

    var topAssets: List<TopAssetItem>? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (value == field) return

            field = value
            notifyDataSetChanged()
        }

    var callback: WalletSearchCallback? = null

    override fun getHeaderId(position: Int): Long = getItemViewType(position).toLong()

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        return HeaderViewHolder(ItemContactHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindHeaderViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind(getItemViewType(position) == TYPE_RECENT)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return if (viewType == TYPE_RECENT) {
            AssetHolder(ItemWalletSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            TopAssetHolder(ItemWalletSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        if (holder is AssetHolder) {
            recentAssets?.get(position)?.let { holder.bind(it, callback) }
        } else {
            holder as TopAssetHolder
            topAssets?.get(position - (recentAssets?.size ?: 0))?.let { holder.bind(it, callback) }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            recentAssets.isNullOrEmpty() -> TYPE_TOP
            topAssets.isNullOrEmpty() -> TYPE_RECENT
            position < recentAssets!!.size -> TYPE_RECENT
            else -> TYPE_TOP
        }
    }

    override fun getItemCount(): Int = (recentAssets?.size ?: 0) + (topAssets?.size ?: 0)

    class HeaderViewHolder(val binding: ItemContactHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(isRecent: Boolean) {
            binding.header.text = itemView.context.getString(
                if (isRecent) R.string.Recent_searches else R.string.Trending
            )
        }
    }
}

abstract class ItemViewHolder(val binding: ItemWalletSearchBinding) : RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bindView(
        assetId: String,
        iconUrl: String,
        chainIconUrl: String?,
        chainId: String,
        name: String,
        symbol: String,
        assetKey: String?,
        priceUsd: String,
        changeUsd: String,
        priceFiat: BigDecimal,
    ) {
        binding.badgeCircleIv.bg.loadImage(iconUrl, R.drawable.ic_avatar_place_holder)
        binding.badgeCircleIv.badge.loadImage(chainIconUrl, R.drawable.ic_avatar_place_holder)
        binding.nameTv.text = name
        binding.symbolTv.text = symbol
        val chainNetwork = getChainNetwork(assetId, chainId, assetKey)
        binding.networkTv.isVisible = chainNetwork != null
        if (chainNetwork != null) {
            binding.networkTv.text = chainNetwork
        }
        if (priceUsd == "0") {
            binding.priceTv.setText(R.string.NA)
            binding.changeTv.visibility = View.GONE
        } else {
            binding.changeTv.visibility = View.VISIBLE
            binding.priceTv.text = "${Fiats.getSymbol()}${priceFiat.priceFormat()}"
            if (changeUsd.isNotEmpty()) {
                val bigChangeUsd = BigDecimal(changeUsd)
                val isPositive = bigChangeUsd > BigDecimal.ZERO
                binding.changeTv.text = "${(bigChangeUsd * BigDecimal(100)).numberFormat2()}%"
                binding.changeTv.textColorResource = if (isPositive) R.color.wallet_green else R.color.wallet_pink
            }
        }
    }
}

class AssetHolder(binding: ItemWalletSearchBinding) : ItemViewHolder(binding) {
    fun bind(asset: AssetItem, callback: WalletSearchCallback? = null) {
        bindView(
            asset.assetId,
            asset.iconUrl,
            asset.chainIconUrl,
            asset.chainId,
            asset.name,
            asset.symbol,
            asset.assetKey,
            asset.priceUsd,
            asset.changeUsd,
            asset.priceFiat()
        )
        itemView.setOnClickListener {
            callback?.onAssetClick(asset.assetId, asset)
        }
    }
}

class TopAssetHolder(binding: ItemWalletSearchBinding) : ItemViewHolder(binding) {
    fun bind(asset: TopAssetItem, callback: WalletSearchCallback? = null) {
        bindView(
            asset.assetId,
            asset.iconUrl,
            asset.chainIconUrl,
            asset.chainId,
            asset.name,
            asset.symbol,
            asset.assetKey,
            asset.priceUsd,
            asset.changeUsd,
            asset.priceFiat()
        )
        itemView.setOnClickListener {
            callback?.onAssetClick(asset.assetId)
        }
    }
}

private val chainNetworks by lazy {
    mapOf(
        "43d61dcd-e413-450d-80b8-101d5e903357" to "ERC-20",
        "17f78d7c-ed96-40ff-980c-5dc62fecbc85" to "BEP-2",
        "1949e683-6a08-49e2-b087-d6b72398588f" to "BEP-20",
        "6cfe566e-4aad-470b-8c9a-2fd35b49c68d" to "EOS",
        "b7938396-3f94-4e0a-9179-d3440718156f" to "Polygon",
    )
}

private val bepChains by lazy {
    arrayOf(
        "17f78d7c-ed96-40ff-980c-5dc62fecbc85",
        "1949e683-6a08-49e2-b087-d6b72398588f",
    )
}

private fun getChainNetwork(assetId: String, chainId: String, assetKey: String?): String? {
    if (assetId == chainId && !bepChains.contains(chainId)) return null

    if (chainId == Constants.ChainId.TRON_CHAIN_ID) {
        return if (assetKey?.isDigitsOnly() == true) {
            "TRC-10"
        } else {
            "TRC-20"
        }
    }
    return chainNetworks[chainId]
}

interface WalletSearchCallback {
    fun onAssetClick(assetId: String, assetItem: AssetItem? = null)
}
