package one.mixin.android.ui.conversation.adapter

import android.arch.paging.PagedList
import android.arch.paging.PagedListAdapter
import android.support.v4.util.ArraySet
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_chat_unread.view.*
import one.mixin.android.R
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.isSameDay
import one.mixin.android.extension.notNullElse
import one.mixin.android.ui.conversation.holder.ActionCardHolder
import one.mixin.android.ui.conversation.holder.ActionHolder
import one.mixin.android.ui.conversation.holder.BillHolder
import one.mixin.android.ui.conversation.holder.CardHolder
import one.mixin.android.ui.conversation.holder.ContactCardHolder
import one.mixin.android.ui.conversation.holder.FileHolder
import one.mixin.android.ui.conversation.holder.HyperlinkHolder
import one.mixin.android.ui.conversation.holder.ImageHolder
import one.mixin.android.ui.conversation.holder.InfoHolder
import one.mixin.android.ui.conversation.holder.MessageHolder
import one.mixin.android.ui.conversation.holder.ReplyHolder
import one.mixin.android.ui.conversation.holder.StickerHolder
import one.mixin.android.ui.conversation.holder.StrangerHolder
import one.mixin.android.ui.conversation.holder.TimeHolder
import one.mixin.android.ui.conversation.holder.TransparentHolder
import one.mixin.android.ui.conversation.holder.UnknowHolder
import one.mixin.android.ui.conversation.holder.VideoHolder
import one.mixin.android.ui.conversation.holder.WaitingHolder
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.User
import one.mixin.android.vo.create
import one.mixin.android.widget.MixinStickyRecyclerHeadersAdapter

class ConversationAdapter(
    private val keyword: String?,
    private val onItemListener: OnItemListener,
    private val isGroup: Boolean
) :
    PagedListAdapter<MessageItem, RecyclerView.ViewHolder>(diffCallback),
    MixinStickyRecyclerHeadersAdapter<TimeHolder> {
    var selectSet: ArraySet<MessageItem> = ArraySet()
    var unreadIndex: Int? = null
    var recipient: User? = null

    var groupName: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var hasBottomView = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getAttachIndex(): Int? = unreadIndex

    override fun onCreateAttach(parent: ViewGroup): View =
        LayoutInflater.from(parent.context).inflate(R.layout.item_chat_unread, parent, false)

    override fun onBindAttachView(view: View) {
        if (hasBottomView) {
            view.unread_tv.text = view.context.getString(R.string.unread, unreadIndex!!)
        } else {
            view.unread_tv.text = view.context.getString(R.string.unread, unreadIndex!! + 1)
        }
    }

    fun markread() {
        unreadIndex?.let {
            unreadIndex = null
            notifyItemChanged(it)
        }
    }

    override fun getHeaderId(position: Int): Long = notNullElse(getItem(position), {
        Math.abs(it.createdAt.hashForDate())
    }, 0)

    override fun onCreateHeaderViewHolder(parent: ViewGroup): TimeHolder =
        TimeHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_time, parent, false))

    override fun onBindHeaderViewHolder(holder: TimeHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it.createdAt)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        getItem(position)?.let {
            when (getItemViewType(position)) {
                MESSAGE_TYPE -> {
                    (holder as MessageHolder).bind(it, keyword, isLast(position),
                        isFirst(position), selectSet.size > 0, isSelect(position), onItemListener)
                }
                IMAGE_TYPE -> {
                    (holder as ImageHolder).bind(it, isLast(position),
                        isFirst(position), selectSet.size > 0, isSelect(position), onItemListener)
                }
                VIDEO_TYPE -> {
                    (holder as VideoHolder).bind(it, isLast(position),
                        isFirst(position), selectSet.size > 0, isSelect(position), onItemListener)
                }
                INFO_TYPE -> {
                    (holder as InfoHolder).bind(it, selectSet.size > 0, isSelect(position), onItemListener, groupName)
                }
                CARD_TYPE -> {
                    (holder as CardHolder).bind(it)
                }
                BILL_TYPE -> {
                    (holder as BillHolder).bind(it, isLast(position),
                        selectSet.size > 0, isSelect(position), onItemListener)
                }
                FILE_TYPE -> {
                    (holder as FileHolder).bind(it, keyword, isFirst(position),
                        isLast(position), selectSet.size > 0, isSelect(position), onItemListener)
                }
                WAITING_TYPE -> {
                    (holder as WaitingHolder).bind(it, isLast(position), isFirst(position), onItemListener)
                }
                REPLY_TYPE -> {
                    (holder as ReplyHolder).bind(it, isLast(position))
                }
                STRANGER_TYPE -> {
                    (holder as StrangerHolder).bind(onItemListener)
                }
                UNKNOWN_TYPE -> {
                    (holder as UnknowHolder).bind()
                }
                STICKER_TYPE -> {
                    (holder as StickerHolder).bind(it, isFirst(position),
                        selectSet.size > 0, isSelect(position), onItemListener)
                }
                LINK_TYPE -> {
                    (holder as HyperlinkHolder).bind(it, keyword, isLast(position),
                        isFirst(position), selectSet.size > 0, isSelect(position), onItemListener)
                }
                ACTION_TYPE -> {
                    (holder as ActionHolder).bind(it, isFirst(position), selectSet.size > 0, isSelect(position), onItemListener)
                }
                ACTION_CARD_TYPE -> {
                    (holder as ActionCardHolder).bind(it, isFirst(position), selectSet.size > 0, isSelect(position), onItemListener)
                }
                CONTACT_CARD_TYPE -> {
                    (holder as ContactCardHolder).bind(it, isFirst(position), isLast(position),
                        selectSet.size > 0, isSelect(position), onItemListener)
                }
                else -> {
                }
            }
        }
    }

    private fun isSelect(position: Int): Boolean {
        return if (selectSet.isEmpty()) {
            false
        } else {
            selectSet.find { it.messageId == getItem(position)?.messageId } != null
        }
    }

    override fun isListLast(position: Int): Boolean {
        return position == 0
    }

    override fun isLast(position: Int): Boolean {
        val currentItem = getItem(position)
        val previousItem = previous(position)
        return when {
            currentItem == null ->
                false
            previousItem == null ->
                true
            currentItem.type == MessageCategory.SYSTEM_CONVERSATION.name ->
                true
            previousItem.type == MessageCategory.SYSTEM_CONVERSATION.name ->
                true
            previousItem.userId != currentItem.userId ->
                true
            !isSameDay(previousItem.createdAt, currentItem.createdAt) ->
                true
            else -> false
        }
    }

    private fun isFirst(position: Int): Boolean {
        return if (isGroup || recipient != null) {
            val currentItem = getItem(position)
            if (!isGroup && (recipient?.isBot() == false || recipient?.userId == currentItem?.userId)) {
                return false
            }
            val nextItem = next(position)
            when {
                currentItem == null ->
                    false
                nextItem == null ->
                    true
                nextItem.type == MessageCategory.SYSTEM_CONVERSATION.name ->
                    true
                nextItem.userId != currentItem.userId ->
                    true
                !isSameDay(nextItem.createdAt, currentItem.createdAt) ->
                    true
                else -> false
            }
        } else {
            false
        }
    }

    private fun previous(position: Int): MessageItem? {
        return if (position > 0) {
            getItem(position - 1)
        } else {
            null
        }
    }

    private fun next(position: Int): MessageItem? {
        return if (position < itemCount - 1) {
            getItem(position + 1)
        } else {
            null
        }
    }

    override fun submitList(pagedList: PagedList<MessageItem>?) {
        super.submitList(pagedList)
        pagedList?.let {
            if (it.size >= 2) {
                lastId = it[1]?.messageId
            }
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (hasBottomView) {
            1
        } else {
            0
        }
    }

    override fun getItem(position: Int): MessageItem? {
        return if (hasBottomView) {
            if (position > 0) {
                super.getItem(position - 1)
            } else {
                if (itemCount > 1) {
                    create(MessageCategory.STRANGER.name, getItem(1)?.createdAt)
                } else {
                    create(MessageCategory.STRANGER.name)
                }
            }
        } else {
            super.getItem(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            MESSAGE_TYPE -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
                MessageHolder(item)
            }
            IMAGE_TYPE -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_image, parent, false)
                ImageHolder(item)
            }
            INFO_TYPE -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_info, parent, false)
                InfoHolder(item)
            }
            CARD_TYPE -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_card, parent, false)
                CardHolder(item)
            }
            BILL_TYPE -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_bill, parent, false)
                BillHolder(item)
            }
            REPLY_TYPE -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_reply, parent, false)
                ReplyHolder(item)
            }
            WAITING_TYPE -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_waiting, parent, false)
                WaitingHolder(item, onItemListener)
            }
            STRANGER_TYPE -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_stranger, parent, false)
                StrangerHolder(item)
            }
            UNKNOWN_TYPE -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_unknow, parent, false)
                UnknowHolder(item)
            }
            FILE_TYPE -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_file, parent, false)
                FileHolder(item)
            }
            STICKER_TYPE -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_sticker, parent, false)
                StickerHolder(item)
            }
            ACTION_TYPE -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_action, parent, false)
                ActionHolder(item)
            }
            ACTION_CARD_TYPE -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_action_card, parent, false)
                ActionCardHolder(item)
            }
            LINK_TYPE -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_hyperlink, parent, false)
                HyperlinkHolder(item)
            }
            CONTACT_CARD_TYPE -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_contact_card, parent, false)
                ContactCardHolder(item)
            }
            VIDEO_TYPE -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_video, parent, false)
                VideoHolder(item)
            }
            else -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_transparent, parent, false)
                TransparentHolder(item)
            }
        }

    fun getItemType(messageItem: MessageItem?): Int =
        notNullElse(messageItem, { item ->
            when {
                item.type == MessageCategory.STRANGER.name -> STRANGER_TYPE
                item.status == MessageStatus.FAILED.name -> WAITING_TYPE
                item.type == MessageCategory.SIGNAL_TEXT.name || item.type == MessageCategory.PLAIN_TEXT.name -> {
                    if (!item.siteName.isNullOrBlank() || !item.siteDescription.isNullOrBlank()) {
                        LINK_TYPE
                    } else {
                        MESSAGE_TYPE
                    }
                }
                item.type == MessageCategory.SIGNAL_IMAGE.name ||
                    item.type == MessageCategory.PLAIN_IMAGE.name -> IMAGE_TYPE
                item.type == MessageCategory.SYSTEM_CONVERSATION.name -> INFO_TYPE
                item.type == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name -> BILL_TYPE
                item.type == MessageCategory.SIGNAL_DATA.name ||
                    item.type == MessageCategory.PLAIN_DATA.name -> FILE_TYPE
                item.type == MessageCategory.SIGNAL_STICKER.name ||
                    item.type == MessageCategory.PLAIN_STICKER.name -> STICKER_TYPE
                item.type == MessageCategory.APP_BUTTON_GROUP.name -> ACTION_TYPE
                item.type == MessageCategory.APP_CARD.name -> ACTION_CARD_TYPE
                item.type == MessageCategory.SIGNAL_CONTACT.name ||
                    item.type == MessageCategory.PLAIN_CONTACT.name -> CONTACT_CARD_TYPE
                item.type == MessageCategory.SIGNAL_VIDEO.name ||
                    item.type == MessageCategory.PLAIN_VIDEO.name -> VIDEO_TYPE
                else -> UNKNOWN_TYPE
            }
        }, NULL_TYPE)

    override fun getItemViewType(position: Int): Int = getItemType(getItem(position))

    companion object {
        const val NULL_TYPE = -2
        const val UNKNOWN_TYPE = -1
        const val MESSAGE_TYPE = 0
        const val IMAGE_TYPE = 1
        const val INFO_TYPE = 2
        const val CARD_TYPE = 3
        const val BILL_TYPE = 4
        const val FILE_TYPE = 6
        const val STICKER_TYPE = 7
        const val ACTION_TYPE = 8
        const val ACTION_CARD_TYPE = 9
        const val REPLY_TYPE = 10
        const val WAITING_TYPE = 11
        const val LINK_TYPE = 12
        const val STRANGER_TYPE = 13
        const val CONTACT_CARD_TYPE = 14
        const val VIDEO_TYPE = 15

        private var lastId: String? = null

        private val diffCallback = object : DiffUtil.ItemCallback<MessageItem>() {
            override fun areItemsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
                return oldItem.messageId == newItem.messageId
            }

            override fun areContentsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
                if (oldItem.messageId == lastId || newItem.messageId == lastId) {
                    return false
                }
                return oldItem == newItem
            }
        }
    }

    open class OnItemListener {

        open fun onSelect(isSelect: Boolean, messageItem: MessageItem, position: Int) {}

        open fun onLongClick(messageItem: MessageItem, position: Int): Boolean = true

        open fun onImageClick(messageItem: MessageItem, view: View) {}

        open fun onFileClick(messageItem: MessageItem) {}

        open fun onCancel(id: String) {}

        open fun onRetryUpload(messageId: String) {}

        open fun onRetryDownload(messageId: String) {}

        open fun onUserClick(userId: String) {}

        open fun onMentionClick(name: String) {}

        open fun onUrlClick(url: String) {}

        open fun onAddClick() {}

        open fun onBlockClick() {}

        open fun onActionClick(action: String) {}

        open fun onBillClick(messageItem: MessageItem) {}

        open fun onContactCardClick(userId: String) {}

        open fun onTransferClick(userId: String) {}
    }

    fun addSelect(messageItem: MessageItem): Boolean {
        return selectSet.add(messageItem)
    }

    fun removeSelect(messageItem: MessageItem): Boolean {
        return selectSet.remove(selectSet.find { it.messageId == messageItem.messageId })
    }
}