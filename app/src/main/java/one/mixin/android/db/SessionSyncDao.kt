package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.SessionSync

@Dao
interface SessionSyncDao : BaseDao<SessionSync> {

    @Query("SELECT * FROM session_sync WHERE conversation_id = :conversationId")
    fun getByConversationId(conversationId: String): List<SessionSync>?

    @Query("SELECT * FROM session_sync ORDER BY created_at DESC limit 50")
    fun getConversations(): List<SessionSync>?
}
