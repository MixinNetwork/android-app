package one.mixin.android.db.fetcher

import kotlinx.coroutines.withContext
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.provider.convertChatHistoryMessageItem
import one.mixin.android.util.SINGLE_FETCHER_THREAD
import one.mixin.android.vo.ChatHistoryMessageItem
import javax.inject.Inject

class TranscriptMessageFetcher @Inject constructor(
    val db: MixinDatabase,
) {
    companion object {
        private const val SQL = """
                SELECT t.transcript_id AS transcriptId, t.message_id AS messageId, t.user_id AS userId , IFNULL(u.full_name, t.user_full_name) AS userFullName, u.app_id AS appId, u.identity_number AS userIdentityNumber,
                t.category AS type, t.content, t.created_at AS createdAt, t.media_status AS mediaStatus, t.media_name AS mediaName, t.media_mime_type AS mediaMimeType, t.media_size AS mediaSize,
                t.thumb_image AS thumbImage, t.thumb_url AS thumbUrl, t.media_url AS mediaUrl, t.media_width AS mediaWidth, t.media_height AS mediaHeight, st.asset_width AS assetWidth, 
                st.asset_height AS assetHeight, st.asset_url AS assetUrl, st.asset_type AS assetType,t.media_duration AS mediaDuration, 
                t.media_waveform AS mediaWaveform, su.user_id AS sharedUserId, su.full_name AS sharedUserFullName, su.avatar_url AS sharedUserAvatarUrl, 
                su.app_id AS sharedUserAppId, su.identity_number AS sharedUserIdentityNumber, su.is_verified AS sharedUserIsVerified, t.quote_id AS quoteId,
                t.quote_content AS quoteContent, t.mentions AS mentions
                FROM transcript_messages t
                LEFT JOIN users u on t.user_id = u.user_id
                LEFT JOIN users su ON t.shared_user_id = su.user_id
                LEFT JOIN stickers st ON st.sticker_id = t.sticker_id
        """
    }

    private val currentlyLoadingIds = mutableSetOf<String>()
    private val loadedIds = mutableSetOf<String>()

    suspend fun initMessages(transcriptId: String): List<ChatHistoryMessageItem> = withContext(SINGLE_FETCHER_THREAD) {
        currentlyLoadingIds.clear()
        loadedIds.clear()
        val idCursor = db.query("SELECT t.message_id FROM transcript_messages t WHERE t.transcript_id = ? ORDER BY t.created_at ASC, t.rowid ASC", arrayOf(transcriptId))
        val ids = mutableListOf<String>()
        while (idCursor.moveToNext()) {
            ids.add(idCursor.getString(0))
        }
        if (ids.isNotEmpty()) {
            return@withContext findMessageById(ids)
        }
        return@withContext emptyList()
    }

    suspend fun findMessageById(messageIds: List<String>) = withContext(SINGLE_FETCHER_THREAD) {
        val cursor = db.query("$SQL WHERE t.message_id IN ${messageIds.joinToString(", ", "(", ")", transform = { "'$it'" })}", arrayOf())
        return@withContext convertChatHistoryMessageItem(cursor)
    }
}
