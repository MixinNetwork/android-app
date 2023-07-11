package one.mixin.android.db.datasource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.RoomDatabase
import java.lang.IllegalArgumentException

class MessageDataSource(private val db: RoomDatabase, val conversationId: String) :
    PagingSource<Int, String>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
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
        var cursor = db.query(
            "SELECT rowid FROM messages WHERE id = (SELECT message_id FROM remote_messages_status WHERE conversation_id = ? ORDER BY rowid ASC LIMIT 1)",
            arrayOf(conversationId),
        )
        while (cursor.moveToNext()) {
            return cursor.getInt(0)
        }
        // Offset by 1 position, including an anchor message
        cursor = db.query(
            "SELECT rowid FROM messages WHERE conversation_id = ? ORDER BY created_at ASC, rowid ASC LIMIT 1",
            arrayOf(conversationId),
        )
        while (cursor.moveToNext()) {
            return cursor.getInt(0)
        }
        return null
    }

    private fun getData(rowId: Int, limit: Int): LoadResult.Page<Int, String> {
        val (prevKey, prevData) = prevKey(rowId)
        val currentPageData = mutableListOf<String>()
        db.query(
            "SELECT content FROM messages WHERE rowid >= ? AND conversation_id = ? ORDER BY created_at ASC, rowid ASC LIMIT ?",
            arrayOf(rowId, conversationId, limit),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                currentPageData.add(cursor.getString(0))
            }
        }
        val (nextKey, nextData) =
            if (currentPageData.size == limit) {
                nextKey(rowId, limit)
            } else {
                Pair(null, null)
            }
        val result = mutableListOf<String>()
        if (!prevData.isNullOrEmpty()) {
            result.addAll(prevData)
        }
        result.addAll(currentPageData)
        if (!nextData.isNullOrEmpty()) {
            result.addAll(nextData)
        }
        return LoadResult.Page(currentPageData, prevKey, nextKey)
    }

    private fun prevKey(rowId: Int): Pair<Int?, List<String>?> {
        val prevCursor = db.query(
            "SELECT rowid FROM messages WHERE rowid < ? AND conversation_id = ? ORDER BY created_at DESC, rowid DESC  LIMIT 1 OFFSET ?",
            arrayOf(rowId, conversationId, PAGE_SIZE - 1),
        )
        if (prevCursor.moveToNext()) {
            return Pair(prevCursor.getInt(0), null)
        } else {
            db.query(
                "SELECT content FROM messages WHERE rowid < ? AND conversation_id = ? ORDER BY created_at ASC, rowid ASC LIMIT ?",
                arrayOf(rowId, conversationId, PAGE_SIZE),
            ).use { cursor ->
                val data = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    data.add(cursor.getString(0))
                }
                return Pair(null, data)
            }
        }
    }

    private fun nextKey(rowId: Int, limit: Int): Pair<Int?, List<String>?> {
        val nextCursor = db.query(
            "SELECT rowid FROM messages WHERE rowid > ? AND conversation_id = ? ORDER BY created_at ASC, rowid ASC LIMIT 1 OFFSET ?",
            arrayOf(rowId, conversationId, limit - 1),
        )
        if (nextCursor.moveToNext()) {
            return Pair(nextCursor.getInt(0), null)
        } else {
            db.query(
                "SELECT content FROM messages WHERE rowid > ? AND conversation_id = ? ORDER BY created_at ASC, rowid ASC LIMIT ? OFFSET ?",
                arrayOf(rowId, conversationId, PAGE_SIZE, limit - 1),
            ).use { cursor ->
                val data = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    data.add(cursor.getString(0))
                }
                return Pair(null, data)
            }
        }
    }

    companion object {
        const val NONE = -1
        const val PAGE_SIZE = 5
    }

    override fun getRefreshKey(state: PagingState<Int, String>): Int {
        return NONE
    }
}
