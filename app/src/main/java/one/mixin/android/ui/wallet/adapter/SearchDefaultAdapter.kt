package one.mixin.android.ui.wallet.adapter

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import kotlinx.android.synthetic.main.item_contact_header.view.*
import kotlinx.android.synthetic.main.item_wallet_search.view.*
import kotlinx.android.synthetic.main.item_wallet_search.view.symbol_tv
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import one.mixin.android.R
import one.mixin.android.extension.inflate
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.priceFormat
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.TopAssetItem
import org.jetbrains.anko.textColorResource
import java.math.BigDecimal

class SearchDefaultAdapter : RecyclerView.Adapter<ItemViewHolder>(), StickyRecyclerHeadersAdapter<SearchDefaultAdapter.HeaderViewHolder> {
    companion object {
        const val TYPE_RECENT = 0
        const val TYPE_TOP = 1
    }

    var recentAssets: List<AssetItem>? = null
        set(value) {
            if (value == field) return

            field = value
            notifyDataSetChanged()
        }

    var topAssets: List<TopAssetItem>? = null
        set(value) {
            if (value == field) return

            field = value
            notifyDataSetChanged()
        }

    var callback: WalletSearchCallback? = null

    override fun getHeaderId(position: Int): Long = getItemViewType(position).toLong()

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        val view = parent.inflate(R.layout.item_contact_header, false)
        return HeaderViewHolder(view)
    }

    override fun onBindHeaderViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind(getItemViewType(position) == TYPE_RECENT)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = parent.inflate(R.layout.item_wallet_search, false)
        return if (viewType == TYPE_RECENT) {
            AssetHolder(view)
        } else {
            TopAssetHolder(view)
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

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(isRecent: Boolean) {
            itemView.header.text = itemView.context.getString(
                if (isRecent) R.string.wallet_recent_search else R.string.wallet_trending
            )
        }
    }
}

abstract class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    @SuppressLint("SetTextI18n")
    fun bindView(
        assetId: String,
        iconUrl: String,
        chainIconUrl: String?,
        name: String,
        symbol: String,
        priceUsd: String,
        changeUsd: String,
        priceFiat: BigDecimal
    ) {
        itemView.badge_circle_iv.bg.loadImage(iconUrl, R.drawable.ic_avatar_place_holder)
        itemView.badge_circle_iv.badge.loadImage(chainIconUrl, R.drawable.ic_avatar_place_holder)
        itemView.name_tv.text = name
        itemView.symbol_tv.text = symbol
        if (priceUsd == "0") {
            itemView.price_tv.setText(R.string.asset_none)
            itemView.change_tv.visibility = View.GONE
        } else {
            itemView.change_tv.visibility = View.VISIBLE
            itemView.price_tv.text = "${Fiats.getSymbol()}${priceFiat.priceFormat()}"
            if (changeUsd.isNotEmpty()) {
                val bigChangeUsd = BigDecimal(changeUsd)
                val isPositive = bigChangeUsd > BigDecimal.ZERO
                itemView.change_tv.text = "${(bigChangeUsd * BigDecimal(100)).numberFormat2()}%"
                itemView.change_tv.textColorResource = if (isPositive) R.color.wallet_green else R.color.wallet_pink
            }
        }
    }
}

class AssetHolder(itemView: View) : ItemViewHolder(itemView) {
    fun bind(asset: AssetItem, callback: WalletSearchCallback? = null) {
        bindView(asset.assetId, asset.iconUrl, asset.chainIconUrl, asset.name, asset.symbol, asset.priceUsd, asset.changeUsd, asset.priceFiat())
        itemView.setOnClickListener {
            callback?.onAssetClick(asset.assetId, asset)
        }
    }
}

class TopAssetHolder(itemView: View) : ItemViewHolder(itemView) {
    fun bind(asset: TopAssetItem, callback: WalletSearchCallback? = null) {
        bindView(asset.assetId, asset.iconUrl, asset.chainIconUrl, asset.name, asset.symbol, asset.priceUsd, asset.changeUsd, asset.priceFiat())
        itemView.setOnClickListener {
            callback?.onAssetClick(asset.assetId)
        }
    }
}

interface WalletSearchCallback {
    fun onAssetClick(assetId: String, assetItem: AssetItem? = null)
}
