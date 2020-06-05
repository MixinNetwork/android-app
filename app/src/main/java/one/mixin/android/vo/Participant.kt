package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.Index

@Entity(
    tableName = "participants",
    indices = [Index(value = arrayOf("conversation_id")), Index(value = arrayOf("created_at"))],
    foreignKeys = [
        (
            ForeignKey(
                entity = Conversation::class,
                onDelete = CASCADE,
                parentColumns = arrayOf("conversation_id"),
                childColumns = arrayOf("conversation_id")
            )
            )
    ],
    primaryKeys = ["conversation_id", "user_id"]
)
data class Participant(
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "role")
    val role: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String
)

enum class ParticipantRole { OWNER, ADMIN }
