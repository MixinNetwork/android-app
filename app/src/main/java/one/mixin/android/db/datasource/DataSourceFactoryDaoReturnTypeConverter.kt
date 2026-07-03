package one.mixin.android.db.datasource

import androidx.paging.DataSource
import androidx.paging.PositionalDataSource
import androidx.room3.DaoReturnTypeConverter
import androidx.room3.OperationType
import androidx.room3.RoomDatabase
import androidx.room3.RoomRawQuery
import androidx.room3.useReaderConnection
import kotlinx.coroutines.runBlocking
import timber.log.Timber

object DataSourceFactoryDaoReturnTypeConverter {
    @DaoReturnTypeConverter(operations = [OperationType.READ])
    fun <T : Any> convert(
        db: RoomDatabase,
        tableNames: Array<String>,
        rawQuery: RoomRawQuery,
        executeAndConvert: suspend (RoomRawQuery) -> List<T>,
    ): DataSource.Factory<Int, T> {
        return object : DataSource.Factory<Int, T>() {
            override fun create(): DataSource<Int, T> {
                return RoomRawQueryDataSource(db, tableNames, rawQuery, executeAndConvert)
            }
        }
    }
}

private class RoomRawQueryDataSource<T : Any>(
    private val db: RoomDatabase,
    private val tableNames: Array<String>,
    private val rawQuery: RoomRawQuery,
    private val executeAndConvert: suspend (RoomRawQuery) -> List<T>,
) : PositionalDataSource<T>() {
    init {
        RoomDatabaseCompat.observeInvalidation(db, this, *tableNames)
    }

    override fun loadInitial(
        params: LoadInitialParams,
        callback: LoadInitialCallback<T>,
    ) {
        val totalCount = countItems()
        if (totalCount == 0) {
            callback.onResult(emptyList(), 0, 0)
            return
        }
        val firstLoadPosition = computeInitialLoadPosition(params, totalCount)
        val firstLoadSize = computeInitialLoadSize(params, firstLoadPosition, totalCount)
        val list = loadRange(firstLoadPosition, firstLoadSize)
        try {
            callback.onResult(list, firstLoadPosition, totalCount)
        } catch (e: IllegalArgumentException) {
            Timber.w(e)
            try {
                callback.onResult(list, firstLoadPosition, firstLoadPosition + list.size)
            } catch (iae: IllegalArgumentException) {
                Timber.w(iae)
            }
        }
    }

    override fun loadRange(
        params: LoadRangeParams,
        callback: LoadRangeCallback<T>,
    ) {
        callback.onResult(loadRange(params.startPosition, params.loadSize))
    }

    private fun countItems(): Int {
        val countSql = "SELECT COUNT(*) FROM (${rawQuery.sql})"
        return runBlocking(RoomDatabaseCompat.queryContext(db)) {
            db.useReaderConnection { connection ->
                connection.usePrepared(countSql) { statement ->
                    RoomRawQueryCompat.bind(rawQuery, statement)
                    if (statement.step()) statement.getLong(0).toInt() else 0
                }
            }
        }
    }

    private fun loadRange(
        startPosition: Int,
        loadCount: Int,
    ): List<T> {
        val limitOffsetQuery =
            RoomRawQuery("${rawQuery.sql} LIMIT $loadCount OFFSET $startPosition") { statement ->
                RoomRawQueryCompat.bind(rawQuery, statement)
            }
        return runBlocking(RoomDatabaseCompat.queryContext(db)) {
            executeAndConvert(limitOffsetQuery)
        }
    }
}
