package one.mixin.android.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import one.mixin.android.R
import one.mixin.android.ui.search.holder.AssetHolder
import one.mixin.android.ui.search.holder.ChatHolder
import one.mixin.android.ui.search.holder.ContactHolder
import one.mixin.android.ui.search.holder.HeaderHolder
import one.mixin.android.ui.search.holder.MessageHolder
import one.mixin.android.ui.search.holder.TipHolder
import one.mixin.android.ui.search.holder.TipItem
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User

class SearchAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), StickyRecyclerHeadersAdapter<HeaderHolder> {

    // workaround with sticky header not refresh by self increment 4
    private var headerRefreshFactor = 0L

    var onItemClickListener: SearchFragment.OnSearchClickListener? = null
    var query: String = ""
        set(value) {
            field = value
            data.showTip = value.all { it.isDigit() }
            headerRefreshFactor += 4
        }

    override fun getHeaderId(position: Int): Long = if (position == 0 && data.showTip) {
        -1
    } else {
        getItemViewType(position).toLong() + headerRefreshFactor
    }

    override fun onBindHeaderViewHolder(holder: HeaderHolder, position: Int) {
        val context = holder.itemView.context
        when (getItemViewType(position)) {
            TypeAsset.index -> holder.bind(context.getText(R.string.search_title_assets).toString(), data.assetShowMore())
            TypeChat.index -> holder.bind(context.getText(R.string.search_title_chat).toString(), data.chatShowMore())
            TypeUser.index -> holder.bind(context.getText(R.string.search_title_contacts).toString(), data.userShowMore())
            TypeMessage.index -> holder.bind(context.getText(R.string.search_title_messages).toString(), data.messageShowMore())
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderHolder {
        val item = LayoutInflater.from(parent.context).inflate(R.layout.item_search_header, parent, false)
        return HeaderHolder(item)
    }

    private var data = SearchDataPackage()

    fun getTypeData(position: Int) =
        when (getItemViewType(position)) {
            TypeAsset.index -> if (data.assetShowMore()) data.assetList else null
            TypeChat.index -> if (data.chatShowMore()) data.chatList else null
            TypeUser.index -> if (data.userShowMore()) data.userList else null
            else -> if (data.messageShowMore()) data.messageList else null
        }

    fun clickMore(position: Int) {
        when (getItemViewType(position)) {
            TypeAsset.index -> data.assetLimit = false
            TypeChat.index -> data.chatLimit = false
            TypeUser.index -> data.userLimit = false
            TypeMessage.index -> data.messageLimit = false
        }
        notifyDataSetChanged()
    }

    fun setAssetData(list: List<AssetItem>?) {
        data.assetList = list
        notifyDataSetChanged()
    }

    fun setUserData(list: List<User>?) {
        data.userList = list?.filter { item ->
            data.chatList?.any { it.category == ConversationCategory.CONTACT.name && it.userId == item.userId } != true
        }
        notifyDataSetChanged()
    }

    fun setChatData(list: List<ChatMinimal>?) {
        data.chatList = list
        data.userList = data.userList?.filter { item ->
            data.chatList?.any { it.category == ConversationCategory.CONTACT.name && it.userId == item.userId } != true
        }
        notifyDataSetChanged()
    }

    fun setMessageData(list: List<SearchMessageItem>?) {
        data.messageList = list
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            0 -> {
                (holder as TipHolder).bind(query, onItemClickListener)
            }
            TypeAsset.index -> {
                data.getItem(position).let {
                    (holder as AssetHolder).bind(it as AssetItem, query, onItemClickListener)
                }
            }
            TypeChat.index -> {
                data.getItem(position).let {
                    (holder as ChatHolder).bind(it as ChatMinimal, query, onItemClickListener)
                }
            }
            TypeUser.index -> {
                data.getItem(position).let {
                    (holder as ContactHolder).bind(it as User, query, onItemClickListener)
                }
            }
            TypeMessage.index -> {
                data.getItem(position).let {
                    (holder as MessageHolder).bind(it as SearchMessageItem, query, onItemClickListener)
                }
            }
        }
    }

    override fun getItemCount(): Int = data.getCount()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            0 -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_search_tip, parent, false)
                TipHolder(item)
            }
            TypeAsset.index -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_search_asset, parent, false)
                AssetHolder(item)
            }
            TypeChat.index -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_search_contact, parent, false)
                ChatHolder(item)
            }
            TypeUser.index -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_search_contact, parent, false)
                ContactHolder(item)
            }
            TypeMessage.index -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_search_message, parent, false)
                MessageHolder(item)
            }
            else -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_search_contact, parent, false)
                ContactHolder(item)
            }
        }

    override fun getItemViewType(position: Int) =
        when (data.getItem(position)) {
            is TipItem -> 0
            is AssetItem -> TypeAsset.index
            is ChatMinimal -> TypeChat.index
            is User -> TypeUser.index
            else -> TypeMessage.index
        }
}