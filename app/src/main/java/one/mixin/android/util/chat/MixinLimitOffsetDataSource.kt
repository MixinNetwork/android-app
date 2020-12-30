package one.mixin.android.util.chat

import android.annotation.SuppressLint
import android.database.Cursor
import androidx.annotation.RestrictTo
import androidx.paging.PositionalDataSource
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

@SuppressLint("RestrictedApi")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class MixinLimitOffsetDataSource<T : Any> protected constructor(
    private val mDb: RoomDatabase, private val mSourceQuery: RoomSQLiteQuery,
    private val mCountQuery: RoomSQLiteQuery,
    private val mInTransaction: Boolean, vararg tables: String?
) : PositionalDataSource<T>() {
    private val mLimitOffsetQuery: String = mSourceQuery.sql + " LIMIT ? OFFSET ?"
    private val mObserver: InvalidationTracker.Observer

    protected constructor(
        db: RoomDatabase, query: SupportSQLiteQuery?,
        countQuery: RoomSQLiteQuery?,
        inTransaction: Boolean, vararg tables: String?
    ) : this(
        db,
        RoomSQLiteQuery.copyFrom(query),
        RoomSQLiteQuery.copyFrom(countQuery),
        inTransaction,
        *tables
    ) {
    }

    /**
     * Count number of rows query can return
     */
    fun countItems(): Int {
        val cursor = mDb.query(mCountQuery)
        return try {
            if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else 0
        } finally {
            cursor.close()
            mCountQuery.release()
        }
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
        if (list != null && list.size == firstLoadSize) {
            callback.onResult(list, firstLoadPosition, totalCount)
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
    fun loadRange(startPosition: Int, loadCount: Int): List<T>? {
        val sqLiteQuery = RoomSQLiteQuery.acquire(
            mLimitOffsetQuery,
            mSourceQuery.argCount + 2
        )
        sqLiteQuery.copyArgumentsFrom(mSourceQuery)
        sqLiteQuery.bindLong(sqLiteQuery.argCount - 1, loadCount.toLong())
        sqLiteQuery.bindLong(sqLiteQuery.argCount, startPosition.toLong())
        return if (mInTransaction) {
            mDb.beginTransaction()
            var cursor: Cursor? = null
            try {
                cursor = mDb.query(sqLiteQuery)
                val rows = convertRows(cursor)
                mDb.setTransactionSuccessful()
                rows
            } finally {
                cursor?.close()
                mDb.endTransaction()
                sqLiteQuery.release()
            }
        } else {
            val cursor = mDb.query(sqLiteQuery)
            try {
                convertRows(cursor)
            } finally {
                cursor.close()
                sqLiteQuery.release()
            }
        }
    }

    init {
        mObserver = object : InvalidationTracker.Observer(tables) {
            override fun onInvalidated(tables: Set<String>) {
                invalidate()
            }
        }
        mDb.invalidationTracker.addWeakObserver(mObserver)
    }
}