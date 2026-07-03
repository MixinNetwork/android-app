package one.mixin.android.db.datasource

import android.database.Cursor
import android.os.CancellationSignal
import androidx.paging.PagingSource
import androidx.room3.PooledConnection
import androidx.room3.RoomDatabase
import androidx.room3.RoomRawQuery
import androidx.room3.Transactor.SQLiteTransactionType
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
                connection.usePrepared(query.sql) { statement ->
                    RoomRawQueryCompat.bind(query, statement)
                    cancellationSignal?.throwIfCanceled()
                    statement.toCursor()
                }
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
        runBlocking(queryContext(db)) {
            db.useWriterConnection { connection ->
                connection.execSQL(sql, bindArgs)
            }
        }
    }

    @JvmStatic
    fun statementDatabase(db: RoomDatabase): RoomStatementDatabase = RoomStatementDatabase(db)

    fun <T> runInWriteTransaction(
        db: RoomDatabase,
        block: suspend androidx.room3.TransactionScope<T>.() -> T,
    ): T {
        return runBlocking(queryContext(db)) {
            db.useWriterConnection { connection ->
                connection.withTransaction(SQLiteTransactionType.IMMEDIATE, block)
            }
        }
    }

    @JvmStatic
    fun observeInvalidation(
        db: RoomDatabase,
        pagingSource: PagingSource<*, *>,
        vararg tables: String,
    ) {
        val job = observeInvalidationForTables(db.getCoroutineScope(), db, tables) { pagingSource.invalidate() }
        pagingSource.registerInvalidatedCallback { job.cancel() }
    }

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
            db.invalidationTracker.createFlow(*tables)
                .drop(1)
                .collect { onInvalidated() }
        }
    }
}

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
