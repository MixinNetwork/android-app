package one.mixin.android.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import one.mixin.android.Constants.MARK_REMOTE_LIMIT
import one.mixin.android.vo.RemoteMessageStatus
import one.mixin.android.vo.StatusMessage

@Dao
interface RemoteMessageStatusDao : BaseDao<RemoteMessageStatus> {
    @Query(
        """
        SELECT rm.*, em.expire_at FROM remote_messages_status rm
        LEFT JOIN expired_messages em ON rm.message_id = em.message_id
        WHERE rm.status = 'READ'
        ORDER BY rm.rowid ASC
        LIMIT $MARK_REMOTE_LIMIT
        """,
    )
    fun findRemoteMessageStatus(): List<StatusMessage>

    @Query("UPDATE remote_messages_status SET status = 'READ' WHERE message_id IN (SELECT message_id FROM remote_messages_status WHERE conversation_id = :conversationId AND status = 'DELIVERED' LIMIT 500)")
    fun markReadByConversationId(conversationId: String): Int

    @Query("UPDATE conversations SET unseen_message_count = (SELECT count(1) FROM remote_messages_status WHERE conversation_id = :conversationId AND status == 'DELIVERED') WHERE conversation_id = :conversationId")
    fun updateConversationUnseen(conversationId: String)

    @Query("UPDATE conversations SET unseen_message_count = 0 WHERE conversation_id = :conversationId")
    fun zeroConversationUnseen(conversationId: String)

    @Query("SELECT count(1) FROM remote_messages_status WHERE message_id IN (:ids) AND status = 'DELIVERED'")
    fun countDelivered(ids: List<String>): Int

    @Query("SELECT conversation_id AS conversationId, count(1) AS count FROM remote_messages_status WHERE message_id IN (:ids) AND status = 'DELIVERED' GROUP BY conversation_id")
    fun countDeliveredByConversation(ids: List<String>): List<ConversationMessageCount>

    @Query("UPDATE conversations SET unseen_message_count = MAX(0, COALESCE(unseen_message_count + :delta, (SELECT count(1) FROM remote_messages_status WHERE conversation_id = :conversationId AND status = 'DELIVERED'))) WHERE conversation_id = :conversationId")
    fun incrementUnseen(conversationId: String, delta: Int)

    @Transaction
    fun insertDelivered(conversationId: String, messages: List<RemoteMessageStatus>) {
        if (messages.isEmpty()) return
        val uniqueMessages = messages.distinctBy { it.messageId }
        val existingCount = countDelivered(uniqueMessages.map { it.messageId })
        insertIgnoreList(uniqueMessages)
        incrementUnseen(conversationId, countDelivered(uniqueMessages.map { it.messageId }) - existingCount)
    }

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
    fun markReadBatch(conversationId: String): Int {
        val count = markReadByConversationId(conversationId)
        if (count == 0) {
            zeroConversationUnseen(conversationId)
        } else {
            incrementUnseen(conversationId, -count)
        }
        return count
    }

    fun markRead(conversationId: String) {
        while (markReadBatch(conversationId) > 0) {
            Unit
        }
    }
}
