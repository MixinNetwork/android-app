package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import one.mixin.android.vo.ParticipantSession

@Dao
interface ParticipantSessionDao : BaseDao<ParticipantSession> {

    @Transaction
    fun replaceAll(conversationId: String, list: List<ParticipantSession>) {
        deleteByConversationId(conversationId)
        insertList(list)
    }

    @Query("DELETE FROM participant_session WHERE conversation_id = :conversationId")
    fun deleteByConversationId(conversationId: String)

    @Transaction
    @Query("SELECT p.* FROM participant_session p WHERE p.conversation_id = :conversationId AND p.session_id != :sessionId AND p.sent_to_server is NULL ")
    fun getNotSendSessionParticipants(conversationId: String, sessionId: String): List<ParticipantSession>?
}
