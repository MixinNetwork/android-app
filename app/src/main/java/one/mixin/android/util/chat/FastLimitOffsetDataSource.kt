@file:Suppress("DEPRECATION")

package one.mixin.android.util.chat

import android.annotation.SuppressLint
import android.database.Cursor
import androidx.annotation.RestrictTo
import androidx.paging.PositionalDataSource
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import one.mixin.android.util.debug.measureTimeMillis
import one.mixin.android.util.reportException
import timber.log.Timber

@SuppressLint("RestrictedApi")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class FastLimitOffsetDataSource<T> protected constructor(
    coroutineScope: CoroutineScope,
    private val db: RoomDatabase,
    private val countQuery: RoomSQLiteQuery,
    private val itemStatement: RoomSQLiteQuery,
    private val conversationId: String,
    private val fastCountCallback: () -> Int?
) : PositionalDataSource<T>() {

    private val DEBUG = false

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
        val fastCont = fastCountCallback.invoke()
        val totalCount = fastCont ?: countItems()
        if (totalCount == 0) {
            callback.onResult(emptyList(), 0, 0)
            return
        }
        // bound the size requested, based on known count
        val firstLoadPosition = computeInitialLoadPosition(params, totalCount)
        val firstLoadSize = computeInitialLoadSize(params, firstLoadPosition, totalCount)
        log("loadInitial")
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

    private fun itemIds(startPosition: Int, loadCount: Int): String {
        val offsetQuery = RoomSQLiteQuery.copyFrom(itemStatement)
        offsetQuery.bindString(1, conversationId)
        offsetQuery.bindLong(2, loadCount.toLong())
        offsetQuery.bindLong(3, startPosition.toLong())
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

    abstract fun querySql(ids: String): RoomSQLiteQuery

    /**
     * Return the rows from startPos to startPos + loadCount
     */
    private fun loadRange(startPosition: Int, loadCount: Int): List<T> {
        log("loadRange $startPosition $loadCount")
        val ids = itemIds(startPosition, loadCount)
        log("ids: $ids")
        val sqLiteQuery = querySql(ids)
        val cursor = measureTime("query") { db.query(sqLiteQuery) }
        try {
            return measureTime("convert") { convertRows(cursor) }
        } finally {
            cursor.close()
            sqLiteQuery.release()
        }
    }

    private fun log(content: String) {
        if (DEBUG) {
            Timber.e(content)
        }
    }

    private inline fun <T> measureTime(tag: String, block: () -> T): T =
        if (DEBUG) {
            measureTimeMillis(tag, block)
        } else {
            block.invoke()
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
