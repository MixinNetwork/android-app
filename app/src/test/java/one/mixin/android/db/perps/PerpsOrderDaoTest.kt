package one.mixin.android.db.perps

import android.content.Context
import androidx.paging.PagingSource
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.response.perps.PerpsOrder
import one.mixin.android.db.PerpsDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PerpsOrderDaoTest {
    private lateinit var database: PerpsDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), PerpsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun allHistoryQueriesIncludeMarginOrdersAndExcludeProcessingOrders() = runBlocking {
        val dao = database.perpsOrderDao()
        dao.insertAll(
            listOf(
                order("01", PerpsOrder.TYPE_OPEN),
                order("02", PerpsOrder.TYPE_INCREASE),
                order("03", PerpsOrder.TYPE_INCREASE_MARGIN),
                order("04", PerpsOrder.TYPE_DECREASE_MARGIN),
                order("05", PerpsOrder.TYPE_CLOSE),
                order("06", PerpsOrder.TYPE_INCREASE_MARGIN).copy(status = PerpsOrder.STATUS_PROCESSING),
                order("07", PerpsOrder.TYPE_DECREASE_MARGIN).copy(status = PerpsOrder.STATUS_REJECTED),
                order("08", "unknown"),
                order("09", PerpsOrder.TYPE_DECREASE_MARGIN).copy(marketId = "other-market"),
            ),
        )
        val expected = listOf("09", "07", "05", "04", "03", "02", "01")
        assertEquals(expected, dao.getOrders(20).map { it.orderId })
        assertEquals(expected, dao.observeOrders(20).first().map { it.orderId })
        assertEquals(expected.drop(1), dao.getOrdersByMarket("market").map { it.orderId })
        assertEquals(listOf("03", "02"), dao.getOrders(2, "2026-09-14T00:00:04Z").map { it.orderId })
        val page = dao.getOrdersPaged().load(PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false))
        assertEquals(expected, (page as PagingSource.LoadResult.Page).data.map { it.orderId })
    }

    private fun order(id: String, type: String) = PerpsOrder(
        orderId = id,
        positionId = "position",
        marketId = "market",
        side = "long",
        orderType = type,
        status = PerpsOrder.STATUS_FILLED,
        leverage = 10,
        quantity = "1",
        payAmount = "10",
        entryPrice = "100",
        closePrice = "0",
        realizedPnl = "0",
        roe = "0",
        closeReason = null,
        triggerPrice = null,
        createdAt = "2026-09-14T00:00:${id}Z",
        updatedAt = "2026-09-14T00:00:${id}Z",
    )
}
