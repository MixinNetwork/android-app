package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.Transcript
import one.mixin.android.vo.TranscriptMessageItem

@Dao
interface TranscriptDao : BaseDao<Transcript> {

    companion object {
        const val ATTACHMENT_CATEGORY =
            "'SIGNAL_DATA', 'PLAIN_DATA', 'ENCRYPTED_DATA', 'SIGNAL_IMAGE', 'PLAIN_IMAGE', 'ENCRYPTED_IMAGE', 'SIGNAL_AUDIO', 'PLAIN_AUDIO', 'ENCRYPTED_AUDIO', 'SIGNAL_VIDEO', 'PLAIN_VIDEO', 'ENCRYPTED_VIDEO'"
    }

    @Query("SELECT count(*) FROM transcripts WHERE transcript_id = :transcriptId AND category IN ($ATTACHMENT_CATEGORY) AND media_status IN ('PENDING', 'CANCELED')")
    fun hasUploadedAttachment(transcriptId: String): Int

    @Query("SELECT * FROM transcripts WHERE transcript_id = :transcriptId")
    fun getTranscript(transcriptId: String): List<Transcript>

    @Query("UPDATE transcripts SET content = :content, media_key = :mediaKey, media_digest =:mediaDigest, media_status = :mediaStatus, media_created_at = :mediaCreatedAt  WHERE transcript_id = :transcriptId AND message_id = :messageId")
    fun updateTranscript(
        transcriptId: String,
        messageId: String,
        content: String,
        mediaKey: ByteArray?,
        mediaDigest: ByteArray?,
        mediaStatus: String,
        mediaCreatedAt: String
    )

    @Query("UPDATE transcripts SET media_url = :mediaUrl, media_size = :mediaSize, media_status = :mediaStatus WHERE transcript_id = :transcriptId AND message_id = :messageId")
    fun updateMedia(
        mediaUrl: String,
        mediaSize: Long,
        mediaStatus: String,
        transcriptId: String,
        messageId: String
    )

    @Query("UPDATE transcripts SET media_status = :mediaStatus WHERE transcript_id = :transcriptId AND message_id = :messageId")
    fun updateMediaStatus(transcriptId: String, messageId: String, mediaStatus: String)

    @Query(
        """
        SELECT t.message_id AS messageId, t.user_id AS userId ,t.user_full_name AS userFullName, u.app_id AS appId, u.identity_number AS userIdentityNumber,
        t.category AS type, t.content, t.created_at AS createdAt, t.media_status AS mediaStatus, t.media_name AS mediaName, 
        t.thumb_image AS thumbImage, t.media_url AS mediaUrl, t.media_width AS mediaWidth, t.media_height AS mediaHeight, st.asset_width AS assetWidth, 
        st.asset_height AS assetHeight, st.asset_url AS assetUrl, st.asset_type AS assetType,t.media_duration AS mediaDuration, 
        t.media_waveform AS mediaWaveform, su.user_id AS sharedUserId, su.full_name AS sharedUserFullName, su.avatar_url AS sharedUserAvatarUrl, 
        su.app_id AS sharedUserAppId, su.identity_number AS sharedUserIdentityNumber, su.is_verified AS sharedUserIsVerified, t.quote_id AS quoteId,
        t.quote_content AS quoteContent, t.mentions AS mentions
        FROM transcripts t
        LEFT JOIN users u on t.user_id = u.user_id
        LEFT JOIN users su ON t.shared_user_id = su.user_id
        LEFT JOIN stickers st ON st.sticker_id = t.sticker_id
        WHERE transcript_id = :transcriptId
    """
    )
    fun getTranscriptMessages(transcriptId: String): LiveData<List<TranscriptMessageItem>>

    @Query("SELECT count(*) FROM transcripts WHERE created_at < (SELECT created_at FROM transcripts WHERE transcript_id = :transcriptId AND message_id = :messageId)")
    suspend fun findTranscriptMessageIndex(transcriptId: String, messageId: String): Int

    @Query(
        """  SELECT t.message_id AS messageId, t.user_id AS userId ,t.user_full_name AS userFullName, u.app_id AS appId, u.identity_number AS userIdentityNumber,
        t.category AS type, t.content, t.created_at AS createdAt, t.media_status AS mediaStatus, t.media_name AS mediaName, 
        t.thumb_image AS thumbImage, t.media_url AS mediaUrl, t.media_width AS mediaWidth, t.media_height AS mediaHeight, st.asset_width AS assetWidth, 
        st.asset_height AS assetHeight, st.asset_url AS assetUrl, st.asset_type AS assetType,t.media_duration AS mediaDuration, 
        t.media_waveform AS mediaWaveform, su.user_id AS sharedUserId, su.full_name AS sharedUserFullName, su.avatar_url AS sharedUserAvatarUrl, 
        su.app_id AS sharedUserAppId, su.identity_number AS sharedUserIdentityNumber, su.is_verified AS sharedUserIsVerified, t.quote_id AS quoteId,
        t.quote_content AS quoteContent, t.mentions AS mentions
        FROM transcripts t
        LEFT JOIN users u on t.user_id = u.user_id
        LEFT JOIN users su ON t.shared_user_id = su.user_id
        LEFT JOIN stickers st ON st.sticker_id = t.sticker_id
        WHERE t.transcript_id = :transcriptId
        AND t.category IN ('SIGNAL_IMAGE','PLAIN_IMAGE', 'SIGNAL_VIDEO', 'PLAIN_VIDEO', 'SIGNAL_LIVE', 'PLAIN_LIVE')
        ORDER BY t.created_at DESC, t.rowid DESC
        """
    )
    suspend fun getTranscriptMediaMessage(transcriptId: String): List<TranscriptMessageItem>

    @Query(
        """
            SELECT count(1) FROM transcripts 
            WHERE transcript_id = :transcriptId
            AND category IN ('SIGNAL_IMAGE','PLAIN_IMAGE', 'SIGNAL_VIDEO', 'PLAIN_VIDEO', 'SIGNAL_LIVE', 'PLAIN_LIVE') 
            AND created_at < (SELECT created_at FROM transcripts WHERE message_id = :messageId)
            ORDER BY created_at DESC, rowid DESC
        """
    )
    suspend fun indexTranscriptMediaMessages(transcriptId: String, messageId: String): Int

    @Query("SELECT * FROM transcripts WHERE transcript_id = :transcriptId")
    suspend fun getTranscriptsById(transcriptId: String): List<Transcript>

    @Query("SELECT * FROM transcripts WHERE transcript_id = :transcriptId AND message_id = :messageId")
    suspend fun getTranscriptById(transcriptId: String, messageId: String): Transcript?
}