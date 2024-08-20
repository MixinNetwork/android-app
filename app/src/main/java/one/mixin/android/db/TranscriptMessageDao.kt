package one.mixin.android.db

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import one.mixin.android.db.contants.AUDIOS
import one.mixin.android.db.contants.DATA
import one.mixin.android.db.contants.IMAGES
import one.mixin.android.db.contants.LIVES
import one.mixin.android.db.contants.TRANSCRIPTS
import one.mixin.android.db.contants.VIDEOS
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.TranscriptAttachmentMigration
import one.mixin.android.vo.TranscriptMessage

@Dao
interface TranscriptMessageDao : BaseDao<TranscriptMessage> {
    companion object {
        const val ATTACHMENT_CATEGORY =
            "$IMAGES, $VIDEOS, $AUDIOS, $DATA"
    }

    @Query("SELECT count(1) FROM transcript_messages WHERE transcript_id = :transcriptId AND category IN ($ATTACHMENT_CATEGORY) AND media_status IN ('PENDING', 'CANCELED')")
    fun hasUploadedAttachment(transcriptId: String): Int

    @Query("SELECT count(1) FROM transcript_messages WHERE transcript_id = :transcriptId AND category IN ($ATTACHMENT_CATEGORY) AND media_status IN ('PENDING', 'CANCELED')")
    suspend fun hasUploadedAttachmentSuspend(transcriptId: String): Int

    @Query("SELECT * FROM transcript_messages WHERE message_id = :messageId AND category IN ($IMAGES, $VIDEOS, $DATA, $AUDIOS) AND (media_status = 'DONE' OR media_status = 'READ')")
    fun findAttachmentMessage(messageId: String): TranscriptMessage?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM transcript_messages WHERE transcript_id = :transcriptId")
    fun getTranscript(transcriptId: String): List<TranscriptMessage>

    @Query("UPDATE transcript_messages SET content = :content, media_key = :mediaKey, media_digest =:mediaDigest, media_status = :mediaStatus, media_created_at = :mediaCreatedAt  WHERE transcript_id = :transcriptId AND message_id = :messageId")
    fun updateTranscript(
        transcriptId: String,
        messageId: String,
        content: String,
        mediaKey: ByteArray?,
        mediaDigest: ByteArray?,
        mediaStatus: String,
        mediaCreatedAt: String,
    )

    @Query("UPDATE transcript_messages SET media_url = :mediaUrl, media_size = :mediaSize, media_status = :mediaStatus WHERE transcript_id = :transcriptId AND message_id = :messageId")
    fun updateMedia(
        mediaUrl: String,
        mediaSize: Long,
        mediaStatus: String,
        transcriptId: String,
        messageId: String,
    )

    @Query("UPDATE transcript_messages SET media_status = :mediaStatus WHERE transcript_id = :transcriptId AND message_id = :messageId")
    fun updateMediaStatus(
        transcriptId: String,
        messageId: String,
        mediaStatus: String,
    )

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT t.transcript_id AS transcriptId, t.message_id AS messageId, t.user_id AS userId , IFNULL(u.full_name, t.user_full_name) AS userFullName, u.app_id AS appId, u.identity_number AS userIdentityNumber,
        t.category AS type, t.content, t.created_at AS createdAt, t.media_status AS mediaStatus, t.media_name AS mediaName, t.media_mime_type AS mediaMimeType, t.media_size AS mediaSize,
        t.thumb_image AS thumbImage, t.thumb_url AS thumbUrl, t.media_url AS mediaUrl, t.media_width AS mediaWidth, t.media_height AS mediaHeight, st.asset_width AS assetWidth, 
        st.asset_height AS assetHeight, st.asset_url AS assetUrl, st.asset_type AS assetType,t.media_duration AS mediaDuration, 
        t.media_waveform AS mediaWaveform, su.user_id AS sharedUserId, su.full_name AS sharedUserFullName, su.avatar_url AS sharedUserAvatarUrl, 
        su.app_id AS sharedUserAppId, su.identity_number AS sharedUserIdentityNumber, su.is_verified AS sharedUserIsVerified, t.quote_id AS quoteId,
        t.quote_content AS quoteContent, t.mentions AS mentions, u.membership as membership 
        FROM transcript_messages t
        LEFT JOIN users u on t.user_id = u.user_id
        LEFT JOIN users su ON t.shared_user_id = su.user_id
        LEFT JOIN stickers st ON st.sticker_id = t.sticker_id
        WHERE t.transcript_id = :transcriptId
        ORDER BY t.created_at ASC, t.rowid ASC
        """,
    )
    fun getTranscriptMessages(transcriptId: String): DataSource.Factory<Int, ChatHistoryMessageItem>

    @Query("SELECT count(1) FROM transcript_messages WHERE created_at < (SELECT created_at FROM transcript_messages WHERE transcript_id = :transcriptId AND message_id = :messageId) AND transcript_id = :transcriptId")
    suspend fun findTranscriptMessageIndex(
        transcriptId: String,
        messageId: String,
    ): Int

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """  SELECT t.transcript_id AS transcriptId, t.message_id AS messageId, t.user_id AS userId , IFNULL(u.full_name, t.user_full_name) AS userFullName, u.app_id AS appId, u.identity_number AS userIdentityNumber,
        t.category AS type, t.content, t.created_at AS createdAt, t.media_status AS mediaStatus, t.media_name AS mediaName, 
        t.thumb_image AS thumbImage, t.media_url AS mediaUrl, t.media_width AS mediaWidth, t.media_height AS mediaHeight, st.asset_width AS assetWidth, 
        st.asset_height AS assetHeight, st.asset_url AS assetUrl, st.asset_type AS assetType,t.media_duration AS mediaDuration, 
        t.media_waveform AS mediaWaveform, su.user_id AS sharedUserId, su.full_name AS sharedUserFullName, su.avatar_url AS sharedUserAvatarUrl, 
        su.app_id AS sharedUserAppId, su.identity_number AS sharedUserIdentityNumber, su.is_verified AS sharedUserIsVerified, t.quote_id AS quoteId,
        t.quote_content AS quoteContent, t.mentions AS mentions, u.membership as membership
        FROM transcript_messages t
        LEFT JOIN users u on t.user_id = u.user_id
        LEFT JOIN users su ON t.shared_user_id = su.user_id
        LEFT JOIN stickers st ON st.sticker_id = t.sticker_id
        WHERE t.transcript_id = :transcriptId
        AND t.category IN ($IMAGES, $VIDEOS, $LIVES)
        ORDER BY t.created_at ASC, t.rowid ASC
        """,
    )
    suspend fun getTranscriptMediaMessage(transcriptId: String): List<ChatHistoryMessageItem>

    @Query(
        """
            SELECT count(1) FROM transcript_messages 
            WHERE transcript_id = :transcriptId
            AND category IN ($IMAGES, $VIDEOS, $LIVES) 
            AND created_at < (SELECT created_at FROM transcript_messages WHERE message_id = :messageId AND transcript_id = :transcriptId)
            ORDER BY created_at ASC, rowid ASC
        """,
    )
    suspend fun indexTranscriptMediaMessages(
        transcriptId: String,
        messageId: String,
    ): Int

    @Query("SELECT * FROM transcript_messages WHERE transcript_id = :transcriptId")
    suspend fun getTranscriptsById(transcriptId: String): List<TranscriptMessage>

    @Query("SELECT * FROM transcript_messages WHERE transcript_id = :transcriptId AND message_id = :messageId")
    suspend fun getTranscriptById(
        transcriptId: String,
        messageId: String,
    ): TranscriptMessage?

    @Query("SELECT * FROM transcript_messages WHERE transcript_id = :transcriptId AND message_id = :messageId")
    fun getTranscriptByIdSync(
        transcriptId: String,
        messageId: String,
    ): TranscriptMessage?

    @Query("SELECT * FROM transcript_messages WHERE message_id = :messageId LIMIT 1")
    fun getTranscriptByMessageId(messageId: String): TranscriptMessage?

    @Query("SELECT sum(media_size) FROM messages WHERE conversation_id = :conversationId AND category IN ($TRANSCRIPTS)")
    fun getMediaSizeTotalById(conversationId: String): Long?

    @Query("SELECT count(1) FROM messages WHERE conversation_id = :conversationId AND category IN ($TRANSCRIPTS)")
    fun countTranscriptByConversationId(conversationId: String): Int

    @Query("SELECT count(1) FROM transcript_messages WHERE message_id = :messageId")
    fun countTranscriptByMessageId(messageId: String): Int

    @Query("DELETE FROM transcript_messages WHERE transcript_id = :transcriptId")
    fun deleteTranscript(transcriptId: String)

    @Query("SELECT rowid FROM transcript_messages WHERE category IN ($IMAGES, $VIDEOS,  $DATA, $AUDIOS) AND media_status = 'DONE' ORDER BY rowid DESC LIMIT 1")
    suspend fun lastDoneAttachmentId(): Long?

    @Query(
        "SELECT rowid, message_id, media_url FROM transcript_messages WHERE category IN ($IMAGES, $VIDEOS,  $DATA, $AUDIOS) AND media_status = 'DONE' AND rowid <= :rowId ORDER BY rowid DESC LIMIT :limit",
    )
    suspend fun findAttachmentMigration(
        rowId: Long,
        limit: Int,
    ): List<TranscriptAttachmentMigration>

    @Query("UPDATE transcript_messages SET media_url = :mediaUrl WHERE message_id = :messageId")
    suspend fun updateMediaUrl(
        mediaUrl: String,
        messageId: String,
    )

    @Query("SELECT count(1) FROM transcript_messages")
    fun countTranscriptMessages(): Long

    @Query("SELECT count(1) FROM transcript_messages WHERE transcript_id IN (SELECT id FROM messages WHERE rowid > :rowId)")
    fun countTranscriptMessages(rowId: Long): Long

    @Query("SELECT count(1) FROM transcript_messages WHERE transcript_id IN (SELECT id FROM messages WHERE conversation_id IN (:conversationIds))")
    fun countTranscriptMessages(conversationIds: Collection<String>): Long

    @Query("SELECT count(1) FROM transcript_messages WHERE transcript_id IN (SELECT id FROM messages WHERE rowid > :rowId AND conversation_id IN (:conversationIds))")
    fun countTranscriptMessages(
        rowId: Long,
        conversationIds: Collection<String>,
    ): Long

    @Query("SELECT count(1) FROM transcript_messages WHERE transcript_id IN (SELECT id FROM messages WHERE rowid > :rowId AND created_at >= :createdAt)")
    fun countTranscriptMessages(
        rowId: Long,
        createdAt: String,
    ): Long

    @Query("SELECT count(1) FROM transcript_messages WHERE transcript_id IN (SELECT id FROM messages WHERE rowid > :rowId AND conversation_id IN (:conversationIds) AND created_at >= :createdAt)")
    fun countTranscriptMessages(
        rowId: Long,
        conversationIds: Collection<String>,
        createdAt: String,
    ): Long
}
