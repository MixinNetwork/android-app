package one.mixin.android.ui.home.web3.trade.perps

import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import one.mixin.android.api.service.RouteService
import one.mixin.android.util.ErrorHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigDecimal

class LiquidationPriceRequestTest {
    @Test
    fun sendsActionAndOnlyTheParametersForEachScenario() = runBlocking {
        val requests = mutableListOf<Request>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            requests += chain.request()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("""{"data":{"liquidation_price":"123.45"}}""".toResponseBody())
                .build()
        }.build()
        val service = Retrofit.Builder()
            .baseUrl("https://example.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RouteService::class.java)

        listOf(
            "open" to false,
            "increase_position" to true,
            "increase_margin" to true,
            "decrease_margin" to true,
            null to false,
            null to true,
        ).forEach { (action, hasPosition) ->
            val response = service.getPerpsLiquidationPrice(
                marketId = "market".takeUnless { hasPosition },
                amount = "10.25",
                side = "long".takeUnless { hasPosition },
                leverage = 10.takeUnless { hasPosition },
                positionId = "position".takeIf { hasPosition },
                action = action,
            )
            val url = requests.last().url
            val expected = mutableMapOf("amount" to "10.25")
            action?.let { expected["action"] = it }
            if (hasPosition) {
                expected["position_id"] = "position"
            } else {
                expected.putAll(mapOf("market_id" to "market", "side" to "long", "leverage" to "10"))
            }
            assertEquals("GET", requests.last().method)
            assertEquals("/perps/markets/liquidation-price", url.encodedPath)
            assertEquals(expected, url.queryParameterNames.associateWith { url.queryParameter(it) })
            assertEquals("123.45", response.data?.liquidationPrice)
        }
    }

    @Test
    fun marginLimitStopsAndReturnsAvailableMarginIncludingZeroOrMissing() = runBlocking {
        listOf(BigDecimal("2.50"), BigDecimal.ZERO, null).forEach { available ->
            var requestCount = 0
            var callbackCount = 0
            val result = liquidationPriceResult(price = null, errorCode = 10653, availableMargin = available)
            assertEquals(LiquidationPriceResult.MarginExceeded(available), result)

            val price = requestLiquidationPrice(
                retryDelayMillis = 0L,
                onMarginExceeded = {
                    callbackCount += 1
                    assertEquals(available, it)
                },
            ) {
                requestCount += 1
                result
            }

            assertNull(price)
            assertEquals(1, requestCount)
            assertEquals(1, callbackCount)
        }
    }

    @Test
    fun nonServerErrorStopsAfterOneRequestAndReportsTheReason() = runBlocking {
        var requestCount = 0
        var errorMessage: String? = null

        val price = requestLiquidationPrice(retryDelayMillis = 0L, onFailure = { errorMessage = it }) {
            requestCount += 1
            liquidationPriceResult(price = null, errorCode = 400, errorMessage = "Position is closed")
        }

        assertNull(price)
        assertEquals(1, requestCount)
        assertEquals("Position is closed", errorMessage)
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
        assertEquals(
            LiquidationPriceResult.Failure(null),
            liquidationPriceResult(price = null, errorCode = 400),
        )
        assertEquals(
            LiquidationPriceResult.Failure(null),
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
