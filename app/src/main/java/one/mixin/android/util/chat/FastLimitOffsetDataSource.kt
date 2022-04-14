package one.mixin.android.util.chat

import android.annotation.SuppressLint
import android.database.Cursor
import androidx.annotation.RestrictTo
import androidx.paging.PositionalDataSource
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import one.mixin.android.util.reportException
import timber.log.Timber

@SuppressLint("RestrictedApi")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class FastLimitOffsetDataSource<T> protected constructor(
    private val db: RoomDatabase,
    private val sourceQuery: RoomSQLiteQuery,
    private val countQuery: RoomSQLiteQuery,
    private val coroutineScope: CoroutineScope,
    private val conversationId: String,
    private val inTransaction: Boolean
) : PositionalDataSource<T>() {
    private val limitOffsetQuery: String = sourceQuery.sql + " LIMIT ? OFFSET ?"

    /**
     * Count number of rows query can return
     */
    fun countItems(): Int {
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
        val totalCount = countItems()
        if (totalCount == 0) {
            callback.onResult(emptyList(), 0, 0)
            return
        }

        // bound the size requested, based on known count
        val firstLoadPosition = computeInitialLoadPosition(params, totalCount)
        val firstLoadSize = computeInitialLoadSize(params, firstLoadPosition, totalCount)
        val list = loadRange(firstLoadPosition, firstLoadSize)
        if (list != null) {
            try {
                callback.onResult(list, firstLoadPosition, totalCount)
            } catch (e: IllegalArgumentException) {
                // workaround with paging initial load size NOT to be a multiple of page size
                Timber.w(e)
                try {
                    callback.onResult(list, firstLoadPosition, firstLoadPosition + list.size)
                } catch (iae: IllegalArgumentException) {
                    // workaround with paging incorrect tiling
                    val message = (
                        "MixinLimitOffsetDataSource " +
                            "firstLoadPosition: " + firstLoadPosition +
                            ", list size: " + list.size +
                            ", count: " + totalCount
                        )
                    reportException(message, iae)
                    Timber.w(iae)
                }
            }
        } else {
            // null list, or size doesn't match request - DB modified between count and load
            invalidate()
        }
    }

    override fun loadRange(
        params: LoadRangeParams,
        callback: LoadRangeCallback<T>
    ) {
        val list = loadRange(params.startPosition, params.loadSize)
        if (list != null) {
            callback.onResult(list)
        } else {
            invalidate()
        }
    }

    /**
     * Return the rows from startPos to startPos + loadCount
     */
    private fun loadRange(startPosition: Int, loadCount: Int): List<T>? {
        val sqLiteQuery = RoomSQLiteQuery.acquire(
            limitOffsetQuery,
            sourceQuery.argCount + 2
        )
        sqLiteQuery.copyArgumentsFrom(sourceQuery)
        sqLiteQuery.bindLong(sqLiteQuery.argCount - 1, loadCount.toLong())
        sqLiteQuery.bindLong(sqLiteQuery.argCount, startPosition.toLong())
        return if (inTransaction) {
            db.beginTransaction()
            var cursor: Cursor? = null
            try {
                cursor = db.query(sqLiteQuery)
                val rows = convertRows(cursor)
                db.setTransactionSuccessful()
                rows
            } finally {
                cursor?.close()
                db.endTransaction()
                sqLiteQuery.release()
            }
        } else {
            val cursor = db.query(sqLiteQuery)
            try {
                convertRows(cursor)
            } finally {
                cursor.close()
                sqLiteQuery.release()
            }
        }
    }

    init {
        coroutineScope.launch {
            InvalidateFlow.collect(
                { this@FastLimitOffsetDataSource.conversationId == conversationId },
                {
                    invalidate()
                }
            )
        }
    }
}
