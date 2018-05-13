package one.mixin.android.repository

import android.arch.lifecycle.LiveData
import io.reactivex.Observable
import one.mixin.android.AppExecutors
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.service.UserService
import one.mixin.android.db.AppDao
import one.mixin.android.db.UserDao
import one.mixin.android.db.insertUpdate
import one.mixin.android.db.updateRelationship
import one.mixin.android.util.Session
import one.mixin.android.vo.App
import one.mixin.android.vo.User
import one.mixin.android.vo.UserRelationship
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository
@Inject
constructor(private val userDao: UserDao, private val appDao: AppDao, private val userService: UserService) {

    fun findFriends(): LiveData<List<User>> = userDao.findFriends()

    fun fuzzySearchUser(query: String): List<User> = userDao.fuzzySearchUser(query, query)

    fun findUserById(query: String): LiveData<User> = userDao.findUserById(query)

    fun getUserById(id: String): User? = userDao.findUser(id)

    fun getFriend(id: String): User? = userDao.findFriend(id)

    fun findUserByConversationId(conversationId: String): LiveData<User> =
        userDao.findUserByConversationId(conversationId)

    fun findSelf(): LiveData<User?> = userDao.findSelf(Session.getAccountId() ?: "")

    fun relationship(request: RelationshipRequest): Observable<MixinResponse<User>> =
        userService.relationship(request)

    fun upsert(user: User) {
        AppExecutors().diskIO().execute {
            userDao.insertUpdate(user, appDao)
        }
    }

    fun insertApp(app: App) {
        AppExecutors().diskIO().execute {
            appDao.insert(app)
        }
    }

    fun upsertBlock(user: User) {
        AppExecutors().diskIO().execute {
            userDao.updateRelationship(user, UserRelationship.BLOCKING.name)
        }
    }

    fun updatePhone(id: String, phone: String) = userDao.updatePhone(id, phone)

    fun findAppById(id: String) = appDao.findAppById(id)
}