package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.MessageMention

@Dao
interface MessageMentionDao : BaseDao<MessageMention> {
    @Query("SELECT * FROM message_mentions WHERE conversation_id = :conversationId AND has_read = 0")
    fun getUnreadMentionMessageByConversationId(conversationId: String): LiveData<List<MessageMention>>

    @Query("UPDATE message_mentions SET has_read = 1 WHERE message_id = :messageId")
    suspend fun suspendMarkMentionRead(messageId: String)

    @Query("UPDATE message_mentions SET has_read = 1 WHERE message_id = :messageId")
    fun markMentionRead(messageId: String)

    @Query("UPDATE message_mentions SET has_read = 1 WHERE conversation_id = :conversationId")
    fun markMentionReadByConversationId(conversationId: String)

    @Query("DELETE FROM message_mentions WHERE message_id = :id")
    fun deleteMessage(id: String)

    @Query("DELETE FROM message_mentions WHERE conversation_id = :conversationId")
    suspend fun deleteMessageByConversationId(conversationId: String)

    @Query("SELECT mentions FROM message_mentions WHERE message_id = :messageId")
    fun getMentionData(messageId: String): String?
}
