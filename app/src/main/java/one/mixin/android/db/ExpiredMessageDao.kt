package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.ExpiredMessage

@Dao
interface ExpiredMessageDao : BaseDao<ExpiredMessage> {
    @Query("SELECT * FROM expired_messages WHERE expire_at <= :currentTime ORDER BY expire_at ASC LIMIT :limit")
    suspend fun getExpiredMessages(currentTime: Long, limit: Int): List<ExpiredMessage>

    @Query("SELECT * FROM expired_messages WHERE message_id = :messageId")
    fun getExpiredMessageById(messageId: String): ExpiredMessage?

    @Query("SELECT * FROM expired_messages WHERE message_id IN (:messageIds)")
    fun getExpiredMessageByIds(messageIds: List<String>): List<ExpiredMessage>

    @Query("SELECT * FROM expired_messages WHERE expire_at IS NOT NULL ORDER BY expire_at ASC LIMIT 1")
    suspend fun getFirstExpiredMessage(): ExpiredMessage?

    @Query("DELETE FROM expired_messages WHERE message_id IN (:messageIds)")
    suspend fun deleteByMessageIds(messageIds: List<String>)

    @Query("UPDATE expired_messages SET expire_at = :expireAt WHERE (expire_at IS NULL OR expire_at > :expireAt) AND message_id = :messageId")
    fun updateExpiredMessage(messageId: String, expireAt: Long)

    @Query("UPDATE expired_messages SET expire_at = (:currentTime + expire_in) WHERE (expire_at > (:currentTime + expire_in) OR expire_at IS NULL)  AND message_id = :messageId")
    fun markRead(messageId: String, currentTime: Long): Int

    @Query("DELETE FROM expired_messages WHERE message_id = :messageId")
    fun deleteByMessageId(messageId: String)

    @Query("DELETE FROM expired_messages WHERE message_id IN (:ids)")
    fun deleteByMessageId(ids: List<String>)

    @Query("SELECT em.* FROM expired_messages em WHERE em.rowid > :rowId ORDER BY em.rowid ASC LIMIT :limit")
    fun getExpiredMessageByLimitAndRowId(limit: Int, rowId: Long): List<ExpiredMessage>

    @Query("SELECT rowid FROM expired_messages WHERE message_id = :messageId")
    fun getExpiredMessageRowId(messageId: String): Long?

    @Query("SELECT count(1) FROM expired_messages")
    fun countExpiredMessages(): Long
}
