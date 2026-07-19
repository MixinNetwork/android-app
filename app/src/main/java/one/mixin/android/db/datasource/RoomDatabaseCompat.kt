package one.mixin.android.db.datasource

import android.database.Cursor
import android.os.CancellationSignal
import androidx.paging.PagingSource
import androidx.room3.PooledConnection
import androidx.room3.RoomDatabase
import androidx.room3.RoomRawQuery
import androidx.room3.useReaderConnection
import androidx.room3.util.performBlocking
import androidx.room3.util.performInTransactionBlocking
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

object RoomDatabaseCompat {
    @JvmStatic
    fun queryContext(db: RoomDatabase): CoroutineContext = db.getQueryContext()

    @JvmStatic
    fun query(
        db: RoomDatabase,
        query: RoomRawQuery,
    ): Cursor = query(db, query, null)

    @JvmStatic
    fun query(
        db: RoomDatabase,
        query: RoomRawQuery,
        cancellationSignal: CancellationSignal?,
    ): Cursor =
        runBlocking(queryContext(db)) {
            cancellationSignal?.throwIfCanceled()
            db.useReaderConnection { connection ->
                connection.query(query, cancellationSignal)
            }
        }

    @JvmStatic
    fun query(
        db: RoomDatabase,
        query: RoomQuery,
    ): Cursor = query(db, query.toRoomRawQuery(), null)

    @JvmStatic
    fun query(
        db: RoomDatabase,
        query: RoomQuery,
        cancellationSignal: CancellationSignal?,
    ): Cursor = query(db, query.toRoomRawQuery(), cancellationSignal)

    @JvmStatic
    fun query(
        db: RoomDatabase,
        sql: String,
        bindArgs: Array<Any?> = emptyArray(),
    ): Cursor {
        return query(
            db,
            RoomRawQuery(sql) { statement ->
                bindArgs.forEachIndexed { index, value ->
                    statement.bindArg(index + 1, value)
                }
            },
        )
    }

    @JvmStatic
    fun execute(
        db: RoomDatabase,
        sql: String,
        bindArgs: Array<Any?> = emptyArray(),
    ) {
        performBlocking(db, isReadOnly = false, inTransaction = true) { connection ->
            connection.prepare(sql).use { statement ->
                bindArgs.forEachIndexed { index, value ->
                    statement.bindArg(index + 1, value)
                }
                statement.step()
            }
        }
    }

    @JvmStatic
    fun statementDatabase(db: RoomDatabase): RoomStatementDatabase = RoomStatementDatabase(db)

    fun <T> runInWriteTransaction(
        db: RoomDatabase,
        block: () -> T,
    ): T = performInTransactionBlocking(db, block)

    @JvmStatic
    fun observeInvalidation(
        db: RoomDatabase,
        pagingSource: PagingSource<*, *>,
        vararg tables: String,
    ): PagingSourceInvalidation = PagingSourceInvalidation(db, pagingSource, tables)

    @JvmStatic
    fun observeInvalidation(
        scope: CoroutineScope,
        db: RoomDatabase,
        tableName: String,
        onInvalidated: Runnable,
    ): Job = observeInvalidationForTables(scope, db, arrayOf(tableName)) { onInvalidated.run() }

    fun observeInvalidation(
        scope: CoroutineScope,
        db: RoomDatabase,
        vararg tables: String,
        onInvalidated: () -> Unit,
    ): Job = observeInvalidationForTables(scope, db, tables, onInvalidated)

    private fun observeInvalidationForTables(
        scope: CoroutineScope,
        db: RoomDatabase,
        tables: Array<out String>,
        onInvalidated: () -> Unit,
    ): Job {
        return scope.launch {
            db.invalidationTracker.createFlow(*tables, emitInitialState = false)
                .collect { onInvalidated() }
        }
    }
}

class PagingSourceInvalidation internal constructor(
    private val db: RoomDatabase,
    private val pagingSource: PagingSource<*, *>,
    private val tables: Array<out String>,
) {
    private val collectorStarted = CompletableDeferred<Unit>()
    private val refreshStarted = AtomicBoolean(false)
    private val job =
        db.getCoroutineScope().launch {
            db.invalidationTracker.createFlow(*tables, emitInitialState = true).collect {
                val initialState = collectorStarted.complete(Unit)
                if (pagingSource.invalid) {
                    throw CancellationException("PagingSource is invalid")
                }
                if (!initialState && refreshStarted.get()) {
                    pagingSource.invalidate()
                }
            }
        }

    init {
        pagingSource.registerInvalidatedCallback {
            job.cancel()
        }
    }

    suspend fun awaitStart() {
        collectorStarted.await()
        refreshStarted.compareAndSet(false, true)
    }

    suspend fun refresh() {
        withContext(RoomDatabaseCompat.queryContext(db)) {
            val invalidated = db.invalidationTracker.refresh(*tables)
            if (invalidated) {
                pagingSource.invalidate()
            }
        }
    }
}

suspend fun PooledConnection.query(
    query: RoomRawQuery,
    cancellationSignal: CancellationSignal? = null,
): Cursor {
    cancellationSignal?.throwIfCanceled()
    return usePrepared(query.sql) { statement ->
        RoomRawQueryCompat.bind(query, statement)
        cancellationSignal?.throwIfCanceled()
        statement.toCursor()
    }
}

suspend fun PooledConnection.query(
    query: RoomQuery,
    cancellationSignal: CancellationSignal? = null,
): Cursor = query(query.toRoomRawQuery(), cancellationSignal)

suspend fun PooledConnection.execSQL(
    sql: String,
    bindArgs: Array<Any?> = emptyArray(),
) {
    usePrepared(sql) { statement ->
        bindArgs.forEachIndexed { index, value ->
            statement.bindArg(index + 1, value)
        }
        statement.step()
    }
}

fun RoomDatabase.query(query: RoomRawQuery): Cursor = RoomDatabaseCompat.query(this, query)

fun RoomDatabase.query(
    query: RoomRawQuery,
    cancellationSignal: CancellationSignal?,
): Cursor = RoomDatabaseCompat.query(this, query, cancellationSignal)

fun RoomDatabase.query(query: RoomQuery): Cursor = RoomDatabaseCompat.query(this, query)

fun RoomDatabase.query(
    query: RoomQuery,
    cancellationSignal: CancellationSignal?,
): Cursor = RoomDatabaseCompat.query(this, query, cancellationSignal)

fun RoomDatabase.query(
    sql: String,
    bindArgs: Array<Any?> = emptyArray(),
): Cursor = RoomDatabaseCompat.query(this, sql, bindArgs)
