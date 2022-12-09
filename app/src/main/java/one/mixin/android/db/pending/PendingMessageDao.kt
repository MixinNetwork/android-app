package one.mixin.android.db.pending

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import kotlinx.coroutines.flow.Flow
import one.mixin.android.db.BaseDao
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageMedia

@Dao
interface PendingMessageDao : BaseDao<PendingMessage> {

    @Query("SELECT * FROM pending_messages ORDER BY created_at ASC limit 100")
    fun getMessages(): Flow<List<Message>>

    @Query("SELECT * FROM pending_messages WHERE id = :messageId")
    fun findMessageById(messageId: String): Message?

    @Query("SELECT id FROM pending_messages WHERE id = :messageId")
    fun findMessageIdById(messageId: String): String?

    @Query("SELECT id FROM pending_messages WHERE conversation_id = :conversationId AND user_id = :userId AND status = 'FAILED' ORDER BY created_at DESC LIMIT 1000")
    fun findFailedMessages(conversationId: String, userId: String): List<String>

    @Query("SELECT count(id) FROM pending_messages WHERE conversation_id = :conversationId AND quote_message_id = :messageId AND quote_content IS NULL")
    fun countMessageByQuoteId(conversationId: String, messageId: String): Int

    @Query(
        """
        SELECT m.id FROM pending_messages m
        WHERE m.conversation_id = :conversationId AND m.id = :messageId AND m.status != 'FAILED'
        """,
    )
    fun findMessageItemById(conversationId: String, messageId: String): String?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT category, id, conversation_id, media_url FROM pending_messages WHERE id = :messageId")
    fun findMessageMediaById(messageId: String): MessageMedia?

    @Query("UPDATE pending_messages SET quote_content = :content WHERE conversation_id = :conversationId AND quote_message_id = :messageId")
    fun updateQuoteContentByQuoteId(conversationId: String, messageId: String, content: String)

    @Query("UPDATE pending_messages SET content = :content, status = :status WHERE id = :id AND category != 'MESSAGE_RECALL'")
    fun updateMessageContentAndStatus(content: String, status: String, id: String)

    @Query(
        """
        UPDATE pending_messages SET content = :content, media_mime_type = :mediaMimeType, 
        media_size = :mediaSize, media_width = :mediaWidth, media_height = :mediaHeight, 
        thumb_image = :thumbImage, media_key = :mediaKey, media_digest = :mediaDigest, media_duration = :mediaDuration, 
        media_status = :mediaStatus, status = :status, name = :name, media_waveform = :mediaWaveform WHERE id = :messageId 
        AND category != 'MESSAGE_RECALL'
        """,
    )
    fun updateAttachmentMessage(messageId: String, content: String, mediaMimeType: String, mediaSize: Long, mediaWidth: Int?, mediaHeight: Int?, thumbImage: String?, name: String?, mediaWaveform: ByteArray?, mediaDuration: String?, mediaKey: ByteArray?, mediaDigest: ByteArray?, mediaStatus: String, status: String)

    @Query("UPDATE pending_messages SET sticker_id = :stickerId, status = :status WHERE id = :messageId AND category != 'MESSAGE_RECALL'")
    fun updateStickerMessage(stickerId: String, status: String, messageId: String)

    @Query("UPDATE pending_messages SET shared_user_id = :sharedUserId, status = :status WHERE id = :messageId AND category != 'MESSAGE_RECALL'")
    fun updateContactMessage(sharedUserId: String, status: String, messageId: String)

    @Query(
        """
        UPDATE pending_messages SET media_width = :width, media_height = :height, media_url=:url, thumb_url = :thumbUrl, status = :status 
        WHERE id = :messageId AND category != 'MESSAGE_RECALL'
    """,
    )
    fun updateLiveMessage(width: Int, height: Int, url: String, thumbUrl: String, status: String, messageId: String)

    @Query(
        """
        UPDATE pending_messages SET content = :content, media_size = :mediaSize, media_status = :mediaStatus, status = :status 
        WHERE id = :messageId AND category != 'MESSAGE_RECALL'
        """,
    )
    fun updateTranscriptMessage(content: String?, mediaSize: Long?, mediaStatus: String?, status: String, messageId: String)

    @Query("UPDATE pending_messages SET status = 'SENT' WHERE id = :id AND status = 'FAILED'")
    fun recallFailedMessage(id: String)

    @Query(
        """
        UPDATE pending_messages SET category = 'MESSAGE_RECALL', content = NULL, media_url = NULL, media_mime_type = NULL, media_size = NULL, 
        media_duration = NULL, media_width = NULL, media_height = NULL, media_hash = NULL, thumb_image = NULL, media_key = NULL, 
        media_digest = NUll, media_status = NULL, `action` = NULL, participant_id = NULL, snapshot_id = NULL, hyperlink = NULL, name = NULL, 
        album_id = NULL, sticker_id = NULL, shared_user_id = NULL, media_waveform = NULL, quote_message_id = NULL, quote_content = NULL WHERE id = :id
        """,
    )
    fun recallMessage(id: String)

    @Query("UPDATE pending_messages SET content = NULL WHERE category = 'MESSAGE_PIN' AND quote_message_id = :id AND conversation_id = :conversationId")
    fun recallPinMessage(id: String, conversationId: String)

    @Query("DELETE FROM pending_messages WHERE id = :id")
    fun deleteById(id: String)

    @Query("DELETE FROM pending_messages WHERE id IN (:ids)")
    fun deleteByIds(ids: List<String>)

    @Query("UPDATE pending_messages SET status = 'READ' WHERE id IN (:ids)")
    fun markReadIds(ids: List<String>)
}
