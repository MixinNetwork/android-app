package one.mixin.android.db.provider

import android.annotation.SuppressLint
import android.database.Cursor
import android.os.CancellationSignal
import androidx.paging.DataSource
import androidx.room.CoroutinesRoom
import androidx.room.RoomSQLiteQuery
import one.mixin.android.db.MixinDatabase
import one.mixin.android.ui.search.CancellationLimitOffsetDataSource
import one.mixin.android.util.chat.FastLimitOffsetDataSource
import one.mixin.android.util.chat.MixinLimitOffsetDataSource
import one.mixin.android.util.chat.NoCountLimitOffsetDataSource
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.SearchMessageDetailItem
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User

@SuppressLint("RestrictedApi")
class DataProvider {
    companion object {

        fun getMessages(database: MixinDatabase, conversationId: String) =
            object : DataSource.Factory<Int, MessageItem>() {
                private val fastCountCallback = fun(): Int {
                    val conversationExtDao = database.conversationExtDao()
                    val count = conversationExtDao.getMessageCountByConversationId(conversationId)
                    if (count != null) {
                        return count
                    }
                    conversationExtDao.refreshCountByConversationId(conversationId)
                    return (conversationExtDao.getMessageCountByConversationId(conversationId) ?: 0)
                }
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
                        pm.message_id IS NOT NULL as isPin, c.name AS groupName, em.expire_in AS expireIn  
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
                        LEFT JOIN expired_messages em ON m.id = em.message_id
                        """
                    val offsetStatement = RoomSQLiteQuery.acquire("SELECT m.id FROM messages m INNER JOIN users u ON m.user_id = u.user_id WHERE conversation_id = ? ORDER BY m.created_at DESC LIMIT ? OFFSET ?", 3).apply {
                        bindString(1, conversationId)
                    }
                    val querySqlGenerator = fun(ids: String): RoomSQLiteQuery {
                        return RoomSQLiteQuery.acquire("$sql WHERE m.id IN ($ids) ORDER BY m.created_at DESC", 0)
                    }
                    return object : FastLimitOffsetDataSource<MessageItem, String>(database, offsetStatement, fastCountCallback, querySqlGenerator) {
                        override fun convertRows(cursor: Cursor?): MutableList<MessageItem> {
                            return convertToMessageItems(cursor)
                        }

                        override fun getUniqueId(cursor: Cursor): String {
                            return cursor.getString(0)
                        }
                    }
                }
            }

        fun observeConversations(database: MixinDatabase) =
            object : DataSource.Factory<Int, ConversationItem>() {
                override fun create(): DataSource<Int, ConversationItem> {
                    val sql =
                        """
                    SELECT c.conversation_id AS conversationId, c.icon_url AS groupIconUrl, c.category AS category,
                    c.name AS groupName, c.status AS status, c.last_read_message_id AS lastReadMessageId,
                    c.unseen_message_count AS unseenMessageCount, c.owner_id AS ownerId, c.pin_time AS pinTime, c.mute_until AS muteUntil,
                    ou.avatar_url AS avatarUrl, ou.full_name AS name, ou.is_verified AS ownerVerified,
                    ou.mute_until AS ownerMuteUntil, ou.app_id AS appId,
                    m.content AS content, m.category AS contentType, m.created_at AS createdAt,
                    m.user_id AS senderId, m.action AS actionName, m.status AS messageStatus,
                    mu.full_name AS senderFullName,
                    pu.full_name AS participantFullName, pu.user_id AS participantUserId,
                    (SELECT count(1) FROM message_mentions me WHERE me.conversation_id = c.conversation_id AND me.has_read = 0) as mentionCount,  
                    mm.mentions AS mentions 
                    FROM conversations c
                    INNER JOIN users ou ON ou.user_id = c.owner_id
                    LEFT JOIN messages m ON c.last_message_id = m.id
                    LEFT JOIN message_mentions mm ON mm.message_id = m.id
                    LEFT JOIN users mu ON mu.user_id = m.user_id
                    LEFT JOIN users pu ON pu.user_id = m.participant_id 
                    """
                    val countStatement = RoomSQLiteQuery.acquire("SELECT count(1) FROM conversations c INNER JOIN users ou ON ou.user_id = c.owner_id WHERE c.category IS NOT NULL", 0)
                    val offsetStatement = RoomSQLiteQuery.acquire("SELECT c.rowid FROM conversations c INNER JOIN users ou ON ou.user_id = c.owner_id ORDER BY c.pin_time DESC, c.last_message_created_at DESC LIMIT ? OFFSET ?", 2)
                    val querySqlGenerator = fun(ids: String): RoomSQLiteQuery {
                        return RoomSQLiteQuery.acquire("$sql WHERE c.rowid IN ($ids) ORDER BY c.pin_time DESC, c.last_message_created_at DESC", 0)
                    }
                    return object : MixinLimitOffsetDataSource<ConversationItem>(database, countStatement, offsetStatement, querySqlGenerator, arrayOf("message_mentions", "conversations", "users")) {
                        override fun convertRows(cursor: Cursor?): List<ConversationItem> {
                            return convertToConversationItems(cursor)
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
                        ou.mute_until AS ownerMuteUntil, ou.app_id AS appId,
                        m.content AS content, m.category AS contentType, m.created_at AS createdAt,
                        m.user_id AS senderId, m.`action` AS actionName, m.status AS messageStatus,
                        mu.full_name AS senderFullName,
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
                        LEFT JOIN users pu ON pu.user_id = m.participant_id 
                        """
                    val countStatement = RoomSQLiteQuery.acquire(
                        """
                     SELECT count(1) FROM circle_conversations cc
                     INNER JOIN circles ci ON ci.circle_id = cc.circle_id
                     INNER JOIN conversations c ON cc.conversation_id = c.conversation_id
                     INNER JOIN users ou ON ou.user_id = c.owner_id
                     WHERE c.category IS NOT NULL AND cc.circle_id = '$circleId'
                    """,
                        0,
                    )
                    val offsetStatement = RoomSQLiteQuery.acquire(
                        """
                        SELECT cc.rowid FROM circle_conversations cc
                        INNER JOIN conversations c ON cc.conversation_id = c.conversation_id
                        INNER JOIN circles ci ON ci.circle_id = cc.circle_id
                        INNER JOIN users ou ON ou.user_id = c.owner_id
                        LEFT JOIN messages m ON c.last_message_id = m.id
                        WHERE c.category IS NOT NULL AND cc.circle_id = '$circleId'
                        ORDER BY cc.pin_time DESC, 
                        CASE 
                            WHEN m.created_at is NULL THEN c.created_at
                            ELSE m.created_at 
                        END 
                        DESC
                        LIMIT ? OFFSET ?
                    """,
                        2,
                    )
                    val querySqlGenerator = fun(ids: String): RoomSQLiteQuery {
                        return RoomSQLiteQuery.acquire(
                            """
                            $sql WHERE cc.rowid IN ($ids)
                            ORDER BY cc.pin_time DESC, 
                            CASE 
                                WHEN m.created_at is NULL THEN c.created_at
                                ELSE m.created_at 
                            END 
                            DESC
                        """,
                            0,
                        )
                    }
                    return object : MixinLimitOffsetDataSource<ConversationItem>(database, countStatement, offsetStatement, querySqlGenerator, arrayOf("message_mentions", "circle_conversations", "conversations", "circles", "users")) {
                        override fun convertRows(cursor: Cursor?): List<ConversationItem> {
                            return convertToConversationItems(cursor)
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
                """SELECT a1.asset_id AS assetId, a1.symbol, a1.name, a1.icon_url AS iconUrl, a1.balance, a1.destination AS destination, a1.tag AS tag, a1.price_btc AS priceBtc, a1.price_usd AS priceUsd, a1.chain_id AS chainId, a1.change_usd AS changeUsd, a1.change_btc AS changeBtc, ae.hidden, a2.price_usd as chainPriceUsd,a1.confirmations, a1.reserve as reserve, a2.icon_url AS chainIconUrl, a2.symbol as chainSymbol, a2.name as chainName, a1.asset_key AS assetKey, a1.deposit_entries AS depositEntries  
        FROM assets a1 
        LEFT JOIN assets a2 ON a1.chain_id = a2.asset_id 
        LEFT JOIN assets_extra ae ON ae.asset_id = a1.asset_id  
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
                db,
                false,
                cancellationSignal,
                callableAssetItem(db, _statement, cancellationSignal),
            )
        }

        @Suppress("LocalVariableName", "JoinDeclarationAndAssignment")
        suspend fun fuzzySearchUser(
            username: String?,
            identityNumber: String?,
            phone: String?,
            id: String?,
            db: MixinDatabase,
            cancellationSignal: CancellationSignal,
        ): List<User> {
            val _sql = """
        SELECT * FROM users 
        WHERE user_id != ? 
        AND relationship = 'FRIEND' 
        AND identity_number != '0'
        AND (full_name LIKE '%' || ? || '%'  ESCAPE '\' OR identity_number like '%' || ? || '%'  ESCAPE '\' OR phone like '%' || ? || '%'  ESCAPE '\')
        ORDER BY 
            full_name = ? COLLATE NOCASE OR identity_number = ? COLLATE NOCASE DESC
        """
            val _statement = RoomSQLiteQuery.acquire(_sql, 6)
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
            if (phone == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, phone)
            }
            _argIndex = 5
            if (username == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, username)
            }
            _argIndex = 6
            if (identityNumber == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, identityNumber)
            }
            return CoroutinesRoom.execute(
                db,
                false,
                cancellationSignal,
                callableUser(db, _statement, cancellationSignal),
            )
        }

        @Suppress("LocalVariableName", "JoinDeclarationAndAssignment")
        suspend fun fuzzySearchChat(
            query: String?,
            db: MixinDatabase,
            cancellationSignal: CancellationSignal,
        ): List<ChatMinimal> {
            val _sql = """
        SELECT c.conversation_id AS conversationId, c.icon_url AS groupIconUrl, c.category AS category, c.name AS groupName,
        ou.identity_number AS ownerIdentityNumber, c.owner_id AS userId, ou.full_name AS fullName, ou.avatar_url AS avatarUrl,
        ou.is_verified AS isVerified, ou.app_id AS appId, ou.mute_until AS ownerMuteUntil, c.mute_until AS muteUntil,
        c.pin_time AS pinTime 
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
                db,
                false,
                cancellationSignal,
                callableChatMinimal(db, _statement, cancellationSignal),
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
                db,
                true,
                cancellationSignal,
                callableSearchMessageItem(db, _statement, cancellationSignal),
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
                            WHERE m.id IN (SELECT message_id FROM messages_fts4 WHERE messages_fts4 MATCH ?) 
                            AND m.conversation_id = ?
                            ORDER BY m.created_at DESC
                        """
                    val countSql =
                        """
                            SELECT count(1) FROM messages m 
                            INNER JOIN users u ON m.user_id = u.user_id 
                            WHERE m.id IN (SELECT message_id FROM messages_fts4 WHERE messages_fts4 MATCH ?) 
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

        fun getPinMessages(database: MixinDatabase, conversationId: String, count: Int) =
            object : DataSource.Factory<Int, ChatHistoryMessageItem>() {
                override fun create(): DataSource<Int, ChatHistoryMessageItem> {
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
                        c.name AS groupName
                        FROM pin_messages pm
                        INNER JOIN messages m ON m.id = pm.message_id
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
                        ORDER BY m.created_at ASC
                        """
                    val statement = RoomSQLiteQuery.acquire(sql, 1)
                    statement.bindString(1, conversationId)
                    return object : NoCountLimitOffsetDataSource<ChatHistoryMessageItem>(database, statement, count, "pin_messages", "messages", "users", "snapshots", "assets", "stickers", "hyperlinks", "conversations", "message_mentions") {
                        @Suppress("LocalVariableName")
                        override fun convertRows(cursor: Cursor?): MutableList<ChatHistoryMessageItem> {
                            val _cursorIndexOfMessageId = 0
                            val _cursorIndexOfConversationId = 1
                            val _cursorIndexOfUserId = 2
                            val _cursorIndexOfUserFullName = 3
                            val _cursorIndexOfUserIdentityNumber = 4
                            val _cursorIndexOfAppId = 5
                            val _cursorIndexOfType = 6
                            val _cursorIndexOfContent = 7
                            val _cursorIndexOfCreatedAt = 8
                            val _cursorIndexOfMediaStatus = 10
                            val _cursorIndexOfMediaWaveform = 11
                            val _cursorIndexOfMediaName = 12
                            val _cursorIndexOfMediaMimeType = 13
                            val _cursorIndexOfMediaSize = 14
                            val _cursorIndexOfMediaWidth = 15
                            val _cursorIndexOfMediaHeight = 16
                            val _cursorIndexOfThumbImage = 17
                            val _cursorIndexOfThumbUrl = 18
                            val _cursorIndexOfMediaUrl = 19
                            val _cursorIndexOfMediaDuration = 20
                            val _cursorIndexOfQuoteId = 21
                            val _cursorIndexOfQuoteContent = 22
                            val _cursorIndexOfAssetUrl = 33
                            val _cursorIndexOfAssetWidth = 34
                            val _cursorIndexOfAssetHeight = 35
                            val _cursorIndexOfAssetType = 38
                            val _cursorIndexOfSharedUserId = 43
                            val _cursorIndexOfSharedUserFullName = 44
                            val _cursorIndexOfSharedUserIdentityNumber = 45
                            val _cursorIndexOfSharedUserAvatarUrl = 46
                            val _cursorIndexOfSharedUserIsVerified = 47
                            val _cursorIndexOfSharedUserAppId = 48
                            val _cursorIndexOfMentions = 49
                            val _res: MutableList<ChatHistoryMessageItem> = ArrayList(
                                cursor!!.count,
                            )
                            while (cursor.moveToNext()) {
                                val _item: ChatHistoryMessageItem
                                val _tmpMessageId: String? = if (cursor.isNull(_cursorIndexOfMessageId)) {
                                    null
                                } else {
                                    cursor.getString(_cursorIndexOfMessageId)
                                }
                                val _tmpConversationId: String? =
                                    if (cursor.isNull(_cursorIndexOfConversationId)) {
                                        null
                                    } else {
                                        cursor.getString(_cursorIndexOfConversationId)
                                    }
                                val _tmpUserId: String? = if (cursor.isNull(_cursorIndexOfUserId)) {
                                    null
                                } else {
                                    cursor.getString(_cursorIndexOfUserId)
                                }
                                val _tmpUserFullName: String? =
                                    if (cursor.isNull(_cursorIndexOfUserFullName)) {
                                        null
                                    } else {
                                        cursor.getString(_cursorIndexOfUserFullName)
                                    }
                                val _tmpUserIdentityNumber: String? =
                                    if (cursor.isNull(_cursorIndexOfUserIdentityNumber)) {
                                        null
                                    } else {
                                        cursor.getString(_cursorIndexOfUserIdentityNumber)
                                    }
                                val _tmpAppId: String? = if (cursor.isNull(_cursorIndexOfAppId)) {
                                    null
                                } else {
                                    cursor.getString(_cursorIndexOfAppId)
                                }
                                val _tmpType: String? = if (cursor.isNull(_cursorIndexOfType)) {
                                    null
                                } else {
                                    cursor.getString(_cursorIndexOfType)
                                }
                                val _tmpContent: String? = if (cursor.isNull(_cursorIndexOfContent)) {
                                    null
                                } else {
                                    cursor.getString(_cursorIndexOfContent)
                                }
                                val _tmpCreatedAt: String? =
                                    if (cursor.isNull(_cursorIndexOfCreatedAt)) {
                                        null
                                    } else {
                                        cursor.getString(_cursorIndexOfCreatedAt)
                                    }
                                val _tmpMediaStatus: String? =
                                    if (cursor.isNull(_cursorIndexOfMediaStatus)) {
                                        null
                                    } else {
                                        cursor.getString(_cursorIndexOfMediaStatus)
                                    }
                                val _tmpMediaWaveform: ByteArray? =
                                    if (cursor.isNull(_cursorIndexOfMediaWaveform)) {
                                        null
                                    } else {
                                        cursor.getBlob(_cursorIndexOfMediaWaveform)
                                    }
                                val _tmpMediaName: String? =
                                    if (cursor.isNull(_cursorIndexOfMediaName)) {
                                        null
                                    } else {
                                        cursor.getString(_cursorIndexOfMediaName)
                                    }
                                val _tmpMediaMimeType: String? =
                                    if (cursor.isNull(_cursorIndexOfMediaMimeType)) {
                                        null
                                    } else {
                                        cursor.getString(_cursorIndexOfMediaMimeType)
                                    }
                                val _tmpMediaSize: Long? = if (cursor.isNull(_cursorIndexOfMediaSize)) {
                                    null
                                } else {
                                    cursor.getLong(_cursorIndexOfMediaSize)
                                }
                                val _tmpMediaWidth: Int? =
                                    if (cursor.isNull(_cursorIndexOfMediaWidth)) {
                                        null
                                    } else {
                                        cursor.getInt(_cursorIndexOfMediaWidth)
                                    }
                                val _tmpMediaHeight: Int? =
                                    if (cursor.isNull(_cursorIndexOfMediaHeight)) {
                                        null
                                    } else {
                                        cursor.getInt(_cursorIndexOfMediaHeight)
                                    }
                                val _tmpThumbImage: String? =
                                    if (cursor.isNull(_cursorIndexOfThumbImage)) {
                                        null
                                    } else {
                                        cursor.getString(_cursorIndexOfThumbImage)
                                    }
                                val _tmpThumbUrl: String? = if (cursor.isNull(_cursorIndexOfThumbUrl)) {
                                    null
                                } else {
                                    cursor.getString(_cursorIndexOfThumbUrl)
                                }
                                val _tmpMediaUrl: String? = if (cursor.isNull(_cursorIndexOfMediaUrl)) {
                                    null
                                } else {
                                    cursor.getString(_cursorIndexOfMediaUrl)
                                }
                                val _tmpMediaDuration: String? =
                                    if (cursor.isNull(_cursorIndexOfMediaDuration)) {
                                        null
                                    } else {
                                        cursor.getString(_cursorIndexOfMediaDuration)
                                    }
                                val _tmpQuoteId: String? = if (cursor.isNull(_cursorIndexOfQuoteId)) {
                                    null
                                } else {
                                    cursor.getString(_cursorIndexOfQuoteId)
                                }
                                val _tmpQuoteContent: String? =
                                    if (cursor.isNull(_cursorIndexOfQuoteContent)) {
                                        null
                                    } else {
                                        cursor.getString(_cursorIndexOfQuoteContent)
                                    }
                                val _tmpAssetUrl: String? = if (cursor.isNull(_cursorIndexOfAssetUrl)) {
                                    null
                                } else {
                                    cursor.getString(_cursorIndexOfAssetUrl)
                                }
                                val _tmpAssetWidth: Int? =
                                    if (cursor.isNull(_cursorIndexOfAssetWidth)) {
                                        null
                                    } else {
                                        cursor.getInt(_cursorIndexOfAssetWidth)
                                    }
                                val _tmpAssetHeight: Int? =
                                    if (cursor.isNull(_cursorIndexOfAssetHeight)) {
                                        null
                                    } else {
                                        cursor.getInt(_cursorIndexOfAssetHeight)
                                    }
                                val _tmpAssetType: String? =
                                    if (cursor.isNull(_cursorIndexOfAssetType)) {
                                        null
                                    } else {
                                        cursor.getString(_cursorIndexOfAssetType)
                                    }
                                val _tmpSharedUserId: String? =
                                    if (cursor.isNull(_cursorIndexOfSharedUserId)) {
                                        null
                                    } else {
                                        cursor.getString(_cursorIndexOfSharedUserId)
                                    }
                                val _tmpSharedUserFullName: String? =
                                    if (cursor.isNull(_cursorIndexOfSharedUserFullName)) {
                                        null
                                    } else {
                                        cursor.getString(_cursorIndexOfSharedUserFullName)
                                    }
                                val _tmpSharedUserIdentityNumber: String? =
                                    if (cursor.isNull(_cursorIndexOfSharedUserIdentityNumber)) {
                                        null
                                    } else {
                                        cursor.getString(_cursorIndexOfSharedUserIdentityNumber)
                                    }
                                val _tmpSharedUserAvatarUrl: String? =
                                    if (cursor.isNull(_cursorIndexOfSharedUserAvatarUrl)) {
                                        null
                                    } else {
                                        cursor.getString(_cursorIndexOfSharedUserAvatarUrl)
                                    }
                                val _tmpSharedUserIsVerified: Boolean?
                                val _tmp: Int? =
                                    if (cursor.isNull(_cursorIndexOfSharedUserIsVerified)) {
                                        null
                                    } else {
                                        cursor.getInt(_cursorIndexOfSharedUserIsVerified)
                                    }
                                _tmpSharedUserIsVerified = if (_tmp == null) null else _tmp != 0
                                val _tmpSharedUserAppId: String? =
                                    if (cursor.isNull(_cursorIndexOfSharedUserAppId)) {
                                        null
                                    } else {
                                        cursor.getString(_cursorIndexOfSharedUserAppId)
                                    }
                                val _tmpMentions: String? = if (cursor.isNull(_cursorIndexOfMentions)) {
                                    null
                                } else {
                                    cursor.getString(_cursorIndexOfMentions)
                                }
                                _item = ChatHistoryMessageItem(
                                    null,
                                    _tmpConversationId,
                                    _tmpMessageId!!,
                                    _tmpUserId,
                                    _tmpUserFullName,
                                    _tmpUserIdentityNumber,
                                    _tmpType!!,
                                    _tmpAppId,
                                    _tmpContent,
                                    _tmpCreatedAt!!,
                                    _tmpMediaStatus,
                                    _tmpMediaName,
                                    _tmpMediaMimeType,
                                    _tmpMediaSize,
                                    _tmpThumbUrl,
                                    _tmpMediaWidth,
                                    _tmpMediaHeight,
                                    _tmpThumbImage,
                                    _tmpMediaUrl,
                                    _tmpMediaDuration,
                                    _tmpMediaWaveform,
                                    _tmpAssetWidth,
                                    _tmpAssetHeight,
                                    _tmpAssetUrl,
                                    _tmpAssetType,
                                    _tmpSharedUserId,
                                    _tmpSharedUserFullName,
                                    _tmpSharedUserAvatarUrl,
                                    _tmpSharedUserIdentityNumber,
                                    _tmpSharedUserIsVerified,
                                    _tmpSharedUserAppId,
                                    _tmpQuoteId,
                                    _tmpQuoteContent,
                                    _tmpMentions,
                                )
                                _res.add(_item)
                            }
                            return _res
                        }
                    }
                }
            }
    }

    private class CancellationMessageDetailItemLimitOffsetDataSource(
        database: MixinDatabase,
        statement: RoomSQLiteQuery,
        countStatement: RoomSQLiteQuery,
        cancellationSignal: CancellationSignal,
    ) : CancellationLimitOffsetDataSource<SearchMessageDetailItem>(database, statement, countStatement, cancellationSignal, true, "pin_messages", "messages", "users", "snapshots", "assets", "stickers", "hyperlinks", "conversations", "message_mentions") {
        override fun convertRows(cursor: Cursor?): MutableList<SearchMessageDetailItem> {
            return convertToSearchMessageDetailItem(cursor)
        }
    }
}
