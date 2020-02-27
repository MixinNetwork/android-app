package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import one.mixin.android.db.BaseDao.Companion.ESCAPE_SUFFIX
import one.mixin.android.vo.MentionUser
import one.mixin.android.vo.User

@Dao
interface UserDao : BaseDao<User> {

    @Query("SELECT * FROM users WHERE relationship = 'FRIEND' ORDER BY full_name, user_id ASC")
    fun findFriends(): LiveData<List<User>>

    @Query("SELECT * FROM users WHERE relationship = 'FRIEND' ORDER BY full_name, user_id ASC")
    suspend fun getFriends(): List<User>

    @Query("SELECT * FROM users WHERE user_id = :id")
    fun findUserById(id: String): LiveData<User>

    @Query("SELECT * FROM users WHERE user_id = :id")
    fun findSelf(id: String): LiveData<User?>

    @Query("SELECT * FROM users WHERE user_id = :id")
    fun findUser(id: String): User?

    @Query("SELECT * FROM users WHERE user_id = :id")
    suspend fun suspendFindUserById(id: String): User?

    @Query("SELECT user_id FROM users WHERE user_id IN (:userIds)")
    suspend fun findUserExist(userIds: List<String>): List<String>

    @Query("SELECT * FROM users WHERE relationship = :relationship")
    fun findUsersByType(relationship: String): LiveData<List<User>>

    @Query("SELECT u.* FROM users u, conversations c WHERE c.owner_id = u.user_id AND c.conversation_id = :conversationId")
    fun findUserByConversationId(conversationId: String): LiveData<User>

    @Query("SELECT u.* FROM users u, conversations c WHERE c.owner_id = u.user_id AND c.conversation_id = :conversationId")
    fun findPlainUserByConversationId(conversationId: String): User?

    @Query("SELECT u.* FROM users u, conversations c WHERE c.owner_id = u.user_id AND c.conversation_id = :conversationId AND c.category = 'CONTACT'")
    fun findContactByConversationId(conversationId: String): User?

    @Query("""
        SELECT * FROM users 
        WHERE user_id != :id 
        AND relationship = 'FRIEND' 
        AND (full_name LIKE '%' || :username || '%' $ESCAPE_SUFFIX OR identity_number like '%' || :identityNumber || '%' $ESCAPE_SUFFIX)
        ORDER BY 
            full_name = :username COLLATE NOCASE OR identity_number = :identityNumber COLLATE NOCASE DESC
        """)
    suspend fun fuzzySearchUser(username: String, identityNumber: String, id: String): List<User>

    @Query("""
        SELECT u.* FROM participants p, users u
        WHERE u.user_id != :id 
        AND p.conversation_id = :conversationId AND p.user_id = u.user_id
        AND (u.full_name LIKE '%' || :username || '%' $ESCAPE_SUFFIX OR u.identity_number like '%' || :identityNumber || '%' $ESCAPE_SUFFIX)
        ORDER BY u.full_name = :username COLLATE NOCASE OR u.identity_number = :identityNumber COLLATE NOCASE DESC
        """)
    suspend fun fuzzySearchGroupUser(conversationId: String, username: String, identityNumber: String, id: String): List<User>

    @Query("SELECT u.* FROM participants p, users u WHERE p.conversation_id = :conversationId AND p.user_id = u.user_id AND u.user_id != :id")
    suspend fun suspendGetGroupParticipants(conversationId: String, id: String): List<User>

    @Query("UPDATE users SET relationship = :relationship WHERE user_id = :id")
    fun updateUserRelationship(id: String, relationship: String)

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT u.user_id, u.identity_number, u.biography, u.full_name, u.relationship FROM participants p, users u " +
        "WHERE p.conversation_id = :conversationId AND p.user_id = u.user_id")
    fun getGroupParticipants(conversationId: String): LiveData<List<User>>

    @Query("UPDATE users SET mute_until = :muteUntil WHERE user_id = :id")
    fun updateDuration(id: String, muteUntil: String)

    @Query("UPDATE users SET phone = :phone WHERE user_id = :id")
    fun updatePhone(id: String, phone: String)

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT u.* FROM users u INNER JOIN conversations c ON c.owner_id = u.user_id WHERE c.category = 'CONTACT' AND u.app_id IS NULL")
    fun findContactUsers(): LiveData<List<User>>

    @Query("SELECT * FROM users WHERE relationship = 'FRIEND' AND app_id IS NULL")
    suspend fun getFriendsNotBot(): List<User>

    @Query("SELECT * FROM users WHERE relationship = 'FRIEND' AND app_id IS NULL")
    fun findFriendsNotBot(): LiveData<List<User>>

    @Query("SELECT u.user_id FROM users u" +
            "   INNER JOIN participants p ON p.user_id = u.user_id" +
            "   WHERE p.conversation_id = :conversationId AND u.identity_number = :appNumber")
    fun findUserIdByAppNumber(conversationId: String, appNumber: String): String?

    @Query("SELECT * FROM users WHERE user_id IN (:userIds)")
    suspend fun findMultiUsersByIds(userIds: Set<String>): List<User>

    @Query("SELECT user_id FROM users WHERE identity_number IN (:identityNumbers)")
    fun findMultiUserIdsByIdentityNumbers(identityNumbers: Set<String>): List<String>

    @Query("SELECT * FROM users WHERE identity_number = :identityNumber LIMIT 1")
    suspend fun suspendFindUserByIdentityNumber(identityNumber: String): User?

    @Query("SELECT * FROM users WHERE identity_number IN (:numbers)")
    fun findUserByIdentityNumbers(numbers: Set<String>): List<MentionUser>
}
