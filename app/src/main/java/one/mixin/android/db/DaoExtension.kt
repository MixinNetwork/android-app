package one.mixin.android.db

import kotlinx.coroutines.withContext
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.pending.PendingDatabase
import one.mixin.android.session.Session
import one.mixin.android.util.SINGLE_DB_THREAD
import one.mixin.android.vo.App
import one.mixin.android.vo.Circle
import one.mixin.android.vo.CircleConversation
import one.mixin.android.vo.Job
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.RemoteMessageStatus
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.User
import one.mixin.android.vo.isKraken
import one.mixin.android.vo.isMine
import one.mixin.android.vo.safe.Output
import timber.log.Timber

fun UserDao.insertUpdate(
    user: User,
    appDao: AppDao,
) {
    runInTransaction {
        if (user.app != null) {
            user.appId = user.app!!.appId
            appDao.insert(user.app!!)
        }
        val u = findUser(user.userId)
        if (u == null) {
            insert(user)
        } else {
            update(user)
        }
    }
}

fun UserDao.insertUpdateList(
    users: List<User>,
    appDao: AppDao,
) {
    runInTransaction {
        val apps = arrayListOf<App>()
        for (u in users) {
            if (u.app != null) {
                u.appId = u.app!!.appId
                apps.add(u.app!!)
            }
        }
        appDao.insertList(apps)
        insertList(users)
    }
}

fun UserDao.updateRelationship(
    user: User,
    relationship: String,
) {
    runInTransaction {
        val u = findUser(user.userId)
        if (u == null) {
            insert(user)
        } else {
            user.relationship = relationship
            update(user)
        }
    }
}

fun StickerDao.insertUpdate(s: Sticker) {
    runInTransaction {
        val sticker = getStickerByUnique(s.stickerId)
        if (sticker != null) {
            s.lastUseAt = sticker.lastUseAt
        }
        if (s.createdAt == "") {
            s.createdAt = System.currentTimeMillis().toString()
        }
        insert(s)
    }
}

fun CircleConversationDao.insertUpdate(
    circleConversation: CircleConversation,
) {
    runInTransaction {
        val c =
            findCircleConversationByCircleId(
                circleConversation.circleId,
                circleConversation.conversationId,
            )
        if (c == null) {
            insert(circleConversation)
        } else {
            updateCheckPin(c, circleConversation)
        }
    }
}

fun CircleConversationDao.updateCheckPin(
    oldCircleConversation: CircleConversation,
    newCircleConversation: CircleConversation,
) {
    if (oldCircleConversation.pinTime != null) {
        update(
            CircleConversation(
                newCircleConversation.conversationId,
                newCircleConversation.circleId,
                newCircleConversation.userId,
                newCircleConversation.createdAt,
                oldCircleConversation.pinTime,
            ),
        )
    } else {
        update(newCircleConversation)
    }
}

suspend fun CircleDao.insertUpdateSuspend(
    circle: Circle,
) {
    withTransaction {
        val c = findCircleById(circle.circleId)
        if (c == null) {
            insert(circle)
        } else {
            update(circle)
        }
    }
}

fun CircleDao.insertUpdate(
    circle: Circle,
) {
    runInTransaction {
        val c = findCircleById(circle.circleId)
        if (c == null) {
            insert(circle)
        } else {
            update(circle)
        }
    }
}

suspend fun StickerAlbumDao.insertUpdate(
    album: StickerAlbum,
) {
    withTransaction {
        val a = findAlbumById(album.albumId)
        if (a == null) {
            insert(album)
        } else {
            update(album)
        }
    }
}

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

suspend fun OutputDao.insertUnspentOutputs(outputs: List<Output>) =
    withContext(SINGLE_DB_THREAD) {
        runInTransaction {
            val signed = findSignedOutput(outputs.map { it.outputId })
            if (signed.isEmpty()) {
                insertList(outputs)
            } else {
                Timber.e("Insert filter ${signed.joinToString(", ") }")
                // Exclude signed data
                val list =
                    outputs.filter {
                        signed.contains(it.outputId)
                    }
                insertList(list)
            }
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
