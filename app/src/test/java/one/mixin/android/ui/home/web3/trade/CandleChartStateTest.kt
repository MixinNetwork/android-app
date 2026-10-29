package one.mixin.android.ui.home.web3.trade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CandleChartStateTest {
    @Test
    fun selectionFollowsTimestampWhenWindowSlides() {
        val state = CandleChartState()
        state.updateCandles(candles(0..99))
        state.scrollByPx(1f)
        state.select(state.xOf(50.5f))

        state.updateCandles(candles(1..100))

        assertEquals(49, state.selectedIndex)
        assertEquals(50L, state.candles[state.selectedIndex!!].timestamp)
    }

    @Test
    fun selectionClearsWhenSelectedCandleDisappears() {
        val state = CandleChartState()
        state.updateCandles(candles(0..29))
        state.select(state.xOf(0.5f))

        state.updateCandles(candles(1..30))

        assertNull(state.selectedIndex)
    }

    @Test
    fun selectionSurvivesPriceRefresh() {
        val state = CandleChartState()
        state.updateCandles(candles(0..29))
        state.select(state.xOf(29.5f))
        val updated = state.candles.map { it.copy(close = 2.0) }

        state.updateCandles(updated)

        assertEquals(29, state.selectedIndex)
        assertEquals(2.0, state.candles[state.selectedIndex!!].close, 0.0)
    }

    private fun candles(indices: IntRange) = indices.map {
        ChartCandle(it.toLong(), 1.0, 2.0, 0.5, 1.0)
    }
}
