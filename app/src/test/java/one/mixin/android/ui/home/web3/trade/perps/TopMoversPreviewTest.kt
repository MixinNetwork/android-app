package one.mixin.android.ui.home.web3.trade.perps

import one.mixin.android.api.response.perps.PerpsMarket
import kotlin.test.assertEquals
import org.junit.Test

class TopMoversPreviewTest {
    @Test
    fun topMoversPreviewUsesTopFourAndBottomFourMarkets() {
        val markets = listOf(
            market("a", "0.01"),
            market("b", "0.08"),
            market("c", "-0.04"),
            market("d", "0.03"),
            market("e", "-0.09"),
            market("f", "0.05"),
            market("g", "-0.02"),
            market("h", "0.13"),
            market("i", "-0.12"),
            market("j", "0.07"),
        )

        val result = markets.topMoversPreview().map { it.marketId }

        assertEquals(
            listOf("h", "b", "j", "f", "i", "e", "c", "g"),
            result,
        )
    }

    private fun market(
        marketId: String,
        change: String,
    ) = PerpsMarket(
        marketId = marketId,
        displaySymbol = marketId,
        tokenSymbol = marketId,
        quoteSymbol = "USDT",
        markPrice = "1",
        priceScale = 2,
        leverage = 10,
        iconUrl = "",
        category = "",
        tags = emptyList(),
        fundingRate = "0",
        minAmount = "0",
        maxAmount = "0",
        last = "1",
        volume = "0",
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
