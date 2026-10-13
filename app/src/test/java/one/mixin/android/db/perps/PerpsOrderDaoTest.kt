package one.mixin.android.db.perps

import android.content.Context
import androidx.paging.PagingSource
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.response.perps.PerpsOrder
import one.mixin.android.api.response.perps.realizedPnlForDisplay
import one.mixin.android.api.response.perps.roeForDisplay
import one.mixin.android.db.PerpsDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal

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

    @Test
    fun syncOffsetIgnoresProcessingOrdersUntilTheyComplete() = runBlocking {
        val dao = database.perpsOrderDao()
        assertNull(dao.getLatestUpdatedAt())
        val pending = order("05", PerpsOrder.TYPE_INCREASE_MARGIN).copy(status = PerpsOrder.STATUS_PROCESSING)
        dao.insertAll(listOf(pending))
        assertNull(dao.getLatestUpdatedAt())

        listOf(PerpsOrder.STATUS_FILLED, PerpsOrder.STATUS_REJECTED, PerpsOrder.STATUS_CLOSED).forEachIndexed { index, status ->
            val completed = order("0${index + 1}", PerpsOrder.TYPE_INCREASE_MARGIN).copy(status = status)
            dao.insertAll(listOf(completed))
            assertEquals(completed.updatedAt, dao.getLatestUpdatedAt())
        }

        dao.insertAll(listOf(pending.copy(status = PerpsOrder.STATUS_FILLED)))
        assertEquals(pending.updatedAt, dao.getLatestUpdatedAt())
        assertEquals(pending.orderId, dao.getOrdersByMarket("market").first().orderId)
    }

    @Test
    fun netPnlAndProfitShareSurviveStorageAndMissingNetValuesUseLegacyPnl() = runBlocking {
        val gson = Gson()
        val json = gson.toJsonTree(order("01", PerpsOrder.TYPE_CLOSE).copy(realizedPnl = "10", roe = "0.1")).asJsonObject
        listOf("net_realized_pnl", "net_roe", "profit_share_amount").forEach { field ->
            assertEquals("", json.get(field).asString)
        }
        json.addProperty("net_realized_pnl", "8")
        json.addProperty("net_roe", "0.08")
        json.addProperty("profit_share_amount", "1.25")
        val response = gson.fromJson(json, PerpsOrder::class.java)
        val dao = database.perpsOrderDao()
        dao.insertAll(
            listOf(
                response,
                order("02", PerpsOrder.TYPE_CLOSE).copy(realizedPnl = "10", roe = "0.1"),
                response.copy(orderId = "03", netRealizedPnl = "", netRoe = ""),
                response.copy(orderId = "04", netRealizedPnl = "0", netRoe = "0"),
            ),
        )

        val stored = requireNotNull(dao.getOrder("01"))
        assertEquals("8", stored.netRealizedPnl)
        assertEquals("0.08", stored.netRoe)
        assertEquals("1.25", stored.profitShareAmount)
        assertEquals("10", stored.realizedPnl)
        assertEquals("0.1", stored.roe)
        assertEquals(BigDecimal("8"), stored.realizedPnlForDisplay())
        assertEquals(BigDecimal("0.08"), stored.roeForDisplay())
        val legacy = requireNotNull(dao.getOrder("02"))
        assertEquals("", legacy.netRealizedPnl)
        assertEquals("", legacy.netRoe)
        assertEquals("", legacy.profitShareAmount)
        listOf("", "invalid").forEach { missing ->
            val fallback = legacy.copy(netRealizedPnl = missing, netRoe = missing)
            assertEquals(BigDecimal("10"), fallback.realizedPnlForDisplay())
            assertEquals(BigDecimal("0.1"), fallback.roeForDisplay())
            assertEquals(BigDecimal("10"), stored.copy(netRealizedPnl = missing).realizedPnlForDisplay())
            assertEquals(BigDecimal("0.1"), stored.copy(netRoe = missing).roeForDisplay())
        }
        val zero = requireNotNull(dao.getOrder("04"))
        assertEquals(BigDecimal.ZERO, zero.realizedPnlForDisplay())
        assertEquals(BigDecimal.ZERO, zero.roeForDisplay())
        assertEquals(28.0, dao.getTotalRealizedPnl())
        assertEquals(28.0, dao.observeTotalRealizedPnl().first(), 0.0)
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
