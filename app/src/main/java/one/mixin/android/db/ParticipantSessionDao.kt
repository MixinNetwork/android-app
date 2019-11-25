package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import one.mixin.android.vo.ParticipantSession

@Dao
interface ParticipantSessionDao : BaseDao<ParticipantSession> {

    @Query("SELECT * FROM participant_session WHERE conversation_id = :conversationId AND user_id = :userId AND session_id = :sessionId")
    fun getParticipantSession(conversationId: String, userId: String, sessionId: String): ParticipantSession?

    @Query("SELECT * FROM participant_session WHERE conversation_id = :conversationId")
    fun getParticipantSessionsByConversationId(conversationId: String): List<ParticipantSession>?

    @Query("UPDATE participant_session SET sent_to_server = NULL WHERE user_id = :userId")
    fun updateStatusByUserId(userId: String)

    @Query("UPDATE participant_session SET sent_to_server = NULL WHERE conversation_id = :conversationId")
    fun updateStatusByConversationId(conversationId: String)

    @Transaction
    fun replaceAll(conversationId: String, list: List<ParticipantSession>) {
        deleteByConversationId(conversationId)
        insertList(list)
    }

    @Query("DELETE FROM participant_session WHERE conversation_id = :conversationId AND user_id = :userId")
    fun deleteByUserId(conversationId: String, userId: String)

    @Query("DELETE FROM participant_session WHERE conversation_id = :conversationId")
    fun deleteByConversationId(conversationId: String)

    @Transaction
    @Query("""SELECT p.* FROM participant_session p LEFT JOIN users u ON p.user_id = u.user_id 
        WHERE p.conversation_id = :conversationId AND p.session_id != :sessionId AND u.app_id IS NULL AND p.sent_to_server IS NULL """)
    fun getNotSendSessionParticipants(conversationId: String, sessionId: String): List<ParticipantSession>?
}
