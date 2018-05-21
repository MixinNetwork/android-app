package one.mixin.android.vo

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.ForeignKey.CASCADE
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Entity(tableName = "messages",
    indices = [Index(value = arrayOf("conversation_id")), Index(value = arrayOf("created_at"))],
    foreignKeys = [(ForeignKey(entity = Conversation::class,
        onDelete = CASCADE,
        parentColumns = arrayOf("conversation_id"),
        childColumns = arrayOf("conversation_id")))])

class Message(
    @PrimaryKey
    @SerializedName("id")
    @ColumnInfo(name = "id")
    var id: String,

    @SerializedName("conversation_id")
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,

    @SerializedName("user_id")
    @ColumnInfo(name = "user_id")
    val userId: String,

    @SerializedName("category")
    @ColumnInfo(name = "category")
    var category: String,

    @SerializedName("content")
    @ColumnInfo(name = "content")
    var content: String?,

    @SerializedName("media_url")
    @ColumnInfo(name = "media_url")
    val mediaUrl: String?,

    @SerializedName("media_mine_type")
    @ColumnInfo(name = "media_mine_type")
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

    @ColumnInfo(name = "media_key", typeAffinity = ColumnInfo.BLOB)
    val mediaKey: ByteArray? = null,

    @ColumnInfo(name = "media_digest", typeAffinity = ColumnInfo.BLOB)
    val mediaDigest: ByteArray? = null,

    @ColumnInfo(name = "media_status")
    val mediaStatus: String? = null,

    @SerializedName("status")
    @ColumnInfo(name = "status")
    val status: String,

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
    val hyperlink: String? = null,

    @SerializedName("name")
    @ColumnInfo(name = "name")
    val name: String? = null,

    @SerializedName("album_id")
    @ColumnInfo(name = "album_id")
    val albumId: String? = null,

    @SerializedName("shared_user_id")
    @ColumnInfo(name = "shared_user_id")
    val sharedUserId: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

fun Message.isPlain(): Boolean {
    return category.startsWith("PLAIN_")
}

enum class MessageCategory {
    SIGNAL_KEY,
    SIGNAL_TEXT,
    SIGNAL_IMAGE,
    SIGNAL_VIDEO,
    SIGNAL_STICKER,
    SIGNAL_DATA,
    SIGNAL_CONTACT,
    PLAIN_TEXT,
    PLAIN_IMAGE,
    PLAIN_VIDEO,
    PLAIN_DATA,
    PLAIN_STICKER,
    PLAIN_CONTACT,
    PLAIN_JSON,
    STRANGER,
    SYSTEM_CONVERSATION,
    SYSTEM_ACCOUNT_SNAPSHOT,
    APP_BUTTON_GROUP,
    APP_CARD,
    UNKNOWN
}

enum class MessageStatus { SENDING, SENT, DELIVERED, READ, FAILED }

enum class MediaStatus { PENDING, DONE, CANCELED, EXPIRED }

fun MessageItem.isMedia(): Boolean = this.type == MessageCategory.SIGNAL_IMAGE.name ||
    this.type == MessageCategory.PLAIN_IMAGE.name ||
    this.type == MessageCategory.SIGNAL_DATA.name ||
    this.type == MessageCategory.PLAIN_DATA.name ||
    this.type == MessageCategory.SIGNAL_VIDEO.name ||
    this.type == MessageCategory.PLAIN_VIDEO.name

fun MessageItem.canNotForward() = this.type == MessageCategory.APP_CARD.name ||
    this.type == MessageCategory.APP_BUTTON_GROUP.name ||
    this.type == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name ||
    this.type == MessageCategory.SYSTEM_CONVERSATION.name ||
    (this.mediaStatus != MediaStatus.DONE.name && this.isMedia())

fun createMessage(
    messageId: String,
    conversationId: String,
    userId: String,
    category: String,
    content: String,
    createdAt: String,
    status: MessageStatus,
    action: String? = null,
    participantId: String? = null,
    snapshotId: String? = null
) = MessageBuilder(messageId, conversationId, userId, category, status.name, createdAt)
    .setContent(content)
    .setAction(action)
    .setParticipantId(participantId)
    .setSnapshotId(snapshotId)
    .build()

fun createAttachmentMessage(
    messageId: String,
    conversationId: String,
    userId: String,
    category: String,
    content: String?,
    name: String?,
    mediaUrl: String?,
    mediaMimeType: String,
    mediaSize: Long,
    createdAt: String,
    key: ByteArray?,
    digest: ByteArray?,
    mediaStatus: MediaStatus,
    status: MessageStatus
) = MessageBuilder(messageId, conversationId, userId, category, status.name, createdAt)
    .setContent(content)
    .setName(name)
    .setMediaUrl(mediaUrl)
    .setMediaMimeType(mediaMimeType)
    .setMediaSize(mediaSize)
    .setMediaKey(key)
    .setMediaDigest(digest)
    .setMediaStatus(mediaStatus.name)
    .build()

fun createVideoMessage(
    messageId: String,
    conversationId: String,
    userId: String,
    category: String,
    content: String?,
    name: String?,
    mediaUrl: String?,
    duration: Long?,
    mediaWidth: Int? = null,
    mediaHeight: Int? = null,
    thumbImage: String? = null,
    mediaMimeType: String,
    mediaSize: Long,
    createdAt: String,
    key: ByteArray?,
    digest: ByteArray?,
    mediaStatus: MediaStatus,
    status: MessageStatus
) = MessageBuilder(messageId, conversationId, userId, category, status.name, createdAt)
    .setContent(content)
    .setName(name)
    .setMediaUrl(mediaUrl)
    .setMediaDuration(duration.toString())
    .setMediaWidth(mediaWidth)
    .setMediaHeight(mediaHeight)
    .setThumbImage(thumbImage)
    .setMediaMimeType(mediaMimeType)
    .setMediaSize(mediaSize)
    .setMediaKey(key)
    .setMediaDigest(digest)
    .setMediaStatus(mediaStatus.name)
    .build()

fun createMediaMessage(
    messageId: String,
    conversationId: String,
    userId: String,
    category: String,
    content: String?,
    mediaUrl: String?,
    mediaMimeType: String,
    mediaSize: Long,
    mediaWidth: Int?,
    mediaHeight: Int?,
    thumbImage: String?,
    key: ByteArray?,
    digest: ByteArray?,
    createdAt: String,
    mediaStatus: MediaStatus,
    status: MessageStatus
) = MessageBuilder(messageId, conversationId, userId, category, status.name, createdAt)
    .setContent(content)
    .setMediaUrl(mediaUrl)
    .setMediaMimeType(mediaMimeType)
    .setMediaSize(mediaSize)
    .setMediaWidth(mediaWidth)
    .setMediaHeight(mediaHeight)
    .setThumbImage(thumbImage)
    .setMediaKey(key)
    .setMediaDigest(digest)
    .setMediaStatus(mediaStatus.name)
    .build()

fun createStickerMessage(
    messageId: String,
    conversationId: String,
    userId: String,
    category: String,
    content: String?,
    albumId: String,
    stickerName: String,
    status: MessageStatus,
    createdAt: String
) = MessageBuilder(messageId, conversationId, userId, category, status.name, createdAt)
    .setContent(content)
    .setAlbumId(albumId)
    .setName(stickerName)
    .build()

fun createContactMessage(
    messageId: String,
    conversationId: String,
    userId: String,
    category: String,
    content: String,
    sharedUserId: String,
    status: MessageStatus,
    createdAt: String
) = MessageBuilder(messageId, conversationId, userId, category, status.name, createdAt)
    .setContent(content)
    .setSharedUserId(sharedUserId)
    .build()