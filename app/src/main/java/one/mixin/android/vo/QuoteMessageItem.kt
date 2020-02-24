package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import one.mixin.android.util.GsonHelper

@SuppressLint("ParcelCreator")
@Entity
@Parcelize
data class QuoteMessageItem(
    @PrimaryKey
    val messageId: String,
    val conversationId: String,
    val userId: String,
    val userFullName: String,
    val userIdentityNumber: String,
    val type: String,
    val content: String?,
    val createdAt: String,
    val status: String,
    val mediaStatus: String?,
    val userAvatarUrl: String?,
    val mediaName: String?,
    val mediaMimeType: String?,
    val mediaSize: Long?,
    val mediaWidth: Int?,
    val mediaHeight: Int?,
    val thumbImage: String?,
    val thumbUrl: String?,
    val mediaUrl: String?,
    val mediaDuration: String?,
    val assetUrl: String?,
    val assetHeight: Int?,
    val assetWidth: Int?,
    val stickerId: String?,
    val assetName: String?,
    val appId: String?,
    val sharedUserId: String? = null,
    val sharedUserFullName: String? = null,
    val sharedUserIdentityNumber: String? = null,
    val sharedUserAvatarUrl: String? = null,
    val mentions: String? = null
) : Parcelable {
    constructor(messageItem: MessageItem) : this(
        messageItem.messageId,
        messageItem.conversationId,
        messageItem.userId,
        messageItem.userFullName,
        messageItem.userIdentityNumber,
        messageItem.type,
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
        messageItem.assetName,
        messageItem.appId,
        messageItem.sharedUserId,
        messageItem.sharedUserFullName,
        messageItem.sharedUserIdentityNumber,
        messageItem.sharedUserAvatarUrl,
        messageItem.mentions
    )
}

fun MessageItem.toQuoteMessageItem(): String? {
    return GsonHelper.customGson.toJson(QuoteMessageItem(this))
}

fun QuoteMessageItem?.toJson(): String? {
    val message = this ?: return null
    return GsonHelper.customGson.toJson(message)
}
