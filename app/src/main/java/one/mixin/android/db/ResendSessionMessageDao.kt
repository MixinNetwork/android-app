package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.ResendSessionMessage

@Dao
interface ResendSessionMessageDao : BaseDao<ResendSessionMessage> {

    @Query("SELECT * FROM resend_session_messages WHERE user_id = :userId AND message_id = :messageId")
    fun findResendMessage(userId: String, messageId: String): ResendSessionMessage?
}
