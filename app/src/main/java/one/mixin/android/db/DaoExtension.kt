package one.mixin.android.db

import one.mixin.android.vo.App
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.User

fun UserDao.insertUpdate(
    user: User,
    appDao: AppDao
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
    appDao: AppDao
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
    relationship: String
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

fun MixinDatabase.clearParticipant(
    conversationId: String,
    participantId: String
) {
    runInTransaction {
        participantDao().deleteById(conversationId, participantId)
        participantSessionDao().deleteByUserId(conversationId, participantId)
        participantSessionDao().emptyStatusByConversationId(conversationId)
    }
}

fun MessageDao.batchMarkReadAndTake(
    conversationId: String,
    userId: String,
    createdAt: String
) {
    runInTransaction {
        batchMarkRead(conversationId, userId, createdAt)
        takeUnseen(userId, conversationId)
    }
}

fun MixinDatabase.deleteMessage(id: String) {
    runInTransaction {
        messageDao().deleteMessage(id)
        mentionMessageDao().deleteMessage(id)
        messageFts4Dao().deleteByMessageId(id)
    }
}
