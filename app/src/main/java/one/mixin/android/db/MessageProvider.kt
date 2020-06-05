package one.mixin.android.db

import android.annotation.SuppressLint
import android.database.Cursor
import androidx.paging.DataSource
import androidx.room.RoomSQLiteQuery
import androidx.room.util.CursorUtil
import one.mixin.android.util.chat.MixinLimitOffsetDataSource
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.SearchMessageDetailItem

@SuppressLint("RestrictedApi")
class MessageProvider {
    companion object {
        fun getMessages(conversationId: String, database: MixinDatabase) =
            object : DataSource.Factory<Int, MessageItem>() {
                override fun create(): DataSource<Int, MessageItem> {
                    val sql =
                        """
                        SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId,
                        u.full_name AS userFullName, u.identity_number AS userIdentityNumber, u.app_id AS appId, m.category AS type,
                        m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus, m.media_waveform AS mediaWaveform,
                        m.name AS mediaName, m.media_mime_type AS mediaMimeType, m.media_size AS mediaSize, m.media_width AS mediaWidth, m.media_height AS mediaHeight,
                        m.thumb_image AS thumbImage, m.thumb_url AS thumbUrl, m.media_url AS mediaUrl, m.media_duration AS mediaDuration, m.quote_message_id as quoteId,
                        m.quote_content as quoteContent, u1.full_name AS participantFullName, m.action AS actionName, u1.user_id AS participantUserId,
                        s.snapshot_id AS snapshotId, s.type AS snapshotType, s.amount AS snapshotAmount, a.symbol AS assetSymbol, s.asset_id AS assetId,
                        a.icon_url AS assetIcon, st.asset_url AS assetUrl, st.asset_width AS assetWidth, st.asset_height AS assetHeight, st.sticker_id AS stickerId,
                        st.name AS assetName, st.asset_type AS assetType, h.site_name AS siteName, h.site_title AS siteTitle, h.site_description AS siteDescription,
                        h.site_image AS siteImage, m.shared_user_id AS sharedUserId, su.full_name AS sharedUserFullName, su.identity_number AS sharedUserIdentityNumber,
                        su.avatar_url AS sharedUserAvatarUrl, su.is_verified AS sharedUserIsVerified, su.app_id AS sharedUserAppId, mm.mentions AS mentions, mm.has_read as mentionRead, 
                        c.name AS groupName
                        FROM messages m
                        INNER JOIN users u ON m.user_id = u.user_id
                        LEFT JOIN users u1 ON m.participant_id = u1.user_id
                        LEFT JOIN snapshots s ON m.snapshot_id = s.snapshot_id
                        LEFT JOIN assets a ON s.asset_id = a.asset_id
                        LEFT JOIN stickers st ON st.sticker_id = m.sticker_id
                        LEFT JOIN hyperlinks h ON m.hyperlink = h.hyperlink
                        LEFT JOIN users su ON m.shared_user_id = su.user_id
                        LEFT JOIN conversations c ON m.conversation_id = c.conversation_id
                        LEFT JOIN message_mentions mm ON m.id = mm.message_id
                        WHERE m.conversation_id = ? 
                        ORDER BY m.created_at DESC 
                    """
                    val statement = RoomSQLiteQuery.acquire(sql, 1)
                    val argIndex = 1
                    statement.bindString(argIndex, conversationId)

                    val countSql = "SELECT COUNT(1) FROM messages WHERE conversation_id = ?"
                    val countStatement = RoomSQLiteQuery.acquire(countSql, 1)
                    countStatement.bindString(argIndex, conversationId)
                    return object : MixinLimitOffsetDataSource<MessageItem>(database, statement, countStatement, false, "messages", "users", "snapshots", "assets", "stickers", "hyperlinks", "conversations", "message_mentions") {
                        override fun convertRows(cursor: Cursor?): MutableList<MessageItem> {
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
                            val cursorIndexOfSharedUserIdentityNumber = cursor.getColumnIndexOrThrow("sharedUserIdentityNumber")
                            val cursorIndexOfSharedUserAvatarUrl = cursor.getColumnIndexOrThrow("sharedUserAvatarUrl")
                            val cursorIndexOfSharedUserIsVerified = cursor.getColumnIndexOrThrow("sharedUserIsVerified")
                            val cursorIndexOfSharedUserAppId = cursor.getColumnIndexOrThrow("sharedUserAppId")
                            val cursorIndexOfGroupName = cursor.getColumnIndexOrThrow("groupName")
                            val cursorIndexOfMentions = cursor.getColumnIndexOrThrow("mentions")
                            val cursorIndexOfMentionRead = cursor.getColumnIndexOrThrow("mentionRead")
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
                                val tmpSharedUserIdentityNumber: String? = cursor.getString(cursorIndexOfSharedUserIdentityNumber)
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
                                val tmp_1 = if (cursor.isNull(cursorIndexOfMentionRead)) {
                                    null
                                } else {
                                    cursor.getInt(cursorIndexOfMentionRead)
                                }
                                val tmpMentionRead = if (tmp_1 == null) null else tmp_1 != 0
                                item = MessageItem(
                                    tmpMessageId, tmpConversationId, tmpUserId, tmpUserFullName, tmpUserIdentityNumber, tmpType, tmpContent,
                                    tmpCreatedAt, tmpStatus, tmpMediaStatus, null, tmpMediaName, tmpMediaMimeType, tmpMediaSize, tmpThumbUrl, tmpMediaWidth,
                                    tmpMediaHeight, tmpThumbImage, tmpMediaUrl, tmpMediaDuration, tmpParticipantFullName, tmpParticipantUserId, tmpActionName, tmpSnapshotId,
                                    tmpSnapshotType, tmpSnapshotAmount, tmpAssetId, tmpAssetType, tmpAssetSymbol, tmpAssetIcon, tmpAssetUrl, tmpAssetHeight, tmpAssetWidth,
                                    null, tmpStickerId, tmpAssetName, tmpAppId, tmpSiteName, tmpSiteTitle, tmpSiteDescription, tmpSiteImage, tmpSharedUserId,
                                    tmpSharedUserFullName, tmpSharedUserIdentityNumber, tmpSharedUserAvatarUrl, tmpSharedUserIsVerified, tmpSharedUserAppId,
                                    tmpMediaWaveform, tmpQuoteId, tmpQuoteContent, tmpGroupName, tmpMentions, tmpMentionRead
                                )
                                res.add(item)
                            }
                            return res
                        }
                    }
                }
            }

        fun getConversations(database: MixinDatabase) =
            object : DataSource.Factory<Int, ConversationItem>() {
                override fun create(): DataSource<Int, ConversationItem> {
                    val sql =
                        """
                    SELECT c.conversation_id AS conversationId, c.icon_url AS groupIconUrl, c.category AS category,
                    c.name AS groupName, c.status AS status, c.last_read_message_id AS lastReadMessageId,
                    c.unseen_message_count AS unseenMessageCount, c.owner_id AS ownerId, c.pin_time AS pinTime, c.mute_until AS muteUntil,
                    ou.avatar_url AS avatarUrl, ou.full_name AS name, ou.is_verified AS ownerVerified,
                    ou.identity_number AS ownerIdentityNumber, ou.mute_until AS ownerMuteUntil, ou.app_id AS appId,
                    m.content AS content, m.category AS contentType, m.created_at AS createdAt, m.media_url AS mediaUrl,
                    m.user_id AS senderId, m.action AS actionName, m.status AS messageStatus,
                    mu.full_name AS senderFullName, s.type AS SnapshotType,
                    pu.full_name AS participantFullName, pu.user_id AS participantUserId,
                    (SELECT count(*) FROM message_mentions me WHERE me.conversation_id = c.conversation_id AND me.has_read = 0) as mentionCount,  
                    mm.mentions AS mentions 
                    FROM conversations c
                    INNER JOIN users ou ON ou.user_id = c.owner_id
                    LEFT JOIN messages m ON c.last_message_id = m.id
                    LEFT JOIN message_mentions mm ON mm.message_id = m.id
                    LEFT JOIN users mu ON mu.user_id = m.user_id
                    LEFT JOIN snapshots s ON s.snapshot_id = m.snapshot_id
                    LEFT JOIN users pu ON pu.user_id = m.participant_id 
                    WHERE c.category IS NOT NULL 
                    ORDER BY c.pin_time DESC, 
                    CASE 
                        WHEN m.created_at is NULL THEN c.created_at
                        ELSE m.created_at 
                    END 
                    DESC
                """
                    val statement = RoomSQLiteQuery.acquire(sql, 0)
                    val countSql = "SELECT COUNT(*) FROM conversations WHERE category IS NOT NULL"
                    val countStatement = RoomSQLiteQuery.acquire(countSql, 0)
                    return object : MixinLimitOffsetDataSource<ConversationItem>(database, statement, countStatement, false, "message_mentions", "conversations", "users", "messages", "snapshots") {
                        override fun convertRows(cursor: Cursor): List<ConversationItem> {
                            val cursorIndexOfConversationId = CursorUtil.getColumnIndexOrThrow(cursor, "conversationId")
                            val cursorIndexOfGroupIconUrl = CursorUtil.getColumnIndexOrThrow(cursor, "groupIconUrl")
                            val cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(cursor, "category")
                            val cursorIndexOfGroupName = CursorUtil.getColumnIndexOrThrow(cursor, "groupName")
                            val cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(cursor, "status")
                            val cursorIndexOfLastReadMessageId = CursorUtil.getColumnIndexOrThrow(cursor, "lastReadMessageId")
                            val cursorIndexOfUnseenMessageCount = CursorUtil.getColumnIndexOrThrow(cursor, "unseenMessageCount")
                            val cursorIndexOfOwnerId = CursorUtil.getColumnIndexOrThrow(cursor, "ownerId")
                            val cursorIndexOfPinTime = CursorUtil.getColumnIndexOrThrow(cursor, "pinTime")
                            val cursorIndexOfMuteUntil = CursorUtil.getColumnIndexOrThrow(cursor, "muteUntil")
                            val cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(cursor, "avatarUrl")
                            val cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(cursor, "name")
                            val cursorIndexOfOwnerVerified = CursorUtil.getColumnIndexOrThrow(cursor, "ownerVerified")
                            val cursorIndexOfOwnerIdentityNumber = CursorUtil.getColumnIndexOrThrow(cursor, "ownerIdentityNumber")
                            val cursorIndexOfOwnerMuteUntil = CursorUtil.getColumnIndexOrThrow(cursor, "ownerMuteUntil")
                            val cursorIndexOfAppId = CursorUtil.getColumnIndexOrThrow(cursor, "appId")
                            val cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(cursor, "content")
                            val cursorIndexOfContentType = CursorUtil.getColumnIndexOrThrow(cursor, "contentType")
                            val cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(cursor, "createdAt")
                            val cursorIndexOfMediaUrl = CursorUtil.getColumnIndexOrThrow(cursor, "mediaUrl")
                            val cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(cursor, "senderId")
                            val cursorIndexOfActionName = CursorUtil.getColumnIndexOrThrow(cursor, "actionName")
                            val cursorIndexOfMessageStatus = CursorUtil.getColumnIndexOrThrow(cursor, "messageStatus")
                            val cursorIndexOfSenderFullName = CursorUtil.getColumnIndexOrThrow(cursor, "senderFullName")
                            val cursorIndexOfParticipantFullName = CursorUtil.getColumnIndexOrThrow(cursor, "participantFullName")
                            val cursorIndexOfParticipantUserId = CursorUtil.getColumnIndexOrThrow(cursor, "participantUserId")
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
                                val tmpOwnerIdentityNumber = cursor.getString(cursorIndexOfOwnerIdentityNumber)
                                val tmpOwnerMuteUntil = cursor.getString(cursorIndexOfOwnerMuteUntil)
                                val tmpAppId = cursor.getString(cursorIndexOfAppId)
                                val tmpContent = cursor.getString(cursorIndexOfContent)
                                val tmpContentType = cursor.getString(cursorIndexOfContentType)
                                val tmpCreatedAt = cursor.getString(cursorIndexOfCreatedAt)
                                val tmpMediaUrl = cursor.getString(cursorIndexOfMediaUrl)
                                val tmpSenderId = cursor.getString(cursorIndexOfSenderId)
                                val tmpActionName = cursor.getString(cursorIndexOfActionName)
                                val tmpMessageStatus = cursor.getString(cursorIndexOfMessageStatus)
                                val tmpSenderFullName = cursor.getString(cursorIndexOfSenderFullName)
                                val tmpParticipantFullName = cursor.getString(cursorIndexOfParticipantFullName)
                                val tmpParticipantUserId = cursor.getString(cursorIndexOfParticipantUserId)
                                val tmpMentionCount: Int? = if (cursor.isNull(cursorIndexOfMentionCount)) {
                                    null
                                } else {
                                    cursor.getInt(cursorIndexOfMentionCount)
                                }
                                val tmpMentions = cursor.getString(cursorIndexOfMentions)
                                item = ConversationItem(tmpConversationId, tmpAvatarUrl, tmpGroupIconUrl, tmpCategory, tmpGroupName, tmpName, tmpOwnerId, tmpOwnerIdentityNumber, tmpStatus, tmpLastReadMessageId, tmpUnseenMessageCount, tmpContent, tmpContentType, tmpMediaUrl, tmpCreatedAt, tmpPinTime, tmpSenderId, tmpSenderFullName, tmpMessageStatus, tmpActionName, tmpParticipantFullName, tmpParticipantUserId, tmpOwnerMuteUntil, tmpOwnerVerified, tmpMuteUntil, null, tmpAppId, tmpMentions, tmpMentionCount)
                                res.add(item)
                            }
                            return res
                        }
                    }
                }
            }

        fun observeConversationsByCircleId(circleId: String, database: MixinDatabase) =
            object : DataSource.Factory<Int, ConversationItem>() {
                override fun create(): DataSource<Int, ConversationItem> {
                    val sql =
                        """
                        SELECT c.conversation_id AS conversationId, c.icon_url AS groupIconUrl, c.category AS category,
                        c.name AS groupName, c.status AS status, c.last_read_message_id AS lastReadMessageId,
                        c.unseen_message_count AS unseenMessageCount, c.owner_id AS ownerId, cc.pin_time AS pinTime, c.mute_until AS muteUntil,
                        ou.avatar_url AS avatarUrl, ou.full_name AS name, ou.is_verified AS ownerVerified,
                        ou.identity_number AS ownerIdentityNumber, ou.mute_until AS ownerMuteUntil, ou.app_id AS appId,
                        m.content AS content, m.category AS contentType, m.created_at AS createdAt, m.media_url AS mediaUrl,
                        m.user_id AS senderId, m.`action` AS actionName, m.status AS messageStatus,
                        mu.full_name AS senderFullName, s.type AS SnapshotType,
                        pu.full_name AS participantFullName, pu.user_id AS participantUserId,
                        (SELECT count(*) FROM message_mentions me WHERE me.conversation_id = c.conversation_id AND me.has_read = 0) AS mentionCount,  
                        mm.mentions AS mentions 
                        FROM circle_conversations cc
                        INNER JOIN conversations c ON cc.conversation_id = c.conversation_id
                        INNER JOIN circles ci ON ci.circle_id = cc.circle_id
                        INNER JOIN users ou ON ou.user_id = c.owner_id
                        LEFT JOIN messages m ON c.last_message_id = m.id
                        LEFT JOIN message_mentions mm ON mm.message_id = m.id
                        LEFT JOIN users mu ON mu.user_id = m.user_id
                        LEFT JOIN snapshots s ON s.snapshot_id = m.snapshot_id
                        LEFT JOIN users pu ON pu.user_id = m.participant_id 
                        WHERE c.category IS NOT NULL AND cc.circle_id = :circleId
                        ORDER BY cc.pin_time DESC, 
                        CASE 
                            WHEN m.created_at is NULL THEN c.created_at
                            ELSE m.created_at 
                        END 
                        DESC
                        """
                    val statement = RoomSQLiteQuery.acquire(sql, 1)
                    statement.bindString(1, circleId)
                    val countSql =
                        """
                        SELECT COUNT(1) FROM circle_conversations cc
	                    INNER JOIN circles ci ON ci.circle_id = cc.circle_id
                        INNER JOIN conversations c ON cc.conversation_id = c.conversation_id
	                    WHERE c.category IS NOT NULL AND cc.circle_id = '$circleId'
                    """
                    val countStatement = RoomSQLiteQuery.acquire(countSql, 0)
                    return object : MixinLimitOffsetDataSource<ConversationItem>(database, statement, countStatement, false, "message_mentions", "circle_conversations", "conversations", "circles", "users", "messages", "snapshots") {
                        override fun convertRows(cursor: Cursor): List<ConversationItem> {
                            val cursorIndexOfConversationId = CursorUtil.getColumnIndexOrThrow(cursor, "conversationId")
                            val cursorIndexOfGroupIconUrl = CursorUtil.getColumnIndexOrThrow(cursor, "groupIconUrl")
                            val cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(cursor, "category")
                            val cursorIndexOfGroupName = CursorUtil.getColumnIndexOrThrow(cursor, "groupName")
                            val cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(cursor, "status")
                            val cursorIndexOfLastReadMessageId = CursorUtil.getColumnIndexOrThrow(cursor, "lastReadMessageId")
                            val cursorIndexOfUnseenMessageCount = CursorUtil.getColumnIndexOrThrow(cursor, "unseenMessageCount")
                            val cursorIndexOfOwnerId = CursorUtil.getColumnIndexOrThrow(cursor, "ownerId")
                            val cursorIndexOfPinTime = CursorUtil.getColumnIndexOrThrow(cursor, "pinTime")
                            val cursorIndexOfMuteUntil = CursorUtil.getColumnIndexOrThrow(cursor, "muteUntil")
                            val cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(cursor, "avatarUrl")
                            val cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(cursor, "name")
                            val cursorIndexOfOwnerVerified = CursorUtil.getColumnIndexOrThrow(cursor, "ownerVerified")
                            val cursorIndexOfOwnerIdentityNumber = CursorUtil.getColumnIndexOrThrow(cursor, "ownerIdentityNumber")
                            val cursorIndexOfOwnerMuteUntil = CursorUtil.getColumnIndexOrThrow(cursor, "ownerMuteUntil")
                            val cursorIndexOfAppId = CursorUtil.getColumnIndexOrThrow(cursor, "appId")
                            val cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(cursor, "content")
                            val cursorIndexOfContentType = CursorUtil.getColumnIndexOrThrow(cursor, "contentType")
                            val cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(cursor, "createdAt")
                            val cursorIndexOfMediaUrl = CursorUtil.getColumnIndexOrThrow(cursor, "mediaUrl")
                            val cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(cursor, "senderId")
                            val cursorIndexOfActionName = CursorUtil.getColumnIndexOrThrow(cursor, "actionName")
                            val cursorIndexOfMessageStatus = CursorUtil.getColumnIndexOrThrow(cursor, "messageStatus")
                            val cursorIndexOfSenderFullName = CursorUtil.getColumnIndexOrThrow(cursor, "senderFullName")
                            val cursorIndexOfParticipantFullName = CursorUtil.getColumnIndexOrThrow(cursor, "participantFullName")
                            val cursorIndexOfParticipantUserId = CursorUtil.getColumnIndexOrThrow(cursor, "participantUserId")
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
                                val tmpOwnerIdentityNumber = cursor.getString(cursorIndexOfOwnerIdentityNumber)
                                val tmpOwnerMuteUntil = cursor.getString(cursorIndexOfOwnerMuteUntil)
                                val tmpAppId = cursor.getString(cursorIndexOfAppId)
                                val tmpContent = cursor.getString(cursorIndexOfContent)
                                val tmpContentType = cursor.getString(cursorIndexOfContentType)
                                val tmpCreatedAt = cursor.getString(cursorIndexOfCreatedAt)
                                val tmpMediaUrl = cursor.getString(cursorIndexOfMediaUrl)
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
                                item = ConversationItem(tmpConversationId, tmpAvatarUrl, tmpGroupIconUrl, tmpCategory, tmpGroupName, tmpName, tmpOwnerId, tmpOwnerIdentityNumber, tmpStatus, tmpLastReadMessageId, tmpUnseenMessageCount, tmpContent, tmpContentType, tmpMediaUrl, tmpCreatedAt, tmpPinTime, tmpSenderId, tmpSenderFullName, tmpMessageStatus, tmpActionName, tmpParticipantFullName, tmpParticipantUserId, tmpOwnerMuteUntil, tmpOwnerVerified, tmpMuteUntil, null, tmpAppId, tmpMentions, tmpMentionCount)
                                res.add(item)
                            }
                            return res
                        }
                    }
                }
            }

        fun fuzzySearchMessageDetail(query: String?, conversationId: String?, database: MixinDatabase) =
            object : DataSource.Factory<Int, SearchMessageDetailItem>() {
                override fun create(): DataSource<Int, SearchMessageDetailItem> {
                    val sql =
                        """
                            SELECT m.id AS messageId, u.user_id AS userId, u.avatar_url AS userAvatarUrl, u.full_name AS userFullName,
                            m.category AS type, m.content AS content, m.created_at AS createdAt, m.name AS mediaName 
                            FROM messages m INNER JOIN users u ON m.user_id = u.user_id 
                            WHERE m.id in (SELECT message_id FROM messages_fts4 WHERE messages_fts4 MATCH ?) 
                            AND m.category IN ('SIGNAL_TEXT', 'PLAIN_TEXT', 'SIGNAL_DATA', 'PLAIN_DATA', 'SIGNAL_POST', 'PLAIN_POST') 
                            AND m.conversation_id = ?
                            AND m.status != 'FAILED'
                            ORDER BY m.created_at DESC
                        """
                    val countSql =
                        """
                            SELECT count(*) FROM messages m 
                            INNER JOIN users u ON m.user_id = u.user_id 
                            WHERE m.id in (SELECT message_id FROM messages_fts4 WHERE messages_fts4 MATCH ?) 
                            AND m.category IN ('SIGNAL_TEXT', 'PLAIN_TEXT', 'SIGNAL_DATA', 'PLAIN_DATA', 'SIGNAL_POST', 'PLAIN_POST') 
                            AND m.conversation_id = ?
                        """
                    val countStatement = RoomSQLiteQuery.acquire(countSql, 2)
                    val statement = RoomSQLiteQuery.acquire(sql, 2)
                    var argIndex = 1
                    if (query == null) {
                        statement.bindNull(argIndex)
                        countStatement.bindNull(argIndex)
                    } else {
                        statement.bindString(argIndex, query)
                        countStatement.bindString(argIndex, query)
                    }
                    argIndex = 2
                    if (conversationId == null) {
                        statement.bindNull(argIndex)
                        countStatement.bindNull(argIndex)
                    } else {
                        statement.bindString(argIndex, conversationId)
                        countStatement.bindString(argIndex, conversationId)
                    }
                    return object : MixinLimitOffsetDataSource<SearchMessageDetailItem>(database, statement, countStatement, false, "messages", "users", "snapshots", "assets", "stickers", "hyperlinks", "conversations", "message_mentions") {
                        override fun convertRows(cursor: Cursor): MutableList<SearchMessageDetailItem> {
                            val cursorIndexOfMessageId = CursorUtil.getColumnIndexOrThrow(cursor, "messageId")
                            val cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(cursor, "userId")
                            val cursorIndexOfUserAvatarUrl = CursorUtil.getColumnIndexOrThrow(cursor, "userAvatarUrl")
                            val cursorIndexOfUserFullName = CursorUtil.getColumnIndexOrThrow(cursor, "userFullName")
                            val cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(cursor, "type")
                            val cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(cursor, "content")
                            val cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(cursor, "createdAt")
                            val cursorIndexOfMediaName = CursorUtil.getColumnIndexOrThrow(cursor, "mediaName")
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
                                item = SearchMessageDetailItem(tmpMessageId, tmpType, tmpContent, tmpCreatedAt, tmpMediaName, tmpUserId, tmpUserFullName, tmpUserAvatarUrl)
                                res.add(item)
                            }
                            return res
                        }
                    }
                }
            }
    }
}
