package one.mixin.android.vo

import android.os.Parcelable
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
data class Transcript(
    @PrimaryKey
    @SerializedName("message_id")
    val messageId: String,
    @SerializedName("user_id")
    val userId: String?,
    @SerializedName("user_full_name")
    val userFullName: String,
    @SerializedName("category")
    override val type: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("content")
    var content: String?,
    @SerializedName("media_url")
    var mediaUrl: String? = null,
    @SerializedName("media_name")
    val mediaName: String? = null,
    @SerializedName("media_size")
    val mediaSize: Long? = null,
    @SerializedName("media_width")
    val mediaWidth: Int? = null,
    @SerializedName("media_height")
    val mediaHeight: Int? = null,
    @SerializedName("media_mime_type")
    val mediaMimeType: String? = null,
    @SerializedName("media_duration")
    val mediaDuration: String? = null,
    @SerializedName("media_status")
    val mediaStatus: String? = null,
    @SerializedName("media_waveform")
    val mediaWaveform: ByteArray? = null,
    @SerializedName("thumb_image")
    val thumbImage: String? = null,
    @SerializedName("thumb_url")
    val thumbUrl: String? = null,
    @SerializedName("media_key")
    var mediaKey: ByteArray? = null,
    @SerializedName("media_digest")
    var mediaDigest: ByteArray? = null,
    @SerializedName("attachment_created_at")
    var attachmentCreatedAt: String? = null,
    @SerializedName("sticker_id")
    val stickerId: String? = null,
    @SerializedName("shared_user_id")
    val sharedUserId: String? = null,
    @SerializedName("shared_user_full_name")
    val sharedUserFullName: String? = null,
    @SerializedName("shared_user_identity_number")
    val sharedUserIdentityNumber: String? = null,
    @SerializedName("shared_user_avatar_url")
    val sharedUserAvatarUrl: String? = null,
    @SerializedName("shared_user_app_id")
    val sharedUserAppId: String? = null,
    @SerializedName("shared_user_is_verified")
    val sharedUserIsVerified: Boolean? = null,
    @SerializedName("mentions")
    val mentions: String? = null,
    @SerializedName("quote_id")
    val quoteId: String? = null,
    @SerializedName("quote_content")
    val quoteContent: String? = null,
) : Serializable, ICategory, Parcelable
