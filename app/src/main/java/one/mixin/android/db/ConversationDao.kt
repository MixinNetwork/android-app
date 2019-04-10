package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Transaction
import io.reactivex.Maybe
import io.reactivex.Single
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.ConversationItemMinimal
import one.mixin.android.vo.ConversationStorageUsage
import one.mixin.android.vo.StorageUsage

@Dao
interface ConversationDao : BaseDao<Conversation> {

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT c.conversation_id AS conversationId, c.icon_url AS groupIconUrl, c.category AS category, " +
        "c.name AS groupName, c.status AS status, c.last_read_message_id AS lastReadMessageId, " +
        "c.unseen_message_count AS unseenMessageCount, c.owner_id AS ownerId, c.pin_time AS pinTime, c.mute_until AS muteUntil, " +
        "ou.avatar_url AS avatarUrl, ou.full_name AS name, ou.is_verified AS ownerVerified, " +
        "ou.identity_number AS ownerIdentityNumber, ou.mute_until AS ownerMuteUntil, ou.app_id AS appId, " +
        "m.content AS content, m.category AS contentType, m.created_at AS createdAt, m.media_url AS mediaUrl, " +
        "m.user_id AS senderId, m.action AS actionName, m.status AS messageStatus, " +
        "mu.full_name AS senderFullName, s.type AS SnapshotType,  " +
        "pu.full_name AS participantFullName, pu.user_id AS participantUserId " +
        "FROM conversations c " +
        "INNER JOIN users ou ON ou.user_id = c.owner_id " +
        "LEFT JOIN messages m ON c.last_message_id = m.id " +
        "LEFT JOIN users mu ON mu.user_id = m.user_id " +
        "LEFT JOIN snapshots s ON s.snapshot_id = m.snapshot_id " +
        "LEFT JOIN users pu ON pu.user_id = m.participant_id " +
        "WHERE c.category IS NOT NULL " +
        "ORDER BY c.pin_time DESC, m.created_at DESC")
    fun conversationList(): LiveData<List<ConversationItem>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT c.conversation_id AS conversationId, c.icon_url AS groupIconUrl, c.category AS category, c.name AS groupName, " +
        "ou.identity_number AS ownerIdentityNumber " +
        "FROM conversations c " +
        "INNER JOIN users ou ON ou.user_id = c.owner_id " +
        "WHERE c.category = 'GROUP' AND c.status != 'SUCCESS' AND c.name LIKE :query ORDER BY c.created_at DESC")
    fun fuzzySearchGroup(query: String): List<ConversationItemMinimal>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT c.conversation_id AS conversationId, c.icon_url AS groupIconUrl, c.category AS category, c.name AS groupName, " +
        "ou.identity_number AS ownerIdentityNumber, c.owner_id AS userId, ou.full_name AS fullName, ou.avatar_url AS avatarUrl, " +
        "ou.is_verified AS isVerified, ou.app_id AS appId "+
        "FROM conversations c " +
        "INNER JOIN users ou ON ou.user_id = c.owner_id " +
        "WHERE (c.category = 'GROUP' AND c.status != 'SUCCESS' AND c.name LIKE :query) " +
        "OR (c.category = 'CONTACT' AND ou.full_name LIKE :query) " +
        "ORDER BY c.created_at DESC")
    fun fuzzySearchChat(query: String): List<ChatMinimal>

    @Query("SELECT DISTINCT c.conversation_id FROM conversations c WHERE c.owner_id = :recipientId and c.category = 'CONTACT'")
    fun getConversationIdIfExistsSync(recipientId: String): String?

    @Query("SELECT c.* FROM conversations c WHERE c.conversation_id = :conversationId")
    fun getConversationById(conversationId: String): LiveData<Conversation>

    @Query("SELECT unseen_message_count FROM conversations WHERE conversation_id = :conversationId")
    fun indexUnread(conversationId: String): Int

    @Transaction
    @Query("SELECT c.* FROM conversations c WHERE c.conversation_id = :conversationId")
    fun findConversationById(conversationId: String): Conversation?

    @Query("SELECT c.* FROM conversations c WHERE c.conversation_id = :conversationId")
    fun searchConversationById(conversationId: String): Maybe<Conversation>

    @Query("UPDATE conversations SET draft = :text WHERE conversation_id = :conversationId")
    fun saveDraft(conversationId: String, text: String)

    @Transaction
    @Query("SELECT c.* FROM conversations c WHERE c.conversation_id = :conversationId")
    fun getConversation(conversationId: String): Conversation?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT c.conversation_id AS conversationId, c.icon_url AS groupIconUrl, c.category AS category, " +
        "c.name AS groupName, c.status AS status, c.last_read_message_id AS lastReadMessageId, " +
        "c.unseen_message_count AS unseenMessageCount, c.owner_id AS ownerId, c.pin_time AS pinTime, c.mute_until AS muteUntil, " +
        "ou.avatar_url AS avatarUrl, ou.full_name AS name, ou.is_verified AS ownerVerified, " +
        "ou.identity_number AS ownerIdentityNumber, ou.mute_until AS ownerMuteUntil " +
        "FROM conversations c " +
        "INNER JOIN users ou ON ou.user_id = c.owner_id " +
        "WHERE c.conversation_id = :conversationId")
    fun getConversationItem(conversationId: String): ConversationItem?

    @Query("UPDATE conversations SET code_url = :codeUrl WHERE conversation_id = :conversationId")
    fun updateCodeUrl(conversationId: String, codeUrl: String)

    @Query("UPDATE conversations SET status = :status WHERE conversation_id = :conversationId")
    fun updateConversationStatusById(conversationId: String, status: Int)

    @Query("UPDATE conversations SET pin_time = :pinTime WHERE conversation_id = :conversationId")
    fun updateConversationPinTimeById(conversationId: String, pinTime: String?)

    @Query("UPDATE conversations SET owner_id = :ownerId, category = :category, name = :name, announcement = :announcement, " +
        "mute_until = :muteUntil, created_at = :createdAt, status = :status WHERE conversation_id = :conversationId")
    fun updateConversation(
        conversationId: String,
        ownerId: String,
        category: String,
        name: String,
        announcement: String?,
        muteUntil: String?,
        createdAt: String,
        status: Int
    )

    @Query("UPDATE conversations SET announcement = :announcement WHERE conversation_id = :conversationId")
    fun updateConversationAnnouncement(conversationId: String, announcement: String)

    @Query("DELETE FROM conversations WHERE conversation_id = :conversationId")
    fun deleteConversationById(conversationId: String)

    @Query("UPDATE conversations SET mute_until = :muteUntil WHERE conversation_id = :conversationId")
    fun updateGroupDuration(conversationId: String, muteUntil: String)

    @Query("UPDATE conversations SET icon_url = :iconUrl WHERE conversation_id = :conversationId")
    fun updateGroupIconUrl(conversationId: String, iconUrl: String)

    @Query("SELECT icon_url FROM conversations WHERE conversation_id = :conversationId")
    fun getGroupIconUrl(conversationId: String): String?

    @Query("SELECT   c.conversation_id as conversationId, c.owner_id as ownerId, c.category, c.icon_url as groupIconUrl, c.name as groupName, " +
        "u.identity_number as ownerIdentityNumber,u.full_name as name, u.avatar_url as avatarUrl, u.is_verified as ownerIsVerified, m.mediaSize " +
        "FROM conversations c " +
        "INNER JOIN (SELECT conversation_id, sum(media_size) as mediaSize FROM messages WHERE IFNULL(media_size,'') != '' GROUP BY conversation_id) m " +
        "ON m.conversation_id = c.conversation_id " +
        "INNER JOIN users u ON u.user_id = c.owner_id " +
        "ORDER BY m.mediaSize DESC")
    fun getConversationStorageUsage(): LiveData<List<ConversationStorageUsage>?>

    @Query("SELECT category, sum(media_size) as mediaSize ,conversation_id as conversationId, count(id) as count FROM messages " +
        "WHERE conversation_id = :conversationId AND IFNULL(media_size,'') != '' GROUP BY category")
    fun getStorageUsage(conversationId: String): Single<List<StorageUsage>?>
}