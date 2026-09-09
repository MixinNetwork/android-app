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
import one.mixin.android.ui.wallet.OrderFilterParams
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

    @Test
    fun filteredPagesKeepOrderAndKeysAcrossRefreshAppendAndPrepend() = runBlocking<Unit> {
        for (index in 0..8) {
            database.orderDao().insertSuspend(
                order().copy(
                    orderId = "order-$index",
                    state = if (index == 4) "done" else "pending",
                    createdAt = "2026-09-08T00:00:0${index}Z",
                ),
            )
        }
        val source = LimitOrderDataProvider.allOrders(database, OrderFilterParams(statuses = listOf("pending")))
        val first = load(source, PagingSource.LoadParams.Refresh(null, 3, true))
        assertEquals(listOf("order-8", "order-7", "order-6"), first.data.map { it.orderId })
        assertEquals(null, first.prevKey)
        assertEquals(3, first.nextKey)
        assertEquals(0, first.itemsBefore)
        assertEquals(5, first.itemsAfter)

        val second = load(source, PagingSource.LoadParams.Append(checkNotNull(first.nextKey), 2, true))
        assertEquals(listOf("order-5", "order-3"), second.data.map { it.orderId })
        assertEquals(3, second.prevKey)
        assertEquals(5, second.nextKey)
        assertEquals(3, second.itemsBefore)
        assertEquals(3, second.itemsAfter)

        val last = load(source, PagingSource.LoadParams.Append(checkNotNull(second.nextKey), 3, true))
        assertEquals(listOf("order-2", "order-1", "order-0"), last.data.map { it.orderId })
        assertEquals(null, last.nextKey)
        assertEquals(0, last.itemsAfter)

        val previous = load(source, PagingSource.LoadParams.Prepend(checkNotNull(second.prevKey), 5, true))
        assertEquals(first.data.map { it.orderId }, previous.data.map { it.orderId })
        assertEquals(null, previous.prevKey)
        assertEquals(3, previous.nextKey)

        assertInvalidatedBy(source) { database.orderDao().deleteAllOrders() }
        withTimeout(5_000) {
            assertIs<PagingSource.LoadResult.Invalid<Int, OrderItem>>(
                source.load(PagingSource.LoadParams.Append(checkNotNull(second.nextKey), 3, true)),
            )
        }
    }

    private fun source() = LimitOrderDataProvider.allOrders(database, OrderFilterParams())

    private suspend fun load(
        source: PagingSource<Int, OrderItem>,
        params: PagingSource.LoadParams<Int> = PagingSource.LoadParams.Refresh(null, 20, true),
    ): PagingSource.LoadResult.Page<Int, OrderItem> =
        withTimeout(5_000) {
            assertIs<PagingSource.LoadResult.Page<Int, OrderItem>>(
                source.load(params),
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
