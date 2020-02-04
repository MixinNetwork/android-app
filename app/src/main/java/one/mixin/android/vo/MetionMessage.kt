package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "mention_message",
    foreignKeys = [(ForeignKey(
        entity = Message::class,
        onDelete = ForeignKey.CASCADE,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("message_id")
    ))]
)
class MentionMessage(
    @ColumnInfo(name = "message_id")
    var messageId: String,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "user_id")
    val userId: String?,
    @ColumnInfo(name = "full_name")
    val fullName: String?,
    @SerializedName("has_read")
    @ColumnInfo(name = "has_read")
    val hasRead: Boolean = false,
    @PrimaryKey(autoGenerate = true)
    val rid: Int = 0
)
