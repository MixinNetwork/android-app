package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@SuppressLint("ParcelCreator")
@Entity
@Parcelize
data class SnakeQuoteMessageItem(
    @PrimaryKey
    @SerializedName("message_id")
    val messageId: String,
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("user_full_name")
    val userFullName: String,
    @SerializedName("user_identity_number")
    val userIdentityNumber: String,
    val type: String,
    val content: String?,
    @SerializedName("created_at")
    val createdAt: String,
    val status: String,
    @SerializedName("media_status")
    val mediaStatus: String?,
    @SerializedName("user_avatar_url")
    val userAvatarUrl: String?,
    @SerializedName("media_name")
    val mediaName: String?,
    @SerializedName("media_mime_type")
    val mediaMimeType: String?,
    @SerializedName("media_size")
    val mediaSize: Long?,
    @SerializedName("media_width")
    val mediaWidth: Int?,
    @SerializedName("media_height")
    val mediaHeight: Int?,
    @SerializedName("thumb_image")
    val thumbImage: String?,
    @SerializedName("thumb_url")
    val thumbUrl: String?,
    @SerializedName("media_url")
    val mediaUrl: String?,
    @SerializedName("media_duration")
    val mediaDuration: String?,
    @SerializedName("asset_url")
    val assetUrl: String?,
    @SerializedName("asset_height")
    val assetHeight: Int?,
    @SerializedName("asset_width")
    val assetWidth: Int?,
    @SerializedName("sticker_id")
    val stickerId: String?,
    @SerializedName("app_id")
    val appId: String?,
    @SerializedName("shared_user_id")
    val sharedUserId: String? = null,
    @SerializedName("shared_user_full_name")
    val sharedUserFullName: String? = null,
    @SerializedName("shared_user_identity_number")
    val sharedUserIdentityNumber: String? = null,
    @SerializedName("shared_user_avatar_url")
    val sharedUserAvatarUrl: String? = null,
    val mentions: String? = null,
) : Parcelable {
    constructor(quoteMessageItem: QuoteMessageItem) : this(
        quoteMessageItem.messageId,
        quoteMessageItem.conversationId,
        quoteMessageItem.userId,
        quoteMessageItem.userFullName,
        quoteMessageItem.userIdentityNumber,
        quoteMessageItem.type,
        quoteMessageItem.content,
        quoteMessageItem.createdAt,
        quoteMessageItem.status,
        quoteMessageItem.mediaStatus,
        quoteMessageItem.userAvatarUrl,
        quoteMessageItem.mediaName,
        quoteMessageItem.mediaMimeType,
        quoteMessageItem.mediaSize,
        quoteMessageItem.mediaWidth,
        quoteMessageItem.mediaHeight,
        quoteMessageItem.thumbImage,
        quoteMessageItem.thumbUrl,
        quoteMessageItem.mediaUrl,
        quoteMessageItem.mediaDuration,
        quoteMessageItem.assetUrl,
        quoteMessageItem.assetHeight,
        quoteMessageItem.assetWidth,
        quoteMessageItem.stickerId,
        quoteMessageItem.appId,
        quoteMessageItem.sharedUserId,
        quoteMessageItem.sharedUserFullName,
        quoteMessageItem.sharedUserIdentityNumber,
        quoteMessageItem.sharedUserAvatarUrl,
        quoteMessageItem.mentions
    )
}
