package one.mixin.android.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.RoomWarnings
import android.arch.persistence.room.Transaction
import io.reactivex.Maybe
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.ConversationItemMinimal

@Dao
interface ConversationDao : BaseDao<Conversation> {

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT c.conversation_id as conversationId, c.icon_url as groupIconUrl, c.category as category, " +
        "c.name as groupName, c.status as status, c.last_read_message_id as lastReadMessageId, " +
        "c.unseen_message_count as unseenMessageCount, c.owner_id as ownerId, c.pin_time as pinTime, c.mute_until as muteUntil, " +
        "ou.avatar_url as avatarUrl, ou.full_name as name, ou.is_verified as ownerVerified, " +
        "ou.identity_number as ownerIdentityNumber, ou.mute_until as ownerMuteUntil, ou.app_id as appId, " +
        "m.content as content, m.category as contentType, m.created_at as createdAt, m.media_url as mediaUrl, " +
        "m.user_id as senderId, m.action as actionName, m.status as messageStatus, " +
        "mu.full_name as senderFullName, s.type as SnapshotType,  " +
        "pu.full_name as participantFullName, pu.user_id as participantUserId " +
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
    @Query("SELECT c.conversation_id as conversationId, c.icon_url as groupIconUrl, c.category as category, c.name as groupName, " +
        "ou.identity_number as ownerIdentityNumber " +
        "FROM conversations c " +
        "INNER JOIN users ou ON ou.user_id = c.owner_id " +
        "WHERE c.category = 'GROUP' AND c.status != 'SUCCESS' AND c.name LIKE :query ORDER BY c.created_at DESC")
    fun fuzzySearchGroup(query: String): List<ConversationItemMinimal>

    @Query("SELECT DISTINCT c.conversation_id FROM conversations c WHERE c.owner_id = :recipientId and c.category = 'CONTACT'")
    fun getConversationIdIfExistsSync(recipientId: String): String?

    @Query("SELECT c.* FROM conversations c WHERE c.conversation_id = :conversationId")
    fun getConversationById(conversationId: String): LiveData<Conversation>

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

    @Query("UPDATE conversations SET last_read_message_id = :messageId WHERE conversation_id = :conversationId")
    fun updateLastReadMessageId(conversationId: String, messageId: String)

    @Query("UPDATE conversations SET code_url = :codeUrl WHERE conversation_id = :conversationId")
    fun updateCodeUrl(conversationId: String, codeUrl: String)

    @Query("UPDATE conversations SET status = :status WHERE conversation_id = :conversationId")
    fun updateConversationStatusById(conversationId: String, status: Int)

    @Query("UPDATE conversations SET pin_time = :pinTime WHERE conversation_id = :conversationId")
    fun updateConversationPinTimeById(conversationId: String, pinTime: String?)

    @Query("UPDATE conversations SET owner_id = :ownerId, category = :category, name = :name, announcement = :announcement, " +
        "created_at = :createdAt, status = :status WHERE conversation_id = :conversationId")
    fun updateConversation(
        conversationId: String,
        ownerId: String,
        category: String,
        name: String,
        announcement: String?,
        createdAt: String,
        status: Int
    )

    @Query("UPDATE conversations SET announcement = :announcement WHERE conversation_id = :conversationId")
    fun updateConversationAnnouncement(conversationId: String, announcement: String)

    @Query("UPDATE messages SET status = 'READ' WHERE conversation_id = :conversationId AND user_id != :userId AND status = 'DELIVERED'")
    fun makeMessageReadByConversationId(conversationId: String, userId: String)

    @Query("DELETE FROM conversations WHERE conversation_id = :conversationId")
    fun deleteConversationById(conversationId: String)

    @Query("UPDATE conversations SET mute_until = :muteUntil WHERE conversation_id = :conversationId")
    fun updateGroupDuration(conversationId: String, muteUntil: String)

    @Query("UPDATE conversations SET icon_url = :iconUrl WHERE conversation_id = :conversationId")
    fun updateGroupIconUrl(conversationId: String, iconUrl: String)

    @Query("SELECT icon_url FROM conversations WHERE conversation_id = :conversationId")
    fun getGroupIconUrl(conversationId: String): String?
}