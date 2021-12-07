package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.MessageFts4

@Dao
interface MessagesFts4Dao : BaseDao<MessageFts4> {

    @Query("DELETE FROM messages_fts4 WHERE message_id = :messageId")
    fun deleteByMessageId(messageId: String)

    @Query("DELETE FROM messages_fts4 WHERE message_id IN (:messageIds)")
    suspend fun deleteByMessageIds(messageIds: List<String>)

    @Query("SELECT message_id FROM messages_fts4 WHERE message_id NOT IN (SELECT id FROM messages) LIMIT :limit")
    suspend fun excludeIds(limit: Int): List<String>
}
