package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.SentSenderKey

@Dao
interface SentSenderKeyDao : BaseDao<SentSenderKey> {

    @Query("DELETE FROM sent_sender_keys WHERE conversation_id = :conversationId")
    fun delete(conversationId: String)
}