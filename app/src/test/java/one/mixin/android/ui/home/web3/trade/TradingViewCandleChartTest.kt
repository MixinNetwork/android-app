package one.mixin.android.ui.home.web3.trade

import com.tradingview.lightweightcharts.runtime.plugins.DateTimeFormat
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
