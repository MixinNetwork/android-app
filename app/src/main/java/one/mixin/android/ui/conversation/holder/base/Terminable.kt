package one.mixin.android.ui.conversation.holder.base

import one.mixin.android.RxBus
import one.mixin.android.event.ExpiredEvent

interface Terminable {
    fun onRead(
        messageId: String,
        expireIn: Long?,
        expireAt: Long?,
    ) {
        if (expireIn != null && expireAt == null) { // Read a message with no expiration time marked
            RxBus.publish(ExpiredEvent(messageId, expireIn))
        }
    }
}
