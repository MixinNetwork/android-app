package one.mixin.android.db.datasource

import android.annotation.SuppressLint
import android.database.Cursor
import androidx.annotation.RestrictTo
import androidx.paging.PositionalDataSource
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import timber.log.Timber

@SuppressLint("RestrictedApi")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class MixinLimitOffsetDataSource<T : Any> protected constructor(
    private val db: RoomDatabase,
    private val countQuery: RoomSQLiteQuery,
    private val offsetStatement: RoomSQLiteQuery,
    private val querySqlGenerator: (String) -> RoomSQLiteQuery,
    private val tables: Array<out String>,
) : PositionalDataSource<T>() {
    private val observer: InvalidationTracker.Observer

    /**
     * Count number of rows query can return
     */
    private fun countItems(): Int {
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

    protected abstract fun convertRows(cursor: Cursor?): List<T>
    override fun loadInitial(
        params: LoadInitialParams,
        callback: LoadInitialCallback<T>,
    ) {
        val totalCount = countItems()
        if (totalCount == 0) {
            callback.onResult(emptyList(), 0, 0)
            return
        }

        // bound the size requested, based on known count
        val firstLoadPosition = computeInitialLoadPosition(params, totalCount)
        val firstLoadSize = computeInitialLoadSize(params, firstLoadPosition, totalCount)
        val list = loadRange(firstLoadPosition, firstLoadSize)
        try {
            callback.onResult(list, firstLoadPosition, totalCount)
        } catch (e: IllegalArgumentException) {
            // workaround with paging initial load size NOT to be a multiple of page size
            Timber.w(e)
            try {
                callback.onResult(list, firstLoadPosition, firstLoadPosition + list.size)
            } catch (iae: IllegalArgumentException) {
                // workaround with paging incorrect tiling
                Timber.w(iae)
            }
        }
    }

    override fun loadRange(
        params: LoadRangeParams,
        callback: LoadRangeCallback<T>,
    ) {
        val list = loadRange(params.startPosition, params.loadSize)
        callback.onResult(list)
    }

    /**
     * Return the rows from startPos to startPos + loadCount
     */
    private fun loadRange(startPosition: Int, loadCount: Int): List<T> {
        val ids = itemIds(startPosition, loadCount)
        val sqLiteQuery = querySqlGenerator(ids)
        val cursor = db.query(sqLiteQuery)
        try {
            return convertRows(cursor)
        } finally {
            cursor.close()
            sqLiteQuery.release()
        }
    }

    private fun itemIds(startPosition: Int, loadCount: Int): String {
        val offsetQuery = RoomSQLiteQuery.copyFrom(offsetStatement)
        val argCount = offsetStatement.argCount
        offsetQuery.bindLong(argCount - 1, loadCount.toLong())
        offsetQuery.bindLong(argCount, startPosition.toLong())
        val cursor = db.query(offsetQuery)
        val ids = mutableListOf<String>()
        try {
            while (cursor.moveToNext()) {
                val rowid = cursor.getLong(0)
                ids.add("'$rowid'")
            }
            return ids.joinToString()
        } finally {
            cursor.close()
            offsetQuery.release()
        }
    }

    init {
        observer = object : InvalidationTracker.Observer(tables) {
            override fun onInvalidated(tables: Set<String>) {
                invalidate()
            }
        }
        db.invalidationTracker.addWeakObserver(observer)
    }
}
