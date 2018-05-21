package one.mixin.android.db

import android.arch.paging.DataSource
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.RoomWarnings
import android.arch.persistence.room.Transaction
import io.reactivex.Flowable
import io.reactivex.Maybe
import one.mixin.android.util.Session
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageItem

@Dao
interface MessageDao : BaseDao<Message> {

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT m.id as messageId, m.conversation_id as conversationId, u.user_id as userId, " +
        "u.full_name as userFullName, u.identity_number as userIdentityNumber, u.app_id as appId, m.category as type, " +
        "m.content as content, m.created_at as createdAt, m.status as status, m.media_status as mediaStatus, " +
        "m.name as mediaName, m.media_mine_type as mediaMimeType, m.media_size as mediaSize, m.media_width as mediaWidth, m.media_height as mediaHeight, " +
        "m.thumb_image as thumbImage, m.media_url as mediaUrl, " +
        "u1.full_name as participantFullName, m.action as actionName, u1.user_id as participantUserId, " +
        "s.snapshot_id as snapshotId, s.type as snapshotType, s.amount as snapshotAmount, a.symbol as assetSymbol, a.asset_id as assetId, " +
        "a.icon_url as assetIcon, st.asset_url as assetUrl, st.asset_width as assetWidth, st.asset_height as assetHeight, st.album_id as albumId, " +
        "st.name as assetName, h.site_name as siteName, h.site_title as siteTitle, h.site_description as siteDescription, h.site_image as siteImage, " +
        "m.shared_user_id as sharedUserId, su.full_name as sharedUserFullName, su.identity_number as sharedUserIdentityNumber, " +
        "su.avatar_url as sharedUserAvatarUrl, su.is_verified as sharedUserIsVerified, su.app_id as sharedUserAppId " +
        "FROM messages m " +
        "INNER JOIN users u ON m.user_id = u.user_id " +
        "LEFT JOIN users u1 ON m.participant_id = u1.user_id " +
        "LEFT JOIN snapshots s ON m.snapshot_id = s.snapshot_id " +
        "LEFT JOIN assets a ON s.asset_id = a.asset_id " +
        "LEFT JOIN stickers st ON st.name = m.name AND st.album_id = m.album_id " +
        "LEFT JOIN hyperlinks h ON m.hyperlink = h.hyperlink " +
        "LEFT JOIN users su ON m.shared_user_id = su.user_id " +
        "WHERE m.conversation_id = :conversationId " +
        "ORDER BY m.created_at DESC")
    fun getMessages(conversationId: String): DataSource.Factory<Int, MessageItem>

    @Query("SELECT m.id as messageId " +
        "FROM messages m " +
        "WHERE m.conversation_id = :conversationId " +
        "ORDER BY m.created_at DESC")
    fun getMessagesMinimal(conversationId: String): List<String>

    @Query("SELECT count(1) FROM messages WHERE conversation_id = :conversationId AND user_id != :userId " +
        "AND created_at > (SELECT created_at FROM messages m WHERE m.conversation_id = :conversationId " +
        "AND m.status = 'READ' AND m.user_id != :userId ORDER BY m.created_at DESC LIMIT 1)")
    fun indexUnread(conversationId: String, userId: String): Maybe<Int>

    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId")
    fun getMessageList(conversationId: String): List<Message>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT m.id as messageId, m.conversation_id as conversationId, u.user_id as userId, " +
        "u.full_name as userFullName, u.identity_number as userIdentityNumber, m.category as type, " +
        "m.content as content, m.created_at as createdAt, m.status as status, m.media_status as mediaStatus," +
        "m.media_width as mediaWidth, m.media_height as mediaHeight, m.thumb_image as thumbImage, m.media_url as mediaUrl " +
        "FROM messages m INNER JOIN users u ON m.user_id = u.user_id WHERE m.conversation_id = :conversationId " +
        "and (m.category = 'SIGNAL_IMAGE' OR m.category = 'PLAIN_IMAGE' OR m.category = 'SIGNAL_VIDEO' OR m.category " +
        "= 'PLAIN_VIDEO') AND m.media_status = 'DONE' " +
        "ORDER BY m.created_at DESC")
    fun getMediaMessages(conversationId: String): List<MessageItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT m.id as messageId, m.conversation_id as conversationId, u.user_id as userId, " +
        "u.avatar_url as userAvatarUrl, u.full_name as userFullName, u.identity_number as userIdentityNumber, m.category as type, " +
        "m.content as content, m.created_at as createdAt, m.status as status, m.media_status as mediaStatus," +
        "m.name as mediaName, m.media_width as mediaWidth, m.media_height as mediaHeight, m.thumb_image as thumbImage, m.media_url as mediaUrl " +
        "FROM messages m INNER JOIN users u ON m.user_id = u.user_id " +
        "WHERE (m.category = 'SIGNAL_TEXT' AND m.status != 'FAILED' AND m.content LIKE :query) " +
        "OR (m.category = 'SIGNAL_DATA' AND m.status != 'FAILED' AND m.name LIKE :query) ORDER BY m.created_at DESC")
    fun fuzzySearchMessage(query: String): List<MessageItem>

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

    @Query("UPDATE messages SET hyperlink = :hyperlink WHERE id = :id")
    fun updateHyperlink(hyperlink: String, id: String)

    @Query("UPDATE messages SET content = :content, media_mine_type = :mediaMimeType, " +
        "media_size = :mediaSize, media_width = :mediaWidth, media_height = :mediaHeight, " +
        "thumb_image = :thumbImage, media_key = :mediaKey, media_digest = :mediaDigest, " +
        "media_status = :mediaStatus, status = :status, name = :name WHERE id = :messageId")
    fun updateAttachmentMessage(
        messageId: String,
        content: String,
        mediaMimeType: String,
        mediaSize: Long,
        mediaWidth: Int?,
        mediaHeight: Int?,
        thumbImage: String?,
        name: String?,
        mediaKey: ByteArray?,
        mediaDigest: ByteArray?,
        mediaStatus: String,
        status: String
    )

    @Query("UPDATE messages SET album_id = :albumId, name =:name, status = :status WHERE id = :messageId")
    fun updateStickerMessage(albumId: String, name: String, status: String, messageId: String)

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
    @Query("SELECT m.id as messageId, m.conversation_id as conversationId, u.user_id as userId, " +
        "u.full_name as userFullName, u.identity_number as userIdentityNumber, m.category as type, " +
        "m.content as content, m.created_at as createdAt, m.status as status, m.media_status as mediaStatus," +
        "m.media_width as mediaWidth, m.media_height as mediaHeight, m.thumb_image as thumbImage, m.media_url as mediaUrl " +
        "FROM messages m INNER JOIN users u ON m.user_id = u.user_id WHERE m.conversation_id = :conversationId " +
        "AND u.user_id != :userId AND m.status == 'DELIVERED' " +
        "ORDER BY m.created_at ASC")
    fun findUnreadMessages(conversationId: String, userId: String = Session.getAccountId()!!): Flowable<List<MessageItem>>

    @Query("SELECT id FROM messages WHERE conversation_id = :conversationId AND user_id = :userId AND " +
        "status = 'FAILED' ORDER BY created_at DESC LIMIT 1000")
    fun findFailedMessages(conversationId: String, userId: String): List<String>?
}