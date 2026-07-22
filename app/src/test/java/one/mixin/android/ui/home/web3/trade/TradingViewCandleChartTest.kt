package one.mixin.android.ui.home.web3.trade

import com.tradingview.lightweightcharts.api.series.models.Time
import com.tradingview.lightweightcharts.runtime.plugins.DateTimeFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import one.mixin.android.api.response.perps.CandleItem
import org.threeten.bp.ZoneOffset

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
    fun `converts utc timestamp to local chart timestamp`() {
        val utcTimestamp = 1_721_600_000L

        assertEquals(
            utcTimestamp + 8 * 60 * 60,
            tradingViewLocalTimestamp(utcTimestamp, ZoneOffset.ofHours(8)),
        )
    }

    @Test
    fun `converts millisecond timestamp to local chart timestamp`() {
        val utcTimestampMillis = 1_721_600_000_000L

        assertEquals(
            1_721_600_000L + 8 * 60 * 60,
            tradingViewLocalTimestamp(utcTimestampMillis, ZoneOffset.ofHours(8)),
        )
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
    fun `converts android touch position to chart coordinate`() {
        assertEquals(100f, tradingViewTouchCoordinate(touchX = 300f, density = 3f))
    }
}
