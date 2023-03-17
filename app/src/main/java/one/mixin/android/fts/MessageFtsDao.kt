package one.mixin.android.fts

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.BaseDao

@Dao
interface MessageFtsDao : BaseDao<MessageFts> {

    @Query("DELETE FROM messages_fts WHERE docid = (SELECT doc_id FROM messages_metas WHERE message_id = :messageId)")
    fun deleteMessageFtsByMessageId(messageId: String)

    @Query("DELETE FROM messages_fts WHERE docid IN (SELECT doc_id FROM messages_metas WHERE message_id IN (:messageIds))")
    fun deleteMessageMetasByMessageIds(messageIds: List<String>)

    @Query("DELETE FROM messages_fts WHERE docid IN (SELECT doc_id FROM messages_metas WHERE conversation_id = :conversationId)")
    fun deleteMessageMetasByConversationId(conversationId: String): Int
}
