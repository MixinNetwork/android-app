package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomWarnings
import androidx.sqlite.db.SupportSQLiteQuery
import one.mixin.android.db.contants.AUDIOS
import one.mixin.android.db.contants.DATA
import one.mixin.android.db.contants.IMAGES
import one.mixin.android.db.contants.LIVES
import one.mixin.android.db.contants.TRANSCRIPTS
import one.mixin.android.db.contants.VIDEOS
import one.mixin.android.ui.transfer.vo.compatible.TransferMessage
import one.mixin.android.vo.AttachmentMigration
import one.mixin.android.vo.ConversationWithStatus
import one.mixin.android.vo.FtsSearchResult
import one.mixin.android.vo.HyperlinkItem
import one.mixin.android.vo.MediaMessageMinimal
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageMedia
import one.mixin.android.vo.MessageMinimal
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.QuoteMinimal
import one.mixin.android.vo.SearchMessageDetailItem

@Dao
interface MessageDao : BaseDao<Message> {
    companion object {
        const val PREFIX_MESSAGE_ITEM = """
        SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId,
        u.full_name AS userFullName, u.identity_number AS userIdentityNumber, u.app_id AS appId, m.category AS type,
        m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus, m.media_waveform AS mediaWaveform,
        m.name AS mediaName, m.media_mime_type AS mediaMimeType, m.media_size AS mediaSize, m.media_width AS mediaWidth, m.media_height AS mediaHeight,
        m.thumb_image AS thumbImage, m.thumb_url AS thumbUrl, m.media_url AS mediaUrl, m.media_duration AS mediaDuration, m.quote_message_id as quoteId,
        m.quote_content as quoteContent, m.caption as caption, u1.full_name AS participantFullName, m.action AS actionName, u1.user_id AS participantUserId,
        COALESCE(s.snapshot_id, ss.snapshot_id) AS snapshotId, COALESCE(s.type, ss.type) AS snapshotType, COALESCE(s.memo, ss.memo) AS snapshotMemo, COALESCE(s.amount, ss.amount) AS snapshotAmount, 
        COALESCE(a.symbol, t.symbol) AS assetSymbol, COALESCE(s.asset_id, ss.asset_id) AS assetId, COALESCE(a.icon_url, t.icon_url) AS assetIcon,
        st.asset_url AS assetUrl, st.asset_width AS assetWidth, st.asset_height AS assetHeight, st.sticker_id AS stickerId,
        st.name AS assetName, st.asset_type AS assetType, h.site_name AS siteName, h.site_title AS siteTitle, h.site_description AS siteDescription,
        h.site_image AS siteImage, m.shared_user_id AS sharedUserId, su.full_name AS sharedUserFullName, su.identity_number AS sharedUserIdentityNumber,
        su.avatar_url AS sharedUserAvatarUrl, su.is_verified AS sharedUserIsVerified, su.app_id AS sharedUserAppId, mm.mentions AS mentions, mm.has_read as mentionRead, 
        pm.message_id IS NOT NULL as isPin, c.name AS groupName, em.expire_in AS expireIn, em.expire_at AS expireAt 
        FROM messages m
        INNER JOIN users u ON m.user_id = u.user_id
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
        private const val CHAT_CATEGORY = "('SIGNAL_TEXT', 'SIGNAL_IMAGE', 'SIGNAL_VIDEO', 'SIGNAL_STICKER', 'SIGNAL_DATA', 'SIGNAL_CONTACT', 'SIGNAL_AUDIO', 'SIGNAL_LIVE', 'SIGNAL_POST', 'SIGNAL_LOCATION', 'ENCRYPTED_TEXT', 'ENCRYPTED_IMAGE', 'ENCRYPTED_VIDEO', 'ENCRYPTED_STICKER', 'ENCRYPTED_DATA', 'ENCRYPTED_CONTACT', 'ENCRYPTED_AUDIO', 'ENCRYPTED_LIVE', 'ENCRYPTED_POST', 'ENCRYPTED_LOCATION', 'PLAIN_TEXT', 'PLAIN_IMAGE', 'PLAIN_VIDEO', 'PLAIN_DATA', 'PLAIN_STICKER', 'PLAIN_CONTACT', 'PLAIN_AUDIO', 'PLAIN_LIVE', 'PLAIN_POST', 'PLAIN_LOCATION', 'APP_BUTTON_GROUP', 'APP_CARD', 'SYSTEM_ACCOUNT_SNAPSHOT', 'SYSTEM_SAFE_SNAPSHOT')"
    }

    // Read SQL
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_MESSAGE_ITEM WHERE m.conversation_id = :conversationId AND m.category IN $CHAT_CATEGORY ORDER BY m.created_at ASC LIMIT :limit OFFSET :offset")
    suspend fun getChatMessages(
        conversationId: String,
        offset: Int,
        limit: Int,
    ): List<MessageItem>

    @Query("SELECT count(1) FROM messages WHERE conversation_id = :conversationId AND rowid > (SELECT rowid FROM messages WHERE id = :messageId) AND created_at >= (SELECT created_at FROM messages WHERE id = :messageId)")
    suspend fun findMessageIndex(
        conversationId: String,
        messageId: String,
    ): Int

    @Query("SELECT content FROM messages WHERE conversation_id = :conversationId AND id = :messageId")
    fun findMessageContentById(
        conversationId: String,
        messageId: String,
    ): String?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId,
        u.full_name AS userFullName, u.identity_number AS userIdentityNumber, m.category AS type,
        m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus, m.media_size AS mediaSize,
        m.media_width AS mediaWidth, m.media_height AS mediaHeight, m.thumb_image AS thumbImage, m.thumb_url AS thumbUrl,
        m.media_url AS mediaUrl, m.media_mime_type AS mediaMimeType, m.media_duration AS mediaDuration
        FROM messages m
        INDEXED BY index_messages_conversation_id_category
        INNER JOIN users u ON m.user_id = u.user_id 
        WHERE m.conversation_id = :conversationId
        AND m.category IN ($IMAGES, $VIDEOS, $LIVES) 
        ORDER BY m.created_at ASC, m.rowid ASC
    """,
    )
    fun getMediaMessages(conversationId: String): DataSource.Factory<Int, MessageItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId,
        u.full_name AS userFullName, u.identity_number AS userIdentityNumber, m.category AS type, m.media_size AS mediaSize,
        m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus,
        m.media_width AS mediaWidth, m.media_height AS mediaHeight, m.thumb_image AS thumbImage, m.thumb_url AS thumbUrl,
        m.media_url AS mediaUrl, m.media_mime_type AS mediaMimeType, m.media_duration AS mediaDuration
        FROM messages m 
        INNER JOIN users u ON m.user_id = u.user_id 
        WHERE m.id = :messageId AND m.conversation_id = :conversationId
    """,
    )
    suspend fun getMediaMessage(
        conversationId: String,
        messageId: String,
    ): MessageItem?

    @Query(
        """
        SELECT count(1) FROM messages 
        INDEXED BY index_messages_conversation_id_category
        WHERE conversation_id = :conversationId
        AND category IN ($IMAGES, $VIDEOS, $LIVES) 
        AND (created_at < (SELECT created_at FROM messages WHERE id = :messageId) OR (created_at = (SELECT created_at FROM messages WHERE id = :messageId) AND rowid < (SELECT rowid FROM messages WHERE id = :messageId)))
        ORDER BY created_at ASC, rowid ASC
    """,
    )
    suspend fun indexMediaMessages(
        conversationId: String,
        messageId: String,
    ): Int

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId,
        u.full_name AS userFullName, u.identity_number AS userIdentityNumber, m.category AS type,
        m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus, m.media_size AS mediaSize,
        m.media_width AS mediaWidth, m.media_height AS mediaHeight, m.thumb_image AS thumbImage, m.thumb_url AS thumbUrl,
        m.media_url AS mediaUrl, m.media_mime_type AS mediaMimeType, m.media_duration AS mediaDuration
        FROM messages m
        INDEXED BY index_messages_conversation_id_category
        INNER JOIN users u ON m.user_id = u.user_id
        WHERE m.conversation_id = :conversationId
        AND m.category IN ($IMAGES, $VIDEOS)
        ORDER BY m.created_at DESC, m.rowid DESC
    """,
    )
    fun getMediaMessagesExcludeLive(conversationId: String): DataSource.Factory<Int, MessageItem>

    @Query(
        """
        SELECT count(1) FROM messages
        INDEXED BY index_messages_conversation_id_category 
        WHERE conversation_id = :conversationId 
        AND category IN ($IMAGES, $VIDEOS)
        AND (created_at < (SELECT created_at FROM messages WHERE id = :messageId) OR (created_at = (SELECT created_at FROM messages WHERE id = :messageId) AND rowid < (SELECT rowid FROM messages WHERE id = :messageId)))
        ORDER BY created_at DESC, rowid DESC
        """,
    )
    suspend fun indexMediaMessagesExcludeLive(
        conversationId: String,
        messageId: String,
    ): Int

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId, u.avatar_url AS userAvatarUrl,
        u.full_name AS userFullName, u.identity_number AS userIdentityNumber, m.category AS type,
        m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus,
        m.media_width AS mediaWidth, m.media_height AS mediaHeight, m.thumb_image AS thumbImage, m.thumb_url AS thumbUrl,
        m.media_url AS mediaUrl, m.media_mime_type AS mediaMimeType, m.media_duration AS mediaDuration,  m.media_waveform AS mediaWaveform
        FROM messages m INNER JOIN users u ON m.user_id = u.user_id 
        WHERE m.conversation_id = :conversationId
        AND m.category IN ($AUDIOS)
        ORDER BY m.created_at DESC
        """,
    )
    fun getAudioMessages(conversationId: String): DataSource.Factory<Int, MessageItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId, u.avatar_url AS userAvatarUrl,
        u.full_name AS userFullName, u.identity_number AS userIdentityNumber, m.category AS type,
        m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus,
        m.media_width AS mediaWidth, m.media_height AS mediaHeight, m.thumb_image AS thumbImage, m.thumb_url AS thumbUrl,
        m.media_url AS mediaUrl, m.media_mime_type AS mediaMimeType, m.media_duration AS mediaDuration,  m.media_waveform AS mediaWaveform
        FROM messages m INNER JOIN users u ON m.user_id = u.user_id 
        WHERE m.conversation_id = :conversationId
        AND m.category IN ('SIGNAL_POST', 'PLAIN_POST', 'ENCRYPTED_POST')
        ORDER BY m.created_at DESC
        """,
    )
    fun getPostMessages(conversationId: String): DataSource.Factory<Int, MessageItem>

    @Query(
        """
        SELECT m.id AS messageId, h.hyperlink AS hyperlink, h.site_description AS siteDescription, h.site_image AS siteImage,
        h.site_name AS siteName, h.site_title AS siteTitle, m.created_at AS createdAt
        FROM hyperlinks h INNER JOIN messages m ON h.hyperlink = m.hyperlink
        WHERE m.conversation_id = :conversationId
        AND m.category IN ('SIGNAL_TEXT', 'PLAIN_TEXT', 'ENCRYPTED_TEXT')
        ORDER BY m.created_at DESC
        """,
    )
    fun getLinkMessages(conversationId: String): DataSource.Factory<Int, HyperlinkItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId,
        u.full_name AS userFullName, u.identity_number AS userIdentityNumber, m.category AS type,
        m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus,
        m.media_url AS mediaUrl, m.media_mime_type AS mediaMimeType, m.name AS mediaName, m.media_size AS mediaSize
        FROM messages m INNER JOIN users u ON m.user_id = u.user_id 
        WHERE m.conversation_id = :conversationId
        AND m.category IN ($DATA)
        ORDER BY m.created_at DESC
        """,
    )
    fun getFileMessages(conversationId: String): DataSource.Factory<Int, MessageItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId, 
        u.full_name AS userFullName, u.identity_number AS userIdentityNumber, u.app_id AS appId, m.category AS type, 
        m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus, m.media_waveform AS mediaWaveform, 
        m.name AS mediaName, m.media_mime_type AS mediaMimeType, m.media_size AS mediaSize, m.media_width AS mediaWidth, m.media_height AS mediaHeight, 
        m.thumb_image AS thumbImage, m.thumb_url AS thumbUrl, m.media_url AS mediaUrl, m.media_duration AS mediaDuration, 
        m.quote_message_id as quoteId, m.quote_content as quoteContent, 
        st.asset_url AS assetUrl, st.asset_width AS assetWidth, st.asset_height AS assetHeight, st.sticker_id AS stickerId, 
        st.name AS assetName, st.asset_type AS assetType, m.shared_user_id AS sharedUserId, su.full_name AS sharedUserFullName, su.identity_number AS sharedUserIdentityNumber, 
        su.avatar_url AS sharedUserAvatarUrl, su.is_verified AS sharedUserIsVerified, su.app_id AS sharedUserAppId, mm.mentions AS mentions, u.membership 
        FROM messages m 
        INNER JOIN users u ON m.user_id = u.user_id 
        LEFT JOIN stickers st ON st.sticker_id = m.sticker_id 
        LEFT JOIN users su ON m.shared_user_id = su.user_id 
        LEFT JOIN message_mentions mm ON m.id = mm.message_id 
        WHERE m.conversation_id = :conversationId AND m.id = :messageId AND m.status != 'FAILED'
        """,
    )
    fun findQuoteMessageItemById(
        conversationId: String,
        messageId: String,
    ): QuoteMessageItem?

    @Query("SELECT rowid, conversation_id, quote_message_id FROM messages WHERE rowid > :rowId AND quote_message_id IS NOT NULL AND quote_message_id != '' AND length(quote_content) > 10240 GROUP BY quote_message_id LIMIT :limit")
    fun findBigQuoteMessage(
        rowId: Long,
        limit: Int,
    ): List<QuoteMinimal>

    @Query("SELECT count(id) FROM messages WHERE conversation_id = :conversationId AND quote_message_id = :quoteMessageId AND quote_content IS NULL")
    fun countMessageByQuoteId(
        conversationId: String,
        quoteMessageId: String,
    ): Int

    @RawQuery
    suspend fun fuzzySearchMessage(query: SupportSQLiteQuery): List<FtsSearchResult>

    @Query(
        """
        SELECT m.id AS messageId, u.user_id AS userId, u.avatar_url AS userAvatarUrl, u.full_name AS userFullName,
        m.category AS type, m.content AS content, m.created_at AS createdAt, m.name AS mediaName, u.membership AS membership, u.app_id AS app_id, u.is_verified AS isVerified  
        FROM messages m INNER JOIN users u ON m.user_id = u.user_id 
        WHERE  m.id IN (:ids)
        ORDER BY m.created_at DESC
    """,
    )
    fun getSearchMessageDetailItemsByIds(ids: List<String>): List<SearchMessageDetailItem>

    @Query("SELECT m.category as type, m.id as messageId, m.media_url as mediaUrl FROM messages m WHERE m.conversation_id = :conversationId AND m.media_url IS NOT NULL AND m.media_status = 'DONE' LIMIT :limit OFFSET :offset")
    suspend fun getMediaMessageMinimalByConversationId(
        conversationId: String,
        limit: Int,
        offset: Int,
    ): List<MediaMessageMinimal>

    @Query("SELECT m.id FROM messages m WHERE m.conversation_id = :conversationId AND (m.category = 'SIGNAL_TRANSCRIPT' OR m.category = 'PLAIN_TRANSCRIPT') LIMIT :limit OFFSET :offset")
    suspend fun getTranscriptMessageIdByConversationId(
        conversationId: String,
        limit: Int,
        offset: Int,
    ): List<String>

    @Query("SELECT rowid, id FROM messages WHERE conversation_id = :conversationId AND status IN ('SENT', 'DELIVERED') AND user_id != :userId ORDER BY rowid ASC LIMIT :limit")
    fun getUnreadMessage(
        conversationId: String,
        userId: String,
        limit: Int,
    ): List<MessageMinimal>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    fun findMessageById(messageId: String): Message?

    @Query("SELECT * FROM messages WHERE id = :messageId AND user_id = :userId")
    fun findMessageById(
        messageId: String,
        userId: String,
    ): Message?

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun suspendFindMessageById(messageId: String): Message?

    @Query("SELECT conversation_id, user_id, status FROM messages WHERE id = :messageId")
    fun findMessageStatusById(messageId: String): ConversationWithStatus?

    @Query(
        """
        SELECT m.* FROM messages m 
        WHERE m.rowid < :rowId AND m.category IN ('SIGNAL_TEXT', 'PLAIN_TEXT', 'ENCRYPTED_TEXT', 'SIGNAL_TRANSCRIPT', 'PLAIN_TRANSCRIPT', 'ENCRYPTED_TRANSCRIPT', 
        'SIGNAL_POST', 'PLAIN_POST', 'ENCRYPTED_POST', 'SIGNAL_DATA', 'PLAIN_DATA', 'ENCRYPTED_DATA', 'SIGNAL_CONTACT', 'PLAIN_CONTACT', 'ENCRYPTED_CONTACT', 'APP_CARD')
        AND m.status != 'FAILED' AND m.status != 'UNKNOWN'
        ORDER BY m.rowid DESC
        LIMIT :limit
    """,
    )
    fun findFtsMessages(
        rowId: Long,
        limit: Int,
    ): List<Message>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT m.* FROM messages m 
        WHERE m.rowid >= :rowId
        ORDER BY m.rowid ASC
        LIMIT :limit 
    """,
    )
    fun getMessageByLimitAndRowId(
        limit: Int,
        rowId: Long,
    ): List<TransferMessage>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT m.* FROM messages m 
        WHERE m.rowid >= :rowId AND m.conversation_id IN (:conversationIds) 
        ORDER BY m.rowid ASC
        LIMIT :limit 
    """,
    )
    fun getMessageByLimitAndRowId(
        limit: Int,
        rowId: Long,
        conversationIds: Collection<String>,
    ): List<TransferMessage>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT m.* FROM messages m 
        WHERE m.rowid >= :rowId AND m.created_at >= :createdAt 
        ORDER BY m.rowid ASC
        LIMIT :limit 
    """,
    )
    fun getMessageByLimitAndRowId(
        limit: Int,
        rowId: Long,
        createdAt: String,
    ): List<TransferMessage>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT m.* FROM messages m 
        WHERE m.rowid >= :rowId AND m.conversation_id IN (:conversationIds) AND m.created_at >= :createdAt 
        ORDER BY m.rowid ASC
        LIMIT :limit 
    """,
    )
    fun getMessageByLimitAndRowId(
        limit: Int,
        rowId: Long,
        conversationIds: Collection<String>,
        createdAt: String,
    ): List<TransferMessage>

    @Query("SELECT rowid FROM messages ORDER BY rowid DESC LIMIT 1")
    fun getLastMessageRowId(): Long?

    // id not null means message exists
    @Query("SELECT id FROM messages WHERE id = :messageId")
    fun findMessageIdById(messageId: String): String?

    @Query("SELECT DISTINCT conversation_id FROM messages WHERE id IN (:messages)")
    fun findConversationsByMessages(messages: List<String>): List<String>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT rowid, id FROM messages WHERE conversation_id = :conversationId
        AND user_id != :userId AND status IN ('SENT', 'DELIVERED') ORDER BY rowid ASC
        """,
    )
    fun findUnreadMessagesSync(
        conversationId: String,
        userId: String,
    ): List<MessageMinimal>?

    @Query("SELECT id FROM messages WHERE conversation_id = :conversationId AND user_id = :userId AND status = 'FAILED' ORDER BY created_at DESC LIMIT 1000")
    fun findFailedMessages(
        conversationId: String,
        userId: String,
    ): List<String>

    @Query(
        """
        SELECT m.category as type, m.id as messageId, m.media_url as mediaUrl FROM messages m 
        WHERE conversation_id = :conversationId AND media_status = 'DONE' 
        AND category IN (:signalCategory, :plainCategory, :encryptedCategory) ORDER BY created_at ASC
        """,
    )
    fun getMediaByConversationIdAndCategory(
        conversationId: String,
        signalCategory: String,
        plainCategory: String,
        encryptedCategory: String,
    ): List<MediaMessageMinimal>?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        $PREFIX_MESSAGE_ITEM WHERE m.conversation_id = :conversationId AND (m.category IN ($AUDIOS)) AND m.created_at >= :createdAt AND 
        m.rowid > (SELECT rowid FROM messages WHERE id = :messageId) LIMIT 1
        """,
    )
    suspend fun findNextAudioMessageItem(
        conversationId: String,
        createdAt: String,
        messageId: String,
    ): MessageItem?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT * FROM messages WHERE conversation_id = :conversationId AND (category IN ($AUDIOS))
        AND created_at >= :createdAt AND rowid > (SELECT rowid FROM messages WHERE id = :messageId) LIMIT 1
        """,
    )
    suspend fun findNextAudioMessage(
        conversationId: String,
        createdAt: String,
        messageId: String,
    ): Message?

    @Query("SELECT id FROM messages WHERE conversation_id =:conversationId ORDER BY created_at DESC, rowid DESC LIMIT 1 OFFSET :offset")
    suspend fun findFirstUnreadMessageId(
        conversationId: String,
        offset: Int,
    ): String?

    @Query("SELECT id FROM messages WHERE conversation_id =:conversationId ORDER BY created_at DESC LIMIT 1")
    suspend fun findLastMessage(conversationId: String): String?

    @Query("SELECT id FROM messages WHERE conversation_id =:conversationId ORDER BY created_at DESC LIMIT 1")
    fun findLastMessageId(conversationId: String): String?

    @Query("SELECT rowid FROM messages WHERE conversation_id =:conversationId ORDER BY rowid DESC LIMIT 1")
    suspend fun findLastMessageRowId(conversationId: String): Long?

    @Query(
        """
        SELECT id FROM messages WHERE conversation_id =:conversationId AND user_id !=:userId AND messages.rowid > 
        (SELECT rowid FROM messages WHERE id = :messageId) ORDER BY rowid ASC LIMIT 1
        """,
    )
    suspend fun findUnreadMessageByMessageId(
        conversationId: String,
        userId: String,
        messageId: String,
    ): String?

    @Query(
        """
        SELECT rowid FROM messages 
        WHERE conversation_id =:conversationId
        AND status IN ('SENDING', 'SENT', 'DELIVERED', 'READ')  /* Make use of `index_messages_conversation_id_status_user_id_created_at` */
        AND user_id =:userId
        LIMIT 1
        """,
    )
    suspend fun isSilence(
        conversationId: String,
        userId: String,
    ): Int?

    @Query("SELECT * FROM messages WHERE id IN (:messageIds) ORDER BY created_at, rowid")
    suspend fun getSortMessagesByIds(messageIds: List<String>): List<Message>

    @Query("SELECT id FROM messages WHERE conversation_id =:conversationId")
    suspend fun getMessageIdsByConversationId(conversationId: String): List<String>

    @Query("SELECT id FROM messages WHERE conversation_id =:conversationId LIMIT :limit OFFSET :offset")
    suspend fun getMessageIdsByConversationId(
        conversationId: String,
        limit: Long,
        offset: Long,
    ): List<String>

    @Query("SELECT id FROM messages WHERE conversation_id =:conversationId AND rowid <= :rowid ORDER BY rowid LIMIT :limit")
    suspend fun getMessageIdsByConversationId(
        conversationId: String,
        rowid: Long,
        limit: Int,
    ): List<String>

    @Query("SELECT id FROM messages WHERE conversation_id =:conversationId AND rowid <= :rowid ORDER BY rowid")
    suspend fun getMessageIdsByConversationId(
        conversationId: String,
        rowid: Long,
    ): List<String>

    @Query(
        """
        SELECT id, conversation_id, name, category, media_url, media_mine_type 
        FROM messages WHERE category IN ($IMAGES, $VIDEOS, $DATA, $AUDIOS) 
        AND media_status = 'DONE' AND rowid <= :rowId LIMIT :limit OFFSET :offset
        """,
    )
    fun findAttachmentMigration(
        rowId: Long,
        limit: Int,
        offset: Long,
    ): List<AttachmentMigration>

    @Query("SELECT * FROM messages WHERE id = :messageId AND category IN ($IMAGES, $VIDEOS, $DATA, $AUDIOS) AND (media_status = 'DONE' OR media_status = 'READ')")
    fun findAttachmentMessage(messageId: String): Message?

    @Query("SELECT rowid IS NOT NULL FROM messages WHERE category IN ($IMAGES, $VIDEOS, $DATA, $AUDIOS) AND media_status = 'DONE' LIMIT 1")
    fun hasDoneAttachment(): Boolean

    @Query("SELECT rowid FROM messages ORDER BY rowid DESC LIMIT 1")
    fun getLastMessageRowid(): Long

    @Query("SELECT rowid FROM messages WHERE id = :messageId")
    fun getMessageRowid(messageId: String): Long?

    @Query("SELECT rowid FROM messages WHERE created_at >= :createdAt ORDER BY rowid ASC LIMIT 1")
    fun getMessageRowidByCreateAt(createdAt: String): Long?

    @Query("SELECT id FROM messages WHERE id = :messageId")
    suspend fun exists(messageId: String): String?

    // DELETE COUNT
    @Query(
        """
        SELECT count(id) FROM messages 
        WHERE conversation_id = :conversationId AND media_status = 'DONE' AND category IN (:signalCategory, :plainCategory, :encryptedCategory)
        """,
    )
    fun countDeleteMediaMessageByConversationAndCategory(
        conversationId: String,
        signalCategory: String,
        plainCategory: String,
        encryptedCategory: String,
    ): Int

    @Query("SELECT count(id) FROM messages WHERE conversation_id = :conversationId")
    suspend fun countDeleteMessageByConversationId(conversationId: String): Int

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId,
        u.full_name AS userFullName, u.identity_number AS userIdentityNumber, m.category AS type,
        m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus,
        m.media_url AS mediaUrl, m.media_mime_type AS mediaMimeType, m.name AS mediaName, m.media_size AS mediaSize
        FROM messages m INNER JOIN users u ON m.user_id = u.user_id 
        WHERE m.conversation_id = :conversationId
        AND (m.category IN ($DATA)) 
        AND m.media_mime_type LIKE 'audio%'
        AND m.media_status != 'EXPIRED'
        ORDER BY m.created_at ASC, m.rowid ASC
        """,
    )
    fun findAudiosByConversationId(conversationId: String): DataSource.Factory<Int, MessageItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT count(1) FROM messages
        INDEXED BY index_messages_conversation_id_category 
        WHERE conversation_id = :conversationId 
        AND category IN ($DATA) 
        AND media_mime_type LIKE 'audio%'
        AND media_status != 'EXPIRED'
        AND created_at < (SELECT created_at FROM messages WHERE id = :messageId)
        ORDER BY created_at ASC, rowid ASC
        """,
    )
    suspend fun indexAudioByConversationId(
        messageId: String,
        conversationId: String,
    ): Int

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId,
        u.full_name AS userFullName, u.identity_number AS userIdentityNumber, m.category AS type,
        m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus,
        m.media_url AS mediaUrl, m.media_mime_type AS mediaMimeType, m.name AS mediaName, m.media_size AS mediaSize
        FROM messages m INNER JOIN users u ON m.user_id = u.user_id 
        WHERE m.conversation_id = :conversationId
        AND m.id IN (:ids)
        """,
    )
    suspend fun suspendFindMessagesByIds(
        conversationId: String,
        ids: List<String>,
    ): List<MessageItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_MESSAGE_ITEM WHERE m.id = :messageId")
    fun findMessageItemByMessageId(messageId: String): LiveData<MessageItem?>

    @Query("SELECT id FROM messages WHERE conversation_id = :conversationId AND category IN ($TRANSCRIPTS)")
    suspend fun findTranscriptIdByConversationId(conversationId: String): List<String>

    @Query("SELECT id FROM messages LIMIT 1")
    suspend fun hasMessage(): String?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM messages WHERE id = :messageId")
    fun findMessageMediaById(messageId: String): MessageMedia?

    @Query("SELECT id FROM messages WHERE conversation_id = :conversationId AND quote_message_id = :quoteMessageId")
    fun findQuoteMessageIdByQuoteId(
        conversationId: String,
        quoteMessageId: String,
    ): List<String>

    // Update SQL
    @Query("UPDATE messages SET quote_content = :content WHERE conversation_id = :conversationId AND quote_message_id = :quoteMessageId")
    fun updateQuoteContentByQuoteId(
        conversationId: String,
        quoteMessageId: String,
        content: String,
    )

    @Query("UPDATE messages SET quote_content = NULL WHERE conversation_id = :conversationId AND id = :quoteMessageId")
    fun updateQuoteContentNullByQuoteMessageId(
        conversationId: String,
        quoteMessageId: String,
    )

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    fun updateMessageStatus(
        status: String,
        id: String,
    )

    @Query("UPDATE messages SET status = 'SENT' WHERE id = :id AND status = 'FAILED'")
    fun recallFailedMessage(id: String)

    @Query(
        """
        UPDATE messages SET category = 'MESSAGE_RECALL', content = NULL, media_url = NULL, media_mime_type = NULL, media_size = NULL, 
        media_duration = NULL, media_width = NULL, media_height = NULL, media_hash = NULL, thumb_image = NULL, media_key = NULL, 
        media_digest = NUll, media_status = NULL, `action` = NULL, participant_id = NULL, snapshot_id = NULL, hyperlink = NULL, name = NULL, 
        album_id = NULL, sticker_id = NULL, shared_user_id = NULL, media_waveform = NULL, quote_message_id = NULL, quote_content = NULL WHERE id = :id
        """,
    )
    fun recallMessage(id: String)

    @Query("UPDATE messages SET content = NULL WHERE category = 'MESSAGE_PIN' AND quote_message_id = :id AND conversation_id = :conversationId")
    fun recallPinMessage(
        id: String,
        conversationId: String,
    )

    @Query("UPDATE messages SET media_status = :status WHERE id = :id AND category != 'MESSAGE_RECALL'")
    fun updateMediaStatus(
        status: String,
        id: String,
    )

    @Query("UPDATE messages SET media_status = :status WHERE id = :id AND category != 'MESSAGE_RECALL'")
    suspend fun updateMediaStatusSuspend(
        status: String,
        id: String,
    )

    @Query("UPDATE messages SET media_size = :mediaSize WHERE id = :id AND category != 'MESSAGE_RECALL'")
    fun updateMediaSize(
        mediaSize: Long,
        id: String,
    )

    @Query("UPDATE messages SET media_url = :mediaUrl WHERE id = :id AND category != 'MESSAGE_RECALL'")
    fun updateMediaMessageUrl(
        mediaUrl: String,
        id: String,
    )

    @Query("UPDATE messages SET media_duration = :mediaDuration WHERE id = :id AND category != 'MESSAGE_RECALL'")
    fun updateMediaDuration(
        mediaDuration: String,
        id: String,
    )

    @Query("UPDATE messages SET media_url = :mediaUrl, media_size = :mediaSize, media_status = :mediaStatus WHERE id = :messageId AND category != 'MESSAGE_RECALL'")
    fun updateMedia(
        messageId: String,
        mediaUrl: String,
        mediaSize: Long,
        mediaStatus: String,
    )

    @Query("UPDATE messages SET media_url = :mediaUrl WHERE media_url = :oldMediaUrl AND category != 'MESSAGE_RECALL'")
    fun updateMediaUrl(
        mediaUrl: String,
        oldMediaUrl: String,
    )

    @Query("UPDATE messages SET hyperlink = :hyperlink WHERE id = :messageId AND category != 'MESSAGE_RECALL'")
    fun updateHyperlink(
        hyperlink: String,
        messageId: String,
    )

    @Query(
        """
        UPDATE messages SET content = :content, media_mime_type = :mediaMimeType, 
        media_size = :mediaSize, media_width = :mediaWidth, media_height = :mediaHeight, 
        thumb_image = :thumbImage, media_key = :mediaKey, media_digest = :mediaDigest, media_duration = :mediaDuration, 
        media_status = :mediaStatus, status = :status, name = :name, media_waveform = :mediaWaveform WHERE id = :messageId 
        AND category != 'MESSAGE_RECALL'
        """,
    )
    fun updateAttachmentMessage(
        messageId: String,
        content: String,
        mediaMimeType: String,
        mediaSize: Long,
        mediaWidth: Int?,
        mediaHeight: Int?,
        thumbImage: String?,
        name: String?,
        mediaWaveform: ByteArray?,
        mediaDuration: String?,
        mediaKey: ByteArray?,
        mediaDigest: ByteArray?,
        mediaStatus: String,
        status: String,
    )

    @Query("UPDATE messages SET sticker_id = :stickerId, status = :status WHERE id = :messageId AND category != 'MESSAGE_RECALL'")
    fun updateStickerMessage(
        stickerId: String,
        status: String,
        messageId: String,
    )

    @Query("UPDATE messages SET shared_user_id = :sharedUserId, status = :status WHERE id = :messageId AND category != 'MESSAGE_RECALL'")
    fun updateContactMessage(
        sharedUserId: String,
        status: String,
        messageId: String,
    )

    @Query(
        """
        UPDATE messages SET media_width = :width, media_height = :height, media_url=:url, thumb_url = :thumbUrl, status = :status 
        WHERE id = :messageId AND category != 'MESSAGE_RECALL'
    """,
    )
    fun updateLiveMessage(
        width: Int,
        height: Int,
        url: String,
        thumbUrl: String,
        status: String,
        messageId: String,
    )

    @Query(
        """
        UPDATE messages SET content = :content, media_size = :mediaSize, media_status = :mediaStatus, status = :status 
        WHERE id = :messageId AND category != 'MESSAGE_RECALL'
        """,
    )
    fun updateTranscriptMessage(
        content: String?,
        mediaSize: Long?,
        mediaStatus: String?,
        status: String,
        messageId: String,
    )

    @Query("UPDATE messages SET content = :content, status = :status WHERE id = :id AND category != 'MESSAGE_RECALL'")
    fun updateMessageContentAndStatus(
        content: String,
        status: String,
        id: String,
    )

    @Query("UPDATE messages SET content = :content WHERE id = :id")
    fun updateMessageContent(
        content: String?,
        id: String,
    )

    @Query("UPDATE messages SET media_url = :mediaUrl, media_size = :mediaSize, thumb_image = :thumbImage WHERE id = :id AND category != 'MESSAGE_RECALL'")
    fun updateGiphyMessage(
        id: String,
        mediaUrl: String,
        mediaSize: Long,
        thumbImage: String?,
    )

    @Query("UPDATE messages SET category = :category WHERE id = :messageId")
    fun updateCategoryById(
        messageId: String,
        category: String,
    )

    @Query("UPDATE messages SET thumb_image = 'K0OWvn_3fQ~qj[fQfQfQfQ' WHERE LENGTH(thumb_image) > 5120")
    fun cleanupBigThumb()

    // Delete SQL
    @Query("DELETE FROM messages WHERE id = :id")
    fun deleteMessageById(id: String)

    @Query("DELETE FROM messages WHERE id IN (:ids)")
    fun deleteMessageById(ids: List<String>)

    @Query(
        """
        SELECT id FROM messages WHERE  conversation_id = :conversationId AND (media_status = 'DONE' OR media_status ISNULL)
        AND category IN (:signalCategory, :plainCategory, :encryptedCategory) LIMIT :limit
        """,
    )
    fun findMediaMessageByConversationAndCategory(
        conversationId: String,
        signalCategory: String,
        plainCategory: String,
        encryptedCategory: String,
        limit: Int,
    ): List<String>

    @Query("DELETE FROM messages WHERE id IN (SELECT id FROM messages WHERE conversation_id = :conversationId LIMIT :limit)")
    suspend fun deleteMessageByConversationId(
        conversationId: String,
        limit: Int,
    )

    @Query("DELETE FROM messages_fts4 WHERE rowid IN (SELECT rowid FROM messages_fts4 LIMIT 1000)")
    fun deleteFts(): Int

    @Query("SELECT count(1) FROM messages")
    fun countMessages(): Long

    @Query("SELECT count(1) FROM messages WHERE (category IN ($DATA, $IMAGES, $AUDIOS, $VIDEOS)) AND media_status IN ('DONE', 'READ')")
    fun countMediaMessages(): Long

    @Query("SELECT count(1) FROM messages WHERE conversation_id IN (:conversationIds)")
    fun countMessages(conversationIds: Collection<String>): Long

    @Query("SELECT count(1) FROM messages WHERE (category IN ($DATA, $IMAGES, $AUDIOS, $VIDEOS)) AND media_status IN ('DONE', 'READ') AND conversation_id IN (:conversationIds)")
    fun countMediaMessages(conversationIds: Collection<String>): Long

    @Query("SELECT count(1) FROM messages WHERE rowid >= :rowId")
    fun countMessages(rowId: Long): Long

    @Query("SELECT count(1) FROM messages WHERE rowid >= :rowId AND (category IN ($DATA, $IMAGES, $AUDIOS, $VIDEOS)) AND media_status IN ('DONE', 'READ')")
    fun countMediaMessages(rowId: Long): Long

    @Query("SELECT count(1) FROM messages WHERE rowid >= :rowId AND conversation_id IN (:conversationIds)")
    fun countMessages(
        rowId: Long,
        conversationIds: Collection<String>,
    ): Long

    @Query("SELECT count(1) FROM messages WHERE rowid >= :rowId AND created_at >= :createdAt")
    fun countMessages(
        rowId: Long,
        createdAt: String,
    ): Long

    @Query("SELECT count(1) FROM messages WHERE rowid >= :rowId AND conversation_id IN (:conversationIds) AND created_at >= :createdAt")
    fun countMessages(
        rowId: Long,
        conversationIds: Collection<String>,
        createdAt: String,
    ): Long

    @Query("SELECT count(1) FROM messages WHERE rowid >= :rowId AND (category IN ($DATA, $IMAGES, $AUDIOS, $VIDEOS)) AND media_status IN ('DONE', 'READ') AND conversation_id IN (:conversationIds)")
    fun countMediaMessages(
        rowId: Long,
        conversationIds: Collection<String>,
    ): Long

    @Query("SELECT count(1) FROM messages WHERE rowid >= :rowId AND (category IN ($DATA, $IMAGES, $AUDIOS, $VIDEOS)) AND media_status IN ('DONE', 'READ') AND created_at >= :createdAt")
    fun countMediaMessages(
        rowId: Long,
        createdAt: String,
    ): Long

    @Query("SELECT count(1) FROM messages WHERE rowid >= :rowId AND (category IN ($DATA, $IMAGES, $AUDIOS, $VIDEOS)) AND media_status IN ('DONE', 'READ') AND conversation_id IN (:conversationIds) AND created_at >= :createdAt")
    fun countMediaMessages(
        rowId: Long,
        conversationIds: Collection<String>,
        createdAt: String,
    ): Long
}
