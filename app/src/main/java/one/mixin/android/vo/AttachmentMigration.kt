package one.mixin.android.vo

import androidx.room.ColumnInfo

class AttachmentMigration(
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "category")
    var category: String,
    @ColumnInfo(name = "media_url")
    val mediaUrl: String?,
    @ColumnInfo(name = "name")
    val name: String?,
    @ColumnInfo(name = "media_mine_type")
    val mediaMimeType: String?
)
