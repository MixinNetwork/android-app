package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.MentionMessage

@Dao
interface MentionMessageDao : BaseDao<MentionMessage> {
    @Query("SELECT * FROM mention_message WHERE conversation_id = :conversationId AND has_read = 0")
    fun getUnreadMentionMessageByConversationId(conversationId: String): LiveData<List<MentionMessage>>

    @Query("UPDATE mention_message SET has_read = 1 WHERE message_id = :messageId")
    suspend fun markMentionRead(messageId: String)
}
