package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.RemoteMessageStatus

@Dao
interface RemoteMessageStatusDao : BaseDao<RemoteMessageStatus> {
    @Query("SELECT * FROM remote_messages_status WHERE status = 'READ' LIMIT 100")
    fun findRemoteMessageStatus(): List<RemoteMessageStatus>

    @Query("UPDATE remote_messages_status SET status = 'READ' WHERE conversation_id = :conversationId")
    suspend fun markReadByConversationId(conversationId: String)

    @Query("UPDATE conversations SET unseen_message_count = (SELECT count(1) FROM remote_messages_status WHERE conversation_id = :conversationId AND status != 'READ') WHERE conversation_id = :conversationId")
    fun updateConversationUnseen(conversationId: String)
}
