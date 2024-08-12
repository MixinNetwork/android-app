package one.mixin.android.db.provider

import android.annotation.SuppressLint
import android.database.Cursor
import android.os.CancellationSignal
import androidx.paging.DataSource
import androidx.room.CoroutinesRoom
import androidx.room.RoomSQLiteQuery
import androidx.room.getQueryDispatcher
import kotlinx.coroutines.withContext
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.TokenDao.Companion.PREFIX_ASSET_ITEM
import one.mixin.android.db.datasource.MixinLimitOffsetDataSource
import one.mixin.android.db.datasource.NoCountLimitOffsetDataSource
import one.mixin.android.fts.FtsDataSource
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.fts.rawSearch
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.SearchMessageDetailItem
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.SafeCollectible
import one.mixin.android.vo.safe.TokenItem

@SuppressLint("RestrictedApi")
class DataProvider {
    companion object {
        fun observeConversations(database: MixinDatabase) =
            object : DataSource.Factory<Int, ConversationItem>() {
                override fun create(): DataSource<Int, ConversationItem> {
                    val sql = ConversationDao.PREFIX_CONVERSATION_ITEM
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

        fun observeConversationsByCircleId(
            circleId: String,
            database: MixinDatabase,
        ) =
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
                        mm.mentions AS mentions, ou.membership AS membership
                        FROM circle_conversations cc
                        INNER JOIN conversations c ON cc.conversation_id = c.conversation_id
                        INNER JOIN circles ci ON ci.circle_id = cc.circle_id
                        INNER JOIN users ou ON ou.user_id = c.owner_id
                        LEFT JOIN messages m ON c.last_message_id = m.id
                        LEFT JOIN message_mentions mm ON mm.message_id = m.id
                        LEFT JOIN users mu ON mu.user_id = m.user_id
                        LEFT JOIN users pu ON pu.user_id = m.participant_id 
                        """
                    val countStatement =
                        RoomSQLiteQuery.acquire(
                            """
                     SELECT count(1) FROM circle_conversations cc
                     INNER JOIN circles ci ON ci.circle_id = cc.circle_id
                     INNER JOIN conversations c ON cc.conversation_id = c.conversation_id
                     INNER JOIN users ou ON ou.user_id = c.owner_id
                     WHERE c.category IS NOT NULL AND cc.circle_id = '$circleId'
                    """,
                            0,
                        )
                    val offsetStatement =
                        RoomSQLiteQuery.acquire(
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
        suspend fun fuzzySearchToken(
            name: String?,
            symbol: String?,
            db: MixinDatabase,
            cancellationSignal: CancellationSignal,
        ): List<TokenItem> {
            val _sql =
                """
            $PREFIX_ASSET_ITEM
            WHERE ae.balance > 0 
            AND (a1.symbol LIKE '%' || ? || '%'  ESCAPE '\' OR a1.name LIKE '%' || ? || '%'  ESCAPE '\')
            ORDER BY 
            a1.symbol = ? COLLATE NOCASE OR a1.name = ? COLLATE NOCASE DESC,
            a1.price_usd*ae.balance DESC
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
                callableTokenItem(db, _statement, cancellationSignal),
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
        suspend fun fuzzySearchBots(
            username: String?,
            identityNumber: String?,
            id: String?,
            db: MixinDatabase,
            cancellationSignal: CancellationSignal,
        ): List<User> {
            val _sql = """
        SELECT * FROM users
        WHERE app_id IS NOT NULL 
        AND user_id != ? 
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
                db,
                false,
                cancellationSignal,
                callableUser(db, _statement, cancellationSignal),
            )
        }

        @Suppress("LocalVariableName", "JoinDeclarationAndAssignment")
        suspend fun fuzzyInscription(
            keyword: String?,
            db: MixinDatabase,
            cancellationSignal: CancellationSignal,
        ): List<SafeCollectible> {
            val _sql = """
            SELECT `i`.`collection_hash`, `i`.`inscription_hash`, `i`.`sequence`, `i`.`content_type`, `i`.`content_url`, `ic`.`collection_hash`, `ic`.`name`, `ic`.`icon_url` FROM outputs o 
            LEFT JOIN inscription_items i ON i.inscription_hash == o.inscription_hash
            LEFT JOIN inscription_collections ic on ic.collection_hash = i.collection_hash
            WHERE i.inscription_hash IS NOT NULL AND o.state = 'unspent' AND (`ic`.name LIKE '%' || ? || '%'  ESCAPE '\') 
            """
            val _statement = RoomSQLiteQuery.acquire(_sql, 1)
            val _argIndex = 1
            if (keyword == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, keyword)
            }
            return CoroutinesRoom.execute(
                db,
                false,
                cancellationSignal,
                callableSafeInscription(db, _statement, cancellationSignal),
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
        c.pin_time AS pinTime, ou.membership AS membership 
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

        suspend fun fuzzySearchMessage(
            ftsDatabase: FtsDatabase,
            query: String,
            db: MixinDatabase,
            cancellationSignal: CancellationSignal,
        ): List<SearchMessageItem> =
            withContext(db.getQueryDispatcher()) {
                val result = ftsDatabase.rawSearch(query, cancellationSignal)
                val sql = """
                SELECT m.conversation_id AS conversationId, c.icon_url AS conversationAvatarUrl,
                c.name AS conversationName, c.category AS conversationCategory, 0 as messageCount,
                u.user_id AS userId, u.avatar_url AS userAvatarUrl, u.full_name AS userFullName
                FROM messages m
                INNER JOIN conversations c ON c.conversation_id = m.conversation_id
				INNER JOIN users u ON c.owner_id = u.user_id
                WHERE m.id IN (*)
                ORDER BY m.created_at DESC
            """
                if (result.isEmpty()) return@withContext emptyList()
                val ids =
                    result.joinToString(
                        prefix = "'",
                        postfix = "'",
                        separator = "', '",
                    ) { it.messageId }
                val statement = RoomSQLiteQuery.acquire(sql.replace("*", ids), 0)
                return@withContext CoroutinesRoom.execute(
                    db,
                    true,
                    cancellationSignal,
                    callableSearchMessageItem(db, statement, cancellationSignal),
                ).map {
                    val obtained = result.find { item -> item.conversationId == it.conversationId }
                    if (obtained != null) {
                        it.messageCount = obtained.messageCount
                    }
                    it
                }
            }

        fun fuzzySearchMessageDetail(
            ftsDatabase: FtsDatabase,
            query: String,
            conversationId: String,
            database: MixinDatabase,
            cancellationSignal: CancellationSignal,
        ) =
            object : DataSource.Factory<Int, SearchMessageDetailItem>() {
                override fun create(): DataSource<Int, SearchMessageDetailItem> {
                    return FtsDataSource(ftsDatabase, database, query, conversationId, cancellationSignal)
                }
            }

        fun getPinMessages(
            database: MixinDatabase,
            conversationId: String,
            count: Int,
        ) =
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
                        su.avatar_url AS sharedUserAvatarUrl, su.is_verified AS sharedUserIsVerified, su.app_id AS sharedUserAppId, su.membership AS sharedMembership, mm.mentions AS mentions, mm.has_read as mentionRead,
                        c.name AS groupName
                        FROM pin_messages pm
                        LEFT JOIN messages m ON m.id = pm.message_id
                        LEFT JOIN users u ON m.user_id = u.user_id
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
                        override fun convertRows(cursor: Cursor?): List<ChatHistoryMessageItem> {
                            return convertChatHistoryMessageItem(cursor)
                        }
                    }
                }
            }
    }
}
