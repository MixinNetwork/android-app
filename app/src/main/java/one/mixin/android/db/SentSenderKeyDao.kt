package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.SentSenderKey

@Dao
interface SentSenderKeyDao : BaseDao<SentSenderKey> {

    @Query("DELETE FROM sent_sender_keys WHERE conversation_id = :conversationId")
    fun deleteByConversationId(conversationId: String)

    @Query("DELETE FROM sent_sender_keys WHERE user_id = :userId")
    fun deleteByUserId(userId: String)
}