package one.mixin.android.repository

import androidx.lifecycle.LiveData
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.service.UserService
import one.mixin.android.db.AppDao
import one.mixin.android.db.UserDao
import one.mixin.android.db.insertUpdate
import one.mixin.android.db.insertUpdateList
import one.mixin.android.db.updateRelationship
import one.mixin.android.util.SINGLE_DB_THREAD
import one.mixin.android.util.Session
import one.mixin.android.vo.App
import one.mixin.android.vo.User
import one.mixin.android.vo.UserRelationship

@Singleton
class UserRepository
@Inject
constructor(private val userDao: UserDao, private val appDao: AppDao, private val userService: UserService) {

    fun findFriends(): LiveData<List<User>> = userDao.findFriends()

    suspend fun getFriends(): List<User> = userDao.getFriends()

    fun syncFindFriends(): List<User> = userDao.syncFindFriends()

    suspend fun fuzzySearchUser(query: String): List<User> = userDao.fuzzySearchUser(query, query, Session.getAccountId() ?: "")

    fun findUserById(query: String): LiveData<User> = userDao.findUserById(query)

    suspend fun suspendFindUserById(query: String) = userDao.suspendFindUserById(query)

    fun getUserById(id: String): User? = userDao.findUser(id)

    suspend fun findUserExist(userIds: List<String>): List<String> = userDao.findUserExist(userIds)

    fun getFriend(id: String): User? = userDao.findFriend(id)

    fun getUser(id: String) = userService.getUserById(id)

    fun findUserByConversationId(conversationId: String): LiveData<User> =
        userDao.findUserByConversationId(conversationId)

    fun findContactByConversationId(conversationId: String): User? =
        userDao.findContactByConversationId(conversationId)

    fun findSelf(): LiveData<User?> = userDao.findSelf(Session.getAccountId() ?: "")

    fun relationship(request: RelationshipRequest): Observable<MixinResponse<User>> =
        userService.relationship(request)

    fun upsert(user: User) {
        GlobalScope.launch(SINGLE_DB_THREAD) {
            userDao.insertUpdate(user, appDao)
        }
    }

    fun upsertList(users: List<User>) {
        GlobalScope.launch(SINGLE_DB_THREAD) {
            userDao.insertUpdateList(users, appDao)
        }
    }

    fun insertApp(app: App) {
        GlobalScope.launch(SINGLE_DB_THREAD) {
            appDao.insert(app)
        }
    }

    fun upsertBlock(user: User) {
        GlobalScope.launch(SINGLE_DB_THREAD) {
            userDao.updateRelationship(user, UserRelationship.BLOCKING.name)
        }
    }

    fun updatePhone(id: String, phone: String) = userDao.updatePhone(id, phone)

    suspend fun findAppById(id: String) = appDao.findAppById(id)

    suspend fun searchAppByHost(query: String) = appDao.searchAppByHost("%$query%")

    fun findContactUsers() = userDao.findContactUsers()

    suspend fun getFriendsNotBot() = userDao.getFriendsNotBot()

    fun findFriendsNotBot() = userDao.findFriendsNotBot()

    fun findAppsByIds(appIds: List<String>) = appDao.findAppsByIds(appIds)
}
