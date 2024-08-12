package one.mixin.android.db.fetcher

import kotlinx.coroutines.withContext
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.provider.convertToMessageItems
import one.mixin.android.util.SINGLE_FETCHER_THREAD
import one.mixin.android.vo.MessageItem
import javax.inject.Inject

class MessageFetcher
    @Inject
    constructor(
        val db: MixinDatabase,
    ) {
        companion object {
            private const val SQL = """
               SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId,
               u.full_name AS userFullName, u.identity_number AS userIdentityNumber, u.app_id AS appId, m.category AS type,
               m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus, m.media_waveform AS mediaWaveform,
               m.name AS mediaName, m.media_mime_type AS mediaMimeType, m.media_size AS mediaSize, m.media_width AS mediaWidth, m.media_height AS mediaHeight,
               m.thumb_image AS thumbImage, m.thumb_url AS thumbUrl, m.media_url AS mediaUrl, m.media_duration AS mediaDuration, m.quote_message_id as quoteId,
               m.quote_content as quoteContent, m.caption as caption, u.membership AS membership, u1.full_name AS participantFullName, m.action AS actionName, u1.user_id AS participantUserId,
               COALESCE(s.snapshot_id, ss.snapshot_id) AS snapshotId, COALESCE(s.memo, ss.memo) AS snapshotMemo, COALESCE(s.type, ss.type) AS snapshotType, COALESCE(s.amount, ss.amount) AS snapshotAmount, 
               COALESCE(a.symbol, t.symbol) AS assetSymbol, COALESCE(s.asset_id, ss.asset_id) AS assetId, COALESCE(a.icon_url, t.icon_url) AS assetIcon, t.collection_hash AS assetCollectionHash, 
               st.asset_url AS assetUrl, st.asset_width AS assetWidth, st.asset_height AS assetHeight, st.sticker_id AS stickerId,
               st.name AS assetName, st.asset_type AS assetType, h.site_name AS siteName, h.site_title AS siteTitle, h.site_description AS siteDescription,
               h.site_image AS siteImage, m.shared_user_id AS sharedUserId, su.full_name AS sharedUserFullName, su.identity_number AS sharedUserIdentityNumber,
               su.avatar_url AS sharedUserAvatarUrl, su.is_verified AS sharedUserIsVerified, su.app_id AS sharedUserAppId, su.membership AS sharedMembership, mm.mentions AS mentions, mm.has_read as mentionRead, 
               pm.message_id IS NOT NULL as isPin, c.name AS groupName, em.expire_in AS expireIn, em.expire_at AS expireAt   
               FROM messages m
               LEFT JOIN users u ON m.user_id = u.user_id
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
            const val SCROLL_THRESHOLD = 15
            const val PAGE_SIZE = 30
            private const val INIT_SIZE = 90 // PAGE_SIZE * 3
        }

        private val currentlyLoadingIds = mutableSetOf<String>()
        private val loadedIds = mutableSetOf<String>()
        private var canLoadAbove = true
        private var canLoadBelow = true

        suspend fun initMessages(
            conversationId: String,
            messageId: String? = null,
            forceBottom: Boolean = false,
        ): Triple<Int, List<MessageItem>, String?> =
            withContext(SINGLE_FETCHER_THREAD) {
                currentlyLoadingIds.clear()
                loadedIds.clear()
                var aroundId = messageId
                if (aroundId == null && !forceBottom) {
                    val idCursor = db.query("SELECT rm.message_id FROM remote_messages_status rm LEFT JOIN messages m ON m.id = rm.message_id WHERE rm.conversation_id = ? AND rm.status = 'DELIVERED' ORDER BY m.created_at ASC, m.rowid ASC LIMIT 1", arrayOf(conversationId))
                    if (idCursor.moveToNext()) {
                        aroundId = idCursor.getString(0)
                    }
                    idCursor.close()
                }
                if (aroundId == null) {
                    // load the last 60 messages
                    val cursor =
                        db.query(
                            "$SQL WHERE m.conversation_id = ? ORDER BY m.created_at DESC, m.rowid DESC LIMIT ?",
                            arrayOf(conversationId, INIT_SIZE),
                        )
                    val result = convertToMessageItems(cursor).reversed()
                    canLoadBelow = false
                    canLoadAbove = result.size >= INIT_SIZE
                    return@withContext Triple(result.size - 1, result, null)
                } else {
                    // Load data containing aroundId
                    val preCursor =
                        db.query("SELECT rowid, created_at FROM messages WHERE id = ?", arrayOf(aroundId))
                    val (rowId, createdAt) =
                        preCursor.use {
                            if (it.moveToNext()) {
                                Pair(it.getInt(0), it.getString(1))
                            } else {
                                return@withContext Triple(-1, emptyList(), null)
                            }
                        }
                    // load next page by aroundId
                    val nextCursor =
                        db.query(
                            "$SQL WHERE m.conversation_id = ? AND m.rowid >= ? AND m.created_at >= ? ORDER BY m.created_at ASC, m.rowid ASC LIMIT ?",
                            arrayOf(conversationId, rowId, createdAt, INIT_SIZE / 2),
                        )
                    val result = convertToMessageItems(nextCursor)
                    canLoadBelow = result.size >= INIT_SIZE / 2
                    val thresholdSize = INIT_SIZE - result.size
                    val previousCursor =
                        db.query(
                            "$SQL WHERE m.conversation_id = ? AND m.rowid < ? AND m.created_at < ? ORDER BY m.created_at DESC, m.rowid DESC LIMIT ?",
                            arrayOf(conversationId, rowId, createdAt, thresholdSize),
                        )
                    val previous = convertToMessageItems(previousCursor).reversed()
                    canLoadAbove = previous.size >= thresholdSize
                    val position =
                        if (previous.isNotEmpty()) {
                            previous.size
                        } else if (result.isNotEmpty()) {
                            0
                        } else {
                            -1
                        }
                    return@withContext Triple(position, previous + result, aroundId)
                }
            }

        suspend fun findMessageById(messageIds: List<String>) =
            withContext(SINGLE_FETCHER_THREAD) {
                val cursor = db.query("$SQL WHERE m.id IN ${messageIds.joinToString(", ", "(", ")", transform = { "'$it'" })}", arrayOf())
                return@withContext convertToMessageItems(cursor)
            }

        fun isBottom() = !canLoadBelow

        fun isTop() = !canLoadAbove

        suspend fun nextPage(
            conversationId: String,
            messageId: String,
        ) =
            withContext(SINGLE_FETCHER_THREAD) {
                if (!canLoadBelow || currentlyLoadingIds.contains(messageId) || loadedIds.contains(messageId)) {
                    return@withContext emptyList()
                }
                currentlyLoadingIds.add(messageId)
                try {
                    val preCursor = db.query("SELECT rowid, created_at FROM messages WHERE id = ?", arrayOf(messageId))
                    val (rowId, createdAt) =
                        preCursor.use {
                            it.moveToNext()
                            Pair(it.getInt(0), it.getString(1))
                        }
                    val cursor =
                        db.query(
                            "$SQL WHERE m.conversation_id = ? AND m.rowid > ? AND m.created_at >= ? ORDER BY m.created_at ASC, m.rowid ASC LIMIT ?",
                            arrayOf(conversationId, rowId, createdAt, PAGE_SIZE),
                        )
                    return@withContext convertToMessageItems(cursor).also {
                        if (it.size < PAGE_SIZE) {
                            canLoadBelow = false
                        }
                    }
                } finally {
                    currentlyLoadingIds.remove(messageId)
                    loadedIds.add(messageId)
                }
            }

        suspend fun previousPage(
            conversationId: String,
            messageId: String,
        ) =
            withContext(SINGLE_FETCHER_THREAD) {
                if (!canLoadAbove || currentlyLoadingIds.contains(messageId) || loadedIds.contains(messageId)) {
                    return@withContext emptyList()
                }
                currentlyLoadingIds.add(messageId)
                try {
                    val preCursor =
                        db.query("SELECT rowid, created_at FROM messages WHERE id = ?", arrayOf(messageId))
                    val (rowId, createdAt) =
                        preCursor.use {
                            it.moveToNext()
                            Pair(it.getInt(0), it.getString(1))
                        }
                    val cursor =
                        db.query(
                            "$SQL WHERE m.conversation_id = ? AND m.rowid < ? AND m.created_at <= ? ORDER BY m.created_at DESC, m.rowid DESC LIMIT ?",
                            arrayOf(conversationId, rowId, createdAt, PAGE_SIZE),
                        )
                    return@withContext convertToMessageItems(cursor).reversed().also {
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
