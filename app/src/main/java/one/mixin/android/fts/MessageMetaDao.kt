package one.mixin.android.fts

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.BaseDao

@Dao
interface MessageMetaDao : BaseDao<MessagesMeta> {
    @Query("DELETE FROM messages_metas WHERE message_id = :messageId")
    fun deleteMessageMetasByMessageId(messageId: String)

    @Query("DELETE FROM messages_metas WHERE message_id IN (:messageIds)")
    fun deleteMessageMetasByMessageIds(messageIds: List<String>)

    @Query("DELETE FROM messages_metas WHERE conversation_id = :conversationId")
    fun deleteMessageMetasByConversationId(conversationId: String): Int

    @Query("SELECT EXISTS (SELECT 1 AS _c1 FROM messages_metas WHERE message_id = :messageId)")
    fun checkMessageMetaExists(messageId: String): Boolean
}
