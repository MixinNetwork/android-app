package one.mixin.android.fts

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageFtsDao {

    @Insert
    fun insertMessageFts(fts: MessageFts): Long

    @Query("DELETE FROM messages_fts WHERE docid = (SELECT doc_id FROM messages_metas WHERE message_id = :messageId)")
    fun deleteMessageMetasByMessageId(messageId: String)

    @Query("DELETE FROM messages_fts WHERE docid IN (SELECT doc_id FROM messages_metas WHERE message_id IN (:messageIds))")
    fun deleteMessageMetasByMessageIds(messageIds: List<String>)

    @Query("DELETE FROM messages_fts WHERE docid IN (SELECT doc_id FROM messages_metas WHERE conversation_id = :conversationId)")
    fun deleteMessageMetasByConversationId(conversationId: String): Int
}
