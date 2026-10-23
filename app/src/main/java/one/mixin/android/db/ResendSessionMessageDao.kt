package one.mixin.android.db

import androidx.room3.Dao
import androidx.room3.Query
import one.mixin.android.vo.ResendSessionMessage

@Dao
interface ResendSessionMessageDao : BaseDao<ResendSessionMessage> {
    @Query("SELECT * FROM resend_session_messages WHERE user_id = :userId AND session_id = :sessionId AND message_id = :messageId")
    fun findResendMessage(
        userId: String,
        sessionId: String,
        messageId: String,
    ): ResendSessionMessage?
}
