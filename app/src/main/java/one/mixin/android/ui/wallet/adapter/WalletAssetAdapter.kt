package one.mixin.android.ui.wallet.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_wallet_asset.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.common.recyclerview.HeaderFooterAdapter
import one.mixin.android.ui.common.recyclerview.HeaderListUpdateCallback
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Fiats
import org.jetbrains.anko.textColorResource
import java.math.BigDecimal

class WalletAssetAdapter(private val slideShow: Boolean) : HeaderFooterAdapter<AssetItem>() {

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
            if (headerView != null) {
                diffResult.dispatchUpdatesTo(HeaderListUpdateCallback(this))
            } else {
                diffResult.dispatchUpdatesTo(this)
            }
        }
    }

    fun removeItem(pos: Int): AssetItem? {
        val list = data?.toMutableList()
        val addr = list?.removeAt(getPosition(pos))
        data = list
        notifyItemRemoved(pos)
        return addr
    }

    fun restoreItem(item: AssetItem, pos: Int) {
        val list = data?.toMutableList()
        list?.add(getPosition(pos), item)
        data = list
        notifyItemInserted(pos)
    }

    fun getPosition(pos: Int) = getPos(pos)

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NormalHolder) {
            val asset = data!![getPos(position)]
            holder.itemView.balance.text = try {
                if (asset.balance.numberFormat8().toFloat() == 0f) {
                    "0.00"
                } else {
                    asset.balance.numberFormat8()
                }
            } catch (ignored: NumberFormatException) {
                asset.balance.numberFormat8()
            }
            holder.itemView.symbol_tv.text = asset.symbol
            holder.itemView.balance_as.text = "≈ ${Fiats.getSymbol()}${asset.fiat().numberFormat2()}"
            if (asset.priceUsd == "0") {
                holder.itemView.price_tv.setText(R.string.asset_none)
                holder.itemView.change_tv.visibility = GONE
            } else {
                holder.itemView.change_tv.visibility = VISIBLE
                holder.itemView.price_tv.text = "${Fiats.getSymbol()}${asset.priceFiat().priceFormat()}"
                if (asset.changeUsd.isNotEmpty()) {
                    val changeUsd = BigDecimal(asset.changeUsd)
                    val isPositive = changeUsd > BigDecimal.ZERO
                    holder.itemView.change_tv.text = "${(changeUsd * BigDecimal(100)).numberFormat2()}%"
                    holder.itemView.change_tv.textColorResource = if (isPositive) R.color.wallet_green else R.color.wallet_pink
                }
            }
            holder.itemView.back_left_tv.setText(if (slideShow) R.string.shown else R.string.hidden)
            holder.itemView.back_right_tv.setText(if (slideShow) R.string.shown else R.string.hidden)
            holder.itemView.avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            holder.itemView.avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            holder.itemView.setOnClickListener { onItemListener?.onNormalItemClick(asset) }
        }
    }

    override fun getNormalViewHolder(context: Context, parent: ViewGroup): NormalHolder =
        NormalHolder(LayoutInflater.from(context).inflate(R.layout.item_wallet_asset, parent, false))
}
