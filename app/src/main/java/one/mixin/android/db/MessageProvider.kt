package one.mixin.android.db

import android.annotation.SuppressLint
import android.database.Cursor
import android.os.CancellationSignal
import androidx.paging.DataSource
import androidx.room.CoroutinesRoom
import androidx.room.RoomSQLiteQuery
import androidx.room.util.CursorUtil
import androidx.room.util.DBUtil
import one.mixin.android.ui.search.CancellationLimitOffsetDataSource
import one.mixin.android.util.chat.FixedLimitOffsetDataSource
import one.mixin.android.util.chat.MixinLimitOffsetDataSource
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.SearchMessageDetailItem
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import java.util.concurrent.Callable

@SuppressLint("RestrictedApi")
class MessageProvider {
    companion object {
        fun getMessages(conversationId: String, database: MixinDatabase, unreadCount: Int, countable: Boolean) =
            object : DataSource.Factory<Int, MessageItem>() {
                override fun create(): DataSource<Int, MessageItem> {
                    val sql =
                        """
                        SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId,
                        u.full_name AS userFullName, u.identity_number AS userIdentityNumber, u.app_id AS appId, m.category AS type,
                        m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus, m.media_waveform AS mediaWaveform,
                        m.name AS mediaName, m.media_mime_type AS mediaMimeType, m.media_size AS mediaSize, m.media_width AS mediaWidth, m.media_height AS mediaHeight,
                        m.thumb_image AS thumbImage, m.thumb_url AS thumbUrl, m.media_url AS mediaUrl, m.media_duration AS mediaDuration, m.quote_message_id as quoteId,
                        m.quote_content as quoteContent, m.caption as caption, u1.full_name AS participantFullName, m.action AS actionName, u1.user_id AS participantUserId,
                        s.snapshot_id AS snapshotId, s.type AS snapshotType, s.amount AS snapshotAmount, a.symbol AS assetSymbol, s.asset_id AS assetId,
                        a.icon_url AS assetIcon, st.asset_url AS assetUrl, st.asset_width AS assetWidth, st.asset_height AS assetHeight, st.sticker_id AS stickerId,
                        st.name AS assetName, st.asset_type AS assetType, h.site_name AS siteName, h.site_title AS siteTitle, h.site_description AS siteDescription,
                        h.site_image AS siteImage, m.shared_user_id AS sharedUserId, su.full_name AS sharedUserFullName, su.identity_number AS sharedUserIdentityNumber,
                        su.avatar_url AS sharedUserAvatarUrl, su.is_verified AS sharedUserIsVerified, su.app_id AS sharedUserAppId, mm.mentions AS mentions, mm.has_read as mentionRead, 
                        pm.message_id IS NOT NULL as isPin, c.name AS groupName
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
                        LEFT JOIN pin_messages pm ON m.id = pm.message_id
                        WHERE m.conversation_id = ? 
                        ORDER BY m.created_at DESC 
                        """
                    val statement = RoomSQLiteQuery.acquire(sql, 1)
                    val argIndex = 1
                    statement.bindString(argIndex, conversationId)
                    return if (countable) {
                        val countSql = "SELECT COUNT(1) FROM messages m INNER JOIN users u ON m.user_id = u.user_id WHERE conversation_id = ?"
                        val countStatement = RoomSQLiteQuery.acquire(countSql, 1)
                        countStatement.bindString(argIndex, conversationId)
                        MixinMessageItemLimitOffsetDataSource(database, statement, countStatement)
                    } else {
                        FixedMessageItemLimitOffsetDataSource(database, statement, unreadCount)
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
                    (SELECT count(1) FROM message_mentions me WHERE me.conversation_id = c.conversation_id AND me.has_read = 0) as mentionCount,  
                    mm.mentions AS mentions 
                    FROM conversations c
                    INNER JOIN users ou ON ou.user_id = c.owner_id
                    LEFT JOIN messages m ON c.last_message_id = m.id
                    LEFT JOIN message_mentions mm ON mm.message_id = m.id
                    LEFT JOIN users mu ON mu.user_id = m.user_id
                    LEFT JOIN snapshots s ON s.snapshot_id = m.snapshot_id
                    LEFT JOIN users pu ON pu.user_id = m.participant_id 
                    WHERE c.category IN ('CONTACT', 'GROUP')
                    ORDER BY c.pin_time DESC, c.last_message_created_at DESC
                        """
                    val statement = RoomSQLiteQuery.acquire(sql, 0)
                    val countSql = "SELECT COUNT(1) FROM conversations c INNER JOIN users ou ON ou.user_id = c.owner_id WHERE category IN ('CONTACT', 'GROUP')"
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
                        (SELECT count(1) FROM message_mentions me WHERE me.conversation_id = c.conversation_id AND me.has_read = 0) AS mentionCount,  
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
                    INNER JOIN users ou ON ou.user_id = c.owner_id
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

        @Suppress("LocalVariableName", "JoinDeclarationAndAssignment")
        suspend fun fuzzySearchAsset(
            name: String?,
            symbol: String?,
            db: MixinDatabase,
            cancellationSignal: CancellationSignal,
        ): List<AssetItem> {
            val _sql =
                """SELECT a1.asset_id AS assetId, a1.symbol, a1.name, a1.icon_url AS iconUrl, a1.balance, a1.destination AS destination, a1.tag AS tag, a1.price_btc AS priceBtc, a1.price_usd AS priceUsd, a1.chain_id AS chainId, a1.change_usd AS changeUsd, a1.change_btc AS changeBtc, ae.hidden, a2.price_usd as chainPriceUsd,a1.confirmations, a1.reserve as reserve, a2.icon_url AS chainIconUrl, a2.symbol as chainSymbol, a2.name as chainName, a1.asset_key AS assetKey FROM assets a1 LEFT JOIN assets a2 ON a1.chain_id = a2.asset_id LEFT JOIN assets_extra ae ON ae.asset_id = a1.asset_id  
        WHERE a1.balance > 0 
        AND (a1.symbol LIKE '%' || ? || '%'  ESCAPE '\' OR a1.name LIKE '%' || ? || '%'  ESCAPE '\')
        ORDER BY 
            a1.symbol = ? COLLATE NOCASE OR a1.name = ? COLLATE NOCASE DESC,
            a1.price_usd*a1.balance DESC
                """
            val _statement = RoomSQLiteQuery.acquire(_sql, 4)
            var _argIndex = 1
            if (symbol == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, symbol)
            }
            _argIndex = 2
            if (name == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, name)
            }
            _argIndex = 3
            if (symbol == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, symbol)
            }
            _argIndex = 4
            if (name == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, name)
            }
            return CoroutinesRoom.execute(
                db, false, cancellationSignal,
                Callable<List<AssetItem>> {
                    val _cursor = DBUtil.query(db, _statement, false, cancellationSignal)
                    try {
                        val _cursorIndexOfAssetId = 0
                        val _cursorIndexOfSymbol = 1
                        val _cursorIndexOfName = 2
                        val _cursorIndexOfIconUrl = 3
                        val _cursorIndexOfBalance = 4
                        val _cursorIndexOfDestination = 5
                        val _cursorIndexOfTag = 6
                        val _cursorIndexOfPriceBtc = 7
                        val _cursorIndexOfPriceUsd = 8
                        val _cursorIndexOfChainId = 9
                        val _cursorIndexOfChangeUsd = 10
                        val _cursorIndexOfChangeBtc = 11
                        val _cursorIndexOfHidden = 12
                        val _cursorIndexOfChainPriceUsd = 13
                        val _cursorIndexOfConfirmations = 14
                        val _cursorIndexOfReserve = 15
                        val _cursorIndexOfChainIconUrl = 16
                        val _cursorIndexOfChainSymbol = 17
                        val _cursorIndexOfChainName = 18
                        val _cursorIndexOfAssetKey = 19
                        val _result: MutableList<AssetItem> = java.util.ArrayList(_cursor.count)
                        while (_cursor.moveToNext()) {
                            val _item: AssetItem
                            val _tmpAssetId: String?
                            _tmpAssetId = if (_cursor.isNull(_cursorIndexOfAssetId)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfAssetId)
                            }
                            val _tmpSymbol: String?
                            _tmpSymbol = if (_cursor.isNull(_cursorIndexOfSymbol)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfSymbol)
                            }
                            val _tmpName: String?
                            _tmpName = if (_cursor.isNull(_cursorIndexOfName)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfName)
                            }
                            val _tmpIconUrl: String?
                            _tmpIconUrl = if (_cursor.isNull(_cursorIndexOfIconUrl)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfIconUrl)
                            }
                            val _tmpBalance: String?
                            _tmpBalance = if (_cursor.isNull(_cursorIndexOfBalance)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfBalance)
                            }
                            val _tmpDestination: String?
                            _tmpDestination = if (_cursor.isNull(_cursorIndexOfDestination)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfDestination)
                            }
                            val _tmpTag: String?
                            _tmpTag = if (_cursor.isNull(_cursorIndexOfTag)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfTag)
                            }
                            val _tmpPriceBtc: String?
                            _tmpPriceBtc = if (_cursor.isNull(_cursorIndexOfPriceBtc)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfPriceBtc)
                            }
                            val _tmpPriceUsd: String?
                            _tmpPriceUsd = if (_cursor.isNull(_cursorIndexOfPriceUsd)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfPriceUsd)
                            }
                            val _tmpChainId: String?
                            _tmpChainId = if (_cursor.isNull(_cursorIndexOfChainId)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfChainId)
                            }
                            val _tmpChangeUsd: String?
                            _tmpChangeUsd = if (_cursor.isNull(_cursorIndexOfChangeUsd)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfChangeUsd)
                            }
                            val _tmpChangeBtc: String?
                            _tmpChangeBtc = if (_cursor.isNull(_cursorIndexOfChangeBtc)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfChangeBtc)
                            }
                            val _tmpHidden: Boolean?
                            val _tmp: Int?
                            _tmp = if (_cursor.isNull(_cursorIndexOfHidden)) {
                                null
                            } else {
                                _cursor.getInt(_cursorIndexOfHidden)
                            }
                            _tmpHidden = if (_tmp == null) null else _tmp != 0
                            val _tmpChainPriceUsd: String?
                            _tmpChainPriceUsd = if (_cursor.isNull(_cursorIndexOfChainPriceUsd)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfChainPriceUsd)
                            }
                            val _tmpConfirmations: Int
                            _tmpConfirmations = _cursor.getInt(_cursorIndexOfConfirmations)
                            val _tmpReserve: String?
                            _tmpReserve = if (_cursor.isNull(_cursorIndexOfReserve)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfReserve)
                            }
                            val _tmpChainIconUrl: String?
                            _tmpChainIconUrl = if (_cursor.isNull(_cursorIndexOfChainIconUrl)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfChainIconUrl)
                            }
                            val _tmpChainSymbol: String?
                            _tmpChainSymbol = if (_cursor.isNull(_cursorIndexOfChainSymbol)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfChainSymbol)
                            }
                            val _tmpChainName: String?
                            _tmpChainName = if (_cursor.isNull(_cursorIndexOfChainName)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfChainName)
                            }
                            val _tmpAssetKey: String?
                            _tmpAssetKey = if (_cursor.isNull(_cursorIndexOfAssetKey)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfAssetKey)
                            }
                            _item = AssetItem(
                                _tmpAssetId!!,
                                _tmpSymbol!!,
                                _tmpName!!,
                                _tmpIconUrl!!,
                                _tmpBalance!!,
                                _tmpDestination!!,
                                _tmpTag,
                                _tmpPriceBtc!!,
                                _tmpPriceUsd!!,
                                _tmpChainId!!,
                                _tmpChangeUsd!!,
                                _tmpChangeBtc!!,
                                _tmpHidden,
                                _tmpConfirmations,
                                _tmpChainIconUrl,
                                _tmpChainSymbol,
                                _tmpChainName,
                                _tmpChainPriceUsd,
                                _tmpAssetKey,
                                _tmpReserve
                            )
                            _result.add(_item)
                        }
                        return@Callable _result
                    } finally {
                        _cursor.close()
                        _statement.release()
                    }
                }
            )
        }

        @Suppress("LocalVariableName", "JoinDeclarationAndAssignment")
        suspend fun fuzzySearchUser(
            username: String?,
            identityNumber: String?,
            id: String?,
            db: MixinDatabase,
            cancellationSignal: CancellationSignal,
        ): List<User> {
            val _sql = """
        SELECT * FROM users 
        WHERE user_id != ? 
        AND relationship = 'FRIEND' 
        AND identity_number != '0'
        AND (full_name LIKE '%' || ? || '%'  ESCAPE '\' OR identity_number like '%' || ? || '%'  ESCAPE '\')
        ORDER BY 
            full_name = ? COLLATE NOCASE OR identity_number = ? COLLATE NOCASE DESC
            """
            val _statement = RoomSQLiteQuery.acquire(_sql, 5)
            var _argIndex = 1
            if (id == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, id)
            }
            _argIndex = 2
            if (username == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, username)
            }
            _argIndex = 3
            if (identityNumber == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, identityNumber)
            }
            _argIndex = 4
            if (username == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, username)
            }
            _argIndex = 5
            if (identityNumber == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, identityNumber)
            }
            return CoroutinesRoom.execute(
                db, false, cancellationSignal,
                Callable<List<User>> {
                    val _cursor = DBUtil.query(db, _statement, false, cancellationSignal)
                    try {
                        val _cursorIndexOfUserId =
                            CursorUtil.getColumnIndexOrThrow(_cursor, "user_id")
                        val _cursorIndexOfIdentityNumber =
                            CursorUtil.getColumnIndexOrThrow(_cursor, "identity_number")
                        val _cursorIndexOfRelationship =
                            CursorUtil.getColumnIndexOrThrow(_cursor, "relationship")
                        val _cursorIndexOfBiography =
                            CursorUtil.getColumnIndexOrThrow(_cursor, "biography")
                        val _cursorIndexOfFullName =
                            CursorUtil.getColumnIndexOrThrow(_cursor, "full_name")
                        val _cursorIndexOfAvatarUrl =
                            CursorUtil.getColumnIndexOrThrow(_cursor, "avatar_url")
                        val _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone")
                        val _cursorIndexOfIsVerified =
                            CursorUtil.getColumnIndexOrThrow(_cursor, "is_verified")
                        val _cursorIndexOfCreatedAt =
                            CursorUtil.getColumnIndexOrThrow(_cursor, "created_at")
                        val _cursorIndexOfMuteUntil =
                            CursorUtil.getColumnIndexOrThrow(_cursor, "mute_until")
                        val _cursorIndexOfHasPin =
                            CursorUtil.getColumnIndexOrThrow(_cursor, "has_pin")
                        val _cursorIndexOfAppId =
                            CursorUtil.getColumnIndexOrThrow(_cursor, "app_id")
                        val _cursorIndexOfIsScam =
                            CursorUtil.getColumnIndexOrThrow(_cursor, "is_scam")
                        val _result: MutableList<User> = java.util.ArrayList(_cursor.count)
                        while (_cursor.moveToNext()) {
                            val _item: User
                            val _tmpUserId: String?
                            _tmpUserId = if (_cursor.isNull(_cursorIndexOfUserId)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfUserId)
                            }
                            val _tmpIdentityNumber: String?
                            _tmpIdentityNumber = if (_cursor.isNull(_cursorIndexOfIdentityNumber)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfIdentityNumber)
                            }
                            val _tmpRelationship: String?
                            _tmpRelationship = if (_cursor.isNull(_cursorIndexOfRelationship)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfRelationship)
                            }
                            val _tmpBiography: String?
                            _tmpBiography = if (_cursor.isNull(_cursorIndexOfBiography)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfBiography)
                            }
                            val _tmpFullName: String?
                            _tmpFullName = if (_cursor.isNull(_cursorIndexOfFullName)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfFullName)
                            }
                            val _tmpAvatarUrl: String?
                            _tmpAvatarUrl = if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfAvatarUrl)
                            }
                            val _tmpPhone: String?
                            _tmpPhone = if (_cursor.isNull(_cursorIndexOfPhone)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfPhone)
                            }
                            val _tmpIsVerified: Boolean?
                            val _tmp: Int?
                            _tmp = if (_cursor.isNull(_cursorIndexOfIsVerified)) {
                                null
                            } else {
                                _cursor.getInt(_cursorIndexOfIsVerified)
                            }
                            _tmpIsVerified = if (_tmp == null) null else _tmp != 0
                            val _tmpCreatedAt: String?
                            _tmpCreatedAt = if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfCreatedAt)
                            }
                            val _tmpMuteUntil: String?
                            _tmpMuteUntil = if (_cursor.isNull(_cursorIndexOfMuteUntil)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfMuteUntil)
                            }
                            val _tmpHasPin: Boolean?
                            val _tmp_1: Int?
                            _tmp_1 = if (_cursor.isNull(_cursorIndexOfHasPin)) {
                                null
                            } else {
                                _cursor.getInt(_cursorIndexOfHasPin)
                            }
                            _tmpHasPin = if (_tmp_1 == null) null else _tmp_1 != 0
                            val _tmpAppId: String?
                            _tmpAppId = if (_cursor.isNull(_cursorIndexOfAppId)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfAppId)
                            }
                            val _tmpIsScam: Boolean?
                            val _tmp_2: Int?
                            _tmp_2 = if (_cursor.isNull(_cursorIndexOfIsScam)) {
                                null
                            } else {
                                _cursor.getInt(_cursorIndexOfIsScam)
                            }
                            _tmpIsScam = if (_tmp_2 == null) null else _tmp_2 != 0
                            _item = User(
                                _tmpUserId!!,
                                _tmpIdentityNumber!!,
                                _tmpRelationship!!,
                                _tmpBiography!!,
                                _tmpFullName,
                                _tmpAvatarUrl,
                                _tmpPhone,
                                _tmpIsVerified,
                                _tmpCreatedAt,
                                _tmpMuteUntil,
                                _tmpHasPin,
                                _tmpAppId,
                                _tmpIsScam
                            )
                            _result.add(_item)
                        }
                        return@Callable _result
                    } finally {
                        _cursor.close()
                        _statement.release()
                    }
                }
            )
        }

        @Suppress("LocalVariableName", "JoinDeclarationAndAssignment")
        suspend fun fuzzySearchChat(
            query: String?,
            db: MixinDatabase,
            cancellationSignal: CancellationSignal
        ): List<ChatMinimal> {
            val _sql = """
        SELECT c.conversation_id AS conversationId, c.icon_url AS groupIconUrl, c.category AS category, c.name AS groupName,
        ou.identity_number AS ownerIdentityNumber, c.owner_id AS userId, ou.full_name AS fullName, ou.avatar_url AS avatarUrl,
        ou.is_verified AS isVerified, ou.app_id AS appId
        FROM conversations c
        INNER JOIN users ou ON ou.user_id = c.owner_id
        LEFT JOIN messages m ON c.last_message_id = m.id
        WHERE (c.category = 'GROUP' AND c.name LIKE '%' || ? || '%'  ESCAPE '\') 
        OR (c.category = 'CONTACT' AND ou.relationship != 'FRIEND' 
            AND (ou.full_name LIKE '%' || ? || '%'  ESCAPE '\' 
                OR ou.identity_number like '%' || ? || '%'  ESCAPE '\'))
        ORDER BY 
            (c.category = 'GROUP' AND c.name = ? COLLATE NOCASE) 
                OR (c.category = 'CONTACT' AND ou.relationship != 'FRIEND' 
                    AND (ou.full_name = ? COLLATE NOCASE
                        OR ou.identity_number = ? COLLATE NOCASE)) DESC,
            c.pin_time DESC, 
            m.created_at DESC
            """
            val _statement = RoomSQLiteQuery.acquire(_sql, 6)
            var _argIndex = 1
            if (query == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, query)
            }
            _argIndex = 2
            if (query == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, query)
            }
            _argIndex = 3
            if (query == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, query)
            }
            _argIndex = 4
            if (query == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, query)
            }
            _argIndex = 5
            if (query == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, query)
            }
            _argIndex = 6
            if (query == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, query)
            }
            return CoroutinesRoom.execute(
                db, false, cancellationSignal,
                Callable<List<ChatMinimal>> {
                    val _cursor = DBUtil.query(db, _statement, false, cancellationSignal)
                    try {
                        val _cursorIndexOfConversationId = 0
                        val _cursorIndexOfGroupIconUrl = 1
                        val _cursorIndexOfCategory = 2
                        val _cursorIndexOfGroupName = 3
                        val _cursorIndexOfOwnerIdentityNumber = 4
                        val _cursorIndexOfUserId = 5
                        val _cursorIndexOfFullName = 6
                        val _cursorIndexOfAvatarUrl = 7
                        val _cursorIndexOfIsVerified = 8
                        val _cursorIndexOfAppId = 9
                        val _result: MutableList<ChatMinimal> = java.util.ArrayList(_cursor.count)
                        while (_cursor.moveToNext()) {
                            val _item: ChatMinimal
                            val _tmpConversationId: String?
                            _tmpConversationId = if (_cursor.isNull(_cursorIndexOfConversationId)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfConversationId)
                            }
                            val _tmpGroupIconUrl: String?
                            _tmpGroupIconUrl = if (_cursor.isNull(_cursorIndexOfGroupIconUrl)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfGroupIconUrl)
                            }
                            val _tmpCategory: String?
                            _tmpCategory = if (_cursor.isNull(_cursorIndexOfCategory)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfCategory)
                            }
                            val _tmpGroupName: String?
                            _tmpGroupName = if (_cursor.isNull(_cursorIndexOfGroupName)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfGroupName)
                            }
                            val _tmpOwnerIdentityNumber: String?
                            _tmpOwnerIdentityNumber =
                                if (_cursor.isNull(_cursorIndexOfOwnerIdentityNumber)) {
                                    null
                                } else {
                                    _cursor.getString(_cursorIndexOfOwnerIdentityNumber)
                                }
                            val _tmpUserId: String?
                            _tmpUserId = if (_cursor.isNull(_cursorIndexOfUserId)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfUserId)
                            }
                            val _tmpFullName: String?
                            _tmpFullName = if (_cursor.isNull(_cursorIndexOfFullName)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfFullName)
                            }
                            val _tmpAvatarUrl: String?
                            _tmpAvatarUrl = if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfAvatarUrl)
                            }
                            val _tmpIsVerified: Boolean?
                            val _tmp: Int?
                            _tmp = if (_cursor.isNull(_cursorIndexOfIsVerified)) {
                                null
                            } else {
                                _cursor.getInt(_cursorIndexOfIsVerified)
                            }
                            _tmpIsVerified = if (_tmp == null) null else _tmp != 0
                            val _tmpAppId: String?
                            _tmpAppId = if (_cursor.isNull(_cursorIndexOfAppId)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfAppId)
                            }
                            _item = ChatMinimal(
                                _tmpCategory!!,
                                _tmpConversationId!!, _tmpGroupIconUrl, _tmpGroupName,
                                _tmpOwnerIdentityNumber!!,
                                _tmpUserId!!, _tmpFullName, _tmpAvatarUrl, _tmpIsVerified, _tmpAppId
                            )
                            _result.add(_item)
                        }
                        return@Callable _result
                    } finally {
                        _cursor.close()
                        _statement.release()
                    }
                }
            )
        }

        @Suppress("LocalVariableName", "JoinDeclarationAndAssignment")
        suspend fun fuzzySearchMessage(
            query: String?,
            limit: Int,
            db: MixinDatabase,
            cancellationSignal: CancellationSignal,
        ): List<SearchMessageItem> {
            val _sql =
                """
                SELECT m.conversation_id AS conversationId, c.icon_url AS conversationAvatarUrl,
                c.name AS conversationName, c.category AS conversationCategory, count(m.id) as messageCount,
                u.user_id AS userId, u.avatar_url AS userAvatarUrl, u.full_name AS userFullName
                FROM messages m, (SELECT message_id FROM messages_fts4 WHERE messages_fts4 MATCH ?) fts
                INNER JOIN users u ON c.owner_id = u.user_id
                INNER JOIN conversations c ON c.conversation_id = m.conversation_id
                WHERE m.id = fts.message_id
                GROUP BY m.conversation_id
                ORDER BY max(m.created_at) DESC
                LIMIT ?
                """
            val _statement = RoomSQLiteQuery.acquire(_sql, 2)
            var _argIndex = 1
            if (query == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, query)
            }
            _argIndex = 2
            _statement.bindLong(_argIndex, limit.toLong())
            return CoroutinesRoom.execute(
                db, false, cancellationSignal,
                Callable<List<SearchMessageItem>> {
                    val _cursor = DBUtil.query(db, _statement, false, cancellationSignal)
                    try {
                        val _cursorIndexOfConversationId = 0
                        val _cursorIndexOfConversationAvatarUrl = 1
                        val _cursorIndexOfConversationName = 2
                        val _cursorIndexOfConversationCategory = 3
                        val _cursorIndexOfMessageCount = 4
                        val _cursorIndexOfUserId = 5
                        val _cursorIndexOfUserAvatarUrl = 6
                        val _cursorIndexOfUserFullName = 7
                        val _result: MutableList<SearchMessageItem> =
                            java.util.ArrayList(_cursor.count)
                        while (_cursor.moveToNext()) {
                            val _item: SearchMessageItem
                            val _tmpConversationId: String?
                            _tmpConversationId = if (_cursor.isNull(_cursorIndexOfConversationId)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfConversationId)
                            }
                            val _tmpConversationAvatarUrl: String?
                            _tmpConversationAvatarUrl =
                                if (_cursor.isNull(_cursorIndexOfConversationAvatarUrl)) {
                                    null
                                } else {
                                    _cursor.getString(_cursorIndexOfConversationAvatarUrl)
                                }
                            val _tmpConversationName: String?
                            _tmpConversationName =
                                if (_cursor.isNull(_cursorIndexOfConversationName)) {
                                    null
                                } else {
                                    _cursor.getString(_cursorIndexOfConversationName)
                                }
                            val _tmpConversationCategory: String?
                            _tmpConversationCategory =
                                if (_cursor.isNull(_cursorIndexOfConversationCategory)) {
                                    null
                                } else {
                                    _cursor.getString(_cursorIndexOfConversationCategory)
                                }
                            val _tmpMessageCount: Int
                            _tmpMessageCount = _cursor.getInt(_cursorIndexOfMessageCount)
                            val _tmpUserId: String?
                            _tmpUserId = if (_cursor.isNull(_cursorIndexOfUserId)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfUserId)
                            }
                            val _tmpUserAvatarUrl: String?
                            _tmpUserAvatarUrl = if (_cursor.isNull(_cursorIndexOfUserAvatarUrl)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfUserAvatarUrl)
                            }
                            val _tmpUserFullName: String?
                            _tmpUserFullName = if (_cursor.isNull(_cursorIndexOfUserFullName)) {
                                null
                            } else {
                                _cursor.getString(_cursorIndexOfUserFullName)
                            }
                            _item = SearchMessageItem(
                                _tmpConversationId!!,
                                _tmpConversationCategory,
                                _tmpConversationName,
                                _tmpMessageCount,
                                _tmpUserId!!,
                                _tmpUserFullName,
                                _tmpUserAvatarUrl,
                                _tmpConversationAvatarUrl
                            )
                            _result.add(_item)
                        }
                        return@Callable _result
                    } finally {
                        _cursor.close()
                        _statement.release()
                    }
                }
            )
        }

        fun fuzzySearchMessageDetail(query: String?, conversationId: String?, database: MixinDatabase, cancellationSignal: CancellationSignal) =
            object : DataSource.Factory<Int, SearchMessageDetailItem>() {
                override fun create(): DataSource<Int, SearchMessageDetailItem> {
                    val sql =
                        """
                            SELECT m.id AS messageId, u.user_id AS userId, u.avatar_url AS userAvatarUrl, u.full_name AS userFullName,
                            m.category AS type, m.content AS content, m.created_at AS createdAt, m.name AS mediaName 
                            FROM messages m INNER JOIN users u ON m.user_id = u.user_id 
                            WHERE m.id in (SELECT message_id FROM messages_fts4 WHERE messages_fts4 MATCH ?) 
                            AND m.conversation_id = ?
                            ORDER BY m.created_at DESC
                        """
                    val countSql =
                        """
                            SELECT count(1) FROM messages m 
                            INNER JOIN users u ON m.user_id = u.user_id 
                            WHERE m.id in (SELECT message_id FROM messages_fts4 WHERE messages_fts4 MATCH ?) 
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
                    return CancellationMessageDetailItemLimitOffsetDataSource(database, statement, countStatement, cancellationSignal)
                }
            }
    }

    private class MixinMessageItemLimitOffsetDataSource(
        database: MixinDatabase,
        statement: RoomSQLiteQuery,
        countStatement: RoomSQLiteQuery,
    ) : MixinLimitOffsetDataSource<MessageItem>(database, statement, countStatement, false, "messages", "users", "snapshots", "assets", "stickers", "hyperlinks", "conversations", "message_mentions") {
        override fun convertRows(cursor: Cursor?): MutableList<MessageItem> {
            return convertToMessageItems(cursor)
        }
    }

    private class FixedMessageItemLimitOffsetDataSource(
        database: MixinDatabase,
        statement: RoomSQLiteQuery,
        unreadCount: Int,
    ) : FixedLimitOffsetDataSource<MessageItem>(database, statement, unreadCount, "messages", "users", "snapshots", "assets", "stickers", "hyperlinks", "conversations", "message_mentions") {
        override fun convertRows(cursor: Cursor?): MutableList<MessageItem> {
            return convertToMessageItems(cursor)
        }
    }

    private class CancellationMessageDetailItemLimitOffsetDataSource(
        database: MixinDatabase,
        statement: RoomSQLiteQuery,
        countStatement: RoomSQLiteQuery,
        cancellationSignal: CancellationSignal,
    ) : CancellationLimitOffsetDataSource<SearchMessageDetailItem>(database, statement, countStatement, cancellationSignal, true, "messages", "users", "snapshots", "assets", "stickers", "hyperlinks", "conversations", "message_mentions") {
        override fun convertRows(cursor: Cursor?): MutableList<SearchMessageDetailItem> {
            return convertToSearchMessageDetailItem(cursor)
        }
    }
}

private fun convertToSearchMessageDetailItem(cursor: Cursor?): ArrayList<SearchMessageDetailItem> {
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

private fun convertToMessageItems(cursor: Cursor?): ArrayList<MessageItem> {
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
    val cursorIndexOfPinTop = cursor.getColumnIndexOrThrow("isPin")
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
        val tmp_2: Int? = if (cursor.isNull(cursorIndexOfPinTop)) {
            null
        } else {
            cursor.getInt(cursorIndexOfPinTop)
        }
        val tmpPinTop = if (tmp_2 == null) null else tmp_2 != 0
        item = MessageItem(
            tmpMessageId, tmpConversationId, tmpUserId, tmpUserFullName, tmpUserIdentityNumber, tmpType, tmpContent,
            tmpCreatedAt, tmpStatus, tmpMediaStatus, null, tmpMediaName, tmpMediaMimeType, tmpMediaSize, tmpThumbUrl, tmpMediaWidth,
            tmpMediaHeight, tmpThumbImage, tmpMediaUrl, tmpMediaDuration, tmpParticipantFullName, tmpParticipantUserId, tmpActionName, tmpSnapshotId,
            tmpSnapshotType, tmpSnapshotAmount, tmpAssetId, tmpAssetType, tmpAssetSymbol, tmpAssetIcon, tmpAssetUrl, tmpAssetHeight, tmpAssetWidth,
            null, tmpStickerId, tmpAssetName, tmpAppId, tmpSiteName, tmpSiteTitle, tmpSiteDescription, tmpSiteImage, tmpSharedUserId,
            tmpSharedUserFullName, tmpSharedUserIdentityNumber, tmpSharedUserAvatarUrl, tmpSharedUserIsVerified, tmpSharedUserAppId,
            tmpMediaWaveform, tmpQuoteId, tmpQuoteContent, tmpGroupName, tmpMentions, tmpMentionRead, tmpPinTop
        )
        res.add(item)
    }
    return res
}
