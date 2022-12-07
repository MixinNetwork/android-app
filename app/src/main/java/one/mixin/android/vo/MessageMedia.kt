package one.mixin.android.vo

import androidx.room.ColumnInfo

class MessageMedia(
    @ColumnInfo(name = "category")
    override val type: String,
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "media_url")
    val mediaUrl: String?,
) : ICategory
