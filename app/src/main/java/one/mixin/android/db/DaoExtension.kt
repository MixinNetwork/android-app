package one.mixin.android.db

import one.mixin.android.vo.App
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.User

suspend fun UserDao.insertUpdate(
    user: User,
    appDao: AppDao
) = withTransaction {
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

suspend fun UserDao.insertUpdateList(
    users: List<User>,
    appDao: AppDao
) = withTransaction {
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

suspend fun ConversationDao.insertConversation(
    conversation: Conversation,
    action: (() -> Unit)? = null
) = withTransaction {
    val c = findConversationById(conversation.conversationId)
    if (c == null) {
        insert(conversation)
    }
    action?.invoke()
}

suspend fun UserDao.updateRelationship(
    user: User,
    relationship: String
) = withTransaction {
    val u = findUser(user.userId)
    if (u == null) {
        insert(user)
    } else {
        user.relationship = relationship
        update(user)
    }
}

suspend fun StickerDao.insertUpdate(
    s: Sticker,
    action: (() -> Unit)? = null
) = withTransaction {
    val sticker = getStickerByUnique(s.stickerId)
    if (sticker != null) {
        s.lastUseAt = sticker.lastUseAt
    }
    if (s.createdAt == "") {
        s.createdAt = System.currentTimeMillis().toString()
    }
    insert(s)
    action?.invoke()
}

suspend fun MixinDatabase.clearParticipant(
    conversationId: String,
    participantId: String
) = withTransaction {
    participantDao().deleteById(conversationId, participantId)
    sentSenderKeyDao().deleteByConversationId(conversationId)
}

suspend fun MessageDao.batchMarkReadAndTake(
    conversationId: String,
    userId: String,
    createdAt: String
) = withTransaction {
    batchMarkRead(conversationId, userId, createdAt)
    takeUnseen(userId, conversationId)
}
