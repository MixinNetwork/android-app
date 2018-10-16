package one.mixin.android.ui.wallet.adapter

import android.annotation.SuppressLint
import android.content.Context
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_wallet_asset.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.ui.common.recyclerview.HeaderListUpdateCallback
import one.mixin.android.vo.AssetItem
import org.jetbrains.anko.textColorResource
import java.math.BigDecimal

class AssetAdapter(private val rv: RecyclerView) : HeaderAdapter<AssetItem>() {

    fun setAssetList(newAssets: List<AssetItem>) {
        if (data == null) {
            data = newAssets
            notifyItemRangeInserted(0, newAssets.size)
        } else {
            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val old = data!![oldItemPosition]
                    val new = newAssets[newItemPosition]
                    return old.assetId == new.assetId
                }

                override fun getOldListSize() = data!!.size

                override fun getNewListSize() = newAssets.size

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val old = data!![oldItemPosition]
                    val new = newAssets[newItemPosition]
                    return old == new
                }
            })
            data = newAssets
            val recyclerViewState = rv.layoutManager?.onSaveInstanceState()
            if (headerView != null) {
                diffResult.dispatchUpdatesTo(HeaderListUpdateCallback(this))
            } else {
                diffResult.dispatchUpdatesTo(this)
            }
            rv.layoutManager?.onRestoreInstanceState(recyclerViewState)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NormalHolder) {
            val asset = data!![getPos(position)]
            val ctx = holder.itemView.context
            holder.itemView.balance.text = asset.balance.numberFormat8() + " " + asset.symbol
            holder.itemView.balance_as.text = ctx.getString(R.string.wallet_unit_usd, "≈ ${asset.usd().numberFormat2()}")
            if (asset.priceUsd == "0") {
                holder.itemView.price_tv.setText(R.string.asset_none)
                holder.itemView.change_tv.visibility = GONE
            } else {
                holder.itemView.change_tv.visibility = VISIBLE
                holder.itemView.price_tv.text = "$${asset.priceUsd.numberFormat()}"
                if (asset.changeUsd.isNotEmpty()) {
                    val changeUsd = BigDecimal(asset.changeUsd)
                    val isPositive = changeUsd > BigDecimal.ZERO
                    val t = "${(changeUsd * BigDecimal(100)).numberFormat2()}%"
                    holder.itemView.change_tv.text = if (isPositive) "+$t" else t
                    holder.itemView.change_tv.textColorResource = if (isPositive) R.color.colorGreen else R.color.colorRed
                }
            }
            holder.itemView.avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            holder.itemView.avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            holder.itemView.setOnClickListener { onItemListener?.onNormalItemClick(asset) }
        }
    }

    override fun getNormalViewHolder(context: Context, parent: ViewGroup): NormalHolder =
        NormalHolder(LayoutInflater.from(context).inflate(R.layout.item_wallet_asset, parent, false))
}
