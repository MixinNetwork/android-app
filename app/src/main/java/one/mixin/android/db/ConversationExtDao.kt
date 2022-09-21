package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.ConversationExt

@Dao
interface ConversationExtDao : BaseDao<ConversationExt> {
    @Query("SELECT count FROM conversation_ext WHERE conversation_id = :conversationId")
    fun getMessageCountByConversationId(conversationId: String): Int?

    @Query("DELETE FROM conversation_ext WHERE conversation_id = :conversationId")
    fun deleteConversationById(conversationId: String)

    @Query("INSERT INTO conversation_ext (`conversation_id`,`count`) VALUES (:conversationId, (SELECT count(1) FROM messages m INNER JOIN users u ON m.user_id = u.user_id WHERE conversation_id = :conversationId))")
    fun refreshCountByConversationId(conversationId: String)

    @Query("UPDATE conversation_ext SET count = count + 1 WHERE conversation_id = :conversationId")
    fun increment(conversationId: String)

    @Query("UPDATE conversation_ext SET count = count - 1 WHERE conversation_id = :conversationId")
    fun decrement(conversationId: String)
}
