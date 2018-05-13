package one.mixin.android.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.Transaction
import one.mixin.android.vo.MessageHistory

@Dao
interface MessageHistoryDao : BaseDao<MessageHistory> {

    @Transaction
    @Query("SELECT * FROM messages_history WHERE message_id = :messageId")
    fun findMessageHistoryById(messageId: String): MessageHistory?
}
