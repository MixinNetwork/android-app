package one.mixin.android.db.datasource

import android.annotation.SuppressLint
import android.database.Cursor
import android.os.CancellationSignal
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.paging.util.ThreadSafeInvalidationObserver
import androidx.room.paging.util.getClippedRefreshKey
import androidx.room.paging.util.getLimit
import androidx.room.paging.util.getOffset
import androidx.room.withTransaction
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext

@SuppressLint("RestrictedApi")
abstract class MixinNonCountLimitOffsetDataSource<Value : Any>(
    private val offsetStatement: RoomSQLiteQuery,
    private val totalCount: Int,
    private val db: RoomDatabase,
    vararg tables: String,
) : PagingSource<Int, Value>() {
    private val observer =
        ThreadSafeInvalidationObserver(
            tables = tables,
            onInvalidated = ::invalidate,
        )

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Value> {
        return withContext(db.queryExecutor.asCoroutineDispatcher()) {
            observer.registerIfNecessary(db)
            initialLoad(params)
        }
    }

    /**
     *  For the very first time that this PagingSource's [load] is called. Executes the count
     *  query (initializes [itemCount]) and db query within a transaction to ensure initial load's
     *  data integrity.
     *
     *  For example, if the database gets updated after the count query but before the db query
     *  completes, the paging source may not invalidate in time, but this method will return
     *  data based on the original database that the count was performed on to ensure a valid
     *  initial load.
     */
    private suspend fun initialLoad(params: LoadParams<Int>): LoadResult<Int, Value> {
        return db.withTransaction {
            queryData(
                params = params,
                itemCount = totalCount,
            )
        }
    }

    private fun queryData(
        params: LoadParams<Int>,
        itemCount: Int,
        cancellationSignal: CancellationSignal? = null,
    ): LoadResult.Page<Int, Value> {
        val key = params.key ?: 0
        val limit: Int = getLimit(params, key)
        val offset: Int = getOffset(params, key, itemCount)
        val offsetQuery = RoomSQLiteQuery.copyFrom(offsetStatement)
        val argCount = offsetStatement.argCount
        offsetQuery.bindLong(argCount - 1, limit.toLong())
        offsetQuery.bindLong(argCount, offset.toLong())
        val cursor = db.query(offsetQuery, cancellationSignal)
        val data = convertRows(cursor)
        val nextPosToLoad = offset + data.size
        val nextKey =
            if (data.isEmpty() || data.size < limit || nextPosToLoad >= itemCount) {
                null
            } else {
                nextPosToLoad
            }
        val prevKey = if (offset <= 0 || data.isEmpty()) null else offset
        return LoadResult.Page(
            data = data,
            prevKey = prevKey,
            nextKey = nextKey,
            itemsBefore = offset,
            itemsAfter = maxOf(0, itemCount - nextPosToLoad),
        )
    }

    protected abstract fun convertRows(cursor: Cursor): List<Value>

    override fun getRefreshKey(state: PagingState<Int, Value>): Int? {
        return state.getClippedRefreshKey()
    }

    override val jumpingSupported: Boolean
        get() = true
}
