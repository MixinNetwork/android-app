package one.mixin.android.db.fetcher

import kotlinx.coroutines.withContext
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.provider.convertChatHistoryMessageItem
import one.mixin.android.util.SINGLE_FETCHER_THREAD
import one.mixin.android.vo.ChatHistoryMessageItem
import timber.log.Timber
import javax.inject.Inject

class PinMessageFetcher @Inject constructor(
    val db: MixinDatabase,
) {
    companion object {
        private const val SQL = """
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
                LEFT JOIN messages m ON m.id = pm.message_id
                LEFT JOIN users u ON m.user_id = u.user_id
                LEFT JOIN users u1 ON m.participant_id = u1.user_id
                LEFT JOIN snapshots s ON m.snapshot_id = s.snapshot_id
                LEFT JOIN assets a ON s.asset_id = a.asset_id
                LEFT JOIN stickers st ON st.sticker_id = m.sticker_id
                LEFT JOIN hyperlinks h ON m.hyperlink = h.hyperlink
                LEFT JOIN users su ON m.shared_user_id = su.user_id
                LEFT JOIN conversations c ON m.conversation_id = c.conversation_id
                LEFT JOIN message_mentions mm ON m.id = mm.message_id
        """
        const val SCROLL_THRESHOLD = 10
        private const val PAGE_SIZE = 20
        private const val INIT_SIZE = 60 // PAGE_SIZE * 3
    }

    private val currentlyLoadingIds = mutableSetOf<String>()
    private val loadedIds = mutableSetOf<String>()
    private var canLoadAbove = false

    suspend fun initMessages(conversationId: String): List<ChatHistoryMessageItem> = withContext(SINGLE_FETCHER_THREAD) {
        currentlyLoadingIds.clear()
        loadedIds.clear()
        val cursor = db.query("SELECT pm.message_id FROM pin_messages pm  LEFT JOIN messages m ON m.id = pm.message_id WHERE pm.conversation_id = ? ORDER BY m.created_at DESC LIMIT ?", arrayOf(conversationId, INIT_SIZE))
        val ids = mutableListOf<String>()
        while (cursor.moveToNext()) {
            ids.add(cursor.getString(0))
        }
        if (ids.isNotEmpty()) {
            Timber.e("${ids}")
            val result = findMessageById(ids)
            canLoadAbove = result.size >= INIT_SIZE
            return@withContext result
        }
        return@withContext emptyList()
    }

    suspend fun findMessageById(messageIds: List<String>) = withContext(SINGLE_FETCHER_THREAD) {
        val cursor = db.query("$SQL WHERE pm.message_id IN ${messageIds.joinToString(", ", "(", ")", transform = { "'$it'" })} ORDER BY m.created_at ASC", arrayOf())
        Timber.e(messageIds.joinToString(", ", "(", ")", transform = { "'$it'" }))
        return@withContext convertChatHistoryMessageItem(cursor)
    }

    suspend fun previousPage(conversationId: String, messageId: String) =
        withContext(SINGLE_FETCHER_THREAD) {
            if (!canLoadAbove || currentlyLoadingIds.contains(messageId) || loadedIds.contains(messageId)) {
                return@withContext emptyList()
            }
            currentlyLoadingIds.add(messageId)
            try {
                val preCursor =
                    db.query("SELECT rowid, created_at FROM messages WHERE id = ?", arrayOf(messageId))
                val (rowId, createdAt) = preCursor.use {
                    it.moveToNext()
                    Pair(it.getInt(0), it.getString(1))
                }
                val cursor = db.query(
                    "$SQL WHERE m.conversation_id = ? AND m.rowid < ? AND m.created_at <= ? ORDER BY m.created_at DESC, m.rowid DESC LIMIT ?",
                    arrayOf(conversationId, rowId, createdAt, PAGE_SIZE),
                )
                return@withContext convertChatHistoryMessageItem(cursor).reversed().also {
                    if (it.size < PAGE_SIZE) {
                        canLoadAbove = false
                    }
                }
            } finally {
                currentlyLoadingIds.remove(messageId)
                loadedIds.add(messageId)
            }
        }
}
