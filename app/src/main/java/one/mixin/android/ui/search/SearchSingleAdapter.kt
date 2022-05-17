package one.mixin.android.ui.search

import android.content.Context
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemSearchAssetBinding
import one.mixin.android.databinding.ItemSearchContactBinding
import one.mixin.android.databinding.ItemSearchMessageBinding
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.holder.AssetHolder
import one.mixin.android.ui.search.holder.ChatHolder
import one.mixin.android.ui.search.holder.ContactHolder
import one.mixin.android.ui.search.holder.MessageHolder
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User

class SearchSingleAdapter(private val type: SearchType) : HeaderAdapter<Parcelable>() {
    var onItemClickListener: SearchFragment.OnSearchClickListener? = null
    var query: String = ""

    override fun getNormalViewHolder(context: Context, parent: ViewGroup): NormalHolder {
        return when (type) {
            SearchType.Asset -> AssetHolder(ItemSearchAssetBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            SearchType.Chat -> ChatHolder(ItemSearchContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            SearchType.User -> ContactHolder(ItemSearchContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            SearchType.Message -> MessageHolder(ItemSearchMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NormalHolder) {
            data?.get(getPos(position)).let {
                when (type) {
                    SearchType.Asset -> (holder as AssetHolder).bind(it as AssetItem, query, onItemClickListener)
                    SearchType.Chat -> (holder as ChatHolder).bind(it as ChatMinimal, query, onItemClickListener)
                    SearchType.User -> (holder as ContactHolder).bind(it as User, query, onItemClickListener)
                    SearchType.Message -> (holder as MessageHolder).bind(it as SearchMessageItem, onItemClickListener)
                }
            }
        }
    }
}
