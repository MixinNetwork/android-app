package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import one.mixin.android.extension.nowInUtc

@SuppressLint("ParcelCreator")
@Entity
@Parcelize
data class MessageItem(
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
    val mediaUrl: String?,
    val mediaDuration: String?,
    val participantFullName: String?,
    val participantUserId: String?,
    val actionName: String?,
    val snapshotId: String?,
    val snapshotType: String?,
    val snapshotAmount: String?,
    val assetId: String?,
    val assetType: String?,
    val assetSymbol: String?,
    val assetIcon: String?,
    val assetUrl: String?,
    val assetHeight: Int?,
    val assetWidth: Int?,
    @Deprecated(
        "Deprecated at database version 15",
        ReplaceWith("@{link sticker_id}", "one.mixin.android.vo.MessageItem.stickerId"),
        DeprecationLevel.ERROR
    )
    val albumId: String?,
    val stickerId: String?,
    val assetName: String?,
    val appId: String?,
    val siteName: String? = null,
    val siteTitle: String? = null,
    val siteDescription: String? = null,
    val siteImage: String? = null,
    val sharedUserId: String? = null,
    val sharedUserFullName: String? = null,
    val sharedUserIdentityNumber: String? = null,
    val sharedUserAvatarUrl: String? = null,
    val sharedUserIsVerified: Boolean? = null,
    val sharedUserAppId: String? = null,
    val mediaWaveform: ByteArray? = null,
    val quoteId: String? = null,
    val quoteContent: String? = null,
    val groupName: String? = null
) : Parcelable

fun create(type: String, createdAt: String? = null) = MessageItem("", "", "", "", "",
    type, null, createdAt
    ?: nowInUtc(), MessageStatus.READ.name, null, null,
    null, null, null, null, null, null, null,
    null, null, null, null, null, null, null, null,
    null, null, null, null, null, null, null, null, null, null)

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
    (this.mediaStatus != MediaStatus.DONE.name && this.isMedia()) ||
    isCallMessage() || isRecall()

fun MessageItem.supportSticker(): Boolean = this.type == MessageCategory.SIGNAL_STICKER.name ||
    this.type == MessageCategory.PLAIN_STICKER.name ||
    this.type == MessageCategory.SIGNAL_IMAGE.name ||
    this.type == MessageCategory.PLAIN_IMAGE.name

fun MessageItem.canNotReply() =
    this.type == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name ||
        this.type == MessageCategory.SYSTEM_CONVERSATION.name ||
        (this.mediaStatus != MediaStatus.DONE.name && this.isMedia()) ||
        isCallMessage() || isRecall()

fun MessageItem.isCallMessage() =
    type == MessageCategory.WEBRTC_AUDIO_CANCEL.name ||
        type == MessageCategory.WEBRTC_AUDIO_DECLINE.name ||
        type == MessageCategory.WEBRTC_AUDIO_END.name ||
        type == MessageCategory.WEBRTC_AUDIO_BUSY.name ||
        type == MessageCategory.WEBRTC_AUDIO_FAILED.name

fun MessageItem.isAudio() =
    type == MessageCategory.PLAIN_AUDIO.name ||
        type == MessageCategory.SIGNAL_AUDIO.name

fun MessageItem.canRecall(): Boolean {
    return this.type == MessageCategory.SIGNAL_TEXT.name ||
        this.type == MessageCategory.SIGNAL_IMAGE.name ||
        this.type == MessageCategory.SIGNAL_VIDEO.name ||
        this.type == MessageCategory.SIGNAL_STICKER.name ||
        this.type == MessageCategory.SIGNAL_DATA.name ||
        this.type == MessageCategory.SIGNAL_CONTACT.name ||
        this.type == MessageCategory.SIGNAL_AUDIO.name ||
        this.type == MessageCategory.PLAIN_TEXT.name ||
        this.type == MessageCategory.PLAIN_IMAGE.name ||
        this.type == MessageCategory.PLAIN_VIDEO.name ||
        this.type == MessageCategory.PLAIN_STICKER.name ||
        this.type == MessageCategory.PLAIN_DATA.name ||
        this.type == MessageCategory.PLAIN_CONTACT.name ||
        this.type == MessageCategory.PLAIN_AUDIO.name
}

fun MessageItem.isRecall() = type == MessageCategory.MESSAGE_RECALL.name
