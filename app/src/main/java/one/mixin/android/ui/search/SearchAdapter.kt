package one.mixin.android.ui.search

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import one.mixin.android.R
import one.mixin.android.databinding.ItemSearchAssetBinding
import one.mixin.android.databinding.ItemSearchContactBinding
import one.mixin.android.databinding.ItemSearchHeaderBinding
import one.mixin.android.databinding.ItemSearchMessageBinding
import one.mixin.android.databinding.ItemSearchTipBinding
import one.mixin.android.ui.search.holder.AssetHolder
import one.mixin.android.ui.search.holder.ChatHolder
import one.mixin.android.ui.search.holder.ContactHolder
import one.mixin.android.ui.search.holder.HeaderHolder
import one.mixin.android.ui.search.holder.MessageHolder
import one.mixin.android.ui.search.holder.TipHolder
import one.mixin.android.ui.search.holder.TipItem
import one.mixin.android.ui.search.holder.TipType
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import one.mixin.android.widget.linktext.Utils
import java.lang.Exception
import java.util.Locale

class SearchAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), StickyRecyclerHeadersAdapter<HeaderHolder> {

    var onItemClickListener: SearchFragment.OnSearchClickListener? = null
    var query: String = ""
        set(value) {
            field = value
            data.tipType = getTipType(value)
        }

    var searchingId = false
        set(value) {
            field = value
            if (data.showTip()) {
                notifyItemChanged(0)
            }
        }

    override fun getHeaderId(position: Int): Long = if (position == 0 && data.showTip()) {
        -1
    } else {
        getItemViewType(position).toLong() + data.getHeaderFactor(position)
    }

    override fun onBindHeaderViewHolder(holder: HeaderHolder, position: Int) {
        val context = holder.itemView.context
        when (getItemViewType(position)) {
            SearchType.Asset.index -> holder.bind(context.getText(R.string.ASSETS).toString(), data.assetShowMore())
            SearchType.User.index -> holder.bind(context.getText(R.string.CONTACTS).toString(), data.userShowMore())
            SearchType.Chat.index -> holder.bind(context.getText(R.string.CHATS).toString(), data.chatShowMore())
            SearchType.Message.index -> holder.bind(context.getText(R.string.SEARCH_MESSAGES).toString(), data.messageShowMore())
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderHolder {
        return HeaderHolder(ItemSearchHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    private var data = SearchDataPackage()

    fun getTypeData(position: Int) =
        when (getItemViewType(position)) {
            SearchType.Asset.index -> if (data.assetShowMore()) data.assetList else null
            SearchType.User.index -> if (data.userShowMore()) data.userList else null
            SearchType.Chat.index -> if (data.chatShowMore()) data.chatList else null
            else -> if (data.messageShowMore()) data.messageList else null
        }

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        data.assetList = null
        data.userList = null
        data.tipType = getTipType(query)
        data.chatList = null
        data.messageList = null
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(assetItems: List<AssetItem>?, users: List<User>?, chatMinimals: List<ChatMinimal>?) {
        data.assetList = assetItems
        data.userList = users
        data.tipType = getTipType(query)
        data.chatList = chatMinimals
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setAssetData(assetItems: List<AssetItem>?) {
        data.assetList = assetItems
        val end = assetItems?.size ?: 0
        if (end > 0) {
            notifyItemRangeChanged(0, end)
        } else {
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setChatData(chatMinimals: List<ChatMinimal>?) {
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

    private fun getTipType(keyword: String): TipType? {
        if (tipPhone(keyword)) return TipType.Phone
        if (tipUrl(keyword)) return TipType.Url
        return null
    }

    private fun tipPhone(keyword: String): Boolean {
        if (keyword.length < 4) return false
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

    private fun tipUrl(keyword: String): Boolean {
        val urlPattern = Utils.getUrlPattern()
        val matcher = urlPattern.matcher(keyword)
        return matcher.find()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            0 -> {
                (holder as TipHolder).bind(query, requireNotNull(data.tipType), searchingId, onItemClickListener)
            }
            SearchType.Asset.index -> {
                data.getItem(position).let {
                    (holder as AssetHolder).bind(it as AssetItem, query, onItemClickListener)
                }
            }
            SearchType.User.index -> {
                data.getItem(position).let {
                    (holder as ContactHolder).bind(it as User, query, onItemClickListener)
                }
            }
            SearchType.Chat.index -> {
                data.getItem(position).let {
                    (holder as ChatHolder).bind(it as ChatMinimal, query, onItemClickListener)
                }
            }
            SearchType.Message.index -> {
                data.getItem(position).let {
                    (holder as MessageHolder).bind(it as SearchMessageItem, onItemClickListener)
                }
            }
        }
    }

    override fun getItemCount(): Int = data.getCount()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            0 -> {
                TipHolder(ItemSearchTipBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            SearchType.Asset.index -> {
                AssetHolder(ItemSearchAssetBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            SearchType.User.index -> {
                ContactHolder(ItemSearchContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            SearchType.Chat.index -> {
                ChatHolder(ItemSearchContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            SearchType.Message.index -> {
                MessageHolder(ItemSearchMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            else -> {
                ContactHolder(ItemSearchContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
        }

    override fun getItemViewType(position: Int) =
        when (data.getItem(position)) {
            is TipItem -> 0
            is AssetItem -> SearchType.Asset.index
            is User -> SearchType.User.index
            is ChatMinimal -> SearchType.Chat.index
            else -> SearchType.Message.index
        }
}
