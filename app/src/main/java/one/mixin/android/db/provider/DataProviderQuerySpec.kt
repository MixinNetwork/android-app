package one.mixin.android.db.provider

import android.os.CancellationSignal
import androidx.paging.PagingSource
import one.mixin.android.codegen.annotation.GeneratedLimitOffsetPagingSourceQuery
import one.mixin.android.codegen.annotation.GeneratedNoCountPagingSourceQuery
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.TokenDao.Companion.PREFIX_ASSET_ITEM
import one.mixin.android.codegen.annotation.GeneratedQuery
import one.mixin.android.codegen.annotation.GeneratedQueryProvider
import one.mixin.android.codegen.annotation.GeneratedRawCursorQuery
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.SearchBot
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.safe.SafeCollectible
import one.mixin.android.vo.safe.TokenItem

private const val FUZZY_SEARCH_TOKEN_SQL =
    PREFIX_ASSET_ITEM +
        """
        WHERE ae.balance > 0
        AND (a1.symbol LIKE '%' || ? || '%'  ESCAPE '\' OR a1.name LIKE '%' || ? || '%'  ESCAPE '\')
        ORDER BY
        a1.symbol = ? COLLATE NOCASE OR a1.name = ? COLLATE NOCASE DESC,
        a1.price_usd*ae.balance DESC
        """

private const val FUZZY_SEARCH_USER_SQL =
    """
    SELECT * FROM users
    WHERE user_id != ?
    AND relationship = 'FRIEND'
    AND identity_number != '0'
    AND (full_name LIKE '%' || ? || '%'  ESCAPE '\' OR identity_number like '%' || ? || '%'  ESCAPE '\' OR phone like '%' || ? || '%'  ESCAPE '\')
    ORDER BY
        full_name = ? COLLATE NOCASE OR identity_number = ? COLLATE NOCASE DESC
    """

private const val FUZZY_SEARCH_BOTS_SQL =
    """
    SELECT * FROM users
    WHERE app_id IS NOT NULL
    AND user_id != ?
    AND relationship = 'FRIEND'
    AND identity_number != '0'
    AND (full_name LIKE '%' || ? || '%'  ESCAPE '\' OR identity_number like '%' || ? || '%'  ESCAPE '\')
    ORDER BY
        full_name = ? COLLATE NOCASE OR identity_number = ? COLLATE NOCASE DESC
    """

private const val FUZZY_MARKETS_SQL =
    """
    SELECT *
    FROM markets
    WHERE symbol LIKE '%' || ? || '%' ESCAPE '\'
       OR name LIKE '%' || ? || '%' ESCAPE '\'
    ORDER BY
      CASE
        WHEN symbol LIKE ? || '%' THEN 1
        WHEN name LIKE ? || '%' THEN 1
        ELSE 2
      END,
      CAST(market_cap_rank AS INTEGER) ASC,
      symbol ASC,
      name ASC;
    """

private const val FUZZY_INSCRIPTION_SQL =
    """
    SELECT `i`.`collection_hash`, `i`.`inscription_hash`, `i`.`sequence`, `i`.`content_type`, `i`.`content_url`, `ic`.`collection_hash`, `ic`.`name`, `ic`.`icon_url` FROM outputs o
    LEFT JOIN inscription_items i ON i.inscription_hash == o.inscription_hash
    LEFT JOIN inscription_collections ic on ic.collection_hash = i.collection_hash
    WHERE i.inscription_hash IS NOT NULL AND o.state = 'unspent' AND (`ic`.name LIKE '%' || ? || '%'  ESCAPE '\')
    """

private const val FUZZY_SEARCH_CHAT_SQL =
    """
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

private const val FUZZY_SEARCH_MESSAGE_SQL =
    """
    SELECT m.conversation_id AS conversationId, c.icon_url AS conversationAvatarUrl,
    c.name AS conversationName, c.category AS conversationCategory, 0 as messageCount,
    u.user_id AS userId, u.app_id AS appId, u.avatar_url AS userAvatarUrl, u.identity_number AS userIdentityNumber,
    u.full_name AS userFullName, u.is_verified as isVerified, u.membership AS membership
    FROM messages m
    INNER JOIN conversations c ON c.conversation_id = m.conversation_id
    INNER JOIN users u ON c.owner_id = u.user_id
    WHERE m.id IN ({{ids}})
    ORDER BY m.created_at DESC
    """

private const val OBSERVE_CONVERSATIONS_COUNT_SQL =
    "SELECT count(1) FROM conversations c INNER JOIN users ou ON ou.user_id = c.owner_id WHERE c.category IS NOT NULL"

private const val OBSERVE_CONVERSATIONS_OFFSET_SQL =
    "SELECT c.rowid FROM conversations c INNER JOIN users ou ON ou.user_id = c.owner_id ORDER BY c.pin_time DESC, c.last_message_created_at DESC LIMIT ? OFFSET ?"

private const val OBSERVE_CONVERSATIONS_QUERY_SQL =
    ConversationDao.PREFIX_CONVERSATION_ITEM +
        " WHERE c.rowid IN ({{ids}}) ORDER BY c.pin_time DESC, c.last_message_created_at DESC"

private const val OBSERVE_CONVERSATIONS_BY_CIRCLE_PREFIX_SQL =
    """
    SELECT c.conversation_id AS conversationId, c.icon_url AS groupIconUrl, c.category AS category,
    c.name AS groupName, c.status AS status, c.last_read_message_id AS lastReadMessageId,
    c.unseen_message_count AS unseenMessageCount, c.owner_id AS ownerId, cc.pin_time AS pinTime, c.mute_until AS muteUntil,
    ou.avatar_url AS avatarUrl, ou.full_name AS name, ou.is_verified AS ownerVerified, ou.identity_number AS ownerIdentityNumber,
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

private const val OBSERVE_CONVERSATIONS_BY_CIRCLE_COUNT_SQL =
    """
    SELECT count(1) FROM circle_conversations cc
    INNER JOIN circles ci ON ci.circle_id = cc.circle_id
    INNER JOIN conversations c ON cc.conversation_id = c.conversation_id
    INNER JOIN users ou ON ou.user_id = c.owner_id
    WHERE c.category IS NOT NULL AND cc.circle_id = '{{circleId}}'
    """

private const val OBSERVE_CONVERSATIONS_BY_CIRCLE_OFFSET_SQL =
    """
    SELECT cc.rowid FROM circle_conversations cc
    INNER JOIN conversations c ON cc.conversation_id = c.conversation_id
    INNER JOIN circles ci ON ci.circle_id = cc.circle_id
    INNER JOIN users ou ON ou.user_id = c.owner_id
    LEFT JOIN messages m ON c.last_message_id = m.id
    WHERE c.category IS NOT NULL AND cc.circle_id = '{{circleId}}'
    ORDER BY cc.pin_time DESC,
    CASE
        WHEN m.created_at is NULL THEN c.created_at
        ELSE m.created_at
    END
    DESC
    LIMIT ? OFFSET ?
    """

private const val OBSERVE_CONVERSATIONS_BY_CIRCLE_QUERY_SQL =
    OBSERVE_CONVERSATIONS_BY_CIRCLE_PREFIX_SQL +
        """
        WHERE cc.rowid IN ({{ids}})
        ORDER BY cc.pin_time DESC,
        CASE
            WHEN m.created_at is NULL THEN c.created_at
            ELSE m.created_at
        END
        DESC
        """

private const val PIN_MESSAGES_SQL =
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
    su.avatar_url AS sharedUserAvatarUrl, su.is_verified AS sharedUserIsVerified, su.app_id AS sharedUserAppId, mm.mentions AS mentions,
    su.membership AS sharedMembership, u.membership AS membership
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

@GeneratedQueryProvider(generatedName = "DataProviderGenerated")
interface DataProviderQuerySpec {
    @GeneratedLimitOffsetPagingSourceQuery(
        countSql = OBSERVE_CONVERSATIONS_COUNT_SQL,
        offsetSql = OBSERVE_CONVERSATIONS_OFFSET_SQL,
        querySql = OBSERVE_CONVERSATIONS_QUERY_SQL,
        tables = ["message_mentions", "conversations", "users"],
        converter = "convertToConversationItems",
    )
    fun observeConversations(database: MixinDatabase): PagingSource<Int, ConversationItem>

    @GeneratedLimitOffsetPagingSourceQuery(
        countSql = OBSERVE_CONVERSATIONS_BY_CIRCLE_COUNT_SQL,
        offsetSql = OBSERVE_CONVERSATIONS_BY_CIRCLE_OFFSET_SQL,
        querySql = OBSERVE_CONVERSATIONS_BY_CIRCLE_QUERY_SQL,
        tables = ["message_mentions", "circle_conversations", "conversations", "circles", "users"],
        converter = "convertToConversationItems",
    )
    fun observeConversationsByCircleId(
        circleId: String,
        database: MixinDatabase,
    ): PagingSource<Int, ConversationItem>

    @GeneratedNoCountPagingSourceQuery(
        sql = PIN_MESSAGES_SQL,
        binds = ["conversationId"],
        count = "count",
        tables = ["pin_messages", "messages", "users", "snapshots", "assets", "stickers", "hyperlinks", "conversations", "message_mentions"],
        converter = "convertChatHistoryMessageItem",
    )
    fun getPinMessages(
        database: MixinDatabase,
        conversationId: String,
        count: Int,
    ): PagingSource<Int, ChatHistoryMessageItem>

    @GeneratedQuery(
        sql = FUZZY_SEARCH_TOKEN_SQL,
        binds = ["symbol", "name", "symbol", "name"],
        callable = "callableTokenItem",
    )
    suspend fun fuzzySearchToken(
        name: String?,
        symbol: String?,
        db: MixinDatabase,
        cancellationSignal: CancellationSignal,
    ): List<TokenItem>

    @GeneratedQuery(
        sql = FUZZY_SEARCH_USER_SQL,
        binds = ["id", "username", "identityNumber", "phone", "username", "identityNumber"],
        callable = "callableUser",
    )
    suspend fun fuzzySearchUser(
        username: String?,
        identityNumber: String?,
        phone: String?,
        id: String?,
        db: MixinDatabase,
        cancellationSignal: CancellationSignal,
    ): List<User>

    @GeneratedQuery(
        sql = FUZZY_SEARCH_BOTS_SQL,
        binds = ["id", "username", "identityNumber", "username", "identityNumber"],
        callable = "callableBot",
    )
    suspend fun fuzzySearchBots(
        username: String?,
        identityNumber: String?,
        id: String?,
        db: MixinDatabase,
        cancellationSignal: CancellationSignal,
    ): List<SearchBot>

    @GeneratedQuery(
        sql = FUZZY_MARKETS_SQL,
        binds = ["keyword", "keyword"],
        callable = "callableMarket",
    )
    suspend fun fuzzyMarkets(
        keyword: String,
        db: MixinDatabase,
        cancellationSignal: CancellationSignal,
    ): List<Market>

    @GeneratedQuery(
        sql = FUZZY_INSCRIPTION_SQL,
        binds = ["keyword"],
        callable = "callableSafeInscription",
    )
    suspend fun fuzzyInscription(
        keyword: String?,
        db: MixinDatabase,
        cancellationSignal: CancellationSignal,
    ): List<SafeCollectible>

    @GeneratedQuery(
        sql = FUZZY_SEARCH_CHAT_SQL,
        binds = ["query", "query", "query", "query", "query", "query"],
        callable = "callableChatMinimal",
    )
    suspend fun fuzzySearchChat(
        query: String?,
        db: MixinDatabase,
        cancellationSignal: CancellationSignal,
    ): List<ChatMinimal>

    @GeneratedRawCursorQuery(
        sql = FUZZY_SEARCH_MESSAGE_SQL,
        binds = [],
        converter = "convertToSearchMessageItems",
    )
    fun fuzzySearchMessageItems(
        db: MixinDatabase,
        ids: String,
    ): List<SearchMessageItem>
}
