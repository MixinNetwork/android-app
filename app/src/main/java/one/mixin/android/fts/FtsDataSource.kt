package one.mixin.android.fts

import android.os.CancellationSignal
import androidx.core.database.getStringOrNull
import androidx.paging.PagingSource
import androidx.paging.PagingState
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.datasource.RoomDatabaseCompat
import one.mixin.android.db.datasource.query
import one.mixin.android.vo.SearchMessageDetailItem
import kotlinx.coroutines.withContext
import timber.log.Timber

class FtsDataSource(
    private val ftsDatabase: FtsDatabase,
    private val mixinDatabase: MixinDatabase,
    private val query: String,
    private val conversationId: String,
    private val cancellationSignal: CancellationSignal,
) : PagingSource<Int, SearchMessageDetailItem>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchMessageDetailItem> {
        return try {
            withContext(RoomDatabaseCompat.queryContext(mixinDatabase)) {
                val offset = params.key ?: 0
                val loadSize = params.loadSize
                val ids = messageIds(loadSize, offset)
                val data = getData(ids)
                LoadResult.Page(
                    data = data,
                    prevKey = if (offset == 0) null else maxOf(0, offset - loadSize),
                    nextKey = if (data.size < loadSize) null else offset + data.size,
                )
            }
        } catch (e: Exception) {
            Timber.e(e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SearchMessageDetailItem>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        return state.closestPageToPosition(anchorPosition)?.prevKey
            ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(state.config.pageSize)
    }

    private fun messageIds(
        size: Int,
        offset: Int,
    ): List<String> {
        return ftsDatabase.query(
            FtsQueryGenerated.messageIdsByConversationPage(conversationId, query, size, offset),
            cancellationSignal,
        ).use { cursor ->
            val ids = mutableListOf<String>()
            while (cursor.moveToNext()) {
                cursor.getStringOrNull(0)?.let(ids::add)
            }
            ids
        }
    }

    private fun getData(
        ids: List<String>,
    ): List<SearchMessageDetailItem> {
        return mixinDatabase.messageDao().getSearchMessageDetailItemsByIds(ids)
    }
}
