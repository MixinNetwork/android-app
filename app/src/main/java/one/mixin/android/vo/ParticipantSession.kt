package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import one.mixin.android.extension.nowInUtc

@Entity(tableName = "participant_session",
    primaryKeys = ["conversation_id", "user_id", "session_id"])
data class ParticipantSession (
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "sent_to_server")
    val sentToServer: Int? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String? = nowInUtc()
)

