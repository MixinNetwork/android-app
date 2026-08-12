package one.mixin.android.ui.home.web3.trade.perps

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.jakewharton.threetenabp.AndroidThreeTen
import one.mixin.android.api.response.perps.PerpsOrderItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

class PerpsActivityDateGroupTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        AndroidThreeTen.init(context)
    }

    @Test
    fun formatsCurrentYearOrdersWithoutTheYear() {
        val currentYear = ZonedDateTime.now(ZoneId.systemDefault()).year

        assertFalse(order("$currentYear-06-04T12:00:00.000Z").createdAtDateLabel(context).contains(currentYear.toString()))
    }

    @Test
    fun formatsPreviousYearOrdersWithTheYear() {
        val previousYear = ZonedDateTime.now(ZoneId.systemDefault()).year - 1

        assertTrue(order("$previousYear-06-04T12:00:00.000Z").createdAtDateLabel(context).contains(previousYear.toString()))
    }

    private fun order(createdAt: String) = PerpsOrderItem(
        orderId = "order_id",
        positionId = "position_id",
        marketId = "market_id",
        side = "long",
        orderType = "open",
        status = "filled",
        leverage = 1,
        quantity = "1",
        payAmount = "1",
        entryPrice = "1",
        closePrice = "0",
        realizedPnl = "0",
        roe = "0",
        closeReason = null,
        triggerPrice = null,
        createdAt = createdAt,
        updatedAt = createdAt,
    )
}
