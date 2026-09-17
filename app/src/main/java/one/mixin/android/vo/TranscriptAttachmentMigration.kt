package one.mixin.android.vo

import androidx.room3.ColumnInfo

class TranscriptAttachmentMigration(
    @ColumnInfo(name = "rowid")
    val rowid: Long,
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @ColumnInfo(name = "media_url")
    val mediaUrl: String?,
)
