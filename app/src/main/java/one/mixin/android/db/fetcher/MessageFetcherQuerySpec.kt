package one.mixin.android.db.fetcher

import one.mixin.android.codegen.annotation.GeneratedQueryProvider
import one.mixin.android.codegen.annotation.GeneratedRawCursorQuery
import one.mixin.android.db.MixinDatabase
import one.mixin.android.vo.MessageItem

private const val MESSAGE_ITEM_SQL =
    """
    SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId,
    u.full_name AS userFullName, u.identity_number AS userIdentityNumber, u.app_id AS appId, m.category AS type,
    m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus, m.media_waveform AS mediaWaveform,
    m.name AS mediaName, m.media_mime_type AS mediaMimeType, m.media_size AS mediaSize, m.media_width AS mediaWidth, m.media_height AS mediaHeight,
    m.thumb_image AS thumbImage, m.thumb_url AS thumbUrl, m.media_url AS mediaUrl, m.media_duration AS mediaDuration, m.quote_message_id as quoteId,
    m.quote_content as quoteContent, m.caption as caption, u.membership AS membership, u1.full_name AS participantFullName, m.action AS actionName, u1.user_id AS participantUserId,
    COALESCE(s.snapshot_id, ss.snapshot_id) AS snapshotId, COALESCE(s.memo, ss.memo) AS snapshotMemo, COALESCE(s.type, ss.type) AS snapshotType, COALESCE(s.amount, ss.amount) AS snapshotAmount,
    COALESCE(a.symbol, t.symbol) AS assetSymbol, COALESCE(s.asset_id, ss.asset_id) AS assetId, COALESCE(a.icon_url, t.icon_url) AS assetIcon, t.collection_hash AS assetCollectionHash,
    st.asset_url AS assetUrl, st.asset_width AS assetWidth, st.asset_height AS assetHeight, st.sticker_id AS stickerId,
    st.name AS assetName, st.asset_type AS assetType, h.site_name AS siteName, h.site_title AS siteTitle, h.site_description AS siteDescription,
    h.site_image AS siteImage, m.shared_user_id AS sharedUserId, su.full_name AS sharedUserFullName, su.identity_number AS sharedUserIdentityNumber,
    su.avatar_url AS sharedUserAvatarUrl, su.is_verified AS sharedUserIsVerified, su.app_id AS sharedUserAppId, su.membership AS sharedMembership, mm.mentions AS mentions, mm.has_read as mentionRead,
    pm.message_id IS NOT NULL as isPin, c.name AS groupName, em.expire_in AS expireIn, em.expire_at AS expireAt
    FROM messages m
    LEFT JOIN users u ON m.user_id = u.user_id
    LEFT JOIN users u1 ON m.participant_id = u1.user_id
    LEFT JOIN snapshots s ON m.snapshot_id = s.snapshot_id
    LEFT JOIN safe_snapshots ss ON m.snapshot_id = ss.snapshot_id
    LEFT JOIN assets a ON s.asset_id = a.asset_id
    LEFT JOIN tokens t ON ss.asset_id = t.asset_id
    LEFT JOIN stickers st ON st.sticker_id = m.sticker_id
    LEFT JOIN hyperlinks h ON m.hyperlink = h.hyperlink
    LEFT JOIN users su ON m.shared_user_id = su.user_id
    LEFT JOIN conversations c ON m.conversation_id = c.conversation_id
    LEFT JOIN message_mentions mm ON m.id = mm.message_id
    LEFT JOIN pin_messages pm ON m.id = pm.message_id
    LEFT JOIN expired_messages em ON m.id = em.message_id
    """

private const val CHAT_MESSAGE_CATEGORIES =
    "('SIGNAL_TEXT', 'SIGNAL_IMAGE', 'SIGNAL_VIDEO', 'SIGNAL_STICKER', 'SIGNAL_DATA', 'SIGNAL_CONTACT', 'SIGNAL_AUDIO', 'SIGNAL_LIVE', 'SIGNAL_POST', 'SIGNAL_LOCATION', 'ENCRYPTED_TEXT', 'ENCRYPTED_IMAGE', 'ENCRYPTED_VIDEO', 'ENCRYPTED_STICKER', 'ENCRYPTED_DATA', 'ENCRYPTED_CONTACT', 'ENCRYPTED_AUDIO', 'ENCRYPTED_LIVE', 'ENCRYPTED_POST', 'ENCRYPTED_LOCATION', 'PLAIN_TEXT', 'PLAIN_IMAGE', 'PLAIN_VIDEO', 'PLAIN_DATA', 'PLAIN_STICKER', 'PLAIN_CONTACT', 'PLAIN_AUDIO', 'PLAIN_LIVE', 'PLAIN_POST', 'PLAIN_LOCATION', 'APP_BUTTON_GROUP', 'APP_CARD', 'SYSTEM_ACCOUNT_SNAPSHOT', 'SYSTEM_SAFE_SNAPSHOT')"

@GeneratedQueryProvider(generatedName = "MessageFetcherGenerated")
interface MessageFetcherQuerySpec {
    @GeneratedRawCursorQuery(
        sql = MESSAGE_ITEM_SQL + " WHERE m.id IN {{ids}}",
        binds = [],
        converter = "one.mixin.android.db.provider.convertToMessageItems",
    )
    fun findMessagesByIds(
        db: MixinDatabase,
        ids: String,
    ): List<MessageItem>

    @GeneratedRawCursorQuery(
        sql = "SELECT id FROM messages WHERE conversation_id = ? ORDER BY created_at DESC, rowid DESC LIMIT ? OFFSET ?",
        binds = ["conversationId", "limit", "offset"],
        converter = "convertToMessageIds",
    )
    fun loadMessageIdsByOffset(
        db: MixinDatabase,
        conversationId: String,
        limit: Int,
        offset: Int,
    ): List<String>

    @GeneratedRawCursorQuery(
        sql = "SELECT id FROM messages WHERE conversation_id = ? AND category IN $CHAT_MESSAGE_CATEGORIES ORDER BY created_at ASC, rowid ASC LIMIT ? OFFSET ?",
        binds = ["conversationId", "limit", "offset"],
        converter = "convertToMessageIds",
    )
    fun loadChatMessageIdsByOffset(
        db: MixinDatabase,
        conversationId: String,
        limit: Int,
        offset: Int,
    ): List<String>

    @GeneratedRawCursorQuery(
        sql = "SELECT id FROM messages WHERE conversation_id = ? AND (created_at, rowid) > (?, ?) ORDER BY created_at ASC, rowid ASC LIMIT ?",
        binds = ["conversationId", "createdAt", "rowId", "limit"],
        converter = "convertToMessageIds",
    )
    fun loadNextPageMessageIds(
        db: MixinDatabase,
        conversationId: String,
        createdAt: String,
        rowId: Long,
        limit: Int,
    ): List<String>

    @GeneratedRawCursorQuery(
        sql = "SELECT id FROM messages WHERE conversation_id = ? AND (created_at, rowid) < (?, ?) ORDER BY created_at DESC, rowid DESC LIMIT ?",
        binds = ["conversationId", "createdAt", "rowId", "limit"],
        converter = "convertToMessageIds",
    )
    fun loadPreviousPageMessageIds(
        db: MixinDatabase,
        conversationId: String,
        createdAt: String,
        rowId: Long,
        limit: Int,
    ): List<String>

    @GeneratedRawCursorQuery(
        sql = "SELECT id FROM messages WHERE conversation_id = ? AND (created_at, rowid) >= (?, ?) ORDER BY created_at ASC, rowid ASC LIMIT ?",
        binds = ["conversationId", "createdAt", "rowId", "limit"],
        converter = "convertToMessageIds",
    )
    fun loadAroundAnchorNextMessageIds(
        db: MixinDatabase,
        conversationId: String,
        createdAt: String,
        rowId: Long,
        limit: Int,
    ): List<String>

    @GeneratedRawCursorQuery(
        sql = "SELECT id FROM messages WHERE conversation_id = ? AND (created_at, rowid) < (?, ?) ORDER BY created_at DESC, rowid DESC LIMIT ?",
        binds = ["conversationId", "createdAt", "rowId", "limit"],
        converter = "convertToMessageIds",
    )
    fun loadAroundAnchorPreviousMessageIds(
        db: MixinDatabase,
        conversationId: String,
        createdAt: String,
        rowId: Long,
        limit: Int,
    ): List<String>

    @GeneratedRawCursorQuery(
        sql = "SELECT count FROM conversation_ext WHERE conversation_id = ?",
        binds = ["conversationId"],
        converter = "convertToNullableMessageCount",
    )
    fun findCachedMessageCount(
        db: MixinDatabase,
        conversationId: String,
    ): Int?

    @GeneratedRawCursorQuery(
        sql = "SELECT unseen_message_count FROM conversations WHERE conversation_id = ?",
        binds = ["conversationId"],
        converter = "convertToMessageCount",
    )
    fun findInitialPosition(
        db: MixinDatabase,
        conversationId: String,
    ): Int

    @GeneratedRawCursorQuery(
        sql = "SELECT count(1) FROM remote_messages_status WHERE conversation_id = ? AND status = 'DELIVERED'",
        binds = ["conversationId"],
        converter = "convertToMessageCount",
    )
    fun countUnreadMessages(
        db: MixinDatabase,
        conversationId: String,
    ): Int

    @GeneratedRawCursorQuery(
        sql = "SELECT message_id FROM remote_messages_status WHERE conversation_id = ? AND status = 'DELIVERED' ORDER BY rowid ASC LIMIT 1",
        binds = ["conversationId"],
        converter = "convertToMessageId",
    )
    fun findFirstUnreadMessageId(
        db: MixinDatabase,
        conversationId: String,
    ): String?

    @GeneratedRawCursorQuery(
        sql = "SELECT rowid, created_at, id FROM messages WHERE id = ?",
        binds = ["messageId"],
        converter = "convertToChatMessageAnchor",
    )
    fun findAnchorByMessageId(
        db: MixinDatabase,
        messageId: String,
    ): ChatMessageAnchor?

    @GeneratedRawCursorQuery(
        sql = """
            SELECT rowid, created_at, id
            FROM messages
            WHERE conversation_id = ? AND created_at >= ?
            ORDER BY created_at ASC, rowid ASC
            LIMIT 1
        """,
        binds = ["conversationId", "createdAt"],
        converter = "convertToChatMessageAnchor",
    )
    fun findAnchorByDateAfter(
        db: MixinDatabase,
        conversationId: String,
        createdAt: String,
    ): ChatMessageAnchor?

    @GeneratedRawCursorQuery(
        sql = """
            SELECT rowid, created_at, id
            FROM messages
            WHERE conversation_id = ? AND created_at < ?
            ORDER BY created_at DESC, rowid DESC
            LIMIT 1
        """,
        binds = ["conversationId", "createdAt"],
        converter = "convertToChatMessageAnchor",
    )
    fun findAnchorByDateBefore(
        db: MixinDatabase,
        conversationId: String,
        createdAt: String,
    ): ChatMessageAnchor?

    @GeneratedRawCursorQuery(
        sql = """
            SELECT rowid, created_at, id
            FROM messages
            WHERE conversation_id = ?
            ORDER BY created_at ASC, rowid ASC
            LIMIT 1 OFFSET ?
        """,
        binds = ["conversationId", "offset"],
        converter = "convertToChatMessageAnchor",
    )
    fun findAnchorByPosition(
        db: MixinDatabase,
        conversationId: String,
        offset: Int,
    ): ChatMessageAnchor?

    @GeneratedRawCursorQuery(
        sql = "SELECT rowid, created_at, id FROM messages WHERE conversation_id = ? ORDER BY created_at DESC, rowid DESC LIMIT 1 OFFSET ?",
        binds = ["conversationId", "offset"],
        converter = "convertToChatMessageAnchor",
    )
    fun findAnchorByPositionFromEnd(
        db: MixinDatabase,
        conversationId: String,
        offset: Int,
    ): ChatMessageAnchor?

    @GeneratedRawCursorQuery(
        sql = "SELECT count(1) FROM messages WHERE conversation_id = ?",
        binds = ["conversationId"],
        converter = "convertToMessageCount",
    )
    fun countMessages(
        db: MixinDatabase,
        conversationId: String,
    ): Int
}
