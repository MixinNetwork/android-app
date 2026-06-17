package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import one.mixin.android.Constants.MARK_REMOTE_LIMIT
import one.mixin.android.vo.RemoteMessageStatus
import one.mixin.android.vo.StatusMessage

@Dao
interface RemoteMessageStatusDao : BaseDao<RemoteMessageStatus> {
    @Query("SELECT rm.*, em.expire_at FROM remote_messages_status rm LEFT JOIN expired_messages em WHERE rm.status = 'READ' ORDER BY rm.rowid ASC LIMIT $MARK_REMOTE_LIMIT")
    fun findRemoteMessageStatus(): List<StatusMessage>

    @Query("UPDATE remote_messages_status SET status = 'READ' WHERE conversation_id = :conversationId")
    fun markReadByConversationId(conversationId: String)

    @Query("UPDATE conversations SET unseen_message_count = (SELECT count(1) FROM remote_messages_status WHERE conversation_id = :conversationId AND status == 'DELIVERED') WHERE conversation_id = :conversationId")
    fun updateConversationUnseen(conversationId: String)

    @Query("UPDATE conversations SET unseen_message_count = 0 WHERE conversation_id = :conversationId")
    fun zeroConversationUnseen(conversationId: String)

    @Query("SELECT count(1) FROM remote_messages_status WHERE conversation_id = :conversationId AND status == 'DELIVERED'")
    fun countUnread(conversationId: String): Int

    @Query("SELECT message_id FROM remote_messages_status WHERE conversation_id = :conversationId AND status == 'DELIVERED' ORDER BY rowid ASC LIMIT 1")
    suspend fun firstUnreadMessageId(conversationId: String): String?

    @Query("DELETE FROM remote_messages_status WHERE message_id = :messageId")
    fun deleteByMessageId(messageId: String)

    @Query("DELETE FROM remote_messages_status WHERE message_id IN (:messageIds)")
    fun deleteByMessageIds(messageIds: List<String>): Int

    @Query("SELECT message_id FROM remote_messages_status WHERE conversation_id = :conversationId AND status != 'READ' LIMIT 50")
    suspend fun getUnreadMessageIds(conversationId: String): List<String>

    @Transaction
    fun markRead(conversationId: String) {
        markReadByConversationId(conversationId)
        zeroConversationUnseen(conversationId)
    }
}
