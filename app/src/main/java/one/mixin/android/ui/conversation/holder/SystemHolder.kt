package one.mixin.android.ui.conversation.holder

import android.content.Context
import android.graphics.Color
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
        onItemListener: MessageAdapter.OnItemListener,
    ) {
        val id = meId
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
            }
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
                                getText(R.string.changed_disappearing_message_settings),
                                name,
                            )
                        }
                        timeInterval <= 0 -> {
                            String.format(
                                getText(R.string.disable_disappearing_message),
                                name,
                            )
                        }
                        else -> {
                            String.format(
                                getText(R.string.set_disappearing_message_time_to),
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
