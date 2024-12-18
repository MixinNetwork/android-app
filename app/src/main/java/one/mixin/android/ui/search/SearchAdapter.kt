package one.mixin.android.ui.search

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import one.mixin.android.R
import one.mixin.android.databinding.ItemSearchAssetBinding
import one.mixin.android.databinding.ItemSearchChatBinding
import one.mixin.android.databinding.ItemSearchContactBinding
import one.mixin.android.databinding.ItemSearchHeaderBinding
import one.mixin.android.databinding.ItemSearchMessageBinding
import one.mixin.android.databinding.ItemSearchTipBinding
import one.mixin.android.extension.isMao
import one.mixin.android.ui.search.holder.AssetHolder
import one.mixin.android.ui.search.holder.ChatHolder
import one.mixin.android.ui.search.holder.ContactHolder
import one.mixin.android.ui.search.holder.HeaderHolder
import one.mixin.android.ui.search.holder.MessageHolder
import one.mixin.android.ui.search.holder.TipHolder
import one.mixin.android.ui.search.holder.TipItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem
import java.util.Locale

class SearchAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), StickyRecyclerHeadersAdapter<HeaderHolder> {
    var onItemClickListener: SearchFragment.OnSearchClickListener? = null
    var query: String = ""
        set(value) {
            field = value
            data.showTip = shouldTips(value)
        }

    var searchingId = false
        set(value) {
            field = value
            if (data.showTip) {
                notifyItemChanged(0)
            }
        }

    override fun getHeaderId(position: Int): Long =
        if (position == 0 && data.showTip) {
            -1
        } else {
            getItemViewType(position).toLong() + data.getHeaderFactor(position)
        }

    override fun onBindHeaderViewHolder(
        holder: HeaderHolder,
        position: Int,
    ) {
        val context = holder.itemView.context
        when (getItemViewType(position)) {
            TypeAsset.index -> holder.bind(context.getText(R.string.ASSETS).toString(), data.assetShowMore())
            TypeUser.index -> holder.bind(context.getText(R.string.CONTACTS).toString(), data.userShowMore())
            TypeChat.index -> holder.bind(context.getText(R.string.CHATS).toString(), data.chatShowMore())
            TypeMessage.index -> holder.bind(context.getText(R.string.SEARCH_MESSAGES).toString(), data.messageShowMore())
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderHolder {
        return HeaderHolder(ItemSearchHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    private var data = SearchDataPackage()

    fun getTypeData(position: Int) =
        when (getItemViewType(position)) {
            TypeAsset.index -> if (data.assetShowMore()) data.assetList else null
            TypeUser.index -> if (data.userShowMore()) data.userList else null
            TypeChat.index -> if (data.chatShowMore()) data.chatList else null
            else -> if (data.messageShowMore()) data.messageList else null
        }

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        data.assetList = null
        data.userList = null
        data.showTip = shouldTips(query)
        data.chatList = null
        data.messageList = null
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(
        tokenItems: List<TokenItem>?,
        users: List<User>?,
        chatMinimals: List<ChatMinimal>?,
    ) {
        data.assetList = tokenItems
        data.userList = users
        data.showTip = shouldTips(query)
        data.chatList = chatMinimals
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setAssetData(tokenItems: List<TokenItem>?) {
        data.assetList = tokenItems
        val end = tokenItems?.size ?: 0
        if (end > 0) {
            notifyItemRangeChanged(0, end)
        } else {
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setChatData(chatMinimals: List<ChatMinimal>?) =
        if ((chatMinimals?.size ?: 0) < (data.chatList?.size ?: 0)) {
            data.chatList = chatMinimals
            notifyDataSetChanged()
        } else {
            data.chatList = chatMinimals
            val end = data.getCount() - data.messageCount()
            val start = end - data.chatCount()
            if (start < end) {
                notifyItemRangeChanged(start, end)
            } else {
                notifyDataSetChanged()
            }
        }

    fun setMessageData(searchMessageItems: List<SearchMessageItem>) {
        data.messageList = searchMessageItems
        val messageCount = data.messageCount()
        notifyItemRangeChanged(data.getCount() - messageCount, messageCount)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setUrlData(url: String?) {
        data.url = url
        data.showTip = shouldTips(query)
        notifyDataSetChanged()
    }

    private fun shouldTips(keyword: String): Boolean {
        if (data.url != null) {
            return true
        }
        if (keyword.length < 4) return false
        if (keyword.isMao()) return true
        if (!keyword.all { it.isDigit() or (it == '+') }) return false
        return if (keyword.startsWith('+')) {
            try {
                val phoneNum = PhoneNumberUtil.getInstance().parse(keyword, Locale.getDefault().country)
                PhoneNumberUtil.getInstance().isValidNumber(phoneNum)
            } catch (e: Exception) {
                false
            }
        } else {
            keyword.all { it.isDigit() }
        } && data.userList.isNullOrEmpty()
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        when (getItemViewType(position)) {
            0 -> {
                (holder as TipHolder).bind(query, searchingId, data.url, onItemClickListener)
            }
            TypeAsset.index -> {
                data.getItem(position).let {
                    (holder as AssetHolder).bind(it as TokenItem, query, onItemClickListener)
                }
            }
            TypeUser.index -> {
                data.getItem(position).let {
                    (holder as ContactHolder).bind(it as User, query, onItemClickListener)
                }
            }
            TypeChat.index -> {
                data.getItem(position).let {
                    (holder as ChatHolder).bind(it as ChatMinimal, query, onItemClickListener)
                }
            }
            TypeMessage.index -> {
                data.getItem(position).let {
                    (holder as MessageHolder).bind(it as SearchMessageItem, onItemClickListener)
                }
            }
        }
    }

    override fun getItemCount(): Int = data.getCount()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        when (viewType) {
            0 -> {
                TipHolder(ItemSearchTipBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            TypeAsset.index -> {
                AssetHolder(ItemSearchAssetBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            TypeUser.index -> {
                ContactHolder(ItemSearchContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            TypeChat.index -> {
                ChatHolder(ItemSearchChatBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            TypeMessage.index -> {
                MessageHolder(ItemSearchMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            else -> {
                ContactHolder(ItemSearchContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
        }

    override fun getItemViewType(position: Int) =
        when (data.getItem(position)) {
            is TipItem -> 0
            is TokenItem -> TypeAsset.index
            is User -> TypeUser.index
            is ChatMinimal -> TypeChat.index
            else -> TypeMessage.index
        }
}
