package one.mixin.android.ui.panel.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_panel_transfer_asset.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.vo.AssetItem

internal class PanelTransferAssetAdapter : ListAdapter<AssetItem, PanelTransferAssetAdapter.ItemHolder>(AssetItem.DIFF_CALLBACK) {

    var currentAsset: AssetItem? = null
        set(value) {
            if (value == field) return
            field = value
            notifyDataSetChanged()
        }
    var onAssetListener: OnAssetListener? = null

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        val asset = getItem(position)
        currentAsset?.let {
            holder.bind(asset, onAssetListener, it)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
        ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_panel_transfer_asset, parent, false))

    internal class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(asset: AssetItem, listener: OnAssetListener? = null, currentAsset: AssetItem) {
            itemView.badge_circle_iv.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            itemView.badge_circle_iv.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            itemView.name_tv.text = itemView.context.getString(R.string.panel_transfer_asset_balance, asset.balance.numberFormat())
            itemView.symbol_tv.text = asset.name
            itemView.checked_iv.visibility = if (asset.assetId == currentAsset.assetId) VISIBLE else GONE
            itemView.setOnClickListener {
                listener?.onItemClick(asset)
            }
        }
    }

    interface OnAssetListener {
        fun onItemClick(assetItem: AssetItem)
    }
}