package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.SentSessionSenderKey

@Dao
interface SentSessionSenderKeyDao : BaseDao<SentSessionSenderKey> {
    @Query("DELETE FROM sent_session_sender_keys WHERE conversation_id = :conversationId")
    fun delete(conversationId: String)
}