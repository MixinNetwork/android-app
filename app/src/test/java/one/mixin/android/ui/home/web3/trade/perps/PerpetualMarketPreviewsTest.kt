package one.mixin.android.ui.home.web3.trade.perps

import one.mixin.android.api.response.perps.PerpsMarket
import org.junit.Assert.assertEquals
import org.junit.Test

class PerpetualMarketPreviewsTest {
    @Test
    fun trendingPreviewOrdersByScore() {
        val markets =
            listOf(
                market("second-low-volume", volume = "10", score = 20),
                market("second-high-volume", volume = "100", score = 20),
                market("fourth", volume = "1000", score = 5),
                market("first", volume = "1", score = 30),
                market("third", volume = "10", score = 10),
            )

        assertEquals(
            listOf("first", "second-high-volume", "second-low-volume"),
            markets.trendingPreview().map { it.marketId },
        )
    }

    @Test
    fun categoryPreviewsOrderByScoreThenVolume() {
        val markets =
            listOf(
                market("second-low-volume", volume = "10", score = 30),
                market("first", volume = "2", score = 50),
                market("second-high-volume", volume = "100", score = 30),
            )

        assertEquals(
            listOf("first", "second-high-volume", "second-low-volume"),
            markets.sortedByScoreAndVolume().map { it.marketId },
        )
    }

    @Test
    fun topMoversTakeFourFromEachExtreme() {
        val markets = (-5..4).map { change -> market(change.toString(), change = change.toString()) }

        assertEquals(
            listOf("4", "3", "2", "1", "-5", "-4", "-3", "-2"),
            markets.topMoversPreview().map { it.marketId },
        )
    }

    private fun market(
        marketId: String,
        volume: String = "1",
        score: Int = 0,
        change: String = "0",
    ) =
        PerpsMarket(
            marketId = marketId,
            displaySymbol = marketId,
            tokenSymbol = marketId,
            quoteSymbol = "USD",
            markPrice = "1",
            leverage = 10,
            iconUrl = "",
            fundingRate = "0",
            minAmount = "0",
            maxAmount = "0",
            last = "1",
            volume = volume,
            tradeVolumeScore1D = score,
            high = "1",
            low = "1",
            open = "1",
            change = change,
            bidPrice = "1",
            askPrice = "1",
            createdAt = "",
            updatedAt = "",
        )
}
