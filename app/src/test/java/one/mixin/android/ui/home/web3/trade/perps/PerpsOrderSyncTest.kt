package one.mixin.android.ui.home.web3.trade.perps

import kotlinx.coroutines.runBlocking
import one.mixin.android.api.response.perps.PerpsOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class PerpsOrderSyncTest {
    @Test
    fun emptyCacheLoadsEveryPageAndStopsAtShortPage() = runBlocking {
        val first = listOf(order("01", "open"), order("02", "open"))
        val last = listOf(order("03", "open"))
        val offsets = mutableListOf<String?>()
        val saved = mutableListOf<PerpsOrder>()
        syncPerpsOrderPages(null, 2, fetch = { offset ->
            offsets += offset
            if (offset == null) first else last
        }, save = { saved += it })
        assertEquals(listOf(null, first.last().updatedAt), offsets)
        assertEquals(first + last, saved)
    }

    @Test(expected = IllegalStateException::class)
    fun repeatedCursorStopsInsteadOfLoopingForever() = runBlocking {
        val order = order("01", "open")
        syncPerpsOrderPages(order.updatedAt, 1, fetch = { listOf(order) }, save = {})
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
        marginAmount = "10",
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
