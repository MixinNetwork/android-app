package one.mixin.android.ui.wallet.adapter

import android.util.ArrayMap
import android.util.ArraySet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_asset_add.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import one.mixin.android.R
import one.mixin.android.extension.arrayMapOf
import one.mixin.android.extension.loadImage
import one.mixin.android.vo.HotAsset

internal class AssetAddAdapter : ListAdapter<HotAsset, AssetAddAdapter.ItemHolder>(HotAsset.DIFF_CALLBACK) {

    var onHotAssetListener: OnHotAssetListener? = null
    val checkedAssets = arrayMapOf<String, HotAsset>()
    var existsSet: ArraySet<String>? = null

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        val asset = getItem(position)
        val exists = existsSet != null && existsSet!!.contains(asset.assetId)
        holder.bind(asset, onHotAssetListener, checkedAssets, exists)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
        ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_asset_add, parent, false))

    internal class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(asset: HotAsset,
            listener: OnHotAssetListener? = null,
            checkedAssets: ArrayMap<String, HotAsset>,
            exists: Boolean
        ) {
            itemView.badge_circle_iv.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            itemView.badge_circle_iv.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            itemView.name_tv.text = asset.name
            itemView.symbol_tv.text = asset.symbol
            itemView.cb.isEnabled = !exists
            itemView.cb.isChecked = checkedAssets.contains(asset.assetId)
            itemView.isEnabled = !exists
            itemView.setOnClickListener {
                itemView.cb.isChecked = !itemView.cb.isChecked
                if (itemView.cb.isChecked) {
                    checkedAssets[asset.assetId] = asset
                } else {
                    checkedAssets.remove(asset.assetId, asset)
                }
                listener?.onItemClick(asset, itemView.cb.isChecked)
            }
        }
    }

    interface OnHotAssetListener {
        fun onItemClick(hotAsset: HotAsset, isChecked: Boolean)
    }
}