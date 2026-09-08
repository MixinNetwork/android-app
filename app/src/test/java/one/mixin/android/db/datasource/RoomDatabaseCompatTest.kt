package one.mixin.android.db.datasource

import android.content.Context
import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException
import android.os.CancellationSignal
import android.os.OperationCanceledException
import androidx.room3.Room
import androidx.room3.RoomRawQuery
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.asCoroutineDispatcher
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.ReportingAndroidSQLiteDriver
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class RoomDatabaseCompatTest {
    private lateinit var database: MixinDatabase
    private lateinit var statements: RoomStatementDatabase

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), MixinDatabase::class.java)
            .setDriver(ReportingAndroidSQLiteDriver("compat-test", 1))
            .allowMainThreadQueries()
            .build()
        RoomDatabaseCompat.execute(database, "CREATE TABLE compat_values (id INTEGER PRIMARY KEY, text_value TEXT, blob_value BLOB, real_value REAL)")
        statements = RoomDatabaseCompat.statementDatabase(database)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun blockingQueryOnQueryExecutorDoesNotDeadlock() {
        val executor = Executors.newSingleThreadExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val singleThreadDatabase = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), MixinDatabase::class.java)
            .setDriver(ReportingAndroidSQLiteDriver("single-thread-test", 1))
            .setQueryCoroutineContext(dispatcher)
            .allowMainThreadQueries()
            .build()
        val query = executor.submit<Long> {
            singleThreadDatabase.query("SELECT 42").use {
                assertTrue(it.moveToFirst())
                it.getLong(0)
            }
        }
        try {
            assertEquals(42L, query.get(5, TimeUnit.SECONDS))
        } finally {
            query.cancel(true)
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
            singleThreadDatabase.close()
            dispatcher.close()
        }
    }

    @Test
    fun queryHonorsCancellationBeforeAndAfterBinding() {
        val signal = CancellationSignal().apply { cancel() }
        assertFailsWith<OperationCanceledException> {
            RoomDatabaseCompat.query(database, RoomRawQuery("SELECT 1"), signal)
        }
        val canceledDuringBinding = CancellationSignal()
        assertFailsWith<OperationCanceledException> {
            RoomDatabaseCompat.query(database, RoomRawQuery("SELECT 1") { canceledDuringBinding.cancel() }, canceledDuringBinding)
        }
        assertEquals(0, rowCount())
    }

    @Test
    fun successfulBatchPreservesBindingsAcrossRepeatedStatement() {
        val statement = statements.compileStatement("INSERT INTO compat_values (id, text_value, blob_value) VALUES (?, ?, ?)")
        statements.beginTransaction()
        statement.bindLong(1, 1)
        statement.bindString(2, "first ' ? 中文")
        statement.bindBlob(3, byteArrayOf(0, 1, -1))
        statement.executeInsert()
        statement.bindLong(1, 2L)
        statement.bindNull(2)
        statement.bindNull(3)
        statement.executeInsert()
        statements.setTransactionSuccessful()
        statements.endTransaction()
        statement.close()
        database.query("SELECT * FROM compat_values ORDER BY id").use {
            assertEquals(2, it.count)
            assertTrue(it.moveToFirst())
            assertEquals(1L, it.getLong(0))
            assertEquals("first ' ? 中文", it.getString(1))
            assertContentEquals(byteArrayOf(0, 1, -1), it.getBlob(2))
            assertTrue(it.moveToNext())
            assertEquals(2L, it.getLong(0))
            assertTrue(it.isNull(1))
            assertTrue(it.isNull(2))
        }
    }

    @Test
    fun unmarkedBatchDoesNotWriteAndNextBatchCanCommit() {
        statements.beginTransaction()
        statements.compileStatement("INSERT INTO compat_values (id) VALUES (1)").executeInsert()
        statements.endTransaction()
        assertEquals(0, rowCount())
        statements.beginTransaction()
        statements.compileStatement("INSERT INTO compat_values (id) VALUES (2)").executeInsert()
        statements.setTransactionSuccessful()
        statements.endTransaction()
        assertEquals(1, rowCount())
    }

    @Test
    fun failedBatchRollsBackEarlierWritesAndWriterCanBeReused() {
        statements.beginTransaction()
        statements.compileStatement("INSERT INTO compat_values (id) VALUES (1)").executeInsert()
        statements.compileStatement("INSERT INTO compat_values (missing_column) VALUES (2)").executeInsert()
        statements.setTransactionSuccessful()
        assertFailsWith<android.database.SQLException> { statements.endTransaction() }
        assertEquals(0, rowCount())
        statements.beginTransaction()
        statements.compileStatement("INSERT INTO compat_values (id) VALUES (3)").executeInsert()
        statements.setTransactionSuccessful()
        statements.endTransaction()
        assertEquals(1, rowCount())
    }

    @Test
    fun standaloneStatementWritesImmediately() {
        statements.compileStatement("INSERT INTO compat_values (id) VALUES (1)").executeInsert()
        assertEquals(1, rowCount())
    }

    @Test
    fun queryRoundTripsSqliteTypesAndBindArguments() {
        RoomDatabaseCompat.execute(database, "INSERT INTO compat_values VALUES (?, ?, ?, ?)", arrayOf(9L, "text", byteArrayOf(2, -1), 1.25))
        database.query("SELECT id, text_value, blob_value, real_value, NULL AS empty_value FROM compat_values WHERE id = ?", arrayOf(9)).use {
            assertTrue(it.moveToFirst())
            assertContentEquals(arrayOf("id", "text_value", "blob_value", "real_value", "empty_value"), it.columnNames)
            assertEquals(Cursor.FIELD_TYPE_INTEGER, it.getType(0))
            assertEquals(9L, it.getLong(0))
            assertEquals(9, it.getInt(0))
            assertEquals(9.toShort(), it.getShort(0))
            assertEquals(Cursor.FIELD_TYPE_STRING, it.getType(1))
            assertEquals("text", it.getString(1))
            assertEquals(Cursor.FIELD_TYPE_BLOB, it.getType(2))
            assertContentEquals(byteArrayOf(2, -1), it.getBlob(2))
            assertEquals(Cursor.FIELD_TYPE_FLOAT, it.getType(3))
            assertEquals(1.25, it.getDouble(3))
            assertEquals(1.25f, it.getFloat(3))
            assertEquals(Cursor.FIELD_TYPE_NULL, it.getType(4))
            assertTrue(it.isNull(4))
            assertNull(it.getString(4))
            assertNull(it.getBlob(4))
            assertEquals(0L, it.getLong(4))
        }
    }

    @Test
    fun emptyCursorKeepsMetadataAndCloses() {
        val cursor = database.query("SELECT id FROM compat_values")
        assertEquals(0, cursor.count)
        assertEquals("id", cursor.columnNames.single())
        assertFalse(cursor.moveToFirst())
        assertFalse(cursor.isClosed)
        cursor.close()
        assertTrue(cursor.isClosed)
        cursor.close()
    }

    @Test
    fun limitOffsetPreservesZeroOneAndMultipleSourceBindings() {
        RoomDatabaseCompat.execute(database, "INSERT INTO compat_values (id, text_value) VALUES (1, 'x'), (2, 'x'), (3, 'x')")
        val queries = listOf(
            RoomQuery.acquire("SELECT id FROM compat_values ORDER BY id", 0),
            RoomQuery.acquire("SELECT id FROM compat_values WHERE id > ? ORDER BY id", 1).apply { bindLong(1, 0) },
            RoomQuery.acquire("SELECT id FROM compat_values WHERE id > ? AND text_value = ? ORDER BY id", 2).apply {
                bindLong(1, 0)
                bindString(2, "x")
            },
        )
        queries.forEachIndexed { index, query ->
            val originalSql = query.sql
            assertEquals(listOf(1L), readIds(query.withLimitOffset(1, 0)))
            assertEquals(listOf(2L), readIds(query.withLimitOffset(1, 1)))
            assertTrue(readIds(query.withLimitOffset(1, 3)).isEmpty())
            assertEquals(listOf(1L, 2L, 3L), readIds(query))
            assertEquals(originalSql, query.sql)
            assertEquals(index, query.argCount)
        }
    }

    @Test
    fun cursorRejectsPositionsOutsideRowsAndSupportsMovingBack() {
        RoomDatabaseCompat.execute(database, "INSERT INTO compat_values (id) VALUES (1), (2)")
        database.query("SELECT id FROM compat_values ORDER BY id").use {
            assertFailsWith<CursorIndexOutOfBoundsException> { it.getLong(0) }
            assertTrue(it.moveToLast())
            assertEquals(2L, it.getLong(0))
            assertFalse(it.moveToNext())
            assertFailsWith<CursorIndexOutOfBoundsException> { it.getLong(0) }
            assertTrue(it.moveToFirst())
            assertEquals(1L, it.getLong(0))
        }
    }

    private fun readIds(query: RoomQuery): List<Long> = database.query(query).use {
        buildList {
            while (it.moveToNext()) add(it.getLong(0))
        }
    }

    private fun rowCount(): Int = database.query("SELECT COUNT(*) FROM compat_values").use {
        assertTrue(it.moveToFirst())
        it.getInt(0)
    }
}
