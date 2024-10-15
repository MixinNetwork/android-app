package one.mixin.android.db.provider

import android.annotation.SuppressLint
import android.database.Cursor
import android.os.CancellationSignal
import androidx.room.RoomSQLiteQuery
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.converter.DepositEntryListConverter
import one.mixin.android.db.converter.MembershipConverter
import one.mixin.android.db.converter.OptionalListConverter
import one.mixin.android.db.converter.WithdrawalMemoPossibilityConverter
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.SearchBot
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import one.mixin.android.vo.WithdrawalMemoPossibility
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.safe.SafeCollectible
import one.mixin.android.vo.safe.TokenItem
import java.util.concurrent.Callable

@SuppressLint("RestrictedApi")
fun convertToConversationItems(cursor: Cursor?): List<ConversationItem> {
    cursor ?: return emptyList()
    val cursorIndexOfConversationId = getColumnIndexOrThrow(cursor, "conversationId")
    val cursorIndexOfGroupIconUrl = getColumnIndexOrThrow(cursor, "groupIconUrl")
    val cursorIndexOfCategory = getColumnIndexOrThrow(cursor, "category")
    val cursorIndexOfGroupName = getColumnIndexOrThrow(cursor, "groupName")
    val cursorIndexOfStatus = getColumnIndexOrThrow(cursor, "status")
    val cursorIndexOfLastReadMessageId =
        getColumnIndexOrThrow(cursor, "lastReadMessageId")
    val cursorIndexOfUnseenMessageCount =
        getColumnIndexOrThrow(cursor, "unseenMessageCount")
    val cursorIndexOfOwnerId = getColumnIndexOrThrow(cursor, "ownerId")
    val cursorIndexOfPinTime = getColumnIndexOrThrow(cursor, "pinTime")
    val cursorIndexOfMuteUntil = getColumnIndexOrThrow(cursor, "muteUntil")
    val cursorIndexOfAvatarUrl = getColumnIndexOrThrow(cursor, "avatarUrl")
    val cursorIndexOfName = getColumnIndexOrThrow(cursor, "name")
    val cursorIndexOfOwnerVerified = getColumnIndexOrThrow(cursor, "ownerVerified")
    val cursorIndexOfOwnerMuteUntil = getColumnIndexOrThrow(cursor, "ownerMuteUntil")
    val cursorIndexOfAppId = getColumnIndexOrThrow(cursor, "appId")
    val cursorIndexOfContent = getColumnIndexOrThrow(cursor, "content")
    val cursorIndexOfContentType = getColumnIndexOrThrow(cursor, "contentType")
    val cursorIndexOfCreatedAt = getColumnIndexOrThrow(cursor, "createdAt")
    val cursorIndexOfSenderId = getColumnIndexOrThrow(cursor, "senderId")
    val cursorIndexOfActionName = getColumnIndexOrThrow(cursor, "actionName")
    val cursorIndexOfMessageStatus = getColumnIndexOrThrow(cursor, "messageStatus")
    val cursorIndexOfSenderFullName = getColumnIndexOrThrow(cursor, "senderFullName")
    val cursorIndexOfParticipantFullName =
        getColumnIndexOrThrow(cursor, "participantFullName")
    val cursorIndexOfParticipantUserId =
        getColumnIndexOrThrow(cursor, "participantUserId")
    val cursorIndexOfMentionCount = getColumnIndexOrThrow(cursor, "mentionCount")
    val cursorIndexOfMentions = getColumnIndexOrThrow(cursor, "mentions")
    val cursorIndexOfMembership = getColumnIndexOrThrow(cursor, "membership")
    val res = ArrayList<ConversationItem>(cursor.count)
    while (cursor.moveToNext()) {
        val item: ConversationItem
        val tmpConversationId = cursor.getString(cursorIndexOfConversationId)
        val tmpGroupIconUrl = cursor.getString(cursorIndexOfGroupIconUrl)
        val tmpCategory = cursor.getString(cursorIndexOfCategory)
        val tmpGroupName = cursor.getString(cursorIndexOfGroupName)
        val tmpStatus = cursor.getInt(cursorIndexOfStatus)
        val tmpLastReadMessageId = cursor.getString(cursorIndexOfLastReadMessageId)
        val tmpUnseenMessageCount =
            if (cursor.isNull(cursorIndexOfUnseenMessageCount)) {
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
        val tmp =
            if (cursor.isNull(cursorIndexOfOwnerVerified)) {
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
        val tmpMentionCount =
            if (cursor.isNull(cursorIndexOfMentionCount)) {
                null
            } else {
                cursor.getInt(cursorIndexOfMentionCount)
            }
        val tmpMentions = cursor.getString(cursorIndexOfMentions)
        val tmpMembership = cursor.getString(cursorIndexOfMembership)
        item =
            ConversationItem(
                tmpConversationId,
                tmpAvatarUrl,
                tmpGroupIconUrl,
                tmpCategory,
                tmpGroupName,
                tmpName,
                tmpOwnerId,
                tmpStatus,
                tmpLastReadMessageId,
                tmpUnseenMessageCount,
                tmpContent,
                tmpContentType,
                tmpCreatedAt,
                tmpPinTime,
                tmpSenderId,
                tmpSenderFullName,
                tmpMessageStatus,
                tmpActionName,
                tmpParticipantFullName,
                tmpParticipantUserId,
                tmpOwnerMuteUntil,
                tmpOwnerVerified,
                tmpMuteUntil,
                tmpAppId,
                tmpMentions,
                tmpMentionCount,
                membershipConverter.revertData(tmpMembership)
            )
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
    val cursorIndexOfSnapshotMemo = cursor.getColumnIndexOrThrow("snapshotMemo")
    val cursorIndexOfSnapshotType = cursor.getColumnIndexOrThrow("snapshotType")
    val cursorIndexOfSnapshotAmount = cursor.getColumnIndexOrThrow("snapshotAmount")
    val cursorIndexOfAssetSymbol = cursor.getColumnIndexOrThrow("assetSymbol")
    val cursorIndexOfAssetId = cursor.getColumnIndexOrThrow("assetId")
    val cursorIndexOfAssetIcon = cursor.getColumnIndexOrThrow("assetIcon")
    val cursorIndexOfAssetCollectionHash = cursor.getColumnIndexOrThrow("assetCollectionHash")
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
    val cursorIndexOfSharedMembership = cursor.getColumnIndexOrThrow("sharedMembership")
    val cursorIndexOfGroupName = cursor.getColumnIndexOrThrow("groupName")
    val cursorIndexOfMentions = cursor.getColumnIndexOrThrow("mentions")
    val cursorIndexOfMentionRead = cursor.getColumnIndexOrThrow("mentionRead")
    val cursorIndexOfPinTop = cursor.getColumnIndexOrThrow("isPin")
    val cursorIndexOfExpireIn = cursor.getColumnIndexOrThrow("expireIn")
    val cursorIndexOfExpireAt = cursor.getColumnIndexOrThrow("expireAt")
    val cursorIndexOfCaption = cursor.getColumnIndexOrThrow("caption")
    val cursorIndexOfMembership = cursor.getColumnIndexOrThrow("membership")
    val res = ArrayList<MessageItem>(cursor.count)
    while (cursor.moveToNext()) {
        val item: MessageItem
        val tmpMessageId: String = cursor.getString(cursorIndexOfMessageId)
        val tmpConversationId: String = cursor.getString(cursorIndexOfConversationId)
        val tmpUserId: String =
            if (cursor.isNull(cursorIndexOfUserId)) {
                ""
            } else {
                cursor.getString(cursorIndexOfUserId)
            }
        val tmpUserFullName: String =
            if (cursor.isNull(cursorIndexOfUserFullName)) {
                ""
            } else {
                cursor.getString(cursorIndexOfUserFullName)
            }
        val tmpUserIdentityNumber: String =
            if (cursor.isNull(cursorIndexOfUserIdentityNumber)) {
                ""
            } else {
                cursor.getString(cursorIndexOfUserIdentityNumber)
            }
        val tmpAppId: String? = cursor.getString(cursorIndexOfAppId)
        val tmpType: String = cursor.getString(cursorIndexOfType)
        val tmpContent: String? = cursor.getString(cursorIndexOfContent)
        val tmpCreatedAt: String = cursor.getString(cursorIndexOfCreatedAt)
        val tmpStatus: String = cursor.getString(cursorIndexOfStatus)
        val tmpMediaStatus: String? = cursor.getString(cursorIndexOfMediaStatus)
        val tmpMediaWaveform: ByteArray? = cursor.getBlob(cursorIndexOfMediaWaveform)
        val tmpMediaName: String? = cursor.getString(cursorIndexOfMediaName)
        val tmpMediaMimeType: String? = cursor.getString(cursorIndexOfMediaMimeType)
        val tmpMediaSize: Long? =
            if (cursor.isNull(cursorIndexOfMediaSize)) {
                null
            } else {
                cursor.getLong(cursorIndexOfMediaSize)
            }
        val tmpMediaWidth: Int? =
            if (cursor.isNull(cursorIndexOfMediaWidth)) {
                null
            } else {
                cursor.getInt(cursorIndexOfMediaWidth)
            }
        val tmpMediaHeight: Int? =
            if (cursor.isNull(cursorIndexOfMediaHeight)) {
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
        val tmpSnapshotMemo: String? = cursor.getString(cursorIndexOfSnapshotMemo)
        val tmpSnapshotType: String? = cursor.getString(cursorIndexOfSnapshotType)
        val tmpSnapshotAmount: String? = cursor.getString(cursorIndexOfSnapshotAmount)
        val tmpAssetSymbol: String? = cursor.getString(cursorIndexOfAssetSymbol)
        val tmpAssetId: String? = cursor.getString(cursorIndexOfAssetId)
        val tmpAssetIcon: String? = cursor.getString(cursorIndexOfAssetIcon)
        val tmpAssetCollectionHash: String? = cursor.getString(cursorIndexOfAssetCollectionHash)
        val tmpAssetUrl: String? = cursor.getString(cursorIndexOfAssetUrl)
        val tmpAssetWidth: Int? =
            if (cursor.isNull(cursorIndexOfAssetWidth)) {
                null
            } else {
                cursor.getInt(cursorIndexOfAssetWidth)
            }
        val tmpAssetHeight: Int? =
            if (cursor.isNull(cursorIndexOfAssetHeight)) {
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
        val tmp: Int? =
            if (cursor.isNull(cursorIndexOfSharedUserIsVerified)) {
                null
            } else {
                cursor.getInt(cursorIndexOfSharedUserIsVerified)
            }
        tmpSharedUserIsVerified = if (tmp == null) null else tmp != 0
        val tmpSharedUserAppId: String? = cursor.getString(cursorIndexOfSharedUserAppId)
        val tmpSharedMembership: String? = cursor.getString(cursorIndexOfSharedMembership)
        val tmpGroupName: String? = cursor.getString(cursorIndexOfGroupName)
        val tmpMentions: String? = cursor.getString(cursorIndexOfMentions)
        val tmp1 =
            if (cursor.isNull(cursorIndexOfMentionRead)) {
                null
            } else {
                cursor.getInt(cursorIndexOfMentionRead)
            }
        val tmpMentionRead = if (tmp1 == null) null else tmp1 != 0
        val tmp2: Int? =
            if (cursor.isNull(cursorIndexOfPinTop)) {
                null
            } else {
                cursor.getInt(cursorIndexOfPinTop)
            }
        val tmpPinTop = if (tmp2 == null) null else tmp2 != 0
        val tempExpireIn: Long? =
            if (cursor.isNull(cursorIndexOfExpireIn)) {
                null
            } else {
                cursor.getLong(cursorIndexOfExpireIn)
            }
        val tempExpireAt: Long? =
            if (cursor.isNull(cursorIndexOfExpireAt)) {
                null
            } else {
                cursor.getLong(cursorIndexOfExpireAt)
            }
        val tempCaption: String? =
            if (cursor.isNull(cursorIndexOfCaption)) {
                null
            } else {
                cursor.getString(cursorIndexOfCaption)
            }
        val tempMembership: String? =
            if (cursor.isNull(cursorIndexOfMembership)) {
                null
            } else {
                cursor.getString(cursorIndexOfMembership)
            }
        item =
            MessageItem(
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
                tmpSnapshotMemo,
                tmpSnapshotAmount,
                tmpAssetId,
                tmpAssetType,
                tmpAssetSymbol,
                tmpAssetIcon,
                tmpAssetCollectionHash,
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
                membershipConverter.revertData(tmpSharedMembership),
                tmpMediaWaveform,
                tmpQuoteId,
                tmpQuoteContent,
                tmpGroupName,
                tmpMentions,
                tmpMentionRead,
                tmpPinTop,
                tempExpireIn,
                tempExpireAt,
                tempCaption,
                membershipConverter.revertData(tempMembership)
            )
        res.add(item)
    }
    return res
}

@SuppressLint("RestrictedApi")
fun callableUser(
    db: MixinDatabase,
    statement: RoomSQLiteQuery,
    cancellationSignal: CancellationSignal,
): Callable<List<User>> {
    return Callable<List<User>> {
        val cursor = query(db, statement, false, cancellationSignal)
        try {
            val cursorIndexOfUserId =
                getColumnIndexOrThrow(cursor, "user_id")
            val cursorIndexOfIdentityNumber =
                getColumnIndexOrThrow(cursor, "identity_number")
            val cursorIndexOfRelationship =
                getColumnIndexOrThrow(cursor, "relationship")
            val cursorIndexOfBiography =
                getColumnIndexOrThrow(cursor, "biography")
            val cursorIndexOfFullName =
                getColumnIndexOrThrow(cursor, "full_name")
            val cursorIndexOfAvatarUrl =
                getColumnIndexOrThrow(cursor, "avatar_url")
            val cursorIndexOfPhone = getColumnIndexOrThrow(cursor, "phone")
            val cursorIndexOfIsVerified =
                getColumnIndexOrThrow(cursor, "is_verified")
            val cursorIndexOfCreatedAt =
                getColumnIndexOrThrow(cursor, "created_at")
            val cursorIndexOfMuteUntil =
                getColumnIndexOrThrow(cursor, "mute_until")
            val cursorIndexOfHasPin =
                getColumnIndexOrThrow(cursor, "has_pin")
            val cursorIndexOfAppId =
                getColumnIndexOrThrow(cursor, "app_id")
            val cursorIndexOfIsScam =
                getColumnIndexOrThrow(cursor, "is_scam")
            val cursorIndexOfIsMembership =
                getColumnIndexOrThrow(cursor, "membership")
            val result: MutableList<User> = java.util.ArrayList(cursor.count)
            while (cursor.moveToNext()) {
                val item: User
                val tmpUserId: String? =
                    if (cursor.isNull(cursorIndexOfUserId)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfUserId)
                    }
                val tmpIdentityNumber: String? =
                    if (cursor.isNull(cursorIndexOfIdentityNumber)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfIdentityNumber)
                    }
                val tmpRelationship: String? =
                    if (cursor.isNull(cursorIndexOfRelationship)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfRelationship)
                    }
                val tmpBiography: String? =
                    if (cursor.isNull(cursorIndexOfBiography)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfBiography)
                    }
                val tmpFullName: String? =
                    if (cursor.isNull(cursorIndexOfFullName)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfFullName)
                    }
                val tmpAvatarUrl: String? =
                    if (cursor.isNull(cursorIndexOfAvatarUrl)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfAvatarUrl)
                    }
                val tmpPhone: String? =
                    if (cursor.isNull(cursorIndexOfPhone)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfPhone)
                    }
                val tmpIsVerified: Boolean?
                val tmp: Int? =
                    if (cursor.isNull(cursorIndexOfIsVerified)) {
                        null
                    } else {
                        cursor.getInt(cursorIndexOfIsVerified)
                    }
                tmpIsVerified = if (tmp == null) null else tmp != 0
                val tmpCreatedAt: String? =
                    if (cursor.isNull(cursorIndexOfCreatedAt)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfCreatedAt)
                    }
                val tmpMuteUntil: String? =
                    if (cursor.isNull(cursorIndexOfMuteUntil)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfMuteUntil)
                    }
                val tmpHasPin: Boolean?
                val tmp1: Int? =
                    if (cursor.isNull(cursorIndexOfHasPin)) {
                        null
                    } else {
                        cursor.getInt(cursorIndexOfHasPin)
                    }
                tmpHasPin = if (tmp1 == null) null else tmp1 != 0
                val tmpAppId: String? =
                    if (cursor.isNull(cursorIndexOfAppId)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfAppId)
                    }
                val tmpIsScam: Boolean?
                val tmp2: Int? =
                    if (cursor.isNull(cursorIndexOfIsScam)) {
                        null
                    } else {
                        cursor.getInt(cursorIndexOfIsScam)
                    }
                val tmpMembership: String? =
                    if (cursor.isNull(cursorIndexOfIsMembership)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfIsMembership)
                    }
                tmpIsScam = if (tmp2 == null) null else tmp2 != 0
                item =
                    User(
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
                        tmpIsScam,
                        membership = membershipConverter.revertData(tmpMembership)
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
fun callableBot(
    db: MixinDatabase,
    statement: RoomSQLiteQuery,
    cancellationSignal: CancellationSignal,
): Callable<List<SearchBot>> {
    return Callable<List<SearchBot>> {
        val cursor = query(db, statement, false, cancellationSignal)
        try {
            val cursorIndexOfUserId =
                getColumnIndexOrThrow(cursor, "user_id")
            val cursorIndexOfIdentityNumber =
                getColumnIndexOrThrow(cursor, "identity_number")
            val cursorIndexOfRelationship =
                getColumnIndexOrThrow(cursor, "relationship")
            val cursorIndexOfBiography =
                getColumnIndexOrThrow(cursor, "biography")
            val cursorIndexOfFullName =
                getColumnIndexOrThrow(cursor, "full_name")
            val cursorIndexOfAvatarUrl =
                getColumnIndexOrThrow(cursor, "avatar_url")
            val cursorIndexOfPhone = getColumnIndexOrThrow(cursor, "phone")
            val cursorIndexOfIsVerified =
                getColumnIndexOrThrow(cursor, "is_verified")
            val cursorIndexOfCreatedAt =
                getColumnIndexOrThrow(cursor, "created_at")
            val cursorIndexOfMuteUntil =
                getColumnIndexOrThrow(cursor, "mute_until")
            val cursorIndexOfHasPin =
                getColumnIndexOrThrow(cursor, "has_pin")
            val cursorIndexOfAppId =
                getColumnIndexOrThrow(cursor, "app_id")
            val cursorIndexOfIsScam =
                getColumnIndexOrThrow(cursor, "is_scam")
            val cursorIndexOfIsMembership =
                getColumnIndexOrThrow(cursor, "membership")
            val result: MutableList<SearchBot> = java.util.ArrayList(cursor.count)
            while (cursor.moveToNext()) {
                val item: SearchBot
                val tmpUserId: String? =
                    if (cursor.isNull(cursorIndexOfUserId)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfUserId)
                    }
                val tmpIdentityNumber: String? =
                    if (cursor.isNull(cursorIndexOfIdentityNumber)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfIdentityNumber)
                    }
                val tmpRelationship: String? =
                    if (cursor.isNull(cursorIndexOfRelationship)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfRelationship)
                    }
                val tmpBiography: String? =
                    if (cursor.isNull(cursorIndexOfBiography)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfBiography)
                    }
                val tmpFullName: String? =
                    if (cursor.isNull(cursorIndexOfFullName)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfFullName)
                    }
                val tmpAvatarUrl: String? =
                    if (cursor.isNull(cursorIndexOfAvatarUrl)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfAvatarUrl)
                    }
                val tmpPhone: String? =
                    if (cursor.isNull(cursorIndexOfPhone)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfPhone)
                    }
                val tmpIsVerified: Boolean?
                val tmp: Int? =
                    if (cursor.isNull(cursorIndexOfIsVerified)) {
                        null
                    } else {
                        cursor.getInt(cursorIndexOfIsVerified)
                    }
                tmpIsVerified = if (tmp == null) null else tmp != 0
                val tmpCreatedAt: String? =
                    if (cursor.isNull(cursorIndexOfCreatedAt)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfCreatedAt)
                    }
                val tmpMuteUntil: String? =
                    if (cursor.isNull(cursorIndexOfMuteUntil)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfMuteUntil)
                    }
                val tmpHasPin: Boolean?
                val tmp1: Int? =
                    if (cursor.isNull(cursorIndexOfHasPin)) {
                        null
                    } else {
                        cursor.getInt(cursorIndexOfHasPin)
                    }
                tmpHasPin = if (tmp1 == null) null else tmp1 != 0
                val tmpAppId: String? =
                    if (cursor.isNull(cursorIndexOfAppId)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfAppId)
                    }
                val tmpIsScam: Boolean?
                val tmp2: Int? =
                    if (cursor.isNull(cursorIndexOfIsScam)) {
                        null
                    } else {
                        cursor.getInt(cursorIndexOfIsScam)
                    }
                val tmpMembership: String? =
                    if (cursor.isNull(cursorIndexOfIsMembership)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfIsMembership)
                    }
                tmpIsScam = if (tmp2 == null) null else tmp2 != 0
                item =
                    SearchBot(
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
                        tmpIsScam,
                        membership = membershipConverter.revertData(tmpMembership)
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

private val depositEntryListConverter by lazy {
    DepositEntryListConverter()
}

private val membershipConverter by lazy {
    MembershipConverter()
}

@SuppressLint("RestrictedApi")
fun callableTokenItem(
    db: MixinDatabase,
    statement: RoomSQLiteQuery,
    cancellationSignal: CancellationSignal,
): Callable<List<TokenItem>> {
    return Callable<List<TokenItem>> {
        val cursor = query(db, statement, false, cancellationSignal)
        try {
            val cursorIndexOfAssetId = 0
            val cursorIndexOfSymbol = 1
            val cursorIndexOfName = 2
            val cursorIndexOfIconUrl = 3
            val cursorIndexOfBalance = 4
            val cursorIndexOfPriceBtc = 5
            val cursorIndexOfPriceUsd = 6
            val cursorIndexOfChainId = 7
            val cursorIndexOfChangeUsd = 8
            val cursorIndexOfChangeBtc = 9
            val cursorIndexOfHidden = 10
            val cursorIndexOfConfirmations = 11
            val cursorIndexOfChainIconUrl = 12
            val cursorIndexOfChainSymbol = 13
            val cursorIndexOfChainName = 14
            val cursorIndexOfChainPriceUsd = 15
            val cursorIndexOfAssetKey = 16
            val cursorIndexOfDust = 17
            val cursorIndexOfWithdrawalMemoPossibility = 18
            val cursorIndexOfCollectionHash = 19

            val result: MutableList<TokenItem> = java.util.ArrayList(cursor.count)
            while (cursor.moveToNext()) {
                val item: TokenItem
                val tmpAssetId: String? =
                    if (cursor.isNull(cursorIndexOfAssetId)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfAssetId)
                    }
                val tmpSymbol: String? =
                    if (cursor.isNull(cursorIndexOfSymbol)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfSymbol)
                    }
                val tmpName: String? =
                    if (cursor.isNull(cursorIndexOfName)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfName)
                    }
                val tmpIconUrl: String? =
                    if (cursor.isNull(cursorIndexOfIconUrl)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfIconUrl)
                    }
                val tmpBalance: String? =
                    if (cursor.isNull(cursorIndexOfBalance)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfBalance)
                    }
                val tmpPriceBtc: String? =
                    if (cursor.isNull(cursorIndexOfPriceBtc)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfPriceBtc)
                    }
                val tmpPriceUsd: String? =
                    if (cursor.isNull(cursorIndexOfPriceUsd)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfPriceUsd)
                    }
                val tmpChainId: String? =
                    if (cursor.isNull(cursorIndexOfChainId)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfChainId)
                    }
                val tmpChangeUsd: String? =
                    if (cursor.isNull(cursorIndexOfChangeUsd)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfChangeUsd)
                    }
                val tmpChangeBtc: String? =
                    if (cursor.isNull(cursorIndexOfChangeBtc)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfChangeBtc)
                    }
                val tmpHidden: Boolean?
                val tmp: Int? =
                    if (cursor.isNull(cursorIndexOfHidden)) {
                        null
                    } else {
                        cursor.getInt(cursorIndexOfHidden)
                    }
                tmpHidden = if (tmp == null) null else tmp != 0
                val tmpChainPriceUsd: String? =
                    if (cursor.isNull(cursorIndexOfChainPriceUsd)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfChainPriceUsd)
                    }
                val tmpConfirmations: Int = cursor.getInt(cursorIndexOfConfirmations)
                val tmpChainIconUrl: String? =
                    if (cursor.isNull(cursorIndexOfChainIconUrl)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfChainIconUrl)
                    }
                val tmpChainSymbol: String? =
                    if (cursor.isNull(cursorIndexOfChainSymbol)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfChainSymbol)
                    }
                val tmpChainName: String? =
                    if (cursor.isNull(cursorIndexOfChainName)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfChainName)
                    }
                val tmpAssetKey: String? =
                    if (cursor.isNull(cursorIndexOfAssetKey)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfAssetKey)
                    }
                val tmpDust: String? =
                    if (cursor.isNull(cursorIndexOfDust)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfDust)
                    }
                val tmpDepositWithdrawalMemoPossibility: WithdrawalMemoPossibility? =
                    if (cursor.isNull(cursorIndexOfWithdrawalMemoPossibility)) {
                        null
                    } else {
                        WithdrawalMemoPossibilityConverter().revertDate(cursor.getString(cursorIndexOfWithdrawalMemoPossibility))
                    }
                val tmpCollectionHash: String? =
                    if (cursor.isNull(cursorIndexOfCollectionHash)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfCollectionHash)
                    }

                item =
                    TokenItem(
                        tmpAssetId!!,
                        tmpSymbol!!,
                        tmpName!!,
                        tmpIconUrl!!,
                        tmpBalance!!,
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
                        tmpDust,
                        tmpDepositWithdrawalMemoPossibility,
                        tmpCollectionHash,
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
    cancellationSignal: CancellationSignal,
): Callable<List<SearchMessageItem>> {
    return Callable<List<SearchMessageItem>> {
        val cursor = query(db, statement, false, cancellationSignal)
        try {
            val cursorIndexOfConversationId = 0
            val cursorIndexOfConversationAvatarUrl = 1
            val cursorIndexOfConversationName = 2
            val cursorIndexOfConversationCategory = 3
            val cursorIndexOfMessageCount = 4
            val cursorIndexOfUserId = 5
            val cursorIndexOfAppId = 6
            val cursorIndexOfUserAvatarUrl = 7
            val cursorIndexOfUserFullName = 8
            val cursorIndexOfUserIsVerified = 9
            val cursorIndexOfUserMembership = 10
            val result: MutableList<SearchMessageItem> =
                java.util.ArrayList(cursor.count)
            while (cursor.moveToNext()) {
                val item: SearchMessageItem
                val tmpConversationId: String? =
                    if (cursor.isNull(cursorIndexOfConversationId)) {
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
                val tmpUserId: String? =
                    if (cursor.isNull(cursorIndexOfUserId)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfUserId)
                    }
                val tempAppId: String? =
                    if (cursor.isNull(cursorIndexOfAppId)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfAppId)
                    }
                val tmpUserAvatarUrl: String? =
                    if (cursor.isNull(cursorIndexOfUserAvatarUrl)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfUserAvatarUrl)
                    }
                val tmpUserFullName: String? =
                    if (cursor.isNull(cursorIndexOfUserFullName)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfUserFullName)
                    }
                val tmpIsVerified: Boolean?
                val tmp: Int? =
                    if (cursor.isNull(cursorIndexOfUserIsVerified)) {
                        null
                    } else {
                        cursor.getInt(cursorIndexOfUserIsVerified)
                    }
                tmpIsVerified = if (tmp == null) null else tmp != 0
                val tmpUserMembership: String? =
                    if (cursor.isNull(cursorIndexOfUserMembership)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfUserMembership)
                    }
                item =
                    SearchMessageItem(
                        tmpConversationId!!,
                        tmpConversationCategory,
                        tmpConversationName,
                        tmpMessageCount,
                        tmpUserId!!,
                        tempAppId,
                        tmpUserFullName,
                        tmpUserAvatarUrl,
                        tmpConversationAvatarUrl,
                        tmpIsVerified,
                        membershipConverter.revertData(tmpUserMembership)
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
    cancellationSignal: CancellationSignal,
): Callable<List<ChatMinimal>> {
    return Callable<List<ChatMinimal>> {
        val cursor = query(db, statement, false, cancellationSignal)
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
            val cursorIndexOfMembership = 13
            val result: MutableList<ChatMinimal> = java.util.ArrayList(cursor.count)
            while (cursor.moveToNext()) {
                val item: ChatMinimal
                val tmpConversationId: String? =
                    if (cursor.isNull(cursorIndexOfConversationId)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfConversationId)
                    }
                val tmpGroupIconUrl: String? =
                    if (cursor.isNull(cursorIndexOfGroupIconUrl)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfGroupIconUrl)
                    }
                val tmpCategory: String? =
                    if (cursor.isNull(cursorIndexOfCategory)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfCategory)
                    }
                val tmpGroupName: String? =
                    if (cursor.isNull(cursorIndexOfGroupName)) {
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
                val tmpUserId: String? =
                    if (cursor.isNull(cursorIndexOfUserId)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfUserId)
                    }
                val tmpFullName: String? =
                    if (cursor.isNull(cursorIndexOfFullName)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfFullName)
                    }
                val tmpAvatarUrl: String? =
                    if (cursor.isNull(cursorIndexOfAvatarUrl)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfAvatarUrl)
                    }
                val tmpIsVerified: Boolean?
                val tmp: Int? =
                    if (cursor.isNull(cursorIndexOfIsVerified)) {
                        null
                    } else {
                        cursor.getInt(cursorIndexOfIsVerified)
                    }
                tmpIsVerified = if (tmp == null) null else tmp != 0
                val tmpAppId: String? =
                    if (cursor.isNull(cursorIndexOfAppId)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfAppId)
                    }
                val tmpOwnerMuteUntil: String? =
                    if (cursor.isNull(cursorIndexOfOwnerMuteUntil)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfOwnerMuteUntil)
                    }
                val tmpMuteUntil: String? =
                    if (cursor.isNull(cursorIndexOfMuteUntil)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfMuteUntil)
                    }
                val tmpPinTime: String? =
                    if (cursor.isNull(cursorIndexOfPinTime)) {
                        null
                    } else {
                        cursor.getString(cursorIndexOfPinTime)
                    }
                val tmpMembership: String? =
                    if (cursor.isNull(cursorIndexOfMembership)) {
                        null
                    } else {

                        cursor.getString(cursorIndexOfMembership)
                    }
                item =
                    ChatMinimal(
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
                        tmpPinTime,
                        membershipConverter.revertData(tmpMembership)
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
fun convertChatHistoryMessageItem(
    cursor: Cursor?,
): List<ChatHistoryMessageItem> {
    cursor ?: return emptyList()
    val cursorIndexOfMessageId = 0
    val cursorIndexOfConversationId = 1
    val cursorIndexOfUserId = 2
    val cursorIndexOfUserFullName = 3
    val cursorIndexOfUserIdentityNumber = 4
    val cursorIndexOfAppId = 5
    val cursorIndexOfType = 6
    val cursorIndexOfContent = 7
    val cursorIndexOfCreatedAt = 8
    val cursorIndexOfMediaStatus = 10
    val cursorIndexOfMediaWaveform = 11
    val cursorIndexOfMediaName = 12
    val cursorIndexOfMediaMimeType = 13
    val cursorIndexOfMediaSize = 14
    val cursorIndexOfMediaWidth = 15
    val cursorIndexOfMediaHeight = 16
    val cursorIndexOfThumbImage = 17
    val cursorIndexOfThumbUrl = 18
    val cursorIndexOfMediaUrl = 19
    val cursorIndexOfMediaDuration = 20
    val cursorIndexOfQuoteId = 21
    val cursorIndexOfQuoteContent = 22
    val cursorIndexOfAssetUrl = 33
    val cursorIndexOfAssetWidth = 34
    val cursorIndexOfAssetHeight = 35
    val cursorIndexOfAssetType = 38
    val cursorIndexOfSharedUserId = 43
    val cursorIndexOfSharedUserFullName = 44
    val cursorIndexOfSharedUserIdentityNumber = 45
    val cursorIndexOfSharedUserAvatarUrl = 46
    val cursorIndexOfSharedUserIsVerified = 47
    val cursorIndexOfSharedUserAppId = 48
    val cursorIndexOfMentions = 49
    val cursorIndexOfSharedUserMembership = 50
    val cursorIndexOfMembership = 51
    val list: MutableList<ChatHistoryMessageItem> =
        ArrayList(
            cursor.count,
        )
    while (cursor.moveToNext()) {
        val item: ChatHistoryMessageItem
        val tmpMessageId: String? =
            if (cursor.isNull(cursorIndexOfMessageId)) {
                null
            } else {
                cursor.getString(cursorIndexOfMessageId)
            }
        val tmpConversationId: String? =
            if (cursor.isNull(cursorIndexOfConversationId)) {
                null
            } else {
                cursor.getString(cursorIndexOfConversationId)
            }
        val tmpUserId: String? =
            if (cursor.isNull(cursorIndexOfUserId)) {
                null
            } else {
                cursor.getString(cursorIndexOfUserId)
            }
        val tmpUserFullName: String? =
            if (cursor.isNull(cursorIndexOfUserFullName)) {
                null
            } else {
                cursor.getString(cursorIndexOfUserFullName)
            }
        val tmpUserIdentityNumber: String? =
            if (cursor.isNull(cursorIndexOfUserIdentityNumber)) {
                null
            } else {
                cursor.getString(cursorIndexOfUserIdentityNumber)
            }
        val tmpAppId: String? =
            if (cursor.isNull(cursorIndexOfAppId)) {
                null
            } else {
                cursor.getString(cursorIndexOfAppId)
            }
        val tmpType: String? =
            if (cursor.isNull(cursorIndexOfType)) {
                null
            } else {
                cursor.getString(cursorIndexOfType)
            }
        val tmpContent: String? =
            if (cursor.isNull(cursorIndexOfContent)) {
                null
            } else {
                cursor.getString(cursorIndexOfContent)
            }
        val tmpCreatedAt: String? =
            if (cursor.isNull(cursorIndexOfCreatedAt)) {
                null
            } else {
                cursor.getString(cursorIndexOfCreatedAt)
            }
        val tmpMediaStatus: String? =
            if (cursor.isNull(cursorIndexOfMediaStatus)) {
                null
            } else {
                cursor.getString(cursorIndexOfMediaStatus)
            }
        val tmpMediaWaveform: ByteArray? =
            if (cursor.isNull(cursorIndexOfMediaWaveform)) {
                null
            } else {
                cursor.getBlob(cursorIndexOfMediaWaveform)
            }
        val tmpMediaName: String? =
            if (cursor.isNull(cursorIndexOfMediaName)) {
                null
            } else {
                cursor.getString(cursorIndexOfMediaName)
            }
        val tmpMediaMimeType: String? =
            if (cursor.isNull(cursorIndexOfMediaMimeType)) {
                null
            } else {
                cursor.getString(cursorIndexOfMediaMimeType)
            }
        val tmpMediaSize: Long? =
            if (cursor.isNull(cursorIndexOfMediaSize)) {
                null
            } else {
                cursor.getLong(cursorIndexOfMediaSize)
            }
        val tmpMediaWidth: Int? =
            if (cursor.isNull(cursorIndexOfMediaWidth)) {
                null
            } else {
                cursor.getInt(cursorIndexOfMediaWidth)
            }
        val tmpMediaHeight: Int? =
            if (cursor.isNull(cursorIndexOfMediaHeight)) {
                null
            } else {
                cursor.getInt(cursorIndexOfMediaHeight)
            }
        val tmpThumbImage: String? =
            if (cursor.isNull(cursorIndexOfThumbImage)) {
                null
            } else {
                cursor.getString(cursorIndexOfThumbImage)
            }
        val tmpThumbUrl: String? =
            if (cursor.isNull(cursorIndexOfThumbUrl)) {
                null
            } else {
                cursor.getString(cursorIndexOfThumbUrl)
            }
        val tmpMediaUrl: String? =
            if (cursor.isNull(cursorIndexOfMediaUrl)) {
                null
            } else {
                cursor.getString(cursorIndexOfMediaUrl)
            }
        val tmpMediaDuration: String? =
            if (cursor.isNull(cursorIndexOfMediaDuration)) {
                null
            } else {
                cursor.getString(cursorIndexOfMediaDuration)
            }
        val tmpQuoteId: String? =
            if (cursor.isNull(cursorIndexOfQuoteId)) {
                null
            } else {
                cursor.getString(cursorIndexOfQuoteId)
            }
        val tmpQuoteContent: String? =
            if (cursor.isNull(cursorIndexOfQuoteContent)) {
                null
            } else {
                cursor.getString(cursorIndexOfQuoteContent)
            }
        val tmpAssetUrl: String? =
            if (cursor.isNull(cursorIndexOfAssetUrl)) {
                null
            } else {
                cursor.getString(cursorIndexOfAssetUrl)
            }
        val tmpAssetWidth: Int? =
            if (cursor.isNull(cursorIndexOfAssetWidth)) {
                null
            } else {
                cursor.getInt(cursorIndexOfAssetWidth)
            }
        val tmpAssetHeight: Int? =
            if (cursor.isNull(cursorIndexOfAssetHeight)) {
                null
            } else {
                cursor.getInt(cursorIndexOfAssetHeight)
            }
        val tmpAssetType: String? =
            if (cursor.isNull(cursorIndexOfAssetType)) {
                null
            } else {
                cursor.getString(cursorIndexOfAssetType)
            }
        val tmpSharedUserId: String? =
            if (cursor.isNull(cursorIndexOfSharedUserId)) {
                null
            } else {
                cursor.getString(cursorIndexOfSharedUserId)
            }
        val tmpSharedUserFullName: String? =
            if (cursor.isNull(cursorIndexOfSharedUserFullName)) {
                null
            } else {
                cursor.getString(cursorIndexOfSharedUserFullName)
            }
        val tmpSharedUserIdentityNumber: String? =
            if (cursor.isNull(cursorIndexOfSharedUserIdentityNumber)) {
                null
            } else {
                cursor.getString(cursorIndexOfSharedUserIdentityNumber)
            }
        val tmpSharedUserAvatarUrl: String? =
            if (cursor.isNull(cursorIndexOfSharedUserAvatarUrl)) {
                null
            } else {
                cursor.getString(cursorIndexOfSharedUserAvatarUrl)
            }
        val tmpSharedUserIsVerified: Boolean?
        val tmp: Int? =
            if (cursor.isNull(cursorIndexOfSharedUserIsVerified)) {
                null
            } else {
                cursor.getInt(cursorIndexOfSharedUserIsVerified)
            }
        tmpSharedUserIsVerified = if (tmp == null) null else tmp != 0
        val tmpSharedUserAppId: String? =
            if (cursor.isNull(cursorIndexOfSharedUserAppId)) {
                null
            } else {
                cursor.getString(cursorIndexOfSharedUserAppId)
            }

        val tmpSharedUserMembership: String? =
            if (cursor.isNull(cursorIndexOfSharedUserMembership)) {
                null
            } else {
                cursor.getString(cursorIndexOfSharedUserMembership)
            }
        val tmpMentions: String? =
            if (cursor.isNull(cursorIndexOfMentions)) {
                null
            } else {
                cursor.getString(cursorIndexOfMentions)
            }
        val tmpMembership: String? =
            if (cursor.isNull(cursorIndexOfMembership)) {
                null
            } else {
                cursor.getString(cursorIndexOfMembership)
            }
        item =
            ChatHistoryMessageItem(
                null,
                tmpConversationId,
                tmpMessageId!!,
                tmpUserId,
                tmpUserFullName,
                tmpUserIdentityNumber,
                tmpType!!,
                tmpAppId,
                tmpContent,
                tmpCreatedAt!!,
                tmpMediaStatus,
                tmpMediaName,
                tmpMediaMimeType,
                tmpMediaSize,
                tmpThumbUrl,
                tmpMediaWidth,
                tmpMediaHeight,
                tmpThumbImage,
                tmpMediaUrl,
                tmpMediaDuration,
                tmpMediaWaveform,
                tmpAssetWidth,
                tmpAssetHeight,
                tmpAssetUrl,
                tmpAssetType,
                tmpSharedUserId,
                tmpSharedUserFullName,
                tmpSharedUserAvatarUrl,
                tmpSharedUserIdentityNumber,
                tmpSharedUserIsVerified,
                tmpSharedUserAppId,
                membershipConverter.revertData(tmpSharedUserMembership),
                tmpQuoteId,
                tmpQuoteContent,
                tmpMentions,
                membershipConverter.revertData(tmpMembership)
            )
        list.add(item)
    }
    return list
}

@SuppressLint("RestrictedApi")
fun callableSafeInscription(
    db: MixinDatabase,
    statement: RoomSQLiteQuery,
    cancellationSignal: CancellationSignal,
): Callable<List<SafeCollectible>> {
    return Callable<List<SafeCollectible>> {
        val cursor = query(db, statement, false, cancellationSignal)
        try {
            val cursorIndexOfCollectionHash = 0
            val cursorIndexOfInscriptionHash = 1
            val cursorIndexOfSequence = 2
            val cursorIndexOfContentType = 3
            val cursorIndexOfContentURL = 4
            val cursorIndexOfName = 6
            val cursorIndexOfIconURL = 7
            val result: MutableList<SafeCollectible> = ArrayList(cursor.count)
            while (cursor.moveToNext()) {
                val item: SafeCollectible
                val tmpCollectionHash: String = cursor.getString(cursorIndexOfCollectionHash)
                val tmpInscriptionHash: String = cursor.getString(cursorIndexOfInscriptionHash)
                val tmpSequence: Long = cursor.getLong(cursorIndexOfSequence)
                val tmpContentType: String = cursor.getString(cursorIndexOfContentType)
                val tmpContentURL: String = cursor.getString(cursorIndexOfContentURL)
                val tmpName: String = cursor.getString(cursorIndexOfName)
                val tmpIconURL: String = cursor.getString(cursorIndexOfIconURL)
                item = SafeCollectible(tmpCollectionHash, tmpInscriptionHash, tmpSequence, tmpName, tmpContentType, tmpContentURL, tmpIconURL)
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
fun callableMarket(
    db: MixinDatabase,
    statement: RoomSQLiteQuery,
    cancellationSignal: CancellationSignal
): Callable<List<Market>> {
    return Callable<List<Market>> {
        val cursor = query(db, statement, false, cancellationSignal)
        try {
            val result: MutableList<Market> = ArrayList(cursor.count)
            while (cursor.moveToNext()) {
                val item = Market(
                    coinId = cursor.getString(cursor.getColumnIndexOrThrow("coin_id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    symbol = cursor.getString(cursor.getColumnIndexOrThrow("symbol")),
                    iconUrl = cursor.getString(cursor.getColumnIndexOrThrow("icon_url")),
                    currentPrice = cursor.getString(cursor.getColumnIndexOrThrow("current_price")),
                    marketCap = cursor.getString(cursor.getColumnIndexOrThrow("market_cap")),
                    marketCapRank = cursor.getString(cursor.getColumnIndexOrThrow("market_cap_rank")),
                    totalVolume = cursor.getString(cursor.getColumnIndexOrThrow("total_volume")),
                    high24h = cursor.getString(cursor.getColumnIndexOrThrow("high_24h")),
                    low24h = cursor.getString(cursor.getColumnIndexOrThrow("low_24h")),
                    priceChange24h = cursor.getString(cursor.getColumnIndexOrThrow("price_change_24h")),
                    priceChangePercentage1H = cursor.getString(cursor.getColumnIndexOrThrow("price_change_percentage_1h")),
                    priceChangePercentage24H = cursor.getString(cursor.getColumnIndexOrThrow("price_change_percentage_24h")),
                    priceChangePercentage7D = cursor.getString(cursor.getColumnIndexOrThrow("price_change_percentage_7d")),
                    priceChangePercentage30D = cursor.getString(cursor.getColumnIndexOrThrow("price_change_percentage_30d")),
                    marketCapChange24h = cursor.getString(cursor.getColumnIndexOrThrow("market_cap_change_24h")),
                    marketCapChangePercentage24h = cursor.getString(cursor.getColumnIndexOrThrow("market_cap_change_percentage_24h")),
                    circulatingSupply = cursor.getString(cursor.getColumnIndexOrThrow("circulating_supply")),
                    totalSupply = cursor.getString(cursor.getColumnIndexOrThrow("total_supply")),
                    maxSupply = cursor.getString(cursor.getColumnIndexOrThrow("max_supply")),
                    ath = cursor.getString(cursor.getColumnIndexOrThrow("ath")),
                    athChangePercentage = cursor.getString(cursor.getColumnIndexOrThrow("ath_change_percentage")),
                    athDate = cursor.getString(cursor.getColumnIndexOrThrow("ath_date")),
                    atl = cursor.getString(cursor.getColumnIndexOrThrow("atl")),
                    atlChangePercentage = cursor.getString(cursor.getColumnIndexOrThrow("atl_change_percentage")),
                    atlDate = cursor.getString(cursor.getColumnIndexOrThrow("atl_date")),
                    assetIds = OptionalListConverter.fromString(cursor.getString(cursor.getColumnIndexOrThrow("asset_ids"))),
                    sparklineIn7d = cursor.getString(cursor.getColumnIndexOrThrow("sparkline_in_7d")),
                    updatedAt = cursor.getString(cursor.getColumnIndexOrThrow("updated_at"))
                )
                result.add(item)
            }
            result
        } finally {
            cursor.close()
            statement.release()
        }
    }
}