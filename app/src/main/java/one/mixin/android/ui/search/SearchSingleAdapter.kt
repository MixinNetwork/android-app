package one.mixin.android.ui.search

import android.content.Context
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
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
            TypeAsset -> AssetHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_search_asset, parent, false))
            TypeChat -> ChatHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_search_contact, parent, false))
            TypeUser -> ContactHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_search_contact, parent, false))
            TypeMessage -> MessageHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_search_message, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NormalHolder) {
            data?.get(getPos(position)).let {
                when (type) {
                    TypeAsset -> (holder as AssetHolder).bind(it as AssetItem, query, onItemClickListener)
                    TypeChat -> (holder as ChatHolder).bind(it as ChatMinimal, query, onItemClickListener)
                    TypeUser -> (holder as ContactHolder).bind(it as User, query, onItemClickListener)
                    TypeMessage -> (holder as MessageHolder).bind(it as SearchMessageItem, onItemClickListener)
                }
            }
        }
    }
}