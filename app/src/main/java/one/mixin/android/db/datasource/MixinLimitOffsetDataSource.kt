package one.mixin.android.db.datasource

import android.annotation.SuppressLint
import android.database.Cursor
import android.os.CancellationSignal
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.getQueryDispatcher
import androidx.room.paging.util.INITIAL_ITEM_COUNT
import androidx.room.paging.util.INVALID
import androidx.room.paging.util.ThreadSafeInvalidationObserver
import androidx.room.paging.util.getClippedRefreshKey
import androidx.room.paging.util.getLimit
import androidx.room.paging.util.getOffset
import androidx.room.withTransaction
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

@SuppressLint("RestrictedApi")
abstract class MixinLimitOffsetDataSource<Value : Any>(
    private val offsetStatement: RoomSQLiteQuery,
    private val countQuery: RoomSQLiteQuery,
    private val querySqlGenerator: (ids: String) -> RoomSQLiteQuery,
    private val db: RoomDatabase,
    vararg tables: String,
) : PagingSource<Int, Value>() {

    internal val itemCount: AtomicInteger = AtomicInteger(INITIAL_ITEM_COUNT)

    private val observer = ThreadSafeInvalidationObserver(
        tables = tables,
        onInvalidated = ::invalidate,
    )

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Value> {
        return withContext(db.getQueryDispatcher()) {
            observer.registerIfNecessary(db)
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
        return db.withTransaction {
            val tempCount = getItemCount()
            itemCount.set(tempCount)
            queryData(
                params = params,
                itemCount = tempCount,
            )
        }
    }

    private fun getItemCount(): Int {
        val cursor = db.query(countQuery)
        return try {
            if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                0
            }
        } finally {
            cursor.close()
            countQuery.release()
        }
    }

    private suspend fun nonInitialLoad(
        params: LoadParams<Int>,
        tempCount: Int,
    ): LoadResult<Int, Value> {
        val loadResult = queryData(
            params = params,
            itemCount = tempCount,
        )
        // manually check if database has been updated. If so, the observer's
        // invalidation callback will invalidate this paging source
        db.invalidationTracker.refreshVersionsSync()
        @Suppress("UNCHECKED_CAST")
        return if (invalid) INVALID as LoadResult.Invalid<Int, Value> else loadResult
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
        val cursor = db.query(offsetQuery)
        val ids: List<String> = convertRowsToIds(cursor)
        Timber.e("size:${ids.size} ${itemCount.toLong()} $limit $offset")
        val data = convertRows(db.query(querySqlGenerator(ids.joinToString())))
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
            Timber.e("count ${c.count}")
            while (cursor.moveToNext()) {
                val rowid = c.getLong(0)
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
