package one.mixin.android.db

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import one.mixin.android.db.datasource.RoomDatabaseCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomDatabaseTransactionTest {
    private lateinit var database: MixinDatabase

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
                .setDriver(AndroidSQLiteDriver())
                .allowMainThreadQueries()
                .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun cancellationReleasesWriterConnection() = runBlocking {
        val transactionStarted = CompletableDeferred<Unit>()
        val transaction =
            launch {
                database.withRoomTransaction {
                    transactionStarted.complete(Unit)
                    awaitCancellation()
                }
            }

        transactionStarted.await()
        transaction.cancelAndJoin()

        withTimeout(5_000) {
            database.withRoomTransaction { Unit }
        }
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
