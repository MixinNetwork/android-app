package one.mixin.android.db.datasource

import android.annotation.SuppressLint
import android.database.Cursor
import android.os.CancellationSignal
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room3.PooledConnection
import androidx.room3.RoomDatabase
import androidx.room3.paging.util.INITIAL_ITEM_COUNT
import androidx.room3.paging.util.getClippedRefreshKey
import androidx.room3.paging.util.getLimit
import androidx.room3.paging.util.getOffset
import androidx.room3.useReaderConnection
import androidx.room3.withReadTransaction
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

@SuppressLint("RestrictedApi")
abstract class MixinCountLimitOffsetDataSource<Value : Any>(
    private val offsetStatement: RoomQuery,
    private val fastCountCallback: suspend (PooledConnection) -> Int,
    private val querySqlGenerator: (ids: String) -> RoomQuery,
    private val db: RoomDatabase,
    vararg tables: String,
) : PagingSource<Int, Value>() {
    internal val itemCount: AtomicInteger = AtomicInteger(INITIAL_ITEM_COUNT)
    private val invalidation = RoomDatabaseCompat.observeInvalidation(db, this, *tables)

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Value> {
        invalidation.awaitStart()
        return withContext(RoomDatabaseCompat.queryContext(db)) {
            val tempCount = itemCount.get()
            // if itemCount is < 0, then it is initial load
            if (tempCount == INITIAL_ITEM_COUNT) {
                initialLoad(params)
            } else {
                nonInitialLoad(params, tempCount)
            }
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
        return db.withReadTransaction {
            val tempCount = getItemCount(this)
            itemCount.set(tempCount)
            queryData(this, params = params, itemCount = tempCount)
        }
    }

    private suspend fun getItemCount(connection: PooledConnection): Int {
        return fastCountCallback.invoke(connection)
    }

    private suspend fun nonInitialLoad(
        params: LoadParams<Int>,
        tempCount: Int,
    ): LoadResult<Int, Value> {
        val loadResult = db.useReaderConnection { queryData(it, params, tempCount) }
        invalidation.refresh()
        @Suppress("UNCHECKED_CAST")
        return if (invalid) LoadResult.Invalid() else loadResult
    }

    private suspend fun queryData(
        connection: PooledConnection,
        params: LoadParams<Int>,
        itemCount: Int,
        cancellationSignal: CancellationSignal? = null,
    ): LoadResult.Page<Int, Value> {
        val key = params.key ?: 0
        val limit: Int = getLimit(params, key)
        val offset: Int = getOffset(params, key, itemCount)
        val offsetQuery = RoomQuery.copyFrom(offsetStatement)
        val argCount = offsetStatement.argCount
        offsetQuery.bindLong(argCount - 1, limit.toLong())
        offsetQuery.bindLong(argCount, offset.toLong())
        val ids =
            try {
                convertRowsToIds(connection.query(offsetQuery))
            } finally {
                offsetQuery.release()
            }
        val data =
            if (ids.isEmpty()) {
                emptyList()
            } else {
                val query = querySqlGenerator(ids.joinToString())
                try {
                    connection.query(query).use(::convertRows)
                } finally {
                    query.release()
                }
            }
        val nextPosToLoad = offset + data.size
        val nextKey =
            if (ids.isEmpty() || ids.size < limit || nextPosToLoad >= itemCount) {
                null
            } else {
                nextPosToLoad
            }
        val prevKey = if (offset <= 0 || ids.isEmpty()) null else offset
        return LoadResult.Page(
            data = data,
            prevKey = prevKey,
            nextKey = nextKey,
            itemsBefore = offset,
            itemsAfter = maxOf(0, itemCount - nextPosToLoad),
        )
    }

    private fun convertRowsToIds(cursor: Cursor): List<String> {
        val ids = mutableListOf<String>()
        cursor.use { c ->
            while (cursor.moveToNext()) {
                val rowid = c.getString(0)
                ids.add("'$rowid'")
            }
            return ids
        }
    }

    protected abstract fun convertRows(cursor: Cursor): List<Value>

    override fun getRefreshKey(state: PagingState<Int, Value>): Int? {
        return state.getClippedRefreshKey()
    }

    override val jumpingSupported: Boolean
        get() = true
}
