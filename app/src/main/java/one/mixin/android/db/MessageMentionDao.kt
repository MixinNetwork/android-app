package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import one.mixin.android.ui.transfer.vo.compatible.TransferMessageMention
import one.mixin.android.vo.MessageMention

@Dao
interface MessageMentionDao : BaseDao<MessageMention> {
    @Query("SELECT count(1) FROM message_mentions WHERE conversation_id = :conversationId AND has_read = 0")
    fun countUnreadMentionMessageByConversationId(conversationId: String): LiveData<Int>

    @Query("SELECT * FROM message_mentions WHERE conversation_id = :conversationId AND has_read = 0 ORDER BY rowid ASC LIMIT 1")
    suspend fun getFirstUnreadMentionMessageByConversationId(conversationId: String): MessageMention?

    @Query("UPDATE message_mentions SET has_read = 1 WHERE message_id = :messageId")
    suspend fun suspendMarkMentionRead(messageId: String)

    @Query("UPDATE message_mentions SET has_read = 1 WHERE message_id = :messageId")
    fun markMentionRead(messageId: String)

    @Query("UPDATE message_mentions SET has_read = 1 WHERE conversation_id = :conversationId")
    fun markMentionReadByConversationId(conversationId: String)

    @Query("SELECT mentions FROM message_mentions WHERE message_id = :messageId")
    fun getMentionData(messageId: String): String?

    // DELETE COUNT
    @Query("SELECT count(1) FROM message_mentions WHERE conversation_id = :conversationId")
    suspend fun countDeleteMessageByConversationId(conversationId: String): Int

    // DELETE
    @Query("DELETE FROM message_mentions WHERE message_id = :id")
    fun deleteMessage(id: String)

    @Query("DELETE FROM message_mentions WHERE message_id IN (:ids)")
    fun deleteMessage(ids: List<String>)

    @Query("DELETE FROM message_mentions WHERE message_id IN (SELECT message_id FROM message_mentions WHERE conversation_id=:conversationId LIMIT :limit)")
    suspend fun deleteMessageByConversationId(
        conversationId: String,
        limit: Int,
    )

    @Query("DELETE FROM message_mentions WHERE message_id IN (SELECT message_id FROM message_mentions WHERE conversation_id=:conversationId LIMIT :limit)")
    fun deleteMessageByConversationIdSync(
        conversationId: String,
        limit: Int,
    )

    @Query("SELECT * FROM message_mentions WHERE message_id = :id")
    fun findMessageMentionById(id: String): MessageMention?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT mm.* FROM message_mentions mm WHERE mm.rowid > :rowId ORDER BY mm.rowid ASC LIMIT :limit")
    fun getMessageMentionByLimitAndRowId(
        limit: Int,
        rowId: Long,
    ): List<TransferMessageMention>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT mm.* FROM message_mentions mm WHERE mm.rowid > :rowId AND conversation_id IN (:conversationIds) ORDER BY mm.rowid ASC LIMIT :limit")
    fun getMessageMentionByLimitAndRowId(
        limit: Int,
        rowId: Long,
        conversationIds: Collection<String>,
    ): List<TransferMessageMention>

    @Query("SELECT rowid FROM message_mentions WHERE message_id = :messageId")
    fun getMessageMentionRowId(messageId: String): Long?

    @Query("SELECT count(1) FROM message_mentions")
    fun countMessageMention(): Long

    @Query("SELECT count(1) FROM message_mentions WHERE conversation_id IN (:conversationIds)")
    fun countMessageMention(conversationIds: Collection<String>): Long

    @Query("SELECT count(1) FROM message_mentions WHERE rowid > :rowId")
    fun countMessageMention(rowId: Long): Long

    @Query("SELECT count(1) FROM message_mentions WHERE rowid > :rowId AND conversation_id IN (:conversationIds)")
    fun countMessageMention(
        rowId: Long,
        conversationIds: Collection<String>,
    ): Long
}
