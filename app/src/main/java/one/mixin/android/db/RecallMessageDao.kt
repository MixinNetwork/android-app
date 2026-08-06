package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.RecallMessage

@Dao
interface RecallMessageDao : BaseDao<RecallMessage> {
    @Query("DELETE FROM recall_messages WHERE message_id = :messageId")
    fun deleteByMessageId(messageId: String)

    @Query("DELETE FROM recall_messages WHERE message_id IN (:messageIds)")
    fun deleteByMessageIds(messageIds: List<String>)

    @Query("SELECT * FROM recall_messages WHERE rowid > :rowId ORDER BY rowid ASC LIMIT :limit")
    fun getRecallMessagesByLimitAndRowId(
        limit: Int,
        rowId: Long,
    ): List<RecallMessage>

    @Query("SELECT rowid FROM recall_messages WHERE message_id = :messageId")
    fun getRecallMessageRowId(messageId: String): Long?

    @Query("SELECT count(1) FROM recall_messages")
    fun countRecallMessages(): Long

    @Query("SELECT count(1) FROM recall_messages WHERE rowid > :rowId")
    fun countRecallMessages(rowId: Long): Long
}
