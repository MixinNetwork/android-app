package one.mixin.android.ui.wallet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import one.mixin.android.databinding.ItemWalletSearchBinding
import one.mixin.android.vo.AssetItem

class SearchAdapter : ListAdapter<AssetItem, AssetHolder>(AssetItem.DIFF_CALLBACK) {
    var callback: WalletSearchCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetHolder {
        return AssetHolder(ItemWalletSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }
    override fun onBindViewHolder(holder: AssetHolder, position: Int) {
        getItem(position)?.let { holder.bind(it, callback) }
    }
}
