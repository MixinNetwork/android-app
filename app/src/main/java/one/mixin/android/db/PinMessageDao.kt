package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.PinMessage
import one.mixin.android.vo.PinMessageMinimal

@Dao
interface PinMessageDao : BaseDao<PinMessage> {

    @Query("DELETE FROM pin_messages WHERE message_id IN (:messageIds)")
    fun deleteByIds(messageIds: List<String>)

    @Query("SELECT * FROM pin_messages WHERE message_id = :messageId")
    suspend fun findPinMessageById(messageId: String): PinMessage?

    @Query(
        """
        SELECT m.id AS messageId, m.category AS type, m.content AS content FROM pin_messages pm
        LEFT JOIN messages m ON m.id = pm.message_id
        WHERE pm.conversation_id = :conversationId
      """
    )
    suspend fun getPinMessageMinimals(conversationId: String): List<PinMessageMinimal>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
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
        LEFT JOIN message_mentions mm ON m.id = mm.message_id  WHERE m.conversation_id = :conversationId ORDER BY m.created_at ASC"""
    )
    fun getPinMessages(conversationId: String): LiveData<List<ChatHistoryMessageItem>>

    @Query("SELECT count(*) FROM pin_messages WHERE created_at < (SELECT created_at FROM pin_messages WHERE conversation_id = :conversationId AND message_id = :messageId)")
    suspend fun findPinMessageIndex(conversationId: String, messageId: String): Int

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
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
        FROM messages m
        INNER JOIN users u ON m.user_id = u.user_id
        INNER JOIN pin_messages pm ON m.quote_message_id = pm.message_id
        LEFT JOIN message_mentions mm ON m.id = mm.message_id
        LEFT JOIN users u1 ON m.participant_id = u1.user_id
        LEFT JOIN snapshots s ON m.snapshot_id = s.snapshot_id
        LEFT JOIN assets a ON s.asset_id = a.asset_id
        LEFT JOIN stickers st ON st.sticker_id = m.sticker_id
        LEFT JOIN hyperlinks h ON m.hyperlink = h.hyperlink
        LEFT JOIN users su ON m.shared_user_id = su.user_id
        LEFT JOIN conversations c ON m.conversation_id = c.conversation_id
        WHERE m.conversation_id = :conversationId AND m.category = 'MESSAGE_PIN'
        ORDER BY m.created_at DESC
        LIMIT 1"""
    )
    fun getLastPinMessages(conversationId: String): LiveData<MessageItem?>

    @Query("SELECT count(*) FROM pin_messages WHERE conversation_id = :conversationId")
    fun countPinMessages(conversationId: String): LiveData<Int>
}
