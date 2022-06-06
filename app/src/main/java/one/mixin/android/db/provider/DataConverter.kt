package one.mixin.android.db.provider

import android.annotation.SuppressLint
import android.database.Cursor
import android.os.CancellationSignal
import androidx.room.RoomSQLiteQuery
import androidx.room.util.CursorUtil
import androidx.room.util.DBUtil
import one.mixin.android.db.MixinDatabase
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.SearchMessageDetailItem
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import java.util.concurrent.Callable

@SuppressLint("RestrictedApi")
fun convertToConversationItems(cursor: Cursor?): List<ConversationItem> {
    cursor ?: return emptyList()
    val cursorIndexOfConversationId = CursorUtil.getColumnIndexOrThrow(cursor, "conversationId")
    val cursorIndexOfGroupIconUrl = CursorUtil.getColumnIndexOrThrow(cursor, "groupIconUrl")
    val cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(cursor, "category")
    val cursorIndexOfGroupName = CursorUtil.getColumnIndexOrThrow(cursor, "groupName")
    val cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(cursor, "status")
    val cursorIndexOfLastReadMessageId =
        CursorUtil.getColumnIndexOrThrow(cursor, "lastReadMessageId")
    val cursorIndexOfUnseenMessageCount =
        CursorUtil.getColumnIndexOrThrow(cursor, "unseenMessageCount")
    val cursorIndexOfOwnerId = CursorUtil.getColumnIndexOrThrow(cursor, "ownerId")
    val cursorIndexOfPinTime = CursorUtil.getColumnIndexOrThrow(cursor, "pinTime")
    val cursorIndexOfMuteUntil = CursorUtil.getColumnIndexOrThrow(cursor, "muteUntil")
    val cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(cursor, "avatarUrl")
    val cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(cursor, "name")
    val cursorIndexOfOwnerVerified = CursorUtil.getColumnIndexOrThrow(cursor, "ownerVerified")
    val cursorIndexOfOwnerMuteUntil = CursorUtil.getColumnIndexOrThrow(cursor, "ownerMuteUntil")
    val cursorIndexOfAppId = CursorUtil.getColumnIndexOrThrow(cursor, "appId")
    val cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(cursor, "content")
    val cursorIndexOfContentType = CursorUtil.getColumnIndexOrThrow(cursor, "contentType")
    val cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(cursor, "createdAt")
    val cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(cursor, "senderId")
    val cursorIndexOfActionName = CursorUtil.getColumnIndexOrThrow(cursor, "actionName")
    val cursorIndexOfMessageStatus = CursorUtil.getColumnIndexOrThrow(cursor, "messageStatus")
    val cursorIndexOfSenderFullName = CursorUtil.getColumnIndexOrThrow(cursor, "senderFullName")
    val cursorIndexOfParticipantFullName =
        CursorUtil.getColumnIndexOrThrow(cursor, "participantFullName")
    val cursorIndexOfParticipantUserId =
        CursorUtil.getColumnIndexOrThrow(cursor, "participantUserId")
    val cursorIndexOfMentionCount = CursorUtil.getColumnIndexOrThrow(cursor, "mentionCount")
    val cursorIndexOfMentions = CursorUtil.getColumnIndexOrThrow(cursor, "mentions")
    val res = ArrayList<ConversationItem>(cursor.count)
    while (cursor.moveToNext()) {
        val item: ConversationItem
        val tmpConversationId = cursor.getString(cursorIndexOfConversationId)
        val tmpGroupIconUrl = cursor.getString(cursorIndexOfGroupIconUrl)
        val tmpCategory = cursor.getString(cursorIndexOfCategory)
        val tmpGroupName = cursor.getString(cursorIndexOfGroupName)
        val tmpStatus = cursor.getInt(cursorIndexOfStatus)
        val tmpLastReadMessageId = cursor.getString(cursorIndexOfLastReadMessageId)
        val tmpUnseenMessageCount = if (cursor.isNull(cursorIndexOfUnseenMessageCount)) {
            null
        } else {
            cursor.getInt(cursorIndexOfUnseenMessageCount)
        }
        val tmpOwnerId = cursor.getString(cursorIndexOfOwnerId)
        val tmpPinTime = cursor.getString(cursorIndexOfPinTime)
        val tmpMuteUntil = cursor.getString(cursorIndexOfMuteUntil)
        val tmpAvatarUrl = cursor.getString(cursorIndexOfAvatarUrl)
        val tmpName = cursor.getString(cursorIndexOfName)
        val tmpOwnerVerified: Boolean?
        val tmp = if (cursor.isNull(cursorIndexOfOwnerVerified)) {
            null
        } else {
            cursor.getInt(cursorIndexOfOwnerVerified)
        }
        tmpOwnerVerified = if (tmp == null) null else tmp != 0
        val tmpOwnerMuteUntil = cursor.getString(cursorIndexOfOwnerMuteUntil)
        val tmpAppId = cursor.getString(cursorIndexOfAppId)
        val tmpContent = cursor.getString(cursorIndexOfContent)
        val tmpContentType = cursor.getString(cursorIndexOfContentType)
        val tmpCreatedAt = cursor.getString(cursorIndexOfCreatedAt)
        val tmpSenderId = cursor.getString(cursorIndexOfSenderId)
        val tmpActionName = cursor.getString(cursorIndexOfActionName)
        val tmpMessageStatus = cursor.getString(cursorIndexOfMessageStatus)
        val tmpSenderFullName = cursor.getString(cursorIndexOfSenderFullName)
        val tmpParticipantFullName = cursor.getString(cursorIndexOfParticipantFullName)
        val tmpParticipantUserId = cursor.getString(cursorIndexOfParticipantUserId)
        val tmpMentionCount = if (cursor.isNull(cursorIndexOfMentionCount)) {
            null
        } else {
            cursor.getInt(cursorIndexOfMentionCount)
        }
        val tmpMentions = cursor.getString(cursorIndexOfMentions)
        item = ConversationItem(tmpConversationId, tmpAvatarUrl, tmpGroupIconUrl, tmpCategory, tmpGroupName, tmpName, tmpOwnerId, tmpStatus, tmpLastReadMessageId, tmpUnseenMessageCount, tmpContent, tmpContentType, tmpCreatedAt, tmpPinTime, tmpSenderId, tmpSenderFullName, tmpMessageStatus, tmpActionName, tmpParticipantFullName, tmpParticipantUserId, tmpOwnerMuteUntil, tmpOwnerVerified, tmpMuteUntil, tmpAppId, tmpMentions, tmpMentionCount)
        res.add(item)
    }
    return res
}

fun convertToMessageItems(cursor: Cursor?): ArrayList<MessageItem> {
    cursor ?: return ArrayList()
    val cursorIndexOfMessageId = cursor.getColumnIndexOrThrow("messageId")
    val cursorIndexOfConversationId = cursor.getColumnIndexOrThrow("conversationId")
    val cursorIndexOfUserId = cursor.getColumnIndexOrThrow("userId")
    val cursorIndexOfUserFullName = cursor.getColumnIndexOrThrow("userFullName")
    val cursorIndexOfUserIdentityNumber = cursor.getColumnIndexOrThrow("userIdentityNumber")
    val cursorIndexOfAppId = cursor.getColumnIndexOrThrow("appId")
    val cursorIndexOfType = cursor.getColumnIndexOrThrow("type")
    val cursorIndexOfContent = cursor.getColumnIndexOrThrow("content")
    val cursorIndexOfCreatedAt = cursor.getColumnIndexOrThrow("createdAt")
    val cursorIndexOfStatus = cursor.getColumnIndexOrThrow("status")
    val cursorIndexOfMediaStatus = cursor.getColumnIndexOrThrow("mediaStatus")
    val cursorIndexOfMediaWaveform = cursor.getColumnIndexOrThrow("mediaWaveform")
    val cursorIndexOfMediaName = cursor.getColumnIndexOrThrow("mediaName")
    val cursorIndexOfMediaMimeType = cursor.getColumnIndexOrThrow("mediaMimeType")
    val cursorIndexOfMediaSize = cursor.getColumnIndexOrThrow("mediaSize")
    val cursorIndexOfMediaWidth = cursor.getColumnIndexOrThrow("mediaWidth")
    val cursorIndexOfMediaHeight = cursor.getColumnIndexOrThrow("mediaHeight")
    val cursorIndexOfThumbImage = cursor.getColumnIndexOrThrow("thumbImage")
    val cursorIndexOfThumbUrl = cursor.getColumnIndexOrThrow("thumbUrl")
    val cursorIndexOfMediaUrl = cursor.getColumnIndexOrThrow("mediaUrl")
    val cursorIndexOfMediaDuration = cursor.getColumnIndexOrThrow("mediaDuration")
    val cursorIndexOfQuoteId = cursor.getColumnIndexOrThrow("quoteId")
    val cursorIndexOfQuoteContent = cursor.getColumnIndexOrThrow("quoteContent")
    val cursorIndexOfParticipantFullName = cursor.getColumnIndexOrThrow("participantFullName")
    val cursorIndexOfActionName = cursor.getColumnIndexOrThrow("actionName")
    val cursorIndexOfParticipantUserId = cursor.getColumnIndexOrThrow("participantUserId")
    val cursorIndexOfSnapshotId = cursor.getColumnIndexOrThrow("snapshotId")
    val cursorIndexOfSnapshotType = cursor.getColumnIndexOrThrow("snapshotType")
    val cursorIndexOfSnapshotAmount = cursor.getColumnIndexOrThrow("snapshotAmount")
    val cursorIndexOfAssetSymbol = cursor.getColumnIndexOrThrow("assetSymbol")
    val cursorIndexOfAssetId = cursor.getColumnIndexOrThrow("assetId")
    val cursorIndexOfAssetIcon = cursor.getColumnIndexOrThrow("assetIcon")
    val cursorIndexOfAssetUrl = cursor.getColumnIndexOrThrow("assetUrl")
    val cursorIndexOfAssetWidth = cursor.getColumnIndexOrThrow("assetWidth")
    val cursorIndexOfAssetHeight = cursor.getColumnIndexOrThrow("assetHeight")
    val cursorIndexOfStickerId = cursor.getColumnIndexOrThrow("stickerId")
    val cursorIndexOfAssetName = cursor.getColumnIndexOrThrow("assetName")
    val cursorIndexOfAssetType = cursor.getColumnIndexOrThrow("assetType")
    val cursorIndexOfSiteName = cursor.getColumnIndexOrThrow("siteName")
    val cursorIndexOfSiteTitle = cursor.getColumnIndexOrThrow("siteTitle")
    val cursorIndexOfSiteDescription = cursor.getColumnIndexOrThrow("siteDescription")
    val cursorIndexOfSiteImage = cursor.getColumnIndexOrThrow("siteImage")
    val cursorIndexOfSharedUserId = cursor.getColumnIndexOrThrow("sharedUserId")
    val cursorIndexOfSharedUserFullName = cursor.getColumnIndexOrThrow("sharedUserFullName")
    val cursorIndexOfSharedUserIdentityNumber =
        cursor.getColumnIndexOrThrow("sharedUserIdentityNumber")
    val cursorIndexOfSharedUserAvatarUrl = cursor.getColumnIndexOrThrow("sharedUserAvatarUrl")
    val cursorIndexOfSharedUserIsVerified = cursor.getColumnIndexOrThrow("sharedUserIsVerified")
    val cursorIndexOfSharedUserAppId = cursor.getColumnIndexOrThrow("sharedUserAppId")
    val cursorIndexOfGroupName = cursor.getColumnIndexOrThrow("groupName")
    val cursorIndexOfMentions = cursor.getColumnIndexOrThrow("mentions")
    val cursorIndexOfMentionRead = cursor.getColumnIndexOrThrow("mentionRead")
    val cursorIndexOfPinTop = cursor.getColumnIndexOrThrow("isPin")
    val cursorIndexOfExpireIn = cursor.getColumnIndexOrThrow("expireIn")
    val res = ArrayList<MessageItem>(cursor.count)
    while (cursor.moveToNext()) {
        val item: MessageItem
        val tmpMessageId: String = cursor.getString(cursorIndexOfMessageId)
        val tmpConversationId: String = cursor.getString(cursorIndexOfConversationId)
        val tmpUserId: String = cursor.getString(cursorIndexOfUserId)
        val tmpUserFullName: String = cursor.getString(cursorIndexOfUserFullName)
        val tmpUserIdentityNumber: String = cursor.getString(cursorIndexOfUserIdentityNumber)
        val tmpAppId: String? = cursor.getString(cursorIndexOfAppId)
        val tmpType: String = cursor.getString(cursorIndexOfType)
        val tmpContent: String? = cursor.getString(cursorIndexOfContent)
        val tmpCreatedAt: String = cursor.getString(cursorIndexOfCreatedAt)
        val tmpStatus: String = cursor.getString(cursorIndexOfStatus)
        val tmpMediaStatus: String? = cursor.getString(cursorIndexOfMediaStatus)
        val tmpMediaWaveform: ByteArray? = cursor.getBlob(cursorIndexOfMediaWaveform)
        val tmpMediaName: String? = cursor.getString(cursorIndexOfMediaName)
        val tmpMediaMimeType: String? = cursor.getString(cursorIndexOfMediaMimeType)
        val tmpMediaSize: Long? = if (cursor.isNull(cursorIndexOfMediaSize)) {
            null
        } else {
            cursor.getLong(cursorIndexOfMediaSize)
        }
        val tmpMediaWidth: Int? = if (cursor.isNull(cursorIndexOfMediaWidth)) {
            null
        } else {
            cursor.getInt(cursorIndexOfMediaWidth)
        }
        val tmpMediaHeight: Int? = if (cursor.isNull(cursorIndexOfMediaHeight)) {
            null
        } else {
            cursor.getInt(cursorIndexOfMediaHeight)
        }
        val tmpThumbImage: String? = cursor.getString(cursorIndexOfThumbImage)
        val tmpThumbUrl: String? = cursor.getString(cursorIndexOfThumbUrl)
        val tmpMediaUrl: String? = cursor.getString(cursorIndexOfMediaUrl)
        val tmpMediaDuration: String? = cursor.getString(cursorIndexOfMediaDuration)
        val tmpQuoteId: String? = cursor.getString(cursorIndexOfQuoteId)
        val tmpQuoteContent: String? = cursor.getString(cursorIndexOfQuoteContent)
        val tmpParticipantFullName: String? = cursor.getString(cursorIndexOfParticipantFullName)
        val tmpActionName: String? = cursor.getString(cursorIndexOfActionName)
        val tmpParticipantUserId: String? = cursor.getString(cursorIndexOfParticipantUserId)
        val tmpSnapshotId: String? = cursor.getString(cursorIndexOfSnapshotId)
        val tmpSnapshotType: String? = cursor.getString(cursorIndexOfSnapshotType)
        val tmpSnapshotAmount: String? = cursor.getString(cursorIndexOfSnapshotAmount)
        val tmpAssetSymbol: String? = cursor.getString(cursorIndexOfAssetSymbol)
        val tmpAssetId: String? = cursor.getString(cursorIndexOfAssetId)
        val tmpAssetIcon: String? = cursor.getString(cursorIndexOfAssetIcon)
        val tmpAssetUrl: String? = cursor.getString(cursorIndexOfAssetUrl)
        val tmpAssetWidth: Int? = if (cursor.isNull(cursorIndexOfAssetWidth)) {
            null
        } else {
            cursor.getInt(cursorIndexOfAssetWidth)
        }
        val tmpAssetHeight: Int? = if (cursor.isNull(cursorIndexOfAssetHeight)) {
            null
        } else {
            cursor.getInt(cursorIndexOfAssetHeight)
        }
        val tmpStickerId: String? = cursor.getString(cursorIndexOfStickerId)
        val tmpAssetName: String? = cursor.getString(cursorIndexOfAssetName)
        val tmpAssetType: String? = cursor.getString(cursorIndexOfAssetType)
        val tmpSiteName: String? = cursor.getString(cursorIndexOfSiteName)
        val tmpSiteTitle: String? = cursor.getString(cursorIndexOfSiteTitle)
        val tmpSiteDescription: String? = cursor.getString(cursorIndexOfSiteDescription)
        val tmpSiteImage: String? = cursor.getString(cursorIndexOfSiteImage)
        val tmpSharedUserId: String? = cursor.getString(cursorIndexOfSharedUserId)
        val tmpSharedUserFullName: String? = cursor.getString(cursorIndexOfSharedUserFullName)
        val tmpSharedUserIdentityNumber: String? =
            cursor.getString(cursorIndexOfSharedUserIdentityNumber)
        val tmpSharedUserAvatarUrl: String? = cursor.getString(cursorIndexOfSharedUserAvatarUrl)
        val tmpSharedUserIsVerified: Boolean?
        val tmp: Int? = if (cursor.isNull(cursorIndexOfSharedUserIsVerified)) {
            null
        } else {
            cursor.getInt(cursorIndexOfSharedUserIsVerified)
        }
        tmpSharedUserIsVerified = if (tmp == null) null else tmp != 0
        val tmpSharedUserAppId: String? = cursor.getString(cursorIndexOfSharedUserAppId)
        val tmpGroupName: String? = cursor.getString(cursorIndexOfGroupName)
        val tmpMentions: String? = cursor.getString(cursorIndexOfMentions)
        val tmp1 = if (cursor.isNull(cursorIndexOfMentionRead)) {
            null
        } else {
            cursor.getInt(cursorIndexOfMentionRead)
        }
        val tmpMentionRead = if (tmp1 == null) null else tmp1 != 0
        val tmp2: Int? = if (cursor.isNull(cursorIndexOfPinTop)) {
            null
        } else {
            cursor.getInt(cursorIndexOfPinTop)
        }
        val tmpPinTop = if (tmp2 == null) null else tmp2 != 0
        val tempExpireIn: Long? = if (cursor.isNull(cursorIndexOfExpireIn)) {
            null
        } else {
            cursor.getLong(cursorIndexOfExpireIn)
        }
        item = MessageItem(
            tmpMessageId,
            tmpConversationId,
            tmpUserId,
            tmpUserFullName,
            tmpUserIdentityNumber,
            tmpType,
            tmpContent,
            tmpCreatedAt,
            tmpStatus,
            tmpMediaStatus,
            null,
            tmpMediaName,
            tmpMediaMimeType,
            tmpMediaSize,
            tmpThumbUrl,
            tmpMediaWidth,
            tmpMediaHeight,
            tmpThumbImage,
            tmpMediaUrl,
            tmpMediaDuration,
            tmpParticipantFullName,
            tmpParticipantUserId,
            tmpActionName,
            tmpSnapshotId,
            tmpSnapshotType,
            tmpSnapshotAmount,
            tmpAssetId,
            tmpAssetType,
            tmpAssetSymbol,
            tmpAssetIcon,
            tmpAssetUrl,
            tmpAssetHeight,
            tmpAssetWidth,
            null,
            tmpStickerId,
            tmpAssetName,
            tmpAppId,
            tmpSiteName,
            tmpSiteTitle,
            tmpSiteDescription,
            tmpSiteImage,
            tmpSharedUserId,
            tmpSharedUserFullName,
            tmpSharedUserIdentityNumber,
            tmpSharedUserAvatarUrl,
            tmpSharedUserIsVerified,
            tmpSharedUserAppId,
            tmpMediaWaveform,
            tmpQuoteId,
            tmpQuoteContent,
            tmpGroupName,
            tmpMentions,
            tmpMentionRead,
            tmpPinTop,
            tempExpireIn
        )
        res.add(item)
    }
    return res
}

fun convertToSearchMessageDetailItem(cursor: Cursor?): ArrayList<SearchMessageDetailItem> {
    cursor ?: return ArrayList()
    val cursorIndexOfMessageId = cursor.getColumnIndexOrThrow("messageId")
    val cursorIndexOfUserId = cursor.getColumnIndexOrThrow("userId")
    val cursorIndexOfUserAvatarUrl = cursor.getColumnIndexOrThrow("userAvatarUrl")
    val cursorIndexOfUserFullName = cursor.getColumnIndexOrThrow("userFullName")
    val cursorIndexOfType = cursor.getColumnIndexOrThrow("type")
    val cursorIndexOfContent = cursor.getColumnIndexOrThrow("content")
    val cursorIndexOfCreatedAt = cursor.getColumnIndexOrThrow("createdAt")
    val cursorIndexOfMediaName = cursor.getColumnIndexOrThrow("mediaName")
    val res = ArrayList<SearchMessageDetailItem>(cursor.count)
    while (cursor.moveToNext()) {
        val item: SearchMessageDetailItem
        val tmpMessageId = cursor.getString(cursorIndexOfMessageId)
        val tmpUserId = cursor.getString(cursorIndexOfUserId)
        val tmpUserAvatarUrl = cursor.getString(cursorIndexOfUserAvatarUrl)
        val tmpUserFullName = cursor.getString(cursorIndexOfUserFullName)
        val tmpType = cursor.getString(cursorIndexOfType)
        val tmpContent = cursor.getString(cursorIndexOfContent)
        val tmpCreatedAt = cursor.getString(cursorIndexOfCreatedAt)
        val tmpMediaName = cursor.getString(cursorIndexOfMediaName)
        item = SearchMessageDetailItem(
            tmpMessageId,
            tmpType,
            tmpContent,
            tmpCreatedAt,
            tmpMediaName,
            tmpUserId,
            tmpUserFullName,
            tmpUserAvatarUrl
        )
        res.add(item)
    }
    return res
}

@SuppressLint("RestrictedApi")
fun callableUser(
    db: MixinDatabase,
    statement: RoomSQLiteQuery,
    cancellationSignal: CancellationSignal
): Callable<List<User>> {
    return Callable<List<User>> {
        val cursor = DBUtil.query(db, statement, false, cancellationSignal)
        try {
            val cursorIndexOfUserId =
                CursorUtil.getColumnIndexOrThrow(cursor, "user_id")
            val cursorIndexOfIdentityNumber =
                CursorUtil.getColumnIndexOrThrow(cursor, "identity_number")
            val cursorIndexOfRelationship =
                CursorUtil.getColumnIndexOrThrow(cursor, "relationship")
            val cursorIndexOfBiography =
                CursorUtil.getColumnIndexOrThrow(cursor, "biography")
            val cursorIndexOfFullName =
                CursorUtil.getColumnIndexOrThrow(cursor, "full_name")
            val cursorIndexOfAvatarUrl =
                CursorUtil.getColumnIndexOrThrow(cursor, "avatar_url")
            val cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(cursor, "phone")
            val cursorIndexOfIsVerified =
                CursorUtil.getColumnIndexOrThrow(cursor, "is_verified")
            val cursorIndexOfCreatedAt =
                CursorUtil.getColumnIndexOrThrow(cursor, "created_at")
            val cursorIndexOfMuteUntil =
                CursorUtil.getColumnIndexOrThrow(cursor, "mute_until")
            val cursorIndexOfHasPin =
                CursorUtil.getColumnIndexOrThrow(cursor, "has_pin")
            val cursorIndexOfAppId =
                CursorUtil.getColumnIndexOrThrow(cursor, "app_id")
            val cursorIndexOfIsScam =
                CursorUtil.getColumnIndexOrThrow(cursor, "is_scam")
            val result: MutableList<User> = java.util.ArrayList(cursor.count)
            while (cursor.moveToNext()) {
                val item: User
                val tmpUserId: String? = if (cursor.isNull(cursorIndexOfUserId)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfUserId)
                }
                val tmpIdentityNumber: String? = if (cursor.isNull(cursorIndexOfIdentityNumber)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfIdentityNumber)
                }
                val tmpRelationship: String? = if (cursor.isNull(cursorIndexOfRelationship)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfRelationship)
                }
                val tmpBiography: String? = if (cursor.isNull(cursorIndexOfBiography)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfBiography)
                }
                val tmpFullName: String? = if (cursor.isNull(cursorIndexOfFullName)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfFullName)
                }
                val tmpAvatarUrl: String? = if (cursor.isNull(cursorIndexOfAvatarUrl)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfAvatarUrl)
                }
                val tmpPhone: String? = if (cursor.isNull(cursorIndexOfPhone)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfPhone)
                }
                val tmpIsVerified: Boolean?
                val tmp: Int? = if (cursor.isNull(cursorIndexOfIsVerified)) {
                    null
                } else {
                    cursor.getInt(cursorIndexOfIsVerified)
                }
                tmpIsVerified = if (tmp == null) null else tmp != 0
                val tmpCreatedAt: String? = if (cursor.isNull(cursorIndexOfCreatedAt)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfCreatedAt)
                }
                val tmpMuteUntil: String? = if (cursor.isNull(cursorIndexOfMuteUntil)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfMuteUntil)
                }
                val tmpHasPin: Boolean?
                val tmp1: Int? = if (cursor.isNull(cursorIndexOfHasPin)) {
                    null
                } else {
                    cursor.getInt(cursorIndexOfHasPin)
                }
                tmpHasPin = if (tmp1 == null) null else tmp1 != 0
                val tmpAppId: String? = if (cursor.isNull(cursorIndexOfAppId)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfAppId)
                }
                val tmpIsScam: Boolean?
                val tmp2: Int? = if (cursor.isNull(cursorIndexOfIsScam)) {
                    null
                } else {
                    cursor.getInt(cursorIndexOfIsScam)
                }
                tmpIsScam = if (tmp2 == null) null else tmp2 != 0
                item = User(
                    tmpUserId!!,
                    tmpIdentityNumber!!,
                    tmpRelationship!!,
                    tmpBiography!!,
                    tmpFullName,
                    tmpAvatarUrl,
                    tmpPhone,
                    tmpIsVerified,
                    tmpCreatedAt,
                    tmpMuteUntil,
                    tmpHasPin,
                    tmpAppId,
                    tmpIsScam
                )
                result.add(item)
            }
            return@Callable result
        } finally {
            cursor.close()
            statement.release()
        }
    }
}

@SuppressLint("RestrictedApi")
fun callableAssetItem(
    db: MixinDatabase,
    statement: RoomSQLiteQuery,
    cancellationSignal: CancellationSignal
): Callable<List<AssetItem>> {
    return Callable<List<AssetItem>> {
        val cursor = DBUtil.query(db, statement, false, cancellationSignal)
        try {
            val cursorIndexOfAssetId = 0
            val cursorIndexOfSymbol = 1
            val cursorIndexOfName = 2
            val cursorIndexOfIconUrl = 3
            val cursorIndexOfBalance = 4
            val cursorIndexOfDestination = 5
            val cursorIndexOfTag = 6
            val cursorIndexOfPriceBtc = 7
            val cursorIndexOfPriceUsd = 8
            val cursorIndexOfChainId = 9
            val cursorIndexOfChangeUsd = 10
            val cursorIndexOfChangeBtc = 11
            val cursorIndexOfHidden = 12
            val cursorIndexOfChainPriceUsd = 13
            val cursorIndexOfConfirmations = 14
            val cursorIndexOfReserve = 15
            val cursorIndexOfChainIconUrl = 16
            val cursorIndexOfChainSymbol = 17
            val cursorIndexOfChainName = 18
            val cursorIndexOfAssetKey = 19
            val result: MutableList<AssetItem> = java.util.ArrayList(cursor.count)
            while (cursor.moveToNext()) {
                val item: AssetItem
                val tmpAssetId: String? = if (cursor.isNull(cursorIndexOfAssetId)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfAssetId)
                }
                val tmpSymbol: String? = if (cursor.isNull(cursorIndexOfSymbol)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfSymbol)
                }
                val tmpName: String? = if (cursor.isNull(cursorIndexOfName)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfName)
                }
                val tmpIconUrl: String? = if (cursor.isNull(cursorIndexOfIconUrl)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfIconUrl)
                }
                val tmpBalance: String? = if (cursor.isNull(cursorIndexOfBalance)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfBalance)
                }
                val tmpDestination: String? = if (cursor.isNull(cursorIndexOfDestination)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfDestination)
                }
                val tmpTag: String? = if (cursor.isNull(cursorIndexOfTag)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfTag)
                }
                val tmpPriceBtc: String? = if (cursor.isNull(cursorIndexOfPriceBtc)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfPriceBtc)
                }
                val tmpPriceUsd: String? = if (cursor.isNull(cursorIndexOfPriceUsd)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfPriceUsd)
                }
                val tmpChainId: String? = if (cursor.isNull(cursorIndexOfChainId)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfChainId)
                }
                val tmpChangeUsd: String? = if (cursor.isNull(cursorIndexOfChangeUsd)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfChangeUsd)
                }
                val tmpChangeBtc: String? = if (cursor.isNull(cursorIndexOfChangeBtc)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfChangeBtc)
                }
                val tmpHidden: Boolean?
                val tmp: Int? = if (cursor.isNull(cursorIndexOfHidden)) {
                    null
                } else {
                    cursor.getInt(cursorIndexOfHidden)
                }
                tmpHidden = if (tmp == null) null else tmp != 0
                val tmpChainPriceUsd: String? = if (cursor.isNull(cursorIndexOfChainPriceUsd)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfChainPriceUsd)
                }
                val tmpConfirmations: Int = cursor.getInt(cursorIndexOfConfirmations)
                val tmpReserve: String? = if (cursor.isNull(cursorIndexOfReserve)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfReserve)
                }
                val tmpChainIconUrl: String? = if (cursor.isNull(cursorIndexOfChainIconUrl)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfChainIconUrl)
                }
                val tmpChainSymbol: String? = if (cursor.isNull(cursorIndexOfChainSymbol)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfChainSymbol)
                }
                val tmpChainName: String? = if (cursor.isNull(cursorIndexOfChainName)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfChainName)
                }
                val tmpAssetKey: String? = if (cursor.isNull(cursorIndexOfAssetKey)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfAssetKey)
                }
                item = AssetItem(
                    tmpAssetId!!,
                    tmpSymbol!!,
                    tmpName!!,
                    tmpIconUrl!!,
                    tmpBalance!!,
                    tmpDestination!!,
                    tmpTag,
                    tmpPriceBtc!!,
                    tmpPriceUsd!!,
                    tmpChainId!!,
                    tmpChangeUsd!!,
                    tmpChangeBtc!!,
                    tmpHidden,
                    tmpConfirmations,
                    tmpChainIconUrl,
                    tmpChainSymbol,
                    tmpChainName,
                    tmpChainPriceUsd,
                    tmpAssetKey,
                    tmpReserve
                )
                result.add(item)
            }
            return@Callable result
        } finally {
            cursor.close()
            statement.release()
        }
    }
}

@SuppressLint("RestrictedApi")

fun callableSearchMessageItem(
    db: MixinDatabase,
    statement: RoomSQLiteQuery,
    cancellationSignal: CancellationSignal
): Callable<List<SearchMessageItem>> {
    return Callable<List<SearchMessageItem>> {
        val cursor = DBUtil.query(db, statement, false, cancellationSignal)
        try {
            val cursorIndexOfConversationId = 0
            val cursorIndexOfConversationAvatarUrl = 1
            val cursorIndexOfConversationName = 2
            val cursorIndexOfConversationCategory = 3
            val cursorIndexOfMessageCount = 4
            val cursorIndexOfUserId = 5
            val cursorIndexOfUserAvatarUrl = 6
            val cursorIndexOfUserFullName = 7
            val result: MutableList<SearchMessageItem> =
                java.util.ArrayList(cursor.count)
            while (cursor.moveToNext()) {
                val item: SearchMessageItem
                val tmpConversationId: String? = if (cursor.isNull(cursorIndexOfConversationId)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfConversationId)
                }
                val tmpConversationAvatarUrl: String? =
                    if (cursor.isNull(cursorIndexOfConversationAvatarUrl)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfConversationAvatarUrl)
                    }
                val tmpConversationName: String? =
                    if (cursor.isNull(cursorIndexOfConversationName)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfConversationName)
                    }
                val tmpConversationCategory: String? =
                    if (cursor.isNull(cursorIndexOfConversationCategory)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfConversationCategory)
                    }
                val tmpMessageCount: Int = cursor.getInt(cursorIndexOfMessageCount)
                val tmpUserId: String? = if (cursor.isNull(cursorIndexOfUserId)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfUserId)
                }
                val tmpUserAvatarUrl: String? = if (cursor.isNull(cursorIndexOfUserAvatarUrl)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfUserAvatarUrl)
                }
                val tmpUserFullName: String? = if (cursor.isNull(cursorIndexOfUserFullName)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfUserFullName)
                }
                item = SearchMessageItem(
                    tmpConversationId!!,
                    tmpConversationCategory,
                    tmpConversationName,
                    tmpMessageCount,
                    tmpUserId!!,
                    tmpUserFullName,
                    tmpUserAvatarUrl,
                    tmpConversationAvatarUrl
                )
                result.add(item)
            }
            return@Callable result
        } finally {
            cursor.close()
            statement.release()
        }
    }
}

@SuppressLint("RestrictedApi")
fun callableChatMinimal(
    db: MixinDatabase,
    statement: RoomSQLiteQuery,
    cancellationSignal: CancellationSignal
): Callable<List<ChatMinimal>> {
    return Callable<List<ChatMinimal>> {
        val cursor = DBUtil.query(db, statement, false, cancellationSignal)
        try {
            val cursorIndexOfConversationId = 0
            val cursorIndexOfGroupIconUrl = 1
            val cursorIndexOfCategory = 2
            val cursorIndexOfGroupName = 3
            val cursorIndexOfOwnerIdentityNumber = 4
            val cursorIndexOfUserId = 5
            val cursorIndexOfFullName = 6
            val cursorIndexOfAvatarUrl = 7
            val cursorIndexOfIsVerified = 8
            val cursorIndexOfAppId = 9
            val cursorIndexOfOwnerMuteUntil = 10
            val cursorIndexOfMuteUntil = 11
            val cursorIndexOfPinTime = 12
            val result: MutableList<ChatMinimal> = java.util.ArrayList(cursor.count)
            while (cursor.moveToNext()) {
                val item: ChatMinimal
                val tmpConversationId: String? = if (cursor.isNull(cursorIndexOfConversationId)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfConversationId)
                }
                val tmpGroupIconUrl: String? = if (cursor.isNull(cursorIndexOfGroupIconUrl)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfGroupIconUrl)
                }
                val tmpCategory: String? = if (cursor.isNull(cursorIndexOfCategory)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfCategory)
                }
                val tmpGroupName: String? = if (cursor.isNull(cursorIndexOfGroupName)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfGroupName)
                }
                val tmpOwnerIdentityNumber: String? =
                    if (cursor.isNull(cursorIndexOfOwnerIdentityNumber)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfOwnerIdentityNumber)
                    }
                val tmpUserId: String? = if (cursor.isNull(cursorIndexOfUserId)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfUserId)
                }
                val tmpFullName: String? = if (cursor.isNull(cursorIndexOfFullName)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfFullName)
                }
                val tmpAvatarUrl: String? = if (cursor.isNull(cursorIndexOfAvatarUrl)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfAvatarUrl)
                }
                val tmpIsVerified: Boolean?
                val tmp: Int? = if (cursor.isNull(cursorIndexOfIsVerified)) {
                    null
                } else {
                    cursor.getInt(cursorIndexOfIsVerified)
                }
                tmpIsVerified = if (tmp == null) null else tmp != 0
                val tmpAppId: String? = if (cursor.isNull(cursorIndexOfAppId)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfAppId)
                }
                val tmpOwnerMuteUntil: String? = if (cursor.isNull(cursorIndexOfOwnerMuteUntil)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfOwnerMuteUntil)
                }
                val tmpMuteUntil: String? = if (cursor.isNull(cursorIndexOfMuteUntil)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfMuteUntil)
                }
                val tmpPinTime: String? = if (cursor.isNull(cursorIndexOfPinTime)) {
                    null
                } else {
                    cursor.getString(cursorIndexOfPinTime)
                }
                item = ChatMinimal(
                    tmpCategory!!,
                    tmpConversationId!!,
                    tmpGroupIconUrl,
                    tmpGroupName,
                    tmpOwnerIdentityNumber!!,
                    tmpUserId!!,
                    tmpFullName,
                    tmpAvatarUrl,
                    tmpIsVerified,
                    tmpAppId,
                    tmpOwnerMuteUntil,
                    tmpMuteUntil,
                    tmpPinTime
                )
                result.add(item)
            }
            return@Callable result
        } finally {
            cursor.close()
            statement.release()
        }
    }
}
