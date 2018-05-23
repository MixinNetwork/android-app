package one.mixin.android.vo

import android.annotation.SuppressLint
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
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
    val assetSymbol: String?,
    val assetIcon: String?,
    val assetUrl: String?,
    val assetHeight: Int?,
    val assetWidth: Int?,
    val albumId: String?,
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
    val sharedUserAppId: String? = null
) : Parcelable

fun create(type: String, createdAt: String? = null) = MessageItem("", "", "", "", "",
    type, null, createdAt
    ?: nowInUtc(), MessageStatus.READ.name, null,
    null, null, null, null, null, null, null,
    null, null, null, null, null, null, null, null,
    null, null, null, null, null, null, null, null, null)