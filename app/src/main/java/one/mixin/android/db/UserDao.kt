package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import one.mixin.android.util.Session
import one.mixin.android.vo.User

@Dao
interface UserDao : BaseDao<User> {

    @Query("SELECT * FROM users WHERE relationship = 'FRIEND'")
    fun findFriends(): LiveData<List<User>>

    @Query("SELECT * FROM users WHERE user_id = :id")
    fun findUserById(id: String): LiveData<User>

    @Query("SELECT * FROM users WHERE user_id = :id")
    fun findSelf(id: String): LiveData<User?>

    @Query("SELECT * FROM users WHERE user_id = :id")
    fun findUser(id: String): User?

    @Query("SELECT * FROM users WHERE user_id = :id AND relationship = 'FRIEND'")
    fun findFriend(id: String): User?

    @Query("SELECT * FROM users WHERE relationship = :relationship")
    fun findUsersByType(relationship: String): LiveData<List<User>>

    @Query("SELECT u.* FROM users u, conversations c WHERE c.owner_id = u.user_id AND c.conversation_id = :conversationId")
    fun findUserByConversationId(conversationId: String): LiveData<User>

    @Query("SELECT u.* FROM users u, conversations c WHERE c.owner_id = u.user_id AND c.conversation_id = :conversationId")
    fun findPlainUserByConversationId(conversationId: String): User?

    @Query("SELECT u.* FROM users u, conversations c WHERE c.owner_id = u.user_id AND c.conversation_id = :conversationId AND c.category = 'CONTACT'")
    fun findContactByConversationId(conversationId: String): User?

    @Query("SELECT * FROM users WHERE user_id != :id AND relationship = 'FRIEND' AND (full_name LIKE :username OR " +
        "identity_number like :identityNumber)")
    fun fuzzySearchUser(username: String, identityNumber: String, id: String = Session.getAccountId()!!): List<User>

    @Query("SELECT * FROM users WHERE relationship = 'FRIEND'")
    fun syncFindFriends(): List<User>

    @Query("UPDATE users SET relationship = :relationship WHERE user_id = :id")
    fun updateUserRelationship(id: String, relationship: String)

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT u.user_id, u.identity_number, u.full_name, u.relationship FROM participants p, users u " +
        "WHERE p.conversation_id = :conversationId AND p.user_id = u.user_id")
    fun getGroupParticipants(conversationId: String): LiveData<List<User>>

    @Query("UPDATE users SET mute_until = :muteUntil WHERE user_id = :id")
    fun updateDuration(id: String, muteUntil: String)

    @Query("UPDATE users SET phone = :phone WHERE user_id = :id")
    fun updatePhone(id: String, phone: String)

    @Query("SELECT * FROM users u INNER JOIN conversations c ON c.owner_id = u.user_id WHERE c.category = 'CONTACT' AND u.app_id IS NULL")
    fun findContactUsers(): LiveData<List<User>>

    @Query("SELECT * FROM users WHERE relationship = 'FRIEND' AND app_id IS NULL")
    fun findFriendsNotBot(): LiveData<List<User>>
}