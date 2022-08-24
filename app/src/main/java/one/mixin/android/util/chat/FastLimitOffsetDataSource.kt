@file:Suppress("DEPRECATION")

package one.mixin.android.util.chat

import android.annotation.SuppressLint
import android.database.Cursor
import androidx.annotation.RestrictTo
import androidx.paging.PositionalDataSource
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import one.mixin.android.util.reportException
import timber.log.Timber

@SuppressLint("RestrictedApi")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class FastLimitOffsetDataSource<T, S>(
    private val db: RoomDatabase,
    private val countQuery: RoomSQLiteQuery,
    private val offsetStatement: RoomSQLiteQuery,
    private val fastCountCallback: () -> Int?,
    private val querySqlGenerator: (String) -> RoomSQLiteQuery,
    private vararg val tables: String
) : PositionalDataSource<T>() {

    init {
        if (tables.isNotEmpty()) {
            db.invalidationTracker.addWeakObserver(object : InvalidationTracker.Observer(tables) {
                override fun onInvalidated(tables: Set<String>) {
                    invalidate()
                }
            })
        }
    }

    /**
     * Count number of rows query can return
     */
    private fun countItems(): Int {
        val cursor = db.query(countQuery)
        return try {
            if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else 0
        } finally {
            cursor.close()
            countQuery.release()
        }
    }

    override fun isInvalid(): Boolean {
        db.invalidationTracker.refreshVersionsSync()
        return super.isInvalid()
    }

    protected abstract fun convertRows(cursor: Cursor?): List<T>
    override fun loadInitial(
        params: LoadInitialParams,
        callback: LoadInitialCallback<T>
    ) {
        val fastCont = fastCountCallback.invoke()
        val totalCount = fastCont ?: countItems()
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
            if (fastCont != null) { // If quick return needs to activate the next query
                invalidate()
            }
        } catch (e: IllegalArgumentException) {
            // workaround with paging initial load size NOT to be a multiple of page size
            Timber.w(e)
            try {
                callback.onResult(list, firstLoadPosition, firstLoadPosition + list.size)
                if (fastCont != null) {
                    invalidate()
                }
            } catch (iae: IllegalArgumentException) {
                // workaround with paging incorrect tiling
                val message = (
                    "FastLimitOffsetDataSource " +
                        "firstLoadPosition: " + firstLoadPosition +
                        ", list size: " + list.size +
                        ", count: " + totalCount
                    )
                reportException(message, iae)
                Timber.w(iae)
            }
        }
    }

    override fun loadRange(
        params: LoadRangeParams,
        callback: LoadRangeCallback<T>
    ) {
        val list = loadRange(params.startPosition, params.loadSize)
        callback.onResult(list)
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
                val id = getUniqueId(cursor)
                ids.add("'$id'")
            }
            return ids.joinToString()
        } finally {
            cursor.close()
            offsetQuery.release()
        }
    }

    abstract fun getUniqueId(cursor: Cursor): S

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
}
