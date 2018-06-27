package one.mixin.android.db

import android.arch.persistence.room.Transaction
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
fun StickerDao.insertWithCreatedAt(s: Sticker) {
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