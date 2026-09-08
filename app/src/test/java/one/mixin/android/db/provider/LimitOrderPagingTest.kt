package one.mixin.android.db.provider

import android.content.Context
import androidx.paging.PagingSource
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import one.mixin.android.db.WalletDatabase
import one.mixin.android.vo.route.Order
import one.mixin.android.vo.route.OrderItem
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class LimitOrderPagingTest {
    private lateinit var database: WalletDatabase

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
            .setDriver(AndroidSQLiteDriver())
            .allowMainThreadQueries()
            .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertingIntoEmptyOrdersInvalidatesLoadedPage() = runBlocking {
        val source = source()
        assertTrue(load(source).data.isEmpty())
        assertInvalidatedBy(source) { database.orderDao().insertSuspend(order()) }
        assertEquals(listOf("order"), load(source()).data.map { it.orderId })
    }

    @Test
    fun updatingOrderInvalidatesLoadedPage() = runBlocking {
        val order = order()
        database.orderDao().insertSuspend(order)
        val source = source()
        assertEquals("pending", load(source).data.single().state)
        assertInvalidatedBy(source) { database.orderDao().update(order.copy(state = "done")) }
        assertEquals("done", load(source()).data.single().state)
    }

    @Test
    fun deletingOrderInvalidatesLoadedPage() = runBlocking {
        database.orderDao().insertSuspend(order())
        val source = source()
        assertEquals(1, load(source).data.size)
        assertInvalidatedBy(source) { database.orderDao().deleteAllOrders() }
        assertTrue(load(source()).data.isEmpty())
    }

    @Test
    fun repeatedLoadsWithoutWritesKeepSourceValid() = runBlocking {
        database.orderDao().insertSuspend(order())
        val source = source()
        assertEquals(1, load(source).data.size)
        assertEquals(1, load(source).data.size)
        assertFalse(source.invalid)
        source.invalidate()
    }

    private fun source() = LimitOrderDataProviderGenerated.allOrders(database, "", "", "o.created_at DESC")

    private suspend fun load(source: PagingSource<Int, OrderItem>): PagingSource.LoadResult.Page<Int, OrderItem> =
        withTimeout(5_000) {
            assertIs<PagingSource.LoadResult.Page<Int, OrderItem>>(
                source.load(PagingSource.LoadParams.Refresh(null, 20, true)),
            )
        }

    private suspend fun assertInvalidatedBy(source: PagingSource<Int, OrderItem>, change: suspend () -> Unit) {
        assertFalse(source.invalid)
        val invalidated = CompletableDeferred<Unit>()
        source.registerInvalidatedCallback { invalidated.complete(Unit) }
        change()
        withTimeout(5_000) { invalidated.await() }
        assertTrue(source.invalid)
    }

    private fun order() = Order(
        orderId = "order",
        walletId = "wallet",
        userId = "user",
        payAssetId = "pay",
        receiveAssetId = "receive",
        payAmount = "1",
        receiveAmount = null,
        payTraceId = null,
        receiveTraceId = null,
        state = "pending",
        createdAt = "2026-09-08T00:00:00Z",
        orderType = "limit",
    )
}
