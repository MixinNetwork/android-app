package one.mixin.android.db.cache

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.BaseDao
import one.mixin.android.vo.Message

@Dao
interface CacheMessageDao : BaseDao<CacheMessage> {

    @Query("SELECT * FROM cache_messages ORDER BY created_at ASC limit 100")
    suspend fun getMessages(): List<Message>

    @Query("SELECT * FROM cache_messages WHERE id = :messageId")
    fun findMessageById(messageId: String): Message?

    @Query("UPDATE cache_messages SET status = 'SENT' WHERE id = :id AND status = 'FAILED'")
    fun recallFailedMessage(id: String)

    @Query(
        """
        UPDATE cache_messages SET category = 'MESSAGE_RECALL', content = NULL, media_url = NULL, media_mime_type = NULL, media_size = NULL, 
        media_duration = NULL, media_width = NULL, media_height = NULL, media_hash = NULL, thumb_image = NULL, media_key = NULL, 
        media_digest = NUll, media_status = NULL, `action` = NULL, participant_id = NULL, snapshot_id = NULL, hyperlink = NULL, name = NULL, 
        album_id = NULL, sticker_id = NULL, shared_user_id = NULL, media_waveform = NULL, quote_message_id = NULL, quote_content = NULL WHERE id = :id
        """
    )
    fun recallMessage(id: String)

    @Query("UPDATE cache_messages SET content = NULL WHERE category = 'MESSAGE_PIN' AND quote_message_id = :id AND conversation_id = :conversationId")
    fun recallPinMessage(id: String, conversationId: String)

    @Query("DELETE FROm cache_messages WHERE id in (:ids)")
    fun deleteByIds(ids: List<String>)
}
