package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.CircleConversation

@Dao
interface CircleConversationDao : BaseDao<CircleConversation> {
    @Query("UPDATE circle_conversations SET pin_time = :pinTime WHERE conversation_id = :conversationId AND circle_id = :circleId")
    fun updateConversationPinTimeById(conversationId: String, circleId: String, pinTime: String?)
}
