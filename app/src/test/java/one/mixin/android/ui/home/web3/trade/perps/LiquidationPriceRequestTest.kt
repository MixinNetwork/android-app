package one.mixin.android.ui.home.web3.trade.perps

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.math.BigDecimal

class LiquidationPriceRequestTest {
    @Test
    fun nonServerErrorStopsAfterOneRequest() = runBlocking {
        var requestCount = 0

        val price = requestLiquidationPrice(retryDelayMillis = 0L) {
            requestCount += 1
            LiquidationPriceResult.Failure
        }

        assertNull(price)
        assertEquals(1, requestCount)
    }

    @Test
    fun serverErrorRetriesUntilSuccess() = runBlocking {
        var requestCount = 0

        val price = requestLiquidationPrice(retryDelayMillis = 0L) {
            requestCount += 1
            if (requestCount == 1) {
                LiquidationPriceResult.Retry
            } else {
                LiquidationPriceResult.Success("123.45")
            }
        }

        assertEquals("123.45", price)
        assertEquals(2, requestCount)
    }

    @Test
    fun responseCodeControlsRetry() {
        assertSame(
            LiquidationPriceResult.Retry,
            liquidationPriceResult(price = null, errorCode = 500),
        )
        assertSame(
            LiquidationPriceResult.Failure,
            liquidationPriceResult(price = null, errorCode = 400),
        )
        assertSame(
            LiquidationPriceResult.Failure,
            liquidationPriceResult(price = null, errorCode = null),
        )
    }

    @Test
    fun amountBelowMinimumSkipsRequest() {
        assertEquals(
            false,
            shouldRequestLiquidationPrice(
                amount = BigDecimal("9.99"),
                minimumAmount = BigDecimal.TEN,
            ),
        )
        assertEquals(
            true,
            shouldRequestLiquidationPrice(
                amount = BigDecimal.TEN,
                minimumAmount = BigDecimal.TEN,
            ),
        )
    }
}
