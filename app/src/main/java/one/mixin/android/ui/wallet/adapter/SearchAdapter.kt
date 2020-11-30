package one.mixin.android.ui.wallet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemWalletSearchBinding
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.TopAssetItem

class SearchAdapter : RecyclerView.Adapter<ItemViewHolder>() {
    companion object {
        const val TYPE_LOCAL = 0
        const val TYPE_REMOTE = 1
    }

    var localAssets: List<AssetItem>? = null
        set(value) {
            if (value == field) return

            field = value
            notifyDataSetChanged()
        }

    var remoteAssets: List<TopAssetItem>? = null
        set(value) {
            if (value == field) return

            field = value
            notifyDataSetChanged()
        }

    var callback: WalletSearchCallback? = null

    fun clear() {
        localAssets = null
        remoteAssets = null
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return if (viewType == TYPE_LOCAL) {
            AssetHolder(ItemWalletSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            TopAssetHolder(ItemWalletSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        if (holder is AssetHolder) {
            localAssets?.get(position)?.let { holder.bind(it, callback) }
        } else {
            holder as TopAssetHolder
            remoteAssets?.get(position - (localAssets?.size ?: 0))?.let { holder.bind(it, callback) }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            localAssets.isNullOrEmpty() -> TYPE_REMOTE
            remoteAssets.isNullOrEmpty() -> TYPE_LOCAL
            position < localAssets!!.size -> TYPE_LOCAL
            else -> TYPE_REMOTE
        }
    }

    override fun getItemCount(): Int = (localAssets?.size ?: 0) + (remoteAssets?.size ?: 0)
}
