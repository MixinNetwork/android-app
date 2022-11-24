package one.mixin.android.db.pending

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import one.mixin.android.vo.Message

@Entity(
    tableName = "pending_messages"
)
class PendingMessage(
    @PrimaryKey
    @SerializedName("id")
    @ColumnInfo(name = "id")
    var messageId: String,

    @SerializedName("conversation_id")
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,

    @SerializedName("user_id")
    @ColumnInfo(name = "user_id")
    var userId: String,

    @SerializedName("category")
    @ColumnInfo(name = "category")
    var category: String,

    @SerializedName("content")
    @ColumnInfo(name = "content")
    var content: String?,

    @SerializedName("media_url")
    @ColumnInfo(name = "media_url")
    val mediaUrl: String?,

    @SerializedName("media_mime_type")
    @ColumnInfo(name = "media_mime_type")
    val mediaMimeType: String?,

    @SerializedName("media_size")
    @ColumnInfo(name = "media_size")
    val mediaSize: Long?,

    @SerializedName("media_duration")
    @ColumnInfo(name = "media_duration")
    val mediaDuration: String?,

    @SerializedName("media_width")
    @ColumnInfo(name = "media_width")
    val mediaWidth: Int?,

    @SerializedName("media_height")
    @ColumnInfo(name = "media_height")
    val mediaHeight: Int?,

    @SerializedName("media_hash")
    @ColumnInfo(name = "media_hash")
    val mediaHash: String?,

    @SerializedName("thumb_image")
    @ColumnInfo(name = "thumb_image")
    val thumbImage: String?,

    @SerializedName("thumb_url")
    @ColumnInfo(name = "thumb_url")
    val thumbUrl: String?,

    @ColumnInfo(name = "media_key", typeAffinity = ColumnInfo.BLOB)
    val mediaKey: ByteArray? = null,

    @ColumnInfo(name = "media_digest", typeAffinity = ColumnInfo.BLOB)
    val mediaDigest: ByteArray? = null,

    @ColumnInfo(name = "media_status")
    var mediaStatus: String? = null,

    @SerializedName("status")
    @ColumnInfo(name = "status")
    var status: String,

    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @SerializedName("action")
    @ColumnInfo(name = "action")
    val action: String? = null,

    @SerializedName("participant_id")
    @ColumnInfo(name = "participant_id")
    val participantId: String? = null,

    @SerializedName("snapshot_id")
    @ColumnInfo(name = "snapshot_id")
    val snapshotId: String? = null,

    @SerializedName("hyperlink")
    @ColumnInfo(name = "hyperlink")
    var hyperlink: String? = null,

    @SerializedName("name")
    @ColumnInfo(name = "name")
    val name: String? = null,

    @SerializedName("album_id")
    @ColumnInfo(name = "album_id")
    val albumId: String? = null,

    @SerializedName("sticker_id")
    @ColumnInfo(name = "sticker_id")
    val stickerId: String? = null,

    @SerializedName("shared_user_id")
    @ColumnInfo(name = "shared_user_id")
    val sharedUserId: String? = null,

    @ColumnInfo(name = "media_waveform", typeAffinity = ColumnInfo.BLOB)
    val mediaWaveform: ByteArray? = null,

    @SerializedName("media_mine_type")
    @ColumnInfo(name = "media_mine_type")
    val mediaMineType: String? = null,

    @SerializedName("quote_message_id")
    @ColumnInfo(name = "quote_message_id")
    val quoteMessageId: String? = null,

    @SerializedName("quote_content")
    @ColumnInfo(name = "quote_content")
    val quoteContent: String? = null,

    @SerializedName("caption")
    @ColumnInfo(name = "caption")
    var caption: String? = null,
) {
    constructor(message: Message) : this(
        message.messageId,
        message.conversationId,
        message.userId,
        message.category,
        message.content,
        message.mediaUrl,
        message.mediaMimeType,
        message.mediaSize,
        message.mediaDuration,
        message.mediaWidth,
        message.mediaHeight,
        message.mediaHash,
        message.thumbImage,
        message.thumbUrl,
        message.mediaKey,
        message.mediaDigest,
        message.mediaStatus,
        message.status,
        message.createdAt,
        message.action,
        message.participantId,
        message.snapshotId,
        message.hyperlink,
        message.name,
        message.albumId,
        message.stickerId,
        message.sharedUserId,
        message.mediaWaveform,
        null,
        message.quoteMessageId,
        message.quoteContent,
        message.caption
    )
}

fun PendingMessage.toMessage() = Message(
    messageId,
    conversationId,
    userId,
    category,
    content,
    mediaUrl,
    mediaMimeType,
    mediaSize,
    mediaDuration,
    mediaWidth,
    mediaHeight,
    mediaHash,
    thumbImage,
    thumbUrl,
    mediaKey,
    mediaDigest,
    mediaStatus,
    status,
    createdAt,
    action,
    participantId,
    snapshotId,
    hyperlink,
    name,
    albumId,
    stickerId,
    sharedUserId,
    mediaWaveform,
    null,
    quoteMessageId,
    quoteContent,
    caption
)
