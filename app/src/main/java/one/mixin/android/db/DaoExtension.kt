package one.mixin.android.db

import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.pending.PendingDatabase
import one.mixin.android.session.Session
import one.mixin.android.vo.Job
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.RemoteMessageStatus
import one.mixin.android.vo.isKraken
import one.mixin.android.vo.isMine

fun MixinDatabase.clearParticipant(
    conversationId: String,
    participantId: String,
) {
    runInTransaction {
        participantDao().deleteById(conversationId, participantId)
        participantSessionDao().deleteByUserId(conversationId, participantId)
        participantSessionDao().emptyStatusByConversationId(conversationId)
    }
}

fun JobDao.insertNoReplace(job: Job) {
    if (findJobById(job.jobId) == null) {
        insert(job)
    }
}

// Delete SQL
fun MixinDatabase.deleteMessageById(messageId: String) {
    deleteMessageByIds(listOf(messageId))
}

fun MixinDatabase.deleteMessageByIds(messageIds: List<String>) {
    messageIds.chunked(500).forEach { ids ->
        runInTransaction {
            val counts = messageDao().countMessagesByConversation(ids)
            val unreadCounts = remoteMessageStatusDao().countDeliveredByConversation(ids)
            pinMessageDao().deleteByIds(ids)
            mentionMessageDao().deleteMessage(ids)
            messageDao().deleteMessageById(ids)
            remoteMessageStatusDao().deleteByMessageIds(ids)
            expiredMessageDao().deleteByMessageId(ids)
            counts.forEach { (conversationId, count) ->
                conversationExtDao().increment(conversationId, -count)
                conversationDao().refreshLastMessageId(conversationId)
            }
            unreadCounts.forEach { (conversationId, count) ->
                remoteMessageStatusDao().incrementUnseen(conversationId, -count)
            }
        }
    }
}

fun MixinDatabase.makeMessageStatus(
    status: String,
    messageId: String,
    noExistCallback: (() -> Unit)? = null,
) {
    val messageStatus = MessageStatus.values().firstOrNull { it.name == status } ?: return
    if (messageStatus != MessageStatus.SENT && messageStatus != MessageStatus.DELIVERED && messageStatus != MessageStatus.READ) {
        return
    }
    val message = messageDao().findMessageStatusById(messageId)
    if (message == null) {
        noExistCallback?.invoke()
        return
    }
    if (messageStatus.ordinal > message.status.ordinal) {
        messageDao().updateMessageStatus(status, messageId)
        if (message.userId == Session.getAccountId()) {
            conversationDao().forceRefreshConversationsByLastMessageId(message.conversationId, messageId)
        }
        MessageFlow.update(message.conversationId, messageId)
    }
}

fun PendingDatabase.makeMessageStatus(
    status: String,
    messageId: String,
    noExistCallback: (() -> Unit)? = null,
) {
    val messageStatus = MessageStatus.values().firstOrNull { it.name == status } ?: return
    if (messageStatus != MessageStatus.SENT && messageStatus != MessageStatus.DELIVERED && messageStatus != MessageStatus.READ) {
        return
    }
    val message = pendingMessageDao().findMessageStatusById(messageId)
    if (message == null) {
        noExistCallback?.invoke()
        return
    }
    if (messageStatus.ordinal > message.status.ordinal) {
        pendingMessageDao().updateMessageStatus(status, messageId)
        MessageFlow.update(message.conversationId, messageId)
    }
}

// Insert message SQL
fun MixinDatabase.insertAndNotifyConversation(message: Message) {
    val statuses =
        if (!message.isMine() && message.status != MessageStatus.READ.name && !message.isKraken()) {
            listOf(RemoteMessageStatus(message.messageId, message.conversationId, MessageStatus.DELIVERED.name))
        } else {
            emptyList()
        }
    insertConversationMessages(message.conversationId, listOf(message), statuses)
    MessageFlow.insert(message.conversationId, message.messageId)
}

fun MixinDatabase.insertConversationMessages(
    conversationId: String,
    messages: List<Message>,
    statuses: List<RemoteMessageStatus>,
) {
    if (messages.isEmpty()) return
    runInTransaction {
        val uniqueMessages = messages.distinctBy { it.messageId }
        val existingCount = messageDao().countExistingMessages(uniqueMessages.map { it.messageId })
        messageDao().insertList(uniqueMessages)
        conversationExtDao().increment(conversationId, uniqueMessages.size - existingCount)
        remoteMessageStatusDao().insertDelivered(conversationId, statuses)
        uniqueMessages.asReversed().maxBy { it.createdAt }.let { message ->
            conversationDao().updateLastMessageId(message.messageId, message.createdAt, conversationId)
        }
    }
}

fun MixinDatabase.insertMessage(message: Message) {
    insertConversationMessages(message.conversationId, listOf(message), emptyList())
}
