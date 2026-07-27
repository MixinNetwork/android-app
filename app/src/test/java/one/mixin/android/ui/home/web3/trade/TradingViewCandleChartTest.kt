package one.mixin.android.ui.home.web3.trade

import com.tradingview.lightweightcharts.api.series.models.Time
import com.tradingview.lightweightcharts.runtime.plugins.DateTimeFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import one.mixin.android.api.response.perps.CandleItem

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
        val timestamp = 1_721_628_800L
        val candle =
            CandleItem(
                timestamp = timestamp,
                open = "1",
                high = "2",
                low = "0.5",
                close = "1.5",
                volume = "10",
                amount = "15",
                count = 2,
            )

        assertEquals(
            candle,
            candleForTradingViewTime(Time.Utc(timestamp), mapOf(timestamp to candle)),
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
        val duplicate = candle(timestamp = 1, close = "9")
        val candles =
            tradingViewCandles(
                listOf(
                    candle(timestamp = 3),
                    candle(timestamp = 1),
                    duplicate,
                    candle(timestamp = 2),
                ),
            )

        assertEquals(listOf(1L, 2L, 3L), candles.map { (it.data.time as Time.Utc).timestamp })
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
                    candle(timestamp = 1, open = "NaN"),
                    candle(timestamp = 2, high = "Infinity"),
                    candle(timestamp = 3),
                ),
            )

        assertEquals(listOf(3L), candles.map { it.item.timestamp })
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
