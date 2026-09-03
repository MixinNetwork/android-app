package one.mixin.android.ui.home.web3.trade.perps

import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import one.mixin.android.util.ErrorHandler
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
        assertEquals(
            LiquidationPriceResult.LimitExceeded(
                LiquidationPriceLimit(maxAmount = "100", maxLeverage = 5),
            ),
            liquidationPriceResult(
                price = null,
                errorCode = ErrorHandler.PERPS_POSITION_SIZE_EXCEEDS_LEVERAGE_LIMIT,
                limit = LiquidationPriceLimit(maxAmount = "100", maxLeverage = 5),
            ),
        )
    }

    @Test
    fun leverageLimitStopsAndReturnsRetryValues() = runBlocking {
        var requestCount = 0
        var limit: LiquidationPriceLimit? = null

        val price = requestLiquidationPrice(
            retryDelayMillis = 0L,
            onLimitExceeded = { limit = it },
        ) {
            requestCount += 1
            LiquidationPriceResult.LimitExceeded(
                LiquidationPriceLimit(maxAmount = "25.5", maxLeverage = 3),
            )
        }

        assertNull(price)
        assertEquals(1, requestCount)
        assertEquals(LiquidationPriceLimit(maxAmount = "25.5", maxLeverage = 3), limit)
    }

    @Test
    fun parsesLeverageLimitExtra() {
        val extra = JsonParser.parseString(
            """{"max_amount":"42.50","max_leverage":7}""",
        )

        assertEquals(
            LiquidationPriceLimit(maxAmount = "42.50", maxLeverage = 7),
            parseLiquidationPriceLimit(extra),
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
