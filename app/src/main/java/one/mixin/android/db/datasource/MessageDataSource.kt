package one.mixin.android.db.datasource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.RoomDatabase
import one.mixin.android.vo.MessageItem

class MessageDataSource(private val db: RoomDatabase, val conversationId:String) : PagingSource<Int, String>() {
    override fun getRefreshKey(state: PagingState<Int, String>): Int? {
        return state.anchorPosition?.let {anchorPosition->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
        return try {
            val key = params.key
            val loadSize = params.loadSize
            val cursor = db.query(
                "SELECT id FROM messages WHERE conversation_id = '$conversationId' AND rowid > ? ORDER BY created_at LIMIT $loadSize",
                arrayOf(key)
            )
            val ids = mutableListOf<String>()
            cursor.use { c ->
                while (cursor.moveToNext()) {
                    val messageId = c.getString(0)
                    ids.add(messageId)
                }
            }

            return LoadResult.Page(
                data = ids, prevKey = null,
                nextKey = null
            )
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }


}