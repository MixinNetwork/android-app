package one.mixin.android.db.flow

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.event.MessageEvent
import one.mixin.android.event.MessageEventAction

object MessageFlow {
    const val ANY_ID = ""
    private val messageEventFlow by lazy {
        MutableSharedFlow<MessageEvent>(0, 1, BufferOverflow.SUSPEND)
    }

    fun update(
        conversationId: String,
        messageId: String,
    ) {
        emit(MessageEvent(conversationId, MessageEventAction.UPDATE, listOf(messageId)))
    }

    fun updateRelationship(conversationId: String) {
        emit(MessageEvent(conversationId, MessageEventAction.RELATIIONSHIP, listOf()))
    }

    fun update(
        conversationId: String,
        messageIds: List<String>,
    ) {
        emit(MessageEvent(conversationId, MessageEventAction.UPDATE, messageIds))
    }

    fun delete(
        conversationId: String,
        messageId: String,
    ) {
        emit(MessageEvent(conversationId, MessageEventAction.DELETE, listOf(messageId)))
    }

    fun delete(
        conversationId: String,
        messageIds: List<String>,
    ) {
        emit(MessageEvent(conversationId, MessageEventAction.DELETE, messageIds))
    }

    fun insert(
        conversationId: String,
        messageId: String,
    ) {
        emit(MessageEvent(conversationId, MessageEventAction.INSERT, listOf(messageId)))
    }

    fun insert(
        conversationId: String,
        messageIds: List<String>,
    ) {
        emit(MessageEvent(conversationId, MessageEventAction.INSERT, messageIds))
    }

    private fun emit(event: MessageEvent) {
        MixinApplication.get().applicationScope.launch {
            messageEventFlow.emit(event)
        }
    }

    suspend fun collect(
        predicate: suspend (MessageEvent) -> Boolean,
        collector: FlowCollector<MessageEvent>,
    ) {
        messageEventFlow.filter(predicate).collect(collector)
    }
}
