package one.mixin.android.vo

import androidx.room.ColumnInfo

class TranscriptAttachmentMigration(
    @ColumnInfo(name = "rowid")
    val rowid: Long,
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @ColumnInfo(name = "media_url")
    val mediaUrl: String?
)
