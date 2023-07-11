package one.mixin.android.db.datasource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.RoomDatabase
import androidx.room.paging.util.getClippedRefreshKey
import timber.log.Timber
import java.lang.IllegalArgumentException

class MessageDataSource(private val db: RoomDatabase, val conversationId: String) : PagingSource<String, String>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, String> {
        return try {
            // Use this message as an anchor to obtain the front and back data
            var anchorKey = params.key
            Timber.e("anchorKey: $anchorKey")
            if (anchorKey == NONE) {
                anchorKey = getAnchorKey()
                if (anchorKey == null) {
                    return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
                }
            } else if (anchorKey == null) {
                throw IllegalArgumentException("Key must be passed in to get the data")
            }
            val loadSize = params.loadSize
            val cursor = db.query(
                "SELECT content, id FROM messages WHERE conversation_id = ? AND created_at >= (SELECT created_at FROM messages WHERE id = ?) AND id != ? ORDER BY created_at ASC, rowid ASC LIMIT ?",
                arrayOf(conversationId, anchorKey, anchorKey, loadSize),
            )
            val ids = mutableListOf<String>()
            var lastId: String? = null
            cursor.use { c ->
                while (cursor.moveToNext()) {
                    val messageId = c.getString(0)
                    ids.add(messageId)
                    lastId = c.getString(1)
                }
            }
            val prevKey = getPrevKey(anchorKey, params.loadSize)
            Timber.e("load key:$anchorKey conversationId:$conversationId load-size:${params.loadSize} total:${ids.size} lastKey:$lastId prevKey:$prevKey")
            return LoadResult.Page(
                data = ids,
                prevKey = prevKey,
                nextKey = lastId,
            )
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }

    private fun getAnchorKey(): String? {
        var cursor = db.query("SELECT message_id FROM remote_messages_status WHERE conversation_id = ? ORDER BY rowid ASC LIMIT 1", arrayOf(conversationId))
        while (cursor.moveToNext()) {
            return cursor.getString(0)
        }
        // Offset by 1 position, including an anchor message
        cursor = db.query("SELECT id FROM messages WHERE conversation_id = ? ORDER BY created_at ASC, rowid ASC LIMIT 1", arrayOf(conversationId))
        while (cursor.moveToNext()) {
            return cursor.getString(0)
        }
        return null
    }

    private fun getPrevKey(key: String, pageSize: Int): String? {
        val cursor = db.query(
            "SELECT id FROM messages WHERE conversation_id = ? AND created_at <= (SELECT created_at FROM messages WHERE id = ?) AND id != ? ORDER BY created_at ASC, rowid ASC LIMIT 1 OFFSET ?",
            arrayOf(conversationId, key, key, pageSize),
        )
        while (cursor.moveToNext()) {
            return cursor.getString(0)
        }
        return null
    }
    companion object {
        const val NONE = "NONE"
    }

    override fun getRefreshKey(state: PagingState<String, String>): String? {
        return NONE
    }
}
