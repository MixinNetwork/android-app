package one.mixin.android.db

import androidx.room3.Dao
import androidx.room3.Query
import one.mixin.android.vo.MessageHistory

@Dao
interface MessageHistoryDao : BaseDao<MessageHistory> {
    @Query("SELECT * FROM messages_history WHERE message_id = :messageId")
    fun findMessageHistoryById(messageId: String): MessageHistory?
}
