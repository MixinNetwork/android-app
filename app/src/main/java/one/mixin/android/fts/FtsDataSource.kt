@file:Suppress("DEPRECATION")

package one.mixin.android.fts

import android.os.CancellationSignal
import androidx.core.database.getStringOrNull
import androidx.paging.PagingSource
import androidx.paging.PagingState
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.datasource.RoomDatabaseCompat
import one.mixin.android.db.datasource.query
import one.mixin.android.vo.SearchMessageDetailItem
import kotlinx.coroutines.CancellationException
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
                val key = params.key ?: 0
                val limit = if (params is LoadParams.Prepend) minOf(key, params.loadSize) else params.loadSize
                val offset = if (params is LoadParams.Prepend) key - limit else key
                val ids = messageIds(limit, offset)
                val data = getData(ids)
                LoadResult.Page(
                    data = data,
                    prevKey = if (offset == 0) null else offset,
                    nextKey = when {
                        params is LoadParams.Prepend -> key
                        ids.size < limit -> null
                        else -> offset + ids.size
                    },
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SearchMessageDetailItem>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        return state.closestPageToPosition(anchorPosition)?.prevKey
            ?: 0
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
        val items = mixinDatabase.messageDao().getSearchMessageDetailItemsByIds(ids).associateBy { it.messageId }
        return ids.mapNotNull(items::get)
    }
}
