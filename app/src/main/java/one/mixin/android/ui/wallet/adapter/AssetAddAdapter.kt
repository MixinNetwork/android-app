package one.mixin.android.ui.wallet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArrayMap
import androidx.collection.ArraySet
import androidx.collection.arrayMapOf
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_asset_add.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.toast
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.TopAssetItem

internal class AssetAddAdapter : ListAdapter<TopAssetItem, AssetAddAdapter.ItemHolder>(TopAssetItem.DIFF_CALLBACK) {

    var onTopAssetListener: OnTopAssetListener? = null
    val checkedAssets = arrayMapOf<String, TopAssetItem>()
    var existsSet: ArraySet<AssetItem>? = null

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        val asset = getItem(position)
        holder.bind(asset, onTopAssetListener, checkedAssets, existsSet)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
        ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_asset_add, parent, false))

    internal class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(
            asset: TopAssetItem,
            listener: OnTopAssetListener? = null,
            checkedAssets: ArrayMap<String, TopAssetItem>,
            exists: ArraySet<AssetItem>?
        ) {
            itemView.badge_circle_iv.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            itemView.badge_circle_iv.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            itemView.name_tv.text = asset.name
            itemView.symbol_tv.text = asset.symbol
            val a = exists?.find { it.assetId == asset.assetId }
            itemView.cb.isEnabled = a == null
            itemView.cb.isChecked = checkedAssets.contains(asset.assetId)
            itemView.setOnClickListener {
                if (a != null) {
                    if (a.hidden == true) {
                        listener?.onHiddenClick(a)
                    } else {
                        itemView.context.toast(R.string.wallet_add_asset_already)
                    }
                } else {
                    itemView.cb.isChecked = !itemView.cb.isChecked
                    if (itemView.cb.isChecked) {
                        checkedAssets[asset.assetId] = asset
                    } else {
                        try {
                            checkedAssets.remove(asset.assetId, asset)
                        } catch (e: NoSuchMethodError) {
                            // Samsung Galaxy Note4 Android M
                            checkedAssets.remove(asset.assetId)
                        }
                    }
                    listener?.onItemClick(asset, itemView.cb.isChecked)
                }
            }
        }
    }

    interface OnTopAssetListener {
        fun onItemClick(topAsset: TopAssetItem, isChecked: Boolean)
        fun onHiddenClick(assetItem: AssetItem)
    }
}
