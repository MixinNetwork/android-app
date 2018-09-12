package one.mixin.android.db

import android.arch.persistence.room.Transaction
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.FloodMessage
import one.mixin.android.vo.Job
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.User

@Transaction
fun UserDao.insertUpdate(user: User, appDao: AppDao) {
    if (user.app != null) {
        user.appId = user.app!!.appId
        appDao.insert(user.app!!)
    }
    val u = findUser(user.userId)
    if (u == null) {
        insert(user)
    } else {
        user.muteUntil = u.muteUntil
        update(user)
    }
}

@Transaction
fun ConversationDao.insertConversation(conversation: Conversation, action: (() -> Unit)? = null, haveAction: ((Conversation) -> Unit)? = null) {
    val c = findConversationById(conversation.conversationId)
    if (c == null) {
        insert(conversation)
        action?.let { it() }
    } else {
        haveAction?.let { it(c) }
    }
}

@Transaction
fun UserDao.updateRelationship(user: User, relationship: String) {
    val u = findUser(user.userId)
    if (u == null) {
        insert(user)
    } else {
        user.relationship = relationship
        update(user)
    }
}

@Transaction
fun StickerDao.insertUpdate(s: Sticker) {
    val sticker = getStickerByUnique(s.stickerId)
    if (sticker != null) {
        s.lastUseAt = sticker.lastUseAt
    }
    if (s.createdAt == "") {
        s.createdAt = System.currentTimeMillis().toString()
    }
    insert(s)
}

@Transaction
fun MixinDatabase.clearParticipant(conversationId: String, participantId: String) {
    participantDao().deleteById(conversationId, participantId)
    sentSenderKeyDao().delete(conversationId)
}

fun JobDao.findAckJobsDeferred(): Deferred<List<Job>?> = async {
    findAckJobsSync()
}

fun FloodMessageDao.findFloodMessageDeferred(): Deferred<List<FloodMessage>?> = async {
    findFloodMessagesSync()
}

@Transaction
fun MessageDao.batchMarkReadAndTake(conversationId: String, userId: String, createdAt: String) {
    batchMarkRead(conversationId, userId, createdAt)
    takeUnseen(userId, conversationId)
}