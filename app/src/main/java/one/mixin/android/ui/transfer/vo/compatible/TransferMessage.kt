package one.mixin.android.ui.transfer.vo.compatible

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import one.mixin.android.Constants
import one.mixin.android.util.serialization.ByteArrayBase64Serializer
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory

@Serializable
class TransferMessage(
    @PrimaryKey
    @SerializedName("message_id")
    @SerialName("message_id")
    @ColumnInfo(name = "id")
    var messageId: String,
    @SerializedName("conversation_id")
    @SerialName("conversation_id")
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @SerializedName("user_id")
    @SerialName("user_id")
    @ColumnInfo(name = "user_id")
    var userId: String,
    @SerializedName("category")
    @SerialName("category")
    @ColumnInfo(name = "category")
    var category: String,
    @SerializedName("content")
    @SerialName("content")
    @ColumnInfo(name = "content")
    var content: String?,
    @SerializedName("media_url")
    @SerialName("media_url")
    @ColumnInfo(name = "media_url")
    val mediaUrl: String?,
    @SerializedName("media_mime_type")
    @SerialName("media_mime_type")
    @ColumnInfo(name = "media_mime_type")
    val mediaMimeType: String?,
    @SerializedName("media_size")
    @SerialName("media_size")
    @ColumnInfo(name = "media_size")
    val mediaSize: Long?,
    @SerializedName("media_duration")
    @SerialName("media_duration")
    @ColumnInfo(name = "media_duration")
    val mediaDuration: String?,
    @SerializedName("media_width")
    @SerialName("media_width")
    @ColumnInfo(name = "media_width")
    val mediaWidth: Int?,
    @SerializedName("media_height")
    @SerialName("media_height")
    @ColumnInfo(name = "media_height")
    val mediaHeight: Int?,
    @SerializedName("media_hash")
    @SerialName("media_hash")
    @ColumnInfo(name = "media_hash")
    val mediaHash: String?,
    @SerializedName("thumb_image")
    @SerialName("thumb_image")
    @ColumnInfo(name = "thumb_image")
    val thumbImage: String?,
    @SerializedName("thumb_url")
    @SerialName("thumb_url")
    @ColumnInfo(name = "thumb_url")
    val thumbUrl: String?,
    @ColumnInfo(name = "media_key", typeAffinity = ColumnInfo.BLOB)
    @SerializedName("media_key")
    @SerialName("media_key")
    @Serializable(with = ByteArrayBase64Serializer::class)
    val mediaKey: ByteArray? = null,
    @ColumnInfo(name = "media_digest", typeAffinity = ColumnInfo.BLOB)
    @SerializedName("media_digest")
    @SerialName("media_digest")
    @Serializable(with = ByteArrayBase64Serializer::class)
    val mediaDigest: ByteArray? = null,
    @SerializedName("media_status")
    @SerialName("media_status")
    @ColumnInfo(name = "media_status")
    var mediaStatus: String? = null,
    @SerializedName("status")
    @SerialName("status")
    @ColumnInfo(name = "status")
    var status: String,
    @SerializedName("created_at")
    @SerialName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @SerializedName("action")
    @SerialName("action")
    @ColumnInfo(name = "action")
    val action: String? = null,
    @SerializedName("participant_id")
    @SerialName("participant_id")
    @ColumnInfo(name = "participant_id")
    val participantId: String? = null,
    @SerializedName("snapshot_id")
    @SerialName("snapshot_id")
    @ColumnInfo(name = "snapshot_id")
    val snapshotId: String? = null,
    @SerializedName("hyperlink")
    @SerialName("hyperlink")
    @ColumnInfo(name = "hyperlink")
    var hyperlink: String? = null,
    @SerializedName("name")
    @SerialName("name")
    @ColumnInfo(name = "name")
    val name: String? = null,
    @SerializedName("album_id")
    @SerialName("album_id")
    @ColumnInfo(name = "album_id")
    val albumId: String? = null,
    @SerializedName("sticker_id")
    @SerialName("sticker_id")
    @ColumnInfo(name = "sticker_id")
    val stickerId: String? = null,
    @SerializedName("shared_user_id")
    @SerialName("shared_user_id")
    @ColumnInfo(name = "shared_user_id")
    val sharedUserId: String? = null,
    @ColumnInfo(name = "media_waveform", typeAffinity = ColumnInfo.BLOB)
    @SerializedName("media_waveform")
    @SerialName("media_waveform")
    @Serializable(with = ByteArrayBase64Serializer::class)
    val mediaWaveform: ByteArray? = null,
    @SerializedName("quote_message_id")
    @SerialName("quote_message_id")
    @ColumnInfo(name = "quote_message_id")
    val quoteMessageId: String? = null,
    @SerializedName("quote_content")
    @SerialName("quote_content")
    @ColumnInfo(name = "quote_content")
    val quoteContent: String? = null,
    @SerializedName("caption")
    @SerialName("caption")
    @ColumnInfo(name = "caption")
    var caption: String? = null,
)

fun TransferMessage.toMessage(): Message {
    return Message(
        messageId = this.messageId,
        conversationId = this.conversationId,
        userId = this.userId,
        category = this.category,
        content = this.content,
        mediaUrl = this.mediaUrl,
        mediaMimeType = this.mediaMimeType,
        mediaSize = this.mediaSize,
        mediaDuration = this.mediaDuration,
        mediaWidth = this.mediaWidth,
        mediaHeight = this.mediaHeight,
        mediaHash = this.mediaHash,
        thumbImage =
            if ((this.thumbImage?.length ?: 0) > Constants.MAX_THUMB_IMAGE_LENGTH) {
                Constants.DEFAULT_THUMB_IMAGE
            } else {
                this.thumbImage
            },
        thumbUrl = this.thumbUrl,
        mediaKey = this.mediaKey,
        mediaDigest = this.mediaDigest,
        mediaStatus = this.mediaStatus,
        status = this.status,
        createdAt = this.createdAt,
        action = this.action,
        participantId = this.participantId,
        snapshotId = this.snapshotId,
        hyperlink = this.hyperlink,
        name = this.name,
        albumId = this.albumId,
        stickerId = this.stickerId,
        sharedUserId = this.sharedUserId,
        mediaWaveform = this.mediaWaveform,
        quoteMessageId = this.quoteMessageId,
        quoteContent = this.quoteContent,
        caption = this.caption,
    )
}

fun TransferMessage.markAttachmentAsPending(): TransferMessage {
    if (category in attachmentCategory && mediaStatus == MediaStatus.PENDING.name) {
        mediaStatus = MediaStatus.CANCELED.name
    }
    return this
}

fun TransferMessage.isAttachment() = category in attachmentCategory

fun TransferMessage.isTranscript() =
    category == MessageCategory.PLAIN_TRANSCRIPT.name || category == MessageCategory.SIGNAL_TRANSCRIPT.name || category == MessageCategory.ENCRYPTED_TRANSCRIPT.name

private val attachmentCategory by lazy {
    listOf(
        MessageCategory.PLAIN_DATA.name,
        MessageCategory.PLAIN_IMAGE.name,
        MessageCategory.PLAIN_VIDEO.name,
        MessageCategory.PLAIN_AUDIO.name,
        MessageCategory.SIGNAL_DATA.name,
        MessageCategory.SIGNAL_IMAGE.name,
        MessageCategory.SIGNAL_VIDEO.name,
        MessageCategory.SIGNAL_AUDIO.name,
        MessageCategory.ENCRYPTED_DATA.name,
        MessageCategory.ENCRYPTED_IMAGE.name,
        MessageCategory.ENCRYPTED_VIDEO.name,
        MessageCategory.ENCRYPTED_AUDIO.name,
    )
}
