package one.mixin.android.vo

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
@Entity(tableName = "transcript_messages", primaryKeys = ["transcript_id", "message_id"])
@JsonClass(generateAdapter = true)
data class TranscriptMessage(
    @Json(name = "transcript_id")
    @ColumnInfo(name = "transcript_id")
    var transcriptId: String,
    @Json(name = "message_id")
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @Json(name = "user_id")
    @ColumnInfo(name = "user_id")
    val userId: String?,
    @Json(name = "user_full_name")
    @ColumnInfo(name = "user_full_name")
    val userFullName: String?,
    @Json(name = "category")
    @ColumnInfo(name = "category")
    override var type: String,
    @Json(name = "created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @Json(name = "content")
    @ColumnInfo(name = "content")
    val content: String?,
    @ColumnInfo(name = "media_url")
    @Transient
    var mediaUrl: String? = null,
    @Json(name = "media_name")
    @ColumnInfo(name = "media_name")
    val mediaName: String? = null,
    @Json(name = "media_size")
    @ColumnInfo(name = "media_size")
    val mediaSize: Long? = null,
    @Json(name = "media_width")
    @ColumnInfo(name = "media_width")
    val mediaWidth: Int? = null,
    @Json(name = "media_height")
    @ColumnInfo(name = "media_height")
    val mediaHeight: Int? = null,
    @Json(name = "media_mime_type")
    @ColumnInfo(name = "media_mime_type")
    val mediaMimeType: String? = null,
    @Json(name = "media_duration")
    @ColumnInfo(name = "media_duration")
    val mediaDuration: Long? = null,
    @ColumnInfo(name = "media_status")
    @Transient
    var mediaStatus: String? = null,
    @Json(name = "media_waveform")
    @ColumnInfo(name = "media_waveform")
    val mediaWaveform: ByteArray? = null,
    @Json(name = "thumb_image")
    @ColumnInfo(name = "thumb_image")
    val thumbImage: String? = null,
    @Json(name = "thumb_url")
    @ColumnInfo(name = "thumb_url")
    val thumbUrl: String? = null,
    @Json(name = "media_key")
    @ColumnInfo(name = "media_key")
    val mediaKey: ByteArray? = null,
    @Json(name = "media_digest")
    @ColumnInfo(name = "media_digest")
    val mediaDigest: ByteArray? = null,
    @Json(name = "media_created_at")
    @ColumnInfo(name = "media_created_at")
    val mediaCreatedAt: String? = null,
    @Json(name = "sticker_id")
    @ColumnInfo(name = "sticker_id")
    val stickerId: String? = null,
    @Json(name = "shared_user_id")
    @ColumnInfo(name = "shared_user_id")
    val sharedUserId: String? = null,
    @Json(name = "mentions")
    @ColumnInfo(name = "mentions")
    val mentions: String? = null,
    @Json(name = "quote_id")
    @ColumnInfo(name = "quote_id")
    val quoteId: String? = null,
    @Json(name = "quote_content")
    @ColumnInfo(name = "quote_content")
    var quoteContent: String? = null,
    @Json(name = "caption")
    @ColumnInfo(name = "caption")
    val caption: String? = null
) : ICategory, Serializable, Parcelable

fun TranscriptMessage.isValidAttachment(): Boolean =
    (isPlain() && mediaKey == null && mediaDigest == null) || ((isSignal() || isEncrypted()) && mediaKey != null && mediaDigest != null)

fun TranscriptMessage.copy(tid: String): TranscriptMessage {
    return TranscriptMessage(
        tid,
        messageId,
        userId,
        userFullName,
        type,
        createdAt,
        content,
        mediaUrl,
        mediaName,
        mediaSize,
        mediaWidth,
        mediaHeight,
        mediaMimeType,
        mediaDuration,
        mediaStatus,
        mediaWaveform,
        thumbImage,
        thumbUrl,
        mediaKey,
        mediaDigest,
        mediaCreatedAt,
        stickerId,
        sharedUserId,
        mentions,
        quoteId,
        quoteContent,
        caption
    )
}
