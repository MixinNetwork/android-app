@file:Suppress("DEPRECATION")

package one.mixin.android.util.chat

import android.annotation.SuppressLint
import android.database.Cursor
import android.os.CancellationSignal
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
    coroutineScope: CoroutineScope,
    private val db: RoomDatabase,
    private val sourceQuery: RoomSQLiteQuery,
    private val countQuery: RoomSQLiteQuery,
    private val conversationId: String,
    private val cancellationSignal: CancellationSignal,
    private val fastCountCallback: () -> Int?
) : PositionalDataSource<T>() {
    private val limitOffsetQuery: String = sourceQuery.sql + " LIMIT ? OFFSET ?"

    /**
     * Count number of rows query can return
     */
    fun countItems(): Int {
        val cursor = db.query(countQuery, cancellationSignal)
        val start = System.currentTimeMillis()
        return try {
            val r = if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else 0
            Timber.d("count $conversationId, ${System.currentTimeMillis() - start}")
            r
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
            Timber.d("fastCont $conversationId, $fastCont")
            if (fastCont != null) { // If quick return needs to activate the next query
                invalidate()
            }
        } catch (e: IllegalArgumentException) {
            // workaround with paging initial load size NOT to be a multiple of page size
            Timber.w(e)
            try {
                callback.onResult(list, firstLoadPosition, firstLoadPosition + list.size)
                Timber.d("catch fastCont $conversationId, $fastCont")
                if (fastCont != null) {
                    invalidate()
                }
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
    }

    override fun loadRange(
        params: LoadRangeParams,
        callback: LoadRangeCallback<T>
    ) {
        val list = loadRange(params.startPosition, params.loadSize)
        callback.onResult(list)
    }

    /**
     * Return the rows from startPos to startPos + loadCount
     */
    private fun loadRange(startPosition: Int, loadCount: Int): List<T> {
        val sqLiteQuery = RoomSQLiteQuery.acquire(
            limitOffsetQuery,
            sourceQuery.argCount + 2
        )
        sqLiteQuery.copyArgumentsFrom(sourceQuery)
        sqLiteQuery.bindLong(sqLiteQuery.argCount - 1, loadCount.toLong())
        sqLiteQuery.bindLong(sqLiteQuery.argCount, startPosition.toLong())

        val start = System.currentTimeMillis()
        val cursor = db.query(sqLiteQuery, cancellationSignal)
        try {
            val r = convertRows(cursor)
            Timber.d("convertRows $conversationId, ${System.currentTimeMillis() -start}")
            return r
        } finally {
            cursor.close()
            sqLiteQuery.release()
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
