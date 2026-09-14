package one.mixin.android.db

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.RoomRawQuery
import androidx.room3.withReadTransaction
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import one.mixin.android.db.datasource.RoomDatabaseCompat
import one.mixin.android.db.datasource.query
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class RoomDatabaseTransactionTest {
    private lateinit var database: MixinDatabase
    private lateinit var databaseName: String

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        databaseName = "transaction-${UUID.randomUUID()}.db"
        database =
            Room.databaseBuilder(context, MixinDatabase::class.java, databaseName)
                .setDriver(AndroidSQLiteDriver())
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .allowMainThreadQueries()
                .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(databaseName)
    }

    @Test
    fun cancellationRollsBackAndReleasesWriterConnection() = runBlocking {
        RoomDatabaseCompat.execute(database, "CREATE TABLE transaction_test (id INTEGER PRIMARY KEY)")
        val transactionStarted = CompletableDeferred<Unit>()
        val transaction =
            launch {
                database.withRoomTransaction {
                    RoomDatabaseCompat.execute(database, "INSERT INTO transaction_test VALUES (1)")
                    transactionStarted.complete(Unit)
                    awaitCancellation()
                }
            }

        withTimeout(5_000) { transactionStarted.await() }
        transaction.cancelAndJoin()
        assertEquals(0, transactionRowCount())

        withTimeout(5_000) {
            database.withRoomTransaction {
                RoomDatabaseCompat.execute(database, "INSERT INTO transaction_test VALUES (2)")
            }
        }
        assertEquals(1, transactionRowCount())
    }

    @Test
    fun readTransactionKeepsSnapshotWhileWriterCommits() = runBlocking {
        RoomDatabaseCompat.execute(database, "CREATE TABLE transaction_test (id INTEGER PRIMARY KEY)")
        RoomDatabaseCompat.execute(database, "INSERT INTO transaction_test VALUES (1)")
        RoomDatabaseCompat.query(database, "PRAGMA journal_mode").use {
            check(it.moveToFirst())
            assertEquals("wal", it.getString(0))
        }
        withTimeout(5_000) {
            val readStarted = CompletableDeferred<Unit>()
            val writeCommitted = CompletableDeferred<Unit>()
            val writer = launch {
                readStarted.await()
                database.withRoomTransaction {
                    RoomDatabaseCompat.execute(database, "INSERT INTO transaction_test VALUES (2)")
                }
                writeCommitted.complete(Unit)
            }
            database.withReadTransaction {
                val query = RoomRawQuery("SELECT COUNT(*) FROM transaction_test")
                query(query).use {
                    check(it.moveToFirst())
                    assertEquals(1, it.getInt(0))
                }
                readStarted.complete(Unit)
                writeCommitted.await()
                query(query).use {
                    check(it.moveToFirst())
                    assertEquals(1, it.getInt(0))
                }
            }
            writer.join()
        }
        assertEquals(2, transactionRowCount())
    }

    @Test
    fun earlyReturnReleasesWriterConnection() = runBlocking {
        database.withRoomTransaction {
            return@withRoomTransaction
        }

        withTimeout(5_000) {
            database.withRoomTransaction { Unit }
        }
    }

    @Test
    fun readTransactionReleasesReaderAfterQueryFailure() = runBlocking {
        RoomDatabaseCompat.execute(database, "CREATE TABLE transaction_test (id INTEGER PRIMARY KEY)")
        assertFailsWith<android.database.SQLException> {
            database.withReadTransaction {
                query(RoomRawQuery("SELECT missing_column FROM transaction_test"))
            }
        }
        assertEquals(0, transactionRowCount())
        withTimeout(5_000) {
            database.withRoomTransaction {
                RoomDatabaseCompat.execute(database, "INSERT INTO transaction_test VALUES (2)")
            }
            database.withReadTransaction {
                query(RoomRawQuery("SELECT id FROM transaction_test")).use {
                    check(it.moveToFirst())
                    assertEquals(2, it.getInt(0))
                }
            }
        }
    }

    @Test
    fun blockingTransactionReturnsResultAndRollsBackOnFailure() {
        RoomDatabaseCompat.execute(database, "CREATE TABLE transaction_test (id INTEGER PRIMARY KEY)")
        assertEquals("committed", database.runInTransaction {
            RoomDatabaseCompat.execute(database, "INSERT INTO transaction_test VALUES (1)")
            "committed"
        })
        assertFailsWith<IllegalStateException> {
            database.runInTransaction {
                RoomDatabaseCompat.execute(database, "INSERT INTO transaction_test VALUES (2)")
                assertEquals(2, transactionRowCount())
                error("abort transaction")
            }
        }
        assertEquals(1, transactionRowCount())
    }

    @Test
    fun blockingReadInsideWriteSeesUncommittedRows() = runBlocking {
        RoomDatabaseCompat.execute(database, "CREATE TABLE transaction_test (id INTEGER PRIMARY KEY)")
        withTimeout(5_000) {
            database.withRoomTransaction {
                RoomDatabaseCompat.execute(database, "INSERT INTO transaction_test VALUES (1)")
                assertEquals(1, transactionRowCount())
            }
        }
        assertEquals(1, transactionRowCount())
    }

    @Test
    fun nestedWritesRollBackWithOuterFailure() = runBlocking {
        RoomDatabaseCompat.execute(database, "CREATE TABLE transaction_test (id INTEGER PRIMARY KEY)")
        assertFailsWith<IllegalStateException> {
            database.withRoomTransaction {
                RoomDatabaseCompat.execute(database, "INSERT INTO transaction_test VALUES (1)")
                database.withRoomTransaction {
                    RoomDatabaseCompat.execute(database, "INSERT INTO transaction_test VALUES (2)")
                }
                assertEquals(2, transactionRowCount())
                error("abort transaction")
            }
        }
        assertEquals(0, transactionRowCount())
        withTimeout(5_000) {
            database.withRoomTransaction {
                RoomDatabaseCompat.execute(database, "INSERT INTO transaction_test VALUES (3)")
            }
        }
        assertEquals(1, transactionRowCount())
    }

    private fun transactionRowCount(): Int = RoomDatabaseCompat.query(database, "SELECT COUNT(*) FROM transaction_test").use {
        check(it.moveToFirst())
        it.getInt(0)
    }

}
