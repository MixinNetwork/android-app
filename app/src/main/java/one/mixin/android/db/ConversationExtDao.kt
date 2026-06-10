package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.extension.nowInUtc
import one.mixin.android.vo.ConversationExt

@Dao
interface ConversationExtDao : BaseDao<ConversationExt> {
    @Query("SELECT count FROM conversation_ext WHERE conversation_id = :conversationId")
    fun getMessageCountByConversationId(conversationId: String): Int?

    @Query("SELECT conversation_id FROM conversation_ext")
    fun getAllConversationId(): List<String>

    @Query("DELETE FROM conversation_ext WHERE conversation_id = :conversationId")
    fun deleteConversationById(conversationId: String)

    @Query("INSERT OR REPLACE INTO conversation_ext (`conversation_id`, `count`, `created_at`) VALUES (:conversationId, (SELECT count(1) FROM messages m INNER JOIN users u ON m.user_id = u.user_id WHERE conversation_id = :conversationId), :createdAt)")
    fun refreshCountByConversationId(
        conversationId: String,
        createdAt: String = nowInUtc(),
    )

    @Query("UPDATE conversation_ext SET count = count + 1 WHERE conversation_id = :conversationId")
    fun increment(conversationId: String)

    @Query("UPDATE conversation_ext SET count = count - 1 WHERE conversation_id = :conversationId")
    fun decrement(conversationId: String)

    @Query("UPDATE conversation_ext SET count = count + :increment WHERE conversation_id = :conversationId")
    fun increment(
        conversationId: String,
        increment: Int,
    )
}
