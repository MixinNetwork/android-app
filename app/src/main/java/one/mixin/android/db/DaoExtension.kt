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
    runInTransaction {
        pinMessageDao().deleteByMessageId(messageId)
        mentionMessageDao().deleteMessage(messageId)
        messageDao().deleteMessageById(messageId)
        remoteMessageStatusDao().deleteByMessageId(messageId)
        expiredMessageDao().deleteByMessageId(messageId)
    }
}

fun MixinDatabase.deleteMessageById(
    messageId: String,
    conversationId: String,
) {
    runInTransaction {
        pinMessageDao().deleteByMessageId(messageId)
        mentionMessageDao().deleteMessage(messageId)
        messageDao().deleteMessageById(messageId)
        conversationExtDao().decrement(conversationId)
        remoteMessageStatusDao().deleteByMessageId(messageId)
        expiredMessageDao().deleteByMessageId(messageId)
        conversationDao().refreshLastMessageId(conversationId, messageId)
    }
}

fun MixinDatabase.deleteMessageByIds(messageIds: List<String>) {
    runInTransaction {
        pinMessageDao().deleteByIds(messageIds)
        mentionMessageDao().deleteMessage(messageIds)
        messageDao().deleteMessageById(messageIds)
        remoteMessageStatusDao().deleteByMessageIds(messageIds)
        expiredMessageDao().deleteByMessageId(messageIds)
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
    runInTransaction {
        messageDao().insert(message)
        conversationExtDao().increment(message.conversationId)
        if (!message.isMine() && message.status != MessageStatus.READ.name && !message.isKraken()) {
            remoteMessageStatusDao().insert(RemoteMessageStatus(message.messageId, message.conversationId, MessageStatus.DELIVERED.name))
        }
        conversationDao().updateLastMessageId(message.messageId, message.createdAt, message.conversationId)
        remoteMessageStatusDao().updateConversationUnseen(message.conversationId)
        MessageFlow.insert(message.conversationId, message.messageId)
    }
}

fun MixinDatabase.insertMessage(message: Message) {
    messageDao().insert(message)
    conversationExtDao().increment(message.conversationId)
    conversationDao().updateLastMessageId(message.messageId, message.createdAt, message.conversationId)
}
