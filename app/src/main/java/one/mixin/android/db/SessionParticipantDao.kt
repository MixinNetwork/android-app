package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import one.mixin.android.vo.SessionParticipant

@Dao
interface SessionParticipantDao : BaseDao<SessionParticipant> {

    @Transaction
    @Query("SELECT p.* FROM session_participants p WHERE p.conversation_id = :conversationId AND p.session_id != :sessionId AND p.sent_to_server is NULL ")
    fun getNotSendSessionParticipants(conversationId: String, sessionId: String): List<SessionParticipant>?

}
