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
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import java.lang.Exception
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

    override fun getHeaderId(position: Int): Long = if (position == 0 && data.showTip) {
        -1
    } else {
        getItemViewType(position).toLong() + data.getHeaderFactor(position)
    }

    override fun onBindHeaderViewHolder(holder: HeaderHolder, position: Int) {
        val context = holder.itemView.context
        when (getItemViewType(position)) {
            TypeAsset.index -> holder.bind(context.getText(R.string.search_title_assets).toString(), data.assetShowMore())
            TypeUser.index -> holder.bind(context.getText(R.string.search_title_contacts).toString(), data.userShowMore())
            TypeChat.index -> holder.bind(context.getText(R.string.search_title_chat).toString(), data.chatShowMore())
            TypeMessage.index -> holder.bind(context.getText(R.string.search_title_messages).toString(), data.messageShowMore())
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
    fun setData(assetItems: List<AssetItem>?, users: List<User>?, chatMinimals: List<ChatMinimal>?) {
        data.assetList = assetItems
        data.userList = users
        data.showTip = shouldTips(query)
        data.chatList = chatMinimals
        notifyDataSetChanged()
    }

    fun setMessageData(searchMessageItems: List<SearchMessageItem>) {
        data.messageList = searchMessageItems
        val messageCount = data.messageCount()
        notifyItemRangeChanged(data.getCount() - messageCount, messageCount)
    }

    private fun shouldTips(keyword: String): Boolean {
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            0 -> {
                (holder as TipHolder).bind(query, searchingId, onItemClickListener)
            }
            TypeAsset.index -> {
                data.getItem(position).let {
                    (holder as AssetHolder).bind(it as AssetItem, query, onItemClickListener)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
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
                ChatHolder(ItemSearchContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))
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
            is AssetItem -> TypeAsset.index
            is User -> TypeUser.index
            is ChatMinimal -> TypeChat.index
            else -> TypeMessage.index
        }
}
