package one.mixin.android.ui.conversation.holder.base

import one.mixin.android.vo.MessageItem

interface Terminable {
    fun onRead(messageItem: MessageItem)
}
