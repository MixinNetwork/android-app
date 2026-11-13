package one.mixin.android.ui.conversation.holder

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import androidx.core.content.ContextCompat
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatSystemBinding
import one.mixin.android.extension.highlightStarTag
import one.mixin.android.ui.conversation.adapter.MessageAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.vo.MessageItem
import one.mixin.android.websocket.SystemConversationAction
import one.mixin.android.widget.picker.toTimeInterval

class SystemHolder constructor(val binding: ItemChatSystemBinding) : BaseViewHolder(binding.root) {
    var context: Context = itemView.context

    private fun getText(id: Int) = context.getText(id).toString()

    fun bind(
        messageItem: MessageItem,
        hasSelect: Boolean,
        isSelect: Boolean,
        canEdit: Boolean,
        onItemListener: MessageAdapter.OnItemListener,
    ) {
        val id = meId
        val isDisappearing = messageItem.actionName == SystemConversationAction.EXPIRE.name
        binding.chatInfo.movementMethod = null
        binding.chatInfo.setOnClickListener(null)
        binding.chatInfo.isClickable = false
        binding.chatInfo.setOnLongClickListener(null)
        binding.chatInfo.isLongClickable = false
        binding.chatInfo.maxLines = if (isDisappearing) Int.MAX_VALUE else 2
        binding.chatInfo.ellipsize = if (isDisappearing) null else TextUtils.TruncateAt.END
        binding.chatInfo.apply {
            val start = paddingStart
            val top = paddingTop
            val end = paddingEnd
            val bottom = paddingBottom
            setBackgroundResource(
                if (!isDisappearing) {
                    R.drawable.chat_bubble_info
                } else if ((messageItem.content?.toLongOrNull() ?: 0L) > 0L) {
                    R.drawable.chat_bubble_disappearing
                } else if (isNightMode) {
                    R.drawable.chat_bubble_disappearing_off_night
                } else {
                    R.drawable.chat_bubble_disappearing_off
                },
            )
            setPaddingRelative(start, top, end, bottom)
        }
        binding.chatInfo.setTextColor(
            ContextCompat.getColor(
                context,
                if (isNightMode && isDisappearing && (messageItem.content?.toLongOrNull() ?: 0L) <= 0L) {
                    R.color.textPrimaryNight
                } else {
                    R.color.textPrimary
                },
            ),
        )
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        itemView.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }
        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            } else if (isDisappearing) {
                onItemListener.onDisappearingMessagesClick()
            }
        }
        if (isDisappearing) {
            binding.chatInfo.setOnClickListener { itemView.performClick() }
            binding.chatInfo.setOnLongClickListener { itemView.performLongClick() }
        }

        when (messageItem.actionName) {
            SystemConversationAction.CREATE.name -> {
                binding.chatInfo.text =
                    String.format(
                        getText(R.string.created_this_group),
                        if (id == messageItem.userId) {
                            getText(R.string.You)
                        } else {
                            messageItem.userFullName
                        },
                    )
            }
            SystemConversationAction.ADD.name -> {
                binding.chatInfo.text =
                    String.format(
                        getText(R.string.chat_group_add),
                        if (id == messageItem.userId) {
                            getText(R.string.You)
                        } else {
                            messageItem.userFullName
                        },
                        if (id == messageItem.participantUserId) {
                            getText(R.string.you)
                        } else {
                            messageItem.participantFullName
                        },
                    )
            }
            SystemConversationAction.REMOVE.name -> {
                binding.chatInfo.text =
                    String.format(
                        getText(R.string.chat_group_remove),
                        if (id == messageItem.userId) {
                            getText(R.string.You)
                        } else {
                            messageItem.userFullName
                        },
                        if (id == messageItem.participantUserId) {
                            getText(R.string.you)
                        } else {
                            messageItem.participantFullName
                        },
                    )
            }
            SystemConversationAction.JOIN.name -> {
                binding.chatInfo.text =
                    String.format(
                        getText(R.string.chat_group_join),
                        if (id == messageItem.participantUserId) {
                            getText(R.string.You)
                        } else {
                            messageItem.participantFullName
                        },
                    )
            }
            SystemConversationAction.EXIT.name -> {
                binding.chatInfo.text =
                    String.format(
                        getText(R.string.chat_group_exit),
                        if (id == messageItem.participantUserId) {
                            getText(R.string.You)
                        } else {
                            messageItem.participantFullName
                        },
                    )
            }
            SystemConversationAction.ROLE.name -> {
                binding.chatInfo.text = getText(R.string.group_role)
            }
            SystemConversationAction.EXPIRE.name -> {
                val timeInterval = messageItem.content?.toLongOrNull()
                val name =
                    if (id == messageItem.userId) {
                        getText(R.string.You)
                    } else {
                        messageItem.userFullName
                    }
                binding.chatInfo.text =
                    when {
                        timeInterval == null -> { // Messages received in the old version
                            String.format(
                                getText(if (canEdit) R.string.changed_disappearing_message_settings_editable else R.string.changed_disappearing_message_settings_read_only),
                                name,
                            )
                        }
                        timeInterval <= 0 -> {
                            String.format(
                                getText(if (canEdit) R.string.disable_disappearing_message_editable else R.string.disable_disappearing_message_read_only),
                                name,
                            )
                        }
                        else -> {
                            String.format(
                                getText(if (canEdit) R.string.set_disappearing_message_time_to_editable else R.string.set_disappearing_message_time_to_read_only),
                                name,
                                toTimeInterval(timeInterval),
                            )
                        }
                    }
            }
            else -> {
                val learn: String = MixinApplication.get().getString(R.string.Learn_More)
                val info = MixinApplication.get().getString(R.string.chat_not_support, "**$learn**")
                val learnUrl = MixinApplication.get().getString(R.string.chat_not_support_url)
                binding.chatInfo.highlightStarTag(
                    info,
                    arrayOf(learnUrl),
                    onItemListener = onItemListener,
                )
            }
        }
    }
}
