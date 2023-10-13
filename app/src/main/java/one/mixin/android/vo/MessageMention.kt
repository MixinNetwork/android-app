package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "message_mentions",
    indices = [Index(value = arrayOf("conversation_id"))],
)
class MessageMention(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    @SerializedName("message_id")
    var messageId: String,
    @ColumnInfo(name = "conversation_id")
    @SerializedName("conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "mentions")
    @SerializedName("mentions")
    val mentions: String,
    @ColumnInfo(name = "has_read")
    @SerializedName("has_read")
    val hasRead: Boolean,
)

enum class MessageMentionStatus { MENTION_READ }
