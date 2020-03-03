package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.MessageFts4

@Dao
interface MessagesFts4Dao : BaseDao<MessageFts4> {

    @Query("DELETE FROM messages_fts4 WHERE message_id = :messageId")
    fun deleteByMessageId(messageId: String)
}
