package one.mixin.android.ui.home.web3.trade

import com.tradingview.lightweightcharts.api.series.models.Time
import com.tradingview.lightweightcharts.runtime.plugins.DateTimeFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import one.mixin.android.api.response.perps.CandleItem

private const val TIMESTAMP = 1_721_628_800L

class TradingViewCandleChartTest {
    @Test
    fun `normalizes millisecond timestamps to seconds`() {
        assertEquals(1_721_600_000L, normalizeTradingViewTimestamp(1_721_600_000_000L))
    }

    @Test
    fun `keeps second timestamps unchanged`() {
        assertEquals(1_721_600_000L, normalizeTradingViewTimestamp(1_721_600_000L))
    }

    @Test
    fun `uses time labels for intraday frames`() {
        listOf("1m", "5m", "15m", "1h", "4h").forEach { timeFrame ->
            assertEquals(DateTimeFormat.TIME, tradingViewDateTimeFormat(timeFrame))
        }
    }

    @Test
    fun `uses date labels for daily frames`() {
        listOf("1d", "1w").forEach { timeFrame ->
            assertEquals(DateTimeFormat.DATE, tradingViewDateTimeFormat(timeFrame))
        }
    }

    @Test
    fun `resolves candle for stationary long press`() {
        val candle = candle(timestamp = TIMESTAMP)

        assertEquals(
            candle,
            candleForTradingViewTime(Time.Utc(TIMESTAMP), mapOf(TIMESTAMP to candle)),
        )
    }

    @Test
    fun `uses fallback for chart errors without a message`() {
        assertEquals("Data error", tradingViewErrorMessage(Throwable(), "Data error"))
    }

    @Test
    fun `keeps chart error details when available`() {
        assertEquals("Chart failed", tradingViewErrorMessage(Throwable("Chart failed"), "Data error"))
    }

    @Test
    fun `sorts and deduplicates candle timestamps`() {
        val duplicate = candle(timestamp = TIMESTAMP, close = "9")
        val candles =
            tradingViewCandles(
                listOf(
                    candle(timestamp = TIMESTAMP + 120),
                    candle(timestamp = TIMESTAMP),
                    duplicate,
                    candle(timestamp = TIMESTAMP + 60),
                ),
            )

        assertEquals(
            listOf(TIMESTAMP, TIMESTAMP + 60, TIMESTAMP + 120),
            candles.map { (it.data.time as Time.Utc).timestamp },
        )
        assertEquals(duplicate, candles.first().item)
    }

    @Test
    fun `preserves distinct utc timestamps across daylight saving fallback`() {
        val candles =
            tradingViewCandles(
                listOf(
                    candle(timestamp = 1_730_610_000L),
                    candle(timestamp = 1_730_613_600L),
                ),
            )

        assertEquals(
            listOf(1_730_610_000L, 1_730_613_600L),
            candles.map { (it.data.time as Time.Utc).timestamp },
        )
    }

    @Test
    fun `drops non-finite candle prices`() {
        val candles =
            tradingViewCandles(
                listOf(
                    candle(timestamp = TIMESTAMP, open = "NaN"),
                    candle(timestamp = TIMESTAMP + 60, high = "Infinity"),
                    candle(timestamp = TIMESTAMP + 120),
                ),
            )

        assertEquals(listOf(TIMESTAMP + 120), candles.map { it.item.timestamp })
    }

    @Test
    fun `drops candles with non-positive prices`() {
        val candles =
            tradingViewCandles(
                listOf(
                    candle(timestamp = TIMESTAMP, open = "0"),
                    candle(timestamp = TIMESTAMP + 60, low = "-1"),
                    candle(timestamp = TIMESTAMP + 120, close = "-0.5"),
                    candle(timestamp = TIMESTAMP + 180),
                ),
            )

        assertEquals(listOf(TIMESTAMP + 180), candles.map { it.item.timestamp })
    }

    @Test
    fun `drops candles whose high is below low`() {
        val candles =
            tradingViewCandles(
                listOf(
                    candle(timestamp = TIMESTAMP, high = "0.5", low = "2"),
                    candle(timestamp = TIMESTAMP + 60),
                ),
            )

        assertEquals(listOf(TIMESTAMP + 60), candles.map { it.item.timestamp })
    }

    @Test
    fun `widens high to enclose open and close`() {
        val bar =
            tradingViewCandles(
                listOf(
                    candle(
                        timestamp = TIMESTAMP,
                        open = "1",
                        high = "1.2",
                        low = "0.9",
                        close = "1.5",
                    ),
                ),
            ).single().data

        assertEquals(1.5f, bar.high)
        assertEquals(0.9f, bar.low)
    }

    @Test
    fun `widens low to enclose open and close`() {
        val bar =
            tradingViewCandles(
                listOf(
                    candle(
                        timestamp = TIMESTAMP,
                        open = "0.4",
                        high = "2",
                        low = "0.9",
                        close = "0.5",
                    ),
                ),
            ).single().data

        assertEquals(2f, bar.high)
        assertEquals(0.4f, bar.low)
    }

    @Test
    fun `drops candles with implausible timestamps`() {
        val candles =
            tradingViewCandles(
                listOf(
                    candle(timestamp = 0),
                    candle(timestamp = -60),
                    candle(timestamp = Long.MAX_VALUE),
                    candle(timestamp = TIMESTAMP),
                ),
            )

        assertEquals(listOf(TIMESTAMP), candles.map { it.item.timestamp })
    }

    @Test
    fun `normalizes millisecond candle timestamps`() {
        val candles = tradingViewCandles(listOf(candle(timestamp = TIMESTAMP * 1000)))

        assertEquals(TIMESTAMP, (candles.single().data.time as Time.Utc).timestamp)
    }

    @Test
    fun `rejects prices the chart cannot render`() {
        assertNull(tradingViewPrice(null))
        assertNull(tradingViewPrice(""))
        assertNull(tradingViewPrice("abc"))
        assertNull(tradingViewPrice("NaN"))
        assertNull(tradingViewPrice("Infinity"))
        assertNull(tradingViewPrice("1e400"))
        assertNull(tradingViewPrice("0"))
        assertNull(tradingViewPrice("-1"))
        assertEquals(1.5f, tradingViewPrice("1.5"))
    }

    @Test
    fun `plots the closes when the chart is drawn as a line`() {
        val points =
            tradingViewLineData(
                tradingViewCandles(
                    listOf(
                        candle(timestamp = TIMESTAMP, close = "1.5"),
                        candle(timestamp = TIMESTAMP + 60, close = "2.5"),
                    ),
                ).map { it.data },
            )

        assertEquals(
            listOf(1.5f, 2.5f),
            points.map { it.value },
        )
        assertEquals(
            listOf(TIMESTAMP, TIMESTAMP + 60),
            points.map { (it.time as Time.Utc).timestamp },
        )
    }

    @Test
    fun `clamps the price scale to a renderable range`() {
        assertEquals(0, tradingViewPriceScale(-1))
        assertEquals(2, tradingViewPriceScale(2))
        assertEquals(18, tradingViewPriceScale(1000))
    }

    @Test
    fun `rejects timestamps outside the supported range`() {
        assertNull(tradingViewTimestamp(0))
        assertNull(tradingViewTimestamp(-1))
        assertNull(tradingViewTimestamp(Long.MAX_VALUE))
        assertEquals(TIMESTAMP, tradingViewTimestamp(TIMESTAMP))
    }

    private fun candle(
        timestamp: Long,
        open: String = "1",
        high: String = "2",
        low: String = "0.5",
        close: String = "1.5",
    ) = CandleItem(
        timestamp = timestamp,
        open = open,
        high = high,
        low = low,
        close = close,
        volume = "10",
        amount = "15",
        count = 2,
    )
}
