package one.mixin.android.db.datasource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.RoomDatabase
import one.mixin.android.db.provider.convertToMessageItems
import one.mixin.android.vo.MessageItem
import timber.log.Timber
import java.lang.IllegalArgumentException

class MessageDataSource(private val db: RoomDatabase, val conversationId: String) :
    PagingSource<Int, MessageItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MessageItem> {
        return try {
            var anchorKey = params.key
            if (anchorKey == NONE) {
                anchorKey = initialKey()
                if (anchorKey == null) {
                    return LoadResult.Page(emptyList(), null, null)
                }
            } else if (anchorKey == null || anchorKey < 0) {
                return LoadResult.Error(IllegalArgumentException("Key cannot be less than 0"))
            }
            return getData(rowId = anchorKey, params.loadSize)
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }

    private fun initialKey(): Int? {
        // Offset by 1 position, including an anchor message
        val cursor = db.query(
            "SELECT rowid FROM messages WHERE conversation_id = ? ORDER BY created_at ASC, rowid ASC LIMIT 1",
            arrayOf(conversationId),
        )
        while (cursor.moveToNext()) {
            return cursor.getInt(0)
        }
        return null
    }

    private fun getData(rowId: Int, limit: Int): LoadResult.Page<Int, MessageItem> {
        val (prevKey, prevData) = prevKey(rowId)
        val currentPageData = mutableListOf<MessageItem>()
        db.query(
            """
            $QUERY_SQL WHERE m.rowid >= ? AND m.conversation_id = ? ORDER BY m.created_at ASC, m.rowid ASC LIMIT ?""",
            arrayOf(rowId, conversationId, limit),
        ).use { cursor ->
            currentPageData.addAll(convertToMessageItems(cursor))
        }
        val (nextKey, nextData) =
            if (currentPageData.size == limit) {
                nextKey(rowId, limit)
            } else {
                Pair(null, null)
            }
        val result = mutableListOf<MessageItem>()
        if (!prevData.isNullOrEmpty()) {
            result.addAll(prevData)
        }
        result.addAll(currentPageData)
        if (!nextData.isNullOrEmpty()) {
            result.addAll(nextData)
        }
        return LoadResult.Page(currentPageData, prevKey, nextKey)
    }

    private fun prevKey(rowId: Int): Pair<Int?, Collection<MessageItem>?> {
        val prevCursor = db.query(
            "SELECT rowid FROM messages WHERE rowid < ? AND conversation_id = ? ORDER BY created_at DESC, rowid DESC  LIMIT 1 OFFSET ?",
            arrayOf(rowId, conversationId, PAGE_SIZE - 1),
        )
        if (prevCursor.moveToNext()) {
            return Pair(prevCursor.getInt(0), null)
        } else {
            db.query(
                "$QUERY_SQL WHERE m.rowid < ? AND m.conversation_id = ? ORDER BY m.created_at ASC, m.rowid ASC LIMIT ?",
                arrayOf(rowId, conversationId, PAGE_SIZE),
            ).use { cursor ->
                return Pair(null, convertToMessageItems(cursor))
            }
        }
    }

    private fun nextKey(rowId: Int, limit: Int): Pair<Int?, Collection<MessageItem>?> {
        val nextCursor = db.query(
            "SELECT rowid FROM messages WHERE rowid > ? AND conversation_id = ? ORDER BY created_at ASC, rowid ASC LIMIT 1 OFFSET ?",
            arrayOf(rowId, conversationId, limit - 1),
        )
        if (nextCursor.moveToNext()) {
            return Pair(nextCursor.getInt(0), null)
        } else {
            db.query(
                "$QUERY_SQL FROM messages WHERE m.rowid > ? AND m.conversation_id = ? ORDER BY m.created_at ASC, m.rowid ASC LIMIT ? OFFSET ?",
                arrayOf(rowId, conversationId, PAGE_SIZE, limit - 1),
            ).use { cursor ->
                return Pair(null, convertToMessageItems(cursor))
            }
        }
    }

    private fun getKeyFromPosition(position: Int): Int? {
        db.query("SELECT rowid FROM messages WHERE conversation_id = ? ORDER BY created_at DESC, rowid DESC LIMIT 1 OFFSET ?", arrayOf(conversationId, position)).use {
            if (it.moveToNext()) {
                return it.getInt(0).apply {
                    db.query("SELECT content FROM messages WHERE rowid = ?", arrayOf(this))
                }
            }
        }
        return null
    }

    companion object {
        const val NONE = -1
        const val PAGE_SIZE = 20

        private const val QUERY_SQL = """
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
            pm.message_id IS NOT NULL as isPin, c.name AS groupName, em.expire_in AS expireIn, em.expire_at AS expireAt
            FROM messages m
            LEFT JOIN users u ON m.user_id = u.user_id
            LEFT JOIN users u1 ON m.participant_id = u1.user_id
            LEFT JOIN snapshots s ON m.snapshot_id = s.snapshot_id
            LEFT JOIN assets a ON s.asset_id = a.asset_id
            LEFT JOIN stickers st ON st.sticker_id = m.sticker_id
            LEFT JOIN hyperlinks h ON m.hyperlink = h.hyperlink
            LEFT JOIN users su ON m.shared_user_id = su.user_id
            LEFT JOIN conversations c ON m.conversation_id = c.conversation_id
            LEFT JOIN message_mentions mm ON m.id = mm.message_id
            LEFT JOIN pin_messages pm ON m.id = pm.message_id
            LEFT JOIN expired_messages em ON m.id = em.message_id
        """
    }

    override val jumpingSupported: Boolean
        get() = true

    override fun getRefreshKey(state: PagingState<Int, MessageItem>): Int? {
        Timber.e("anchorPosition ${state.anchorPosition}")
        return when (val anchorPosition = state.anchorPosition) {
            null -> null
            else -> getKeyFromPosition(anchorPosition)
        }
    }
}
