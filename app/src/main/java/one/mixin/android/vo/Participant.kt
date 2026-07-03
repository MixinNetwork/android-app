package one.mixin.android.vo

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.ForeignKey.Companion.CASCADE
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(
    tableName = "participants",
    foreignKeys = [
        (
            ForeignKey(
                entity = Conversation::class,
                onDelete = CASCADE,
                parentColumns = arrayOf("conversation_id"),
                childColumns = arrayOf("conversation_id"),
            )
        ),
    ],
    primaryKeys = ["conversation_id", "user_id"],
)
@Serializable
data class Participant(
    @ColumnInfo(name = "conversation_id")
    @SerializedName("conversation_id")
    @SerialName("conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "user_id")
    @SerializedName("user_id")
    @SerialName("user_id")
    val userId: String,
    @ColumnInfo(name = "role")
    @SerializedName("role")
    @SerialName("role")
    val role: String,
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    @SerialName("created_at")
    val createdAt: String,
)

enum class ParticipantRole { OWNER, ADMIN }
