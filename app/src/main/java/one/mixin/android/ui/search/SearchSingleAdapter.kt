package one.mixin.android.ui.search

import android.content.Context
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemSearchAssetBinding
import one.mixin.android.databinding.ItemSearchChatBinding
import one.mixin.android.databinding.ItemSearchContactBinding
import one.mixin.android.databinding.ItemSearchMessageBinding
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.holder.AssetHolder
import one.mixin.android.ui.search.holder.ChatHolder
import one.mixin.android.ui.search.holder.ContactHolder
import one.mixin.android.ui.search.holder.MessageHolder
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem

class SearchSingleAdapter(private val type: SearchType) : HeaderAdapter<Parcelable>() {
    var onItemClickListener: SearchFragment.OnSearchClickListener? = null
    var query: String = ""

    override fun getNormalViewHolder(
        context: Context,
        parent: ViewGroup,
    ): NormalHolder {
        return when (type) {
            TypeAsset -> AssetHolder(ItemSearchAssetBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            TypeChat -> ChatHolder(ItemSearchChatBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            TypeUser -> ContactHolder(ItemSearchContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            TypeMessage -> MessageHolder(ItemSearchMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> {
                MessageHolder(ItemSearchMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
                // Todo
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (holder is NormalHolder) {
            data?.get(getPos(position)).let {
                when (type) {
                    TypeAsset -> (holder as AssetHolder).bind(it as TokenItem, query, onItemClickListener)
                    TypeChat -> (holder as ChatHolder).bind(it as ChatMinimal, query, onItemClickListener)
                    TypeUser -> (holder as ContactHolder).bind(it as User, query, onItemClickListener)
                    TypeMessage -> (holder as MessageHolder).bind(it as SearchMessageItem, onItemClickListener)
                    else->{
                        // todo
                    }
                }
            }
        }
    }
}
