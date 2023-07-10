package one.mixin.android.db.datasource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.RoomDatabase
import timber.log.Timber

class MessageDataSource(private val db: RoomDatabase, val conversationId: String) : PagingSource<Int, String>() {
    override fun getRefreshKey(state: PagingState<Int, String>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
        return try {
            val key = params.key
            val loadSize = params.loadSize
            val cursor = db.query(
                "SELECT id, rowid FROM messages WHERE conversation_id = '$conversationId' AND rowid > ? ORDER BY created_at LIMIT $loadSize",
                arrayOf(key),
            )
            val ids = mutableListOf<String>()
            var rowId:Int? = null
            cursor.use { c ->
                while (cursor.moveToNext()) {
                    val messageId = c.getString(0)
                    ids.add(messageId)
                    rowId = c.getInt(1)
                }
            }
            Timber.e("load $key $conversationId ${params.loadSize} ${ids.size}")
            return LoadResult.Page(
                data = ids,
                prevKey = null,
                nextKey = rowId,
            )
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }
}
