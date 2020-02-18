package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.MentionMessage

@Dao
interface MentionMessageDao : BaseDao<MentionMessage> {
    @Query("SELECT * FROM mention_message WHERE conversation_id = :conversationId")
    fun getUnreadMentionMessageByConversationId(conversationId: String): LiveData<List<MentionMessage>>
}
