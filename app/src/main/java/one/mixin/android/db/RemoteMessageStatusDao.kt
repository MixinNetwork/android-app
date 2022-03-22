package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.Constants.MARK_REMOTE_LIMIT
import one.mixin.android.vo.RemoteMessageStatus

@Dao
interface RemoteMessageStatusDao : BaseDao<RemoteMessageStatus> {
    @Query("SELECT * FROM remote_messages_status WHERE status = 'READ' LIMIT $MARK_REMOTE_LIMIT")
    fun findRemoteMessageStatus(): List<RemoteMessageStatus>

    @Query("UPDATE remote_messages_status SET status = 'READ' WHERE conversation_id = :conversationId")
    fun markReadByConversationId(conversationId: String)

    @Query("UPDATE conversations SET unseen_message_count = (SELECT count(1) FROM remote_messages_status WHERE conversation_id = :conversationId AND status == 'DELIVERED') WHERE conversation_id = :conversationId")
    fun updateConversationUnseen(conversationId: String)

    @Query("SELECT count(1) FROM remote_messages_status WHERE conversation_id = :conversationId AND status == 'DELIVERED'")
    fun countUnread(conversationId: String): Int

    @Query("DELETE FROM remote_messages_status WHERE message_id = :messageId")
    fun deleteByMessageId(messageId: String)

    @Query("DELETE FROM remote_messages_status WHERE message_id IN (:messageIds)")
    fun deleteByMessageIds(messageIds: List<String>)
}
