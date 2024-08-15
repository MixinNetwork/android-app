package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.Entity
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.Constants.DEFAULT_THUMB_IMAGE
import one.mixin.android.Constants.MAX_THUMB_IMAGE_LENGTH
import one.mixin.android.util.GsonHelper

@SuppressLint("ParcelCreator")
@Entity
@Parcelize
data class QuoteMessageItem(
    @SerializedName(value = "message_id", alternate = ["messageId"])
    val messageId: String,
    @SerializedName(value = "conversation_id", alternate = ["conversationId"])
    val conversationId: String,
    @SerializedName(value = "user_id", alternate = ["userId"])
    val userId: String,
    @SerializedName(value = "user_full_name", alternate = ["userFullName"])
    val userFullName: String,
    @SerializedName(value = "user_identity_number", alternate = ["userIdentityNumber"])
    val userIdentityNumber: String,
    val type: String,
    val content: String?,
    @SerializedName(value = "created_at", alternate = ["createdAt"])
    val createdAt: String,
    val status: String,
    @SerializedName(value = "media_status", alternate = ["mediaStatus"])
    val mediaStatus: String?,
    @SerializedName(value = "user_avatar_url", alternate = ["userAvatarUrl"])
    val userAvatarUrl: String?,
    @SerializedName(value = "media_name", alternate = ["mediaName"])
    val mediaName: String?,
    @SerializedName(value = "media_mime_type", alternate = ["mediaMimeType"])
    val mediaMimeType: String?,
    @SerializedName(value = "media_size", alternate = ["mediaSize"])
    val mediaSize: Long?,
    @SerializedName(value = "media_width", alternate = ["mediaWidth"])
    val mediaWidth: Int?,
    @SerializedName(value = "media_height", alternate = ["mediaHeight"])
    val mediaHeight: Int?,
    @SerializedName(value = "thumb_image", alternate = ["thumbImage"])
    var thumbImage: String?,
    @SerializedName(value = "thumb_url", alternate = ["thumbUrl"])
    val thumbUrl: String?,
    @SerializedName(value = "media_url", alternate = ["mediaUrl"])
    val mediaUrl: String?,
    @SerializedName(value = "media_duration", alternate = ["mediaDuration"])
    val mediaDuration: String?,
    @SerializedName(value = "asset_url", alternate = ["assetUrl"])
    val assetUrl: String?,
    @SerializedName(value = "asset_height", alternate = ["assetHeight"])
    val assetHeight: Int?,
    @SerializedName(value = "asset_width", alternate = ["assetWidth"])
    val assetWidth: Int?,
    @SerializedName(value = "sticker_id", alternate = ["stickerId"])
    val stickerId: String?,
    @SerializedName(value = "asset_name", alternate = ["assetName"])
    val assetName: String?,
    @SerializedName(value = "app_id", alternate = ["appId"])
    val appId: String?,
    @SerializedName(value = "shared_user_id", alternate = ["sharedUserId"])
    val sharedUserId: String? = null,
    @SerializedName(value = "shared_user_full_name", alternate = ["sharedUserFullName"])
    val sharedUserFullName: String? = null,
    @SerializedName(value = "shared_user_identity_number", alternate = ["sharedUserIdentityNumber"])
    val sharedUserIdentityNumber: String? = null,
    @SerializedName(value = "shared_user_avatar_url", alternate = ["sharedUserAvatarUrl"])
    val sharedUserAvatarUrl: String? = null,
    val mentions: String? = null,
    val membership: Membership? = null,
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
        if ((messageItem.thumbImage?.length ?: 0) > MAX_THUMB_IMAGE_LENGTH) {
            DEFAULT_THUMB_IMAGE
        } else {
            messageItem.thumbImage
        },
        messageItem.thumbUrl,
        messageItem.mediaUrl,
        messageItem.mediaDuration,
        messageItem.assetUrl,
        messageItem.assetHeight,
        messageItem.assetWidth,
        messageItem.stickerId,
        messageItem.assetName,
        messageItem.appId,
        messageItem.sharedUserId,
        messageItem.sharedUserFullName,
        messageItem.sharedUserIdentityNumber,
        messageItem.sharedUserAvatarUrl,
        messageItem.mentions,
        messageItem.membership
    )
}

fun MessageItem.toQuoteMessageItem(): String? {
    return GsonHelper.customGson.toJson(QuoteMessageItem(this))
}

fun QuoteMessageItem?.toJson(): String? {
    val message = this ?: return null
    return GsonHelper.customGson.toJson(message)
}
