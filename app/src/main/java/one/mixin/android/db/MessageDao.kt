package one.mixin.android.db

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Transaction
import one.mixin.android.util.Session
import one.mixin.android.vo.MediaMessageMinimal
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageMinimal
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.SearchMessageItem

@Dao
interface MessageDao : BaseDao<Message> {

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId, " +
        "u.full_name AS userFullName, u.identity_number AS userIdentityNumber, u.app_id AS appId, m.category AS type, " +
        "m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus, m.media_waveform AS mediaWaveform, " +
        "m.name AS mediaName, m.media_mime_type AS mediaMimeType, m.media_size AS mediaSize, m.media_width AS mediaWidth, m.media_height AS mediaHeight, " +
        "m.thumb_image AS thumbImage, m.media_url AS mediaUrl, m.media_duration AS mediaDuration, m.quote_message_id as quoteId, m.quote_content as quoteContent, " +
        "u1.full_name AS participantFullName, m.action AS actionName, u1.user_id AS participantUserId, " +
        "s.snapshot_id AS snapshotId, s.type AS snapshotType, s.amount AS snapshotAmount, a.symbol AS assetSymbol, a.asset_id AS assetId, " +
        "a.icon_url AS assetIcon, st.asset_url AS assetUrl, st.asset_width AS assetWidth, st.asset_height AS assetHeight, st.sticker_id AS stickerId, " +
        "st.name AS assetName, st.asset_type AS assetType, h.site_name AS siteName, h.site_title AS siteTitle, h.site_description AS siteDescription, " +
        "h.site_image AS siteImage, m.shared_user_id AS sharedUserId, su.full_name AS sharedUserFullName, su.identity_number AS sharedUserIdentityNumber, " +
        "su.avatar_url AS sharedUserAvatarUrl, su.is_verified AS sharedUserIsVerified, su.app_id AS sharedUserAppId, " +
        "c.name AS groupName " +
        "FROM messages m " +
        "INNER JOIN users u ON m.user_id = u.user_id " +
        "LEFT JOIN users u1 ON m.participant_id = u1.user_id " +
        "LEFT JOIN snapshots s ON m.snapshot_id = s.snapshot_id " +
        "LEFT JOIN assets a ON s.asset_id = a.asset_id " +
        "LEFT JOIN stickers st ON st.sticker_id = m.sticker_id " +
        "LEFT JOIN hyperlinks h ON m.hyperlink = h.hyperlink " +
        "LEFT JOIN users su ON m.shared_user_id = su.user_id " +
        "LEFT JOIN conversations c ON m.conversation_id = c.conversation_id " +
        "WHERE m.conversation_id = :conversationId " +
        "ORDER BY m.created_at DESC")
    fun getMessages(conversationId: String): DataSource.Factory<Int, MessageItem>

    @Query("SELECT count(*) FROM messages WHERE conversation_id = :conversationId " +
        "AND created_at > (SELECT created_at FROM messages WHERE id = :messageId)")
    fun findMessageIndex(conversationId: String, messageId: String): Int

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId, " +
        "u.full_name AS userFullName, u.identity_number AS userIdentityNumber, m.category AS type, " +
        "m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus," +
        "m.media_width AS mediaWidth, m.media_height AS mediaHeight, m.thumb_image AS thumbImage, m.media_url AS mediaUrl, " +
        "m.media_mime_type AS mediaMimeType, m.media_duration AS mediaDuration " +
        "FROM messages m INNER JOIN users u ON m.user_id = u.user_id WHERE m.conversation_id = :conversationId " +
        "and (m.category = 'SIGNAL_IMAGE' OR m.category = 'PLAIN_IMAGE' OR m.category = 'SIGNAL_VIDEO' OR m.category = 'PLAIN_VIDEO') " +
        "AND m.media_status = 'DONE' " +
        "ORDER BY m.created_at DESC")
    fun getMediaMessages(conversationId: String): List<MessageItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId, " +
        "u.full_name AS userFullName, u.identity_number AS userIdentityNumber, u.app_id AS appId, m.category AS type, " +
        "m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus, m.media_waveform AS mediaWaveform, " +
        "m.name AS mediaName, m.media_mime_type AS mediaMimeType, m.media_size AS mediaSize, m.media_width AS mediaWidth, m.media_height AS mediaHeight, " +
        "m.thumb_image AS thumbImage, m.media_url AS mediaUrl, m.media_duration AS mediaDuration, m.quote_message_id as quoteId, m.quote_content as quoteContent, " +
        "st.asset_url AS assetUrl, st.asset_width AS assetWidth, st.asset_height AS assetHeight, st.sticker_id AS stickerId, " +
        "st.name AS assetName, st.asset_type AS assetType, m.shared_user_id AS sharedUserId, su.full_name AS sharedUserFullName, su.identity_number AS sharedUserIdentityNumber, " +
        "su.avatar_url AS sharedUserAvatarUrl, su.is_verified AS sharedUserIsVerified, su.app_id AS sharedUserAppId " +
        "FROM messages m " +
        "INNER JOIN users u ON m.user_id = u.user_id " +
        "LEFT JOIN stickers st ON st.sticker_id = m.sticker_id " +
        "LEFT JOIN users su ON m.shared_user_id = su.user_id " +
        "WHERE m.conversation_id = :conversationId AND m.id = :messageId AND m.status != 'FAILED'")
    fun findMessageItemById(conversationId: String, messageId: String): QuoteMessageItem?

    @Query("SELECT count(id) FROM messages WHERE conversation_id = :conversationId AND quote_message_id = :messageId AND quote_content IS NULL")
    fun countMessageByQuoteId(conversationId: String, messageId: String): Int

    @Query("UPDATE messages SET quote_content = :content WHERE conversation_id = :conversationId AND quote_message_id = :messageId")
    fun updateQuoteContentByQuoteId(conversationId: String, messageId: String, content: String)

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId, " +
        "u.avatar_url AS userAvatarUrl, u.full_name AS userFullName, m.category AS type, " +
        "u1.avatar_url AS botAvatarUrl, u1.full_name AS botFullName, u1.user_id AS botUserId," +
        "m.content AS content, m.created_at AS createdAt, m.name AS mediaName, " +
        "c.icon_url AS conversationAvatarUrl, c.name AS conversationName, c.category AS conversationCategory " +
        "FROM messages m INNER JOIN users u ON m.user_id = u.user_id " +
        "LEFT JOIN conversations c ON c.conversation_id = m.conversation_id " +
        "LEFT JOIN users u1 ON c.owner_id = u1.user_id " +
        "WHERE ((m.category = 'SIGNAL_TEXT' OR m.category = 'PLAIN_TEXT') AND m.status != 'FAILED' AND m.content LIKE :query) " +
        "OR ((m.category = 'SIGNAL_DATA' OR m.category = 'PLAIN_DATA') AND m.status != 'FAILED' AND m.name LIKE :query) ORDER BY m.created_at DESC LIMIT 200")
    fun fuzzySearchMessage(query: String): List<SearchMessageItem>

    @Query("DELETE FROM messages WHERE id = :id")
    fun deleteMessage(id: String)

    @Query("DELETE FROM messages WHERE conversation_id = :conversationId")
    fun deleteMessageByConversationId(conversationId: String)

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    fun updateMessageStatus(status: String, id: String)

    @Query("UPDATE messages SET media_status = :status WHERE id = :id")
    fun updateMediaStatus(status: String, id: String)

    @Query("UPDATE messages SET media_url = :mediaUrl WHERE id = :id")
    fun updateMediaMessageUrl(mediaUrl: String, id: String)

    @Query("UPDATE messages SET media_url = :mediaUrl WHERE media_url = :oldMediaUrl")
    fun updateMediaUrl(mediaUrl: String, oldMediaUrl: String)

    @Query("UPDATE messages SET hyperlink = :hyperlink WHERE id = :id")
    fun updateHyperlink(hyperlink: String, id: String)

    @Query("SELECT id,created_at FROM messages WHERE conversation_id = :conversationId AND user_id != :userId " +
        "AND status = 'DELIVERED' ORDER BY created_at ASC")
    fun getUnreadMessage(conversationId: String, userId: String): List<MessageMinimal>?

    @Query("UPDATE messages SET content = :content, media_mime_type = :mediaMimeType, " +
        "media_size = :mediaSize, media_width = :mediaWidth, media_height = :mediaHeight, " +
        "thumb_image = :thumbImage, media_key = :mediaKey, media_digest = :mediaDigest, media_duration = :mediaDuration, " +
        "media_status = :mediaStatus, status = :status, name = :name, media_waveform = :mediaWaveform WHERE id = :messageId")
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
        status: String
    )

    @Query("UPDATE messages SET sticker_id = :stickerId, status = :status WHERE id = :messageId")
    fun updateStickerMessage(stickerId: String, status: String, messageId: String)

    @Query("UPDATE messages SET shared_user_id = :sharedUserId, status = :status WHERE id = :messageId")
    fun updateContactMessage(sharedUserId: String, status: String, messageId: String)

    @Query("UPDATE messages SET content = :content, status = :status WHERE id = :id")
    fun updateMessageContentAndStatus(content: String, status: String, id: String)

    @Query("UPDATE messages SET content = :content WHERE id = :id")
    fun updateMessageContent(content: String, id: String)

    @Transaction
    @Query("SELECT * FROM messages WHERE id = :messageId")
    fun findMessageById(messageId: String): Message?

    @Query("SELECT status FROM messages WHERE id = :messageId")
    fun findMessageStatusById(messageId: String): String?

    // id not null means message exists
    @Query("SELECT id FROM messages WHERE id = :messageId")
    fun findMessageIdById(messageId: String): String?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT id, created_at FROM messages WHERE conversation_id = :conversationId " +
        "AND user_id != :userId AND status = 'DELIVERED' ORDER BY created_at ASC")
    fun findUnreadMessagesSync(conversationId: String, userId: String = Session.getAccountId()!!): List<MessageMinimal>?

    @Query("SELECT id FROM messages WHERE conversation_id = :conversationId AND user_id = :userId AND " +
        "status = 'FAILED' ORDER BY created_at DESC LIMIT 1000")
    fun findFailedMessages(conversationId: String, userId: String): List<String>?

    @Query("SELECT m.id as messageId, m.media_url as mediaUrl FROM messages m WHERE conversation_id = :conversationId " +
        "AND category = :category ORDER BY created_at ASC")
    fun getMediaByConversationIdAndCategory(conversationId: String, category: String): List<MediaMessageMinimal>?

    @Query("UPDATE messages SET status = 'READ' WHERE conversation_id = :conversationId AND user_id != :userId " +
        "AND status = 'DELIVERED' AND created_at <= :createdAt")
    fun batchMarkRead(conversationId: String, userId: String, createdAt: String)

    @Query("UPDATE conversations SET unseen_message_count = (SELECT count(1) FROM messages m WHERE m.user_id != :userId " +
        "AND m.status = 'DELIVERED' AND m.conversation_id = :conversationId) WHERE conversation_id = :conversationId")
    fun takeUnseen(userId: String, conversationId: String)
}