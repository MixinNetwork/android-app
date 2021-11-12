package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.Entity
import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import one.mixin.android.util.MoshiHelper

@SuppressLint("ParcelCreator")
@Entity
@Parcelize
@JsonClass(generateAdapter = true)
data class QuoteMessageItem(
    @SerializedName("message_id")
    @Json(name = "message_id")
    val messageId: String,
    @SerializedName("conversation_id")
    @Json(name = "conversation_id")
    val conversationId: String,
    @SerializedName("user_id")
    @Json(name = "user_id")
    val userId: String,
    @SerializedName("user_full_name")
    @Json(name = "user_full_name")
    val userFullName: String,
    @SerializedName("user_identity_number")
    @Json(name = "user_identity_number")
    val userIdentityNumber: String,
    val type: String,
    val content: String?,
    @SerializedName("created_at")
    @Json(name = "created_at")
    val createdAt: String,
    val status: String,
    @SerializedName("media_status")
    @Json(name = "media_status")
    val mediaStatus: String?,
    @SerializedName("user_avatar_url")
    @Json(name = "user_avatar_url")
    val userAvatarUrl: String?,
    @SerializedName("media_name")
    @Json(name = "media_name")
    val mediaName: String?,
    @SerializedName("media_mime_type")
    @Json(name = "media_mime_type")
    val mediaMimeType: String?,
    @SerializedName("media_size")
    @Json(name = "media_size")
    val mediaSize: Long?,
    @SerializedName("media_width")
    @Json(name = "media_width")
    val mediaWidth: Int?,
    @SerializedName("media_height")
    @Json(name = "media_height")
    val mediaHeight: Int?,
    @SerializedName("thumb_image")
    @Json(name = "thumb_image")
    val thumbImage: String?,
    @SerializedName("thumb_url")
    @Json(name = "thumb_url")
    val thumbUrl: String?,
    @SerializedName("media_url")
    @Json(name = "media_url")
    val mediaUrl: String?,
    @SerializedName("media_duration")
    @Json(name = "media_duration")
    val mediaDuration: String?,
    @SerializedName("asset_url")
    @Json(name = "asset_url")
    val assetUrl: String?,
    @SerializedName("asset_height")
    @Json(name = "asset_height")
    val assetHeight: Int?,
    @SerializedName("asset_width")
    @Json(name = "asset_width")
    val assetWidth: Int?,
    @SerializedName("sticker_id")
    @Json(name = "sticker_id")
    val stickerId: String?,
    @SerializedName("app_id")
    @Json(name = "app_id")
    val appId: String?,
    @SerializedName("shared_user_id")
    @Json(name = "shared_user_id")
    val sharedUserId: String? = null,
    @SerializedName("shared_user_full_name")
    @Json(name = "shared_user_full_name")
    val sharedUserFullName: String? = null,
    @SerializedName("shared_user_identity_number")
    @Json(name = "shared_user_identity_number")
    val sharedUserIdentityNumber: String? = null,
    @SerializedName("shared_user_avatar_url")
    @Json(name = "shared_user_avatar_url")
    val sharedUserAvatarUrl: String? = null,
    val mentions: String? = null,
) : Parcelable {
    constructor(messageItem: MessageItem) : this(
        messageItem.messageId,
        messageItem.conversationId,
        messageItem.userId,
        messageItem.userFullName,
        messageItem.userIdentityNumber,
        messageItem.type!!,
        messageItem.content,
        messageItem.createdAt,
        messageItem.status,
        messageItem.mediaStatus,
        messageItem.userAvatarUrl,
        messageItem.mediaName,
        messageItem.mediaMimeType,
        messageItem.mediaSize,
        messageItem.mediaWidth,
        messageItem.mediaHeight,
        messageItem.thumbImage,
        messageItem.thumbUrl,
        messageItem.mediaUrl,
        messageItem.mediaDuration,
        messageItem.assetUrl,
        messageItem.assetHeight,
        messageItem.assetWidth,
        messageItem.stickerId,
        messageItem.appId,
        messageItem.sharedUserId,
        messageItem.sharedUserFullName,
        messageItem.sharedUserIdentityNumber,
        messageItem.sharedUserAvatarUrl,
        messageItem.mentions
    )
}

fun MessageItem.toQuoteMessageItemJson(): String? {
    return MoshiHelper.getTypeAdapter<QuoteMessageItem>(QuoteMessageItem::class.java)
        .toJson(QuoteMessageItem(this))
}

fun QuoteMessageItem?.toJson(): String? {
    val message = this ?: return null
    return MoshiHelper.getTypeAdapter<QuoteMessageItem>(QuoteMessageItem::class.java)
        .toJson(message)
}
