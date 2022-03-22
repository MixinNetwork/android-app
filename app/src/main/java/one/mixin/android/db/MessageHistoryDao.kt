package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.MessageHistory

@Dao
interface MessageHistoryDao : BaseDao<MessageHistory> {

    @Query("SELECT * FROM messages_history WHERE message_id = :messageId")
    fun findMessageHistoryById(messageId: String): MessageHistory?
}
