package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.PinMessage
import one.mixin.android.vo.PinMessageItem
import one.mixin.android.vo.PinMessageMinimal

@Dao
interface PinMessageDao : BaseDao<PinMessage> {
    @Query("DELETE FROM pin_messages WHERE message_id IN (:messageIds)")
    fun deleteByIds(messageIds: List<String>)

    @Query("DELETE FROM pin_messages WHERE message_id = :messageId")
    fun deleteByMessageId(messageId: String)

    @Query("DELETE FROM pin_messages WHERE conversation_id = :conversationId")
    fun deleteConversationId(conversationId: String)

    @Query("SELECT * FROM pin_messages WHERE message_id = :messageId")
    suspend fun findPinMessageById(messageId: String): PinMessage?

    @Query(
        """
        SELECT m.id AS messageId, m.category AS type, m.content AS content FROM pin_messages pm
        INNER JOIN messages m ON m.id = pm.message_id
        WHERE pm.conversation_id = :conversationId
        """,
    )
    suspend fun getPinMessageMinimals(conversationId: String): List<PinMessageMinimal>

    @Query("SELECT count(1) FROM pin_messages pm INNER JOIN messages m ON m.id = pm.message_id WHERE m.created_at < (SELECT created_at FROM messages WHERE conversation_id = :conversationId AND id = :messageId) AND pm.conversation_id = :conversationId")
    suspend fun findPinMessageIndex(
        conversationId: String,
        messageId: String,
    ): Int

    @Query(
        """
        SELECT u.user_id, m.content, mm.mentions, u.full_name FROM pin_messages pm
        INNER JOIN users u ON m.user_id = u.user_id      
        INNER JOIN messages m ON m.quote_message_id = pm.message_id
        LEFT JOIN message_mentions mm ON m.id = mm.message_id
        WHERE m.conversation_id = :conversationId AND m.category = 'MESSAGE_PIN'
        ORDER BY m.created_at DESC
        LIMIT 1
        """,
    )
    fun getLastPinMessages(conversationId: String): LiveData<PinMessageItem?>

    @Query("SELECT count(1) FROM pin_messages pm INNER JOIN messages m ON m.id = pm.message_id WHERE pm.conversation_id = :conversationId")
    fun countPinMessages(conversationId: String): LiveData<Int>

    @Query("SELECT pm.* FROM pin_messages pm WHERE pm.rowid > :rowId ORDER BY pm.rowid ASC LIMIT :limit")
    fun getPinMessageByLimitAndRowId(
        limit: Int,
        rowId: Long,
    ): List<PinMessage>

    @Query("SELECT pm.* FROM pin_messages pm WHERE pm.rowid > :rowId AND conversation_id IN (:conversationIds) ORDER BY pm.rowid ASC LIMIT :limit")
    fun getPinMessageByLimitAndRowId(
        limit: Int,
        rowId: Long,
        conversationIds: Collection<String>,
    ): List<PinMessage>

    @Query("SELECT rowid FROM pin_messages WHERE message_id = :messageId")
    fun getPinMessageRowId(messageId: String): Long?

    @Query("SELECT rowid FROM pin_messages WHERE created_at >= :createdAt LIMIT 1")
    fun getMessageRowidByCreateAt(createdAt: String): Long?

    @Query("SELECT count(1) FROM pin_messages")
    fun countPinMessages(): Long

    @Query("SELECT count(1) FROM pin_messages WHERE conversation_id IN (:conversationIds)")
    fun countPinMessages(conversationIds: Collection<String>): Long

    @Query("SELECT count(1) FROM pin_messages WHERE rowid > :rowId")
    fun countPinMessages(rowId: Long): Long

    @Query("SELECT count(1) FROM pin_messages WHERE rowid > :rowId AND conversation_id IN (:conversationIds)")
    fun countPinMessages(
        rowId: Long,
        conversationIds: Collection<String>,
    ): Long
}
